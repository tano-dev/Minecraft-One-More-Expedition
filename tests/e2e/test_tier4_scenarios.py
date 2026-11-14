"""
Tier 4: Real-World Application Scenarios for expedition-skyblock.
Simulates comprehensive, end-to-end multi-player scenarios:
1. Scenario 1: Complete multi-player Skyblock lifecycle (build, invite, anti-grief, kick, void rescue, return).
2. Scenario 2: Two-player mutual island isolation and 768m void buffer defense.
3. Scenario 3: Combat evasion prevention and dimensional return cycle from Nether fortress.
4. Scenario 4: Portal suppression and dimension isolation defense.
5. Scenario 5: Persistence integrity, atomic save, and server crash recovery.
"""
import os
import tempfile
from .contract.test_framework import test, TestContext
from .contract.island_math import IslandMath
from .contract.skyblock_engine import SkyblockEngine, MockPlayer


@test(tier="Tier 4", description="S1: Full Multi-Player Skyblock Lifecycle Scenario")
def test_scenario_01_complete_multiplayer_island_lifecycle(ctx: TestContext):
    """
    Simulates:
    1. Player A and Player B start in Overworld.
    2. Player A creates island, expands platform, sets custom home.
    3. Player A invites Player B; Player B visits.
    4. Player B attempts griefing (break block, steal from chest, hit Player A) -> all blocked.
    5. Player A kicks Player B -> Player B removed.
    6. Player A falls into void -> auto-rescued back to custom home.
    7. Player A returns to Overworld -> exact original spot restored.
    """
    engine = SkyblockEngine()
    player_a = MockPlayer(uuid="uuid-a", name="PlayerA", dimension="minecraft:overworld", x=100.5, y=64.0, z=-200.5)
    player_b = MockPlayer(uuid="uuid-b", name="PlayerB", dimension="minecraft:overworld", x=50.0, y=70.0, z=50.0)

    # Step 1: Player A teleports to Skyblock
    ok_a, _ = engine.cmd_is(player_a)
    ctx.assert_true(ok_a, "Player A enters Skyblock")
    island_a = engine.get_island_by_owner(player_a.uuid)
    cx, cz = island_a.center_x, island_a.center_z

    # Step 2: Player A customizes home
    player_a.x = cx + 25.0
    player_a.y = 66.0
    player_a.z = cz + 25.0
    ok_home, _ = engine.cmd_sethome(player_a)
    ctx.assert_true(ok_home, "Player A sets custom home")

    # Step 3: Player A invites Player B; Player B visits
    engine.cmd_invite(player_a, player_b)
    ok_visit, _ = engine.cmd_visit(player_b, "PlayerA")
    ctx.assert_true(ok_visit, "Player B visits Player A's island")
    ctx.assert_equal((player_b.x, player_b.y, player_b.z), (cx + 25.0, 66.0, cz + 25.0), "Player B at custom home")

    # Step 4: Player B attempts griefing
    can_break = engine.can_player_interact_block(player_b, cx, 64, cz, "break")
    can_chest = engine.can_player_interact_block(player_b, cx + 1, 65, cz, "interact")
    can_pvp = engine.can_player_damage_entity(player_b, target_is_player=True, ex=player_a.x, ez=player_a.z)
    ctx.assert_false(can_break, "Griefing block break blocked")
    ctx.assert_false(can_chest, "Griefing chest access blocked")
    ctx.assert_false(can_pvp, "Griefing PvP attack blocked")

    # Step 5: Player A kicks Player B
    ok_kick, _ = engine.cmd_kick(player_a, player_b)
    ctx.assert_true(ok_kick, "Player A kicks Player B")
    ctx.assert_not_equal((player_b.x, player_b.z), (player_a.x, player_a.z), "Player B removed from island")

    # Step 6: Player A falls into void below -64
    player_a.y = -70.0
    player_a.fall_distance = 150.0
    engine.tick_void_fall(player_a)
    ctx.assert_equal((player_a.x, player_a.y, player_a.z), (cx + 25.0, 66.0, cz + 25.0), "Auto-rescued to custom home")
    ctx.assert_equal(player_a.fall_distance, 0.0, "Fall distance cleared")

    # Step 7: Player A returns to Overworld
    ok_ret, _ = engine.cmd_return(player_a)
    ctx.assert_true(ok_ret, "Player A returns to main world")
    ctx.assert_equal(player_a.dimension, "minecraft:overworld", "Back in Overworld")
    ctx.assert_almost_equal(player_a.x, 100.5, message="Exact Overworld X restored")
    ctx.assert_almost_equal(player_a.y, 64.0, message="Exact Overworld Y restored")
    ctx.assert_almost_equal(player_a.z, -200.5, message="Exact Overworld Z restored")


@test(tier="Tier 4", description="S2: Mutual Island Isolation and 768m Void Buffer Defense")
def test_scenario_02_two_players_mutual_isolation_and_buffer_defense(ctx: TestContext):
    """
    Simulates:
    1. Player 1 allocated Island 0 (center 0, 0).
    2. Player 2 allocated Island 1 (center 1024, 0).
    3. Center-to-center distance is 1,024 blocks.
    4. Distance between buildable borders is 768 blocks (1024 - 128 - 128).
    5. Neither player can build on each other's island.
    6. Neither player can place blocks in the void buffer (x=200, 500, 800).
    7. Flight into void buffer past dx=135 triggers emergency rescue.
    """
    engine = SkyblockEngine()
    p1 = MockPlayer(uuid="p1-iso", name="Islander1", dimension="minecraft:overworld")
    p2 = MockPlayer(uuid="p2-iso", name="Islander2", dimension="minecraft:overworld")

    engine.cmd_is(p1)
    engine.cmd_is(p2)

    i1 = engine.get_island_by_owner(p1.uuid)
    i2 = engine.get_island_by_owner(p2.uuid)

    ctx.assert_equal((i1.center_x, i1.center_z), (0, 0), "Island 1 center is (0, 0)")
    ctx.assert_equal((i2.center_x, i2.center_z), (1024, 0), "Island 2 center is (1024, 0)")

    # Border distance: (1024 - 128) - 128 = 768
    border_gap = (i2.center_x - IslandMath.ISLAND_RADIUS) - (i1.center_x + IslandMath.ISLAND_RADIUS)
    ctx.assert_equal(border_gap, 768, "Buffer gap between island boundaries is exactly 768 blocks")

    # Mutual permission denial
    ctx.assert_false(engine.can_player_interact_block(p1, i2.center_x, 64, i2.center_z, "break"), "P1 cannot edit P2 island")
    ctx.assert_false(engine.can_player_interact_block(p2, i1.center_x, 64, i1.center_z, "break"), "P2 cannot edit P1 island")

    # Void buffer build denial
    for buf_x in (150, 300, 512, 700, 850):
        ctx.assert_false(engine.can_player_interact_block(p1, buf_x, 64, 0, "place"), f"P1 cannot place at x={buf_x}")
        ctx.assert_false(engine.can_player_interact_block(p2, buf_x, 64, 0, "place"), f"P2 cannot place at x={buf_x}")

    # Elytra flight penetration into buffer: P1 flies to x = 145.0
    p1.x = 145.0
    p1.z = 0.0
    res = engine.tick_player_boundary(p1)
    ctx.assert_true(res["rescued"], "Emergency rescue triggered for deep buffer flight")
    ctx.assert_equal((p1.x, p1.y, p1.z), (i1.home_x, i1.home_y, i1.home_z), "P1 returned to Island 1 home")


@test(tier="Tier 4", description="S3: Combat Evasion Prevention and Nether Return Cycle")
def test_scenario_03_combat_evasion_and_safe_return_cycle(ctx: TestContext):
    """
    Simulates:
    1. Player in the Nether fighting at (-500, 70, 800).
    2. Takes damage from mob -> attempts immediate /is escape -> rejected.
    3. Dodges attacks and retreats for 5.1s.
    4. /is succeeds -> Nether coordinates saved.
    5. Returns via /return -> returns to Nether at exact coordinates (-500, 70, 800).
    """
    engine = SkyblockEngine()
    player = MockPlayer(
        uuid="p-nether", name="NetherExplorer",
        dimension="minecraft:the_nether",
        x=-500.25, y=70.0, z=800.75, yaw=180.0, pitch=5.0
    )

    # 1. Take damage at t = 1000s
    player.take_damage(6.0, current_time=1000.0)
    engine.current_time_seconds = 1003.0

    # 2. Immediate /is escape blocked
    s1, _ = engine.cmd_is(player)
    ctx.assert_false(s1, "Evasion /is blocked during combat")
    ctx.assert_equal(player.dimension, "minecraft:the_nether", "Remains in Nether")

    # 3. Retreat until t = 1005.2s (> 5s cooldown)
    engine.current_time_seconds = 1005.2
    s2, _ = engine.cmd_is(player)
    ctx.assert_true(s2, "/is succeeds after cooldown expires")
    ctx.assert_equal(player.dimension, "expedition:skyblock", "Teleported to Skyblock")

    # 4. Return to Nether
    s_ret, _ = engine.cmd_return(player)
    ctx.assert_true(s_ret, "/return succeeds")
    ctx.assert_equal(player.dimension, "minecraft:the_nether", "Returned to Nether")
    ctx.assert_almost_equal(player.x, -500.25, message="Nether X restored")
    ctx.assert_almost_equal(player.y, 70.0, message="Nether Y restored")
    ctx.assert_almost_equal(player.z, 800.75, message="Nether Z restored")
    ctx.assert_almost_equal(player.yaw, 180.0, message="Nether yaw restored")


@test(tier="Tier 4", description="S4: Dimensional Isolation and Portal Suppression")
def test_scenario_04_portal_suppression_and_anti_cheat(ctx: TestContext):
    """
    Simulates:
    1. Player in Skyblock builds Nether portal frame and uses flint and steel -> blocked.
    2. Player attempts to traverse pre-existing nether portal block -> blocked.
    3. Player attempts to activate End Portal frame with Eye of Ender -> blocked.
    4. Player attempts to traverse End portal block -> blocked.
    5. Confirms Skyblock dimension remains hermetically sealed against portal leaks.
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-portal", name="PortalBuilder", dimension="minecraft:overworld")
    engine.cmd_is(player)

    sb_dim = player.dimension
    ctx.assert_equal(sb_dim, "expedition:skyblock", "Player is in Skyblock")

    # 1. Nether portal ignition
    ctx.assert_false(engine.can_ignite_nether_portal(sb_dim), "Nether portal ignition suppressed in Skyblock")

    # 2. Nether portal traversal
    ctx.assert_false(engine.can_traverse_nether_portal(sb_dim), "Nether portal traversal suppressed in Skyblock")

    # 3. End portal activation
    ctx.assert_false(engine.can_activate_end_portal(sb_dim), "End portal frame activation suppressed in Skyblock")

    # 4. End portal traversal
    ctx.assert_false(engine.can_traverse_end_portal(sb_dim), "End portal traversal suppressed in Skyblock")


@test(tier="Tier 4", description="S5: Persistence Integrity and Crash Recovery")
def test_scenario_05_crash_recovery_and_persistence_integrity(ctx: TestContext):
    """
    Simulates:
    1. Multi-player server with 3 players creating islands and setting custom homes.
    2. Server state saved to disk via atomic move.
    3. Simulated server crash / restart with fresh engine instance.
    4. Reloads state from disk.
    5. All islands, coordinates, permissions, and return locations fully recovered.
    6. Players can execute /return and get back to exact pre-crash Overworld locations.
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        data_file = os.path.join(tmpdir, "expedition_skyblock.json")
        engine_live = SkyblockEngine(world_data_path=data_file)

        p1 = MockPlayer(uuid="u1", name="P1", dimension="minecraft:overworld", x=10.0, y=64.0, z=10.0)
        p2 = MockPlayer(uuid="u2", name="P2", dimension="minecraft:overworld", x=20.0, y=65.0, z=20.0)
        p3 = MockPlayer(uuid="u3", name="P3", dimension="minecraft:the_end", x=100.0, y=50.0, z=-100.0)

        engine_live.cmd_is(p1)
        engine_live.cmd_is(p2)
        engine_live.cmd_is(p3)

        # P1 customizes home
        i1 = engine_live.get_island_by_owner("u1")
        p1.x = i1.center_x + 30.0
        p1.y = 75.0
        p1.z = i1.center_z - 30.0
        engine_live.cmd_sethome(p1)

        # P1 invites P2
        engine_live.cmd_invite(p1, p2)

        # Atomic flush to disk
        engine_live.save_to_file()
        ctx.assert_true(os.path.exists(data_file), "Data file exists on disk")

        # Simulate sudden crash and reboot
        engine_rebooted = SkyblockEngine(world_data_path=data_file)
        engine_rebooted.load_from_file()

        ctx.assert_equal(engine_rebooted.next_island_index, 3, "Next island index recovered as 3")
        ctx.assert_equal(len(engine_rebooted.islands), 3, "All 3 islands recovered")

        # Verify P1 custom home recovered
        rec_i1 = engine_rebooted.get_island_by_owner("u1")
        ctx.assert_equal((rec_i1.home_x, rec_i1.home_y, rec_i1.home_z), (i1.center_x + 30.0, 75.0, i1.center_z - 30.0), "P1 custom home recovered")
        ctx.assert_in("u2", rec_i1.invited_players, "P2 invitation recovered")

        # Verify P3 return location to The End recovered
        ret_p3 = engine_rebooted.player_returns.get("u3")
        ctx.assert_equal(ret_p3.dimension, "minecraft:the_end", "P3 saved dimension recovered as The End")
        ctx.assert_almost_equal(ret_p3.x, 100.0, message="P3 End X recovered")

        # P3 runs /return in rebooted server
        p3.dimension = "expedition:skyblock"
        engine_rebooted.cmd_return(p3)
        ctx.assert_equal(p3.dimension, "minecraft:the_end", "P3 returned to The End successfully")
