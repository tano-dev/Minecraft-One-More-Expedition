"""
Tier 3: Cross-Feature Combinations Test Suite for expedition-skyblock.
Validates multi-feature interactions:
1. Combat damage cooldown + /is from Overworld (cooldown resets on subsequent hits)
2. Multiple /is executions within Skyblock + /is sethome + /return
3. Island lock + /visit rejection + /invite + /visit success + anti-grief + /is kick + invite revocation
4. Void fall during combat in Skyblock (safe rescue without death)
5. Dying in Skyblock with keepInventory then dying in Overworld with standard gamerule
"""
from .contract.test_framework import test, TestContext
from .contract.island_math import IslandMath
from .contract.skyblock_engine import SkyblockEngine, MockPlayer


@test(tier="Tier 3", description="C1: Combat Damage Cooldown and Overworld Teleport Interaction")
def test_cross_01_combat_cooldown_and_overworld_teleport(ctx: TestContext):
    """
    Scenario:
    - Player takes hit at t=1000s in Overworld.
    - Immediate /is at t=1002s rejected.
    - Player takes second hit at t=1003s (resets cooldown timer!).
    - /is at t=1006s (3s after 2nd hit, 6s after 1st hit) must STILL be rejected.
    - Player retreats, avoids damage until t=1008.1s (> 5s after 2nd hit).
    - /is now succeeds, saving Overworld coordinates.
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-c1", name="Fighter", dimension="minecraft:overworld", x=300.0, y=65.0, z=-150.0)

    # 1. First damage at t = 1000.0s
    player.take_damage(4.0, current_time=1000.0)
    engine.current_time_seconds = 1002.0

    s1, msg1 = engine.cmd_is(player)
    ctx.assert_false(s1, "Immediate /is must be rejected")
    ctx.assert_equal(player.dimension, "minecraft:overworld", "Player remains in Overworld")

    # 2. Second damage at t = 1003.0s (resets timer)
    player.take_damage(4.0, current_time=1003.0)
    engine.current_time_seconds = 1006.0  # 3.0s after second hit

    s2, msg2 = engine.cmd_is(player)
    ctx.assert_false(s2, "Cooldown resets on second damage hit; /is must be rejected at t=1006")
    ctx.assert_in("remaining", msg2.lower(), "Message shows remaining cooldown")

    # 3. Player avoids damage until t = 1008.1s (5.1s after second hit)
    engine.current_time_seconds = 1008.1
    s3, _ = engine.cmd_is(player)
    ctx.assert_true(s3, "Escaping combat for >5s allows /is to succeed")
    ctx.assert_equal(player.dimension, "expedition:skyblock", "Player safely reached Skyblock")

    saved_ret = engine.player_returns.get(player.uuid)
    ctx.assert_equal((saved_ret.x, saved_ret.y, saved_ret.z), (300.0, 65.0, -150.0), "Original escape coords saved")


@test(tier="Tier 3", description="C2: Multiple /is Executions in Skyblock + /return Preservation")
def test_cross_02_multiple_is_in_skyblock_then_return(ctx: TestContext):
    """
    Scenario:
    - Player teleports from Overworld at (123.4, 65.0, 789.1) to Skyblock via /is.
    - Player wanders to island edge (cx+100, 70, cz-80).
    - Player runs /is again: teleports back to island home.
    - Player runs /is sethome at (cx+50, 70, cz+50).
    - Player runs /is again: teleports to new custom home.
    - Player runs /return: must return to (123.4, 65.0, 789.1) in Overworld!
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-c2", name="Wanderer", dimension="minecraft:overworld", x=123.4, y=65.0, z=789.1)

    # 1. Teleport from Overworld
    engine.cmd_is(player)
    island = engine.get_island_by_owner(player.uuid)
    cx, cz = island.center_x, island.center_z

    # 2. Wander to edge of island
    player.x = cx + 100.0
    player.y = 70.0
    player.z = cz - 80.0

    # 3. /is again inside Skyblock
    engine.cmd_is(player)
    ctx.assert_equal((player.x, player.y, player.z), (island.home_x, island.home_y, island.home_z), "Teleported to home")

    # 4. Set custom home
    player.x = cx + 50.0
    player.y = 70.0
    player.z = cz + 50.0
    engine.cmd_sethome(player)

    # 5. Move away and /is again
    player.x = cx - 20.0
    player.z = cz - 20.0
    engine.cmd_is(player)
    ctx.assert_equal((player.x, player.y, player.z), (cx + 50.0, 70.0, cz + 50.0), "Teleported to updated custom home")

    # 6. /return to main world
    ret_ok, _ = engine.cmd_return(player)
    ctx.assert_true(ret_ok, "/return succeeds")
    ctx.assert_equal(player.dimension, "minecraft:overworld", "Player is back in Overworld")
    ctx.assert_almost_equal(player.x, 123.4, message="Original Overworld X preserved")
    ctx.assert_almost_equal(player.y, 65.0, message="Original Overworld Y preserved")
    ctx.assert_almost_equal(player.z, 789.1, message="Original Overworld Z preserved")


@test(tier="Tier 3", description="C3: Island Permissions Lifecycle (Lock, Visit, Invite, Grief, Kick)")
def test_cross_03_lock_visit_invite_kick_lifecycle(ctx: TestContext):
    """
    Scenario:
    - Host island is locked by default.
    - Guest attempts /visit -> rejected.
    - Host attempts /is kick Guest -> rejected (Guest not present).
    - Host runs /invite Guest.
    - Guest runs /visit -> succeeds.
    - Guest attempts to break blocks or open chests -> denied.
    - Guest attempts to hit Host -> canceled (PvP disabled).
    - Host runs /is kick Guest -> Guest teleported away, invite revoked.
    - Guest attempts /visit again -> rejected.
    """
    engine = SkyblockEngine()
    host = MockPlayer(uuid="host-uuid", name="AliceHost", dimension="minecraft:overworld")
    guest = MockPlayer(uuid="guest-uuid", name="BobGuest", dimension="minecraft:overworld")

    engine.cmd_is(host)
    host_island = engine.get_island_by_owner(host.uuid)

    # 1. Guest visits locked island -> rejected
    v1, _ = engine.cmd_visit(guest, "AliceHost")
    ctx.assert_false(v1, "Visiting locked island without invite fails")

    # 2. Host tries kicking guest who isn't there -> rejected
    k1, _ = engine.cmd_kick(host, guest)
    ctx.assert_false(k1, "Cannot kick player who is not on island")

    # 3. Host invites guest, guest visits -> succeeds
    engine.cmd_invite(host, guest)
    v2, _ = engine.cmd_visit(guest, "AliceHost")
    ctx.assert_true(v2, "Visiting with invite succeeds")
    ctx.assert_equal(guest.dimension, "expedition:skyblock", "Guest in Skyblock")

    # 4. Anti-Grief on host island
    can_break = engine.can_player_interact_block(guest, host_island.center_x, 64, host_island.center_z, "break")
    can_chest = engine.can_player_interact_block(guest, host_island.center_x + 1, 65, host_island.center_z, "interact")
    ctx.assert_false(can_break, "Guest cannot break blocks on host island")
    ctx.assert_false(can_chest, "Guest cannot open chest on host island")

    # 5. PvP protection
    can_pvp = engine.can_player_damage_entity(guest, target_is_player=True, ex=host.x, ez=host.z)
    ctx.assert_false(can_pvp, "PvP is canceled in Skyblock")

    # 6. Host kicks guest
    k2, _ = engine.cmd_kick(host, guest)
    ctx.assert_true(k2, "Kick succeeds")
    ctx.assert_not_in(guest.uuid, host_island.invited_players, "Invite was revoked upon kick")

    # 7. Guest attempts re-visit -> rejected
    v3, _ = engine.cmd_visit(guest, "AliceHost")
    ctx.assert_false(v3, "Guest cannot re-visit locked island after being kicked")


@test(tier="Tier 3", description="C4: Void Fall During Combat in Skyblock")
def test_cross_04_void_fall_during_combat_in_skyblock(ctx: TestContext):
    """
    Scenario:
    - Player in Skyblock is fighting mobs, takes damage (health 20 -> 14).
    - Player falls off island into the void (accelerating downward, fall_distance > 100).
    - Player reaches y = -65.0.
    - Void rescue triggers: player teleported to island home.
    - Velocity and fall distance reset to 0.
    - Player health preserved at 14 (no lethal void damage, no death).
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-c4", name="ClumsyFighter", dimension="minecraft:overworld")
    engine.cmd_is(player)
    island = engine.get_island_by_owner(player.uuid)

    # 1. Takes mob damage in Skyblock
    player.take_damage(6.0, current_time=engine.current_time_seconds)
    ctx.assert_equal(player.health, 14.0, "Player took 6 damage, health is 14")

    # 2. Falls into void
    player.y = -65.0
    player.vy = -4.5
    player.fall_distance = 150.0

    # 3. Void rescue tick
    rescued = engine.tick_void_fall(player)
    ctx.assert_true(rescued, "Void fall auto-rescue must trigger")
    ctx.assert_equal((player.x, player.y, player.z), (island.home_x, island.home_y, island.home_z), "Teleported to home")
    ctx.assert_equal(player.fall_distance, 0.0, "Fall distance cleared to 0")
    ctx.assert_equal(player.vy, 0.0, "Vertical velocity cleared to 0")
    ctx.assert_equal(player.health, 14.0, "Player health preserved without void damage")
    ctx.assert_true(player.is_alive, "Player is alive")


@test(tier="Tier 3", description="C5: keepInventory Skyblock Death vs Overworld Death Lifecycle")
def test_cross_05_keep_inventory_skyblock_vs_overworld_lifecycle(ctx: TestContext):
    """
    Scenario:
    - Global gamerule keepInventory is False.
    - Player with full inventory and 30 levels dies in Skyblock -> keeps everything, drops nothing.
    - Player respawns in Skyblock with full gear.
    - Player returns to Overworld via /return.
    - Player dies in Overworld -> drops all gear and loses all XP.
    """
    engine = SkyblockEngine()
    engine.global_keep_inventory = False

    player = MockPlayer(uuid="p-c5", name="DyingWarrior", dimension="minecraft:overworld")
    engine.cmd_is(player)

    initial_items_count = len(player.inventory)
    initial_xp = player.total_xp

    # 1. Die in Skyblock
    sb_death = engine.handle_player_death(player)
    ctx.assert_true(sb_death["keep_inventory_applied"], "Skyblock keepInventory active")
    ctx.assert_equal(len(sb_death["dropped_items"]), 0, "No items dropped in Skyblock death")

    # Respawn in Skyblock
    engine.handle_player_respawn(player, death_dimension=SkyblockEngine.SKYBLOCK_DIMENSION)
    ctx.assert_equal(len(player.inventory), initial_items_count, "Inventory intact after Skyblock respawn")
    ctx.assert_equal(player.total_xp, initial_xp, "XP intact after Skyblock respawn")

    # 2. Return to Overworld
    engine.cmd_return(player)
    ctx.assert_equal(player.dimension, "minecraft:overworld", "Player in Overworld")

    # 3. Die in Overworld
    ow_death = engine.handle_player_death(player)
    ctx.assert_false(ow_death["keep_inventory_applied"], "Overworld keepInventory inactive")
    ctx.assert_equal(len(ow_death["dropped_items"]), initial_items_count, "All items dropped in Overworld")
    ctx.assert_equal(ow_death["dropped_xp"], initial_xp, "All XP dropped in Overworld")
    ctx.assert_equal(len(player.inventory), 0, "Player inventory now empty")
    ctx.assert_equal(player.xp_level, 0, "Player XP level reset to 0")
