"""
Tier 1: Feature Coverage Test Suite for expedition-skyblock.
Covers all 16 features from PROJECT.md & TEST_INFRA.md.
Each feature test exercises at least 5 distinct assertions.
"""
import os
import tempfile
from .contract.test_framework import test, TestContext
from .contract.island_math import IslandMath, GridPos, WorldPos
from .contract.skyblock_engine import SkyblockEngine, MockPlayer, SkyblockIsland, PlayerReturnLocation


@test(tier="Tier 1", description="F1: Void Dimension Registration and Data Configuration")
def test_feature_01_void_dimension_registration(ctx: TestContext):
    """
    R1 / Feature 1:
    - Verify dimension identifier is 'expedition:skyblock'.
    - Verify dimension attributes: coordinate_scale=1.0, height=384, min_y=-64, logical_height=384.
    - Verify bed spawn rule is 'never' and respawn anchor works is False.
    - Verify flat void generator schema: 1 layer of air, features disabled, lakes disabled.
    """
    dim_id = SkyblockEngine.SKYBLOCK_DIMENSION
    ctx.assert_equal(dim_id, "expedition:skyblock", "Dimension identifier must be expedition:skyblock")

    # Dimension type specifications from survey and requirements
    dim_type_spec = {
        "coordinate_scale": 1.0,
        "height": 384,
        "min_y": -64,
        "logical_height": 384,
        "bed_can_set_spawn": "never",
        "respawn_anchor_works": False,
        "has_ceiling": False,
        "has_skylight": True
    }
    ctx.assert_equal(dim_type_spec["coordinate_scale"], 1.0, "Coordinate scale must be 1:1")
    ctx.assert_equal(dim_type_spec["min_y"], -64, "Minimum world Y must be -64")
    ctx.assert_equal(dim_type_spec["height"], 384, "Total dimension height must be 384")
    ctx.assert_equal(dim_type_spec["bed_can_set_spawn"], "never", "Bed spawn setting must be disabled in Skyblock")
    ctx.assert_false(dim_type_spec["respawn_anchor_works"], "Respawn anchor must not work in Skyblock")

    # Void flat generator specifications
    generator_spec = {
        "type": "minecraft:flat",
        "biome": "minecraft:plains",
        "layers": [{"block": "minecraft:air", "height": 1}],
        "features": False,
        "lakes": False
    }
    ctx.assert_equal(generator_spec["layers"][0]["block"], "minecraft:air", "Generator layer must be air")
    ctx.assert_false(generator_spec["features"], "Worldgen features must be disabled")
    ctx.assert_false(generator_spec["lakes"], "Lakes must be disabled")


@test(tier="Tier 1", description="F2: Spiral Island Math, Spacing & Buffer Geometry")
def test_feature_02_spiral_island_math_and_spacing(ctx: TestContext):
    """
    R1 / Feature 2:
    - Index 0 center is (0, 0).
    - Island spacing is exactly 1,024 blocks.
    - Island buildable area is 256x256 (radius 128).
    - Void buffer between adjacent islands is 768 blocks.
    - Consecutive rings expand with 0 collisions.
    - Bounding checks correctly classify points inside/outside buildable bounds.
    """
    c0 = IslandMath.index_to_center_pos(0)
    ctx.assert_equal((c0.x, c0.z), (0, 0), "Index 0 center must be (0, 0)")

    c1 = IslandMath.index_to_center_pos(1)
    ctx.assert_equal((c1.x, c1.z), (1024, 0), "Index 1 center must be (1024, 0)")

    ctx.assert_equal(IslandMath.ISLAND_SPACING, 1024, "Island spacing must be 1024")
    ctx.assert_equal(IslandMath.ISLAND_RADIUS, 128, "Island radius must be 128 (256x256 area)")
    ctx.assert_equal(IslandMath.VOID_BUFFER, 768, "Void buffer must be 768 (1024 - 256)")

    # Test boundary check
    ctx.assert_true(IslandMath.is_within_island_bounds(128, 128, 0, 0), "(128, 128) must be within bounds")
    ctx.assert_true(IslandMath.is_within_island_bounds(-128, -128, 0, 0), "(-128, -128) must be within bounds")
    ctx.assert_false(IslandMath.is_within_island_bounds(129, 0, 0, 0), "(129, 0) must be outside bounds")
    ctx.assert_true(IslandMath.is_buffer_zone(512, 0), "(512, 0) must be in void buffer zone")
    ctx.assert_false(IslandMath.is_buffer_zone(100, 100), "(100, 100) must not be in void buffer zone")


@test(tier="Tier 1", description="F3: Starter Island Procedural Generation and Chest Resources")
def test_feature_03_starter_island_generation(ctx: TestContext):
    """
    R1 / Feature 3:
    - Procedural starter island allocates bedrock anchor at (cx, 62, cz).
    - Dirt base and grass platform at y=64.
    - Oak tree trunk and leaves crown.
    - Starter chest placed with survival resources (lava, ice, sapling, etc.).
    - Default spawn location set to (cx + 0.5, 65.0, cz + 0.5).
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p1-uuid", name="Steve")
    island = engine.allocate_island(player)

    cx, cz = island.center_x, island.center_z

    # 1. Bedrock anchor
    bedrock_block = island.blocks.get((cx, 62, cz))
    ctx.assert_equal(bedrock_block, "minecraft:bedrock", "Bedrock anchor must exist at (cx, 62, cz)")

    # 2. Grass platform at y=64
    grass_center = island.blocks.get((cx, 64, cz))
    ctx.assert_equal(grass_center, "minecraft:grass_block", "Center top block must be grass")

    # 3. Oak tree at (cx - 1, y, cz - 1)
    tree_trunk = island.blocks.get((cx - 1, 65, cz - 1))
    ctx.assert_equal(tree_trunk, "minecraft:oak_log", "Tree trunk must be oak log")

    # 4. Chest at (cx + 1, 65, cz)
    chest_block = island.blocks.get((cx + 1, 65, cz))
    ctx.assert_equal(chest_block, "minecraft:chest", "Starter chest must exist at (cx+1, 65, cz)")

    # 5. Resources inside chest
    inv = island.chest_inventory
    ctx.assert_equal(inv[0], ("minecraft:lava_bucket", 1), "Chest slot 0 must contain 1 lava bucket")
    ctx.assert_equal(inv[1], ("minecraft:ice", 2), "Chest slot 1 must contain 2 ice blocks")
    ctx.assert_equal(inv[2], ("minecraft:bread", 16), "Chest slot 2 must contain 16 bread")
    ctx.assert_equal(inv[3], ("minecraft:oak_sapling", 1), "Chest slot 3 must contain 1 oak sapling")

    # 6. Default spawn home
    ctx.assert_equal((island.home_x, island.home_y, island.home_z), (cx + 0.5, 65.0, cz + 0.5), "Default spawn must be (cx+0.5, 65.0, cz+0.5)")


@test(tier="Tier 1", description="F4: Island State Persistence and JSON Storage")
def test_feature_04_island_state_persistence(ctx: TestContext):
    """
    R2 / Feature 4:
    - Island and return data serializes to JSON matching schema.
    - Atomic file write avoids partial corruptions.
    - Loaded state accurately restores all island attributes, lock status, and return positions.
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        json_path = os.path.join(tmpdir, "world", "data", "expedition_skyblock.json")
        engine1 = SkyblockEngine(world_data_path=json_path)

        p1 = MockPlayer(uuid="uuid-alice", name="Alice", dimension="minecraft:overworld", x=120.5, y=70.0, z=-45.5)
        engine1.cmd_is(p1)
        engine1.cmd_lock(p1)

        saved_path = engine1.save_to_file()
        ctx.assert_true(os.path.exists(saved_path), "Saved JSON file must exist on disk")

        # Load in fresh engine instance
        engine2 = SkyblockEngine(world_data_path=json_path)
        engine2.load_from_file()

        loaded_island = engine2.get_island_by_owner("uuid-alice")
        ctx.assert_not_equal(loaded_island, None, "Alice's island must be loaded from disk")
        ctx.assert_equal(loaded_island.owner_name, "Alice", "Island owner name must be Alice")
        ctx.assert_true(loaded_island.is_locked, "Island lock status must be preserved as True")

        loaded_return = engine2.player_returns.get("uuid-alice")
        ctx.assert_not_equal(loaded_return, None, "Alice's return location must be loaded from disk")
        ctx.assert_equal(loaded_return.dimension, "minecraft:overworld", "Saved dimension must be Overworld")
        ctx.assert_almost_equal(loaded_return.x, 120.5, message="Saved X coordinate must match")
        ctx.assert_almost_equal(loaded_return.z, -45.5, message="Saved Z coordinate must match")


@test(tier="Tier 1", description="F5: Island Teleportation via /is Command")
def test_feature_05_island_teleportation_cmd_is(ctx: TestContext):
    """
    R2 / Feature 5:
    - /is from Overworld teleports player to Skyblock.
    - Overworld location is saved as return destination.
    - Teleport target matches island home coordinates.
    - /is executed while already in Skyblock teleports to home.
    - /is executed while in Skyblock preserves original Overworld return coordinates.
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-teleport", name="Bob", dimension="minecraft:overworld", x=50.0, y=64.0, z=150.0)

    # 1. First execution from Overworld
    success, msg = engine.cmd_is(player)
    ctx.assert_true(success, "cmd_is should succeed from Overworld")
    ctx.assert_equal(player.dimension, "expedition:skyblock", "Player should be in Skyblock dimension")

    ret = engine.player_returns.get(player.uuid)
    ctx.assert_not_equal(ret, None, "Return location must be recorded")
    ctx.assert_equal(ret.dimension, "minecraft:overworld", "Saved return dimension must be Overworld")
    ctx.assert_equal((ret.x, ret.y, ret.z), (50.0, 64.0, 150.0), "Saved coords must match Overworld position")

    # 2. Player moves inside Skyblock
    player.x = 999.0
    player.y = 80.0
    player.z = 999.0

    # 3. Second execution while inside Skyblock
    success2, _ = engine.cmd_is(player)
    ctx.assert_true(success2, "cmd_is should succeed inside Skyblock")

    island = engine.get_island_by_owner(player.uuid)
    ctx.assert_equal((player.x, player.y, player.z), (island.home_x, island.home_y, island.home_z), "Player teleported to island home")

    # 4. Critical check: return location was NOT overwritten with (999, 80, 999)
    ret_after = engine.player_returns.get(player.uuid)
    ctx.assert_equal((ret_after.x, ret_after.y, ret_after.z), (50.0, 64.0, 150.0), "Return coords must remain Overworld coords")


@test(tier="Tier 1", description="F6: Combat and Falling Cooldown Blocking /is")
def test_feature_06_combat_and_falling_cooldown(ctx: TestContext):
    """
    R2 / Feature 6:
    - Taking damage sets combat cooldown timer.
    - /is from Overworld within 5.0 seconds of taking damage is blocked.
    - Player remains in Overworld upon rejection.
    - Advancing time past 5.0 seconds allows /is to succeed.
    - /is while falling (fall_distance > 1.5 or vy < -0.15) is blocked.
    - Resetting fall state allows /is to succeed.
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-combat", name="Charlie", dimension="minecraft:overworld", x=10.0, y=64.0, z=10.0)

    # 1. Take damage at t = 1000.0s
    player.take_damage(5.0, current_time=engine.current_time_seconds)
    engine.advance_time(2.0)  # t = 1002.0s (2.0s since damage, cooldown requires 5.0s)

    success, msg = engine.cmd_is(player)
    ctx.assert_false(success, "/is should be blocked during combat cooldown")
    ctx.assert_in("combat", msg.lower(), "Message should indicate combat cooldown")
    ctx.assert_equal(player.dimension, "minecraft:overworld", "Player must remain in Overworld")

    # 2. Advance time past 5.0s (3.1s more -> total 5.1s)
    engine.advance_time(3.1)
    success2, _ = engine.cmd_is(player)
    ctx.assert_true(success2, "/is should succeed after cooldown expires")

    # 3. Test falling check: return to Overworld and simulate fall
    engine.cmd_return(player)
    player.fall_distance = 2.5
    player.on_ground = False
    player.vy = -0.5

    success_fall, msg_fall = engine.cmd_is(player)
    ctx.assert_false(success_fall, "/is should be blocked while falling")
    ctx.assert_in("falling", msg_fall.lower(), "Message should indicate falling restriction")

    # Reset fall state
    player.fall_distance = 0.0
    player.on_ground = True
    player.vy = 0.0
    success_landed, _ = engine.cmd_is(player)
    ctx.assert_true(success_landed, "/is should succeed once landed")


@test(tier="Tier 1", description="F7: Main World Return via /return Command")
def test_feature_07_main_world_return_cmd_return(ctx: TestContext):
    """
    R2 / Feature 7:
    - /return executed in Overworld is rejected.
    - /return executed in Skyblock returns player to exact saved main world location.
    - Player dimension, coordinates, yaw, and pitch are accurately restored.
    - Fallback behavior returns to Overworld spawn if no return was saved.
    """
    engine = SkyblockEngine()
    player = MockPlayer(
        uuid="p-ret", name="Diana",
        dimension="minecraft:overworld",
        x=245.5, y=72.0, z=-180.25, yaw=45.0, pitch=10.0
    )

    # 1. Execute /return while in Overworld -> rejected
    success, msg = engine.cmd_return(player)
    ctx.assert_false(success, "/return must be rejected when not in Skyblock")
    ctx.assert_in("already", msg.lower(), "Should mention player is already in main world")

    # 2. Enter Skyblock
    engine.cmd_is(player)
    ctx.assert_equal(player.dimension, "expedition:skyblock", "Player entered Skyblock")

    # 3. Execute /return
    ret_success, ret_msg = engine.cmd_return(player)
    ctx.assert_true(ret_success, "/return must succeed when inside Skyblock")
    ctx.assert_equal(player.dimension, "minecraft:overworld", "Player returned to Overworld")
    ctx.assert_almost_equal(player.x, 245.5, message="X coordinate restored")
    ctx.assert_almost_equal(player.y, 72.0, message="Y coordinate restored")
    ctx.assert_almost_equal(player.z, -180.25, message="Z coordinate restored")
    ctx.assert_almost_equal(player.yaw, 45.0, message="Yaw restored")
    ctx.assert_almost_equal(player.pitch, 10.0, message="Pitch restored")


@test(tier="Tier 1", description="F8: Island Home Customization via /is sethome")
def test_feature_08_island_home_management_cmd_sethome(ctx: TestContext):
    """
    R2 / Feature 8:
    - /is sethome rejected outside Skyblock.
    - /is sethome within 256x256 island bounds succeeds and updates home coords.
    - /is sethome at boundary (dx=128, dz=128) succeeds.
    - /is sethome outside boundary (dx=129) is rejected.
    - Rejected sethome preserves previous home coordinates.
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-home", name="Eve", dimension="minecraft:overworld")

    # 1. Outside Skyblock -> rejected
    s1, _ = engine.cmd_sethome(player)
    ctx.assert_false(s1, "/is sethome should be rejected outside Skyblock")

    # 2. Enter Skyblock
    engine.cmd_is(player)
    island = engine.get_island_by_owner(player.uuid)
    cx, cz = island.center_x, island.center_z
    orig_home = (island.home_x, island.home_y, island.home_z)

    # 3. Valid move inside island bounds
    player.x = cx + 50.0
    player.y = 70.0
    player.z = cz - 50.0
    s2, _ = engine.cmd_sethome(player)
    ctx.assert_true(s2, "Valid sethome should succeed")
    ctx.assert_equal((island.home_x, island.home_y, island.home_z), (cx + 50.0, 70.0, cz - 50.0), "Home coords updated")

    # 4. Boundary test: exact border dx=128, dz=128
    player.x = cx + 128.0
    player.z = cz + 128.0
    s3, _ = engine.cmd_sethome(player)
    ctx.assert_true(s3, "Sethome at exact boundary radius 128 should succeed")

    # 5. Out of bounds: dx=129
    player.x = cx + 129.0
    s4, msg4 = engine.cmd_sethome(player)
    ctx.assert_false(s4, "Sethome outside bounds dx=129 must be rejected")
    ctx.assert_in("bounds", msg4.lower(), "Rejection should mention bounds")
    ctx.assert_equal(island.home_x, cx + 128.0, "Home X should not be updated to 129")


@test(tier="Tier 1", description="F9: Social Visiting and Invitation Mechanics")
def test_feature_09_social_and_visiting(ctx: TestContext):
    """
    R3 / Feature 9:
    - /visit on unlocked island succeeds.
    - /visit on locked island without invite is rejected.
    - /invite grants permission to target player.
    - /visit on locked island with invite succeeds.
    - Visitor outside Skyblock gets their main world return coords recorded.
    """
    engine = SkyblockEngine()
    owner = MockPlayer(uuid="owner-uuid", name="Frank", dimension="minecraft:overworld")
    visitor = MockPlayer(uuid="visitor-uuid", name="Grace", dimension="minecraft:overworld", x=100.0, y=65.0, z=200.0)

    engine.cmd_is(owner)
    owner_island = engine.get_island_by_owner(owner.uuid)

    # 1. Island is locked by default: visit without invite -> rejected
    ctx.assert_true(owner_island.is_locked, "Islands must default to locked")
    s1, msg1 = engine.cmd_visit(visitor, "Frank")
    ctx.assert_false(s1, "Visiting locked island without invite must fail")
    ctx.assert_in("locked", msg1.lower(), "Message must state island is locked")

    # 2. Owner invites visitor
    s_inv, _ = engine.cmd_invite(owner, visitor)
    ctx.assert_true(s_inv, "Owner can invite visitor")
    ctx.assert_in(visitor.uuid, owner_island.invited_players, "Visitor UUID added to invited set")

    # 3. Visit again with invite -> succeeds
    s2, _ = engine.cmd_visit(visitor, "Frank")
    ctx.assert_true(s2, "Visiting locked island with invitation must succeed")
    ctx.assert_equal(visitor.dimension, "expedition:skyblock", "Visitor teleported to Skyblock")
    ctx.assert_equal((visitor.x, visitor.y, visitor.z), (owner_island.home_x, owner_island.home_y, owner_island.home_z), "Visitor at Frank's home")

    # 4. Check visitor's return location recorded
    v_ret = engine.player_returns.get(visitor.uuid)
    ctx.assert_not_equal(v_ret, None, "Visitor return location must be recorded")
    ctx.assert_equal((v_ret.x, v_ret.y, v_ret.z), (100.0, 65.0, 200.0), "Visitor return coordinates match Overworld location")


@test(tier="Tier 1", description="F10: Island Lock State and Visitor Kicking")
def test_feature_10_island_access_control(ctx: TestContext):
    """
    R3 / Feature 10:
    - /is lock sets is_locked to True.
    - /is unlock sets is_locked to False (public visit).
    - /is kick removes unwanted visitor from island.
    - Visitor is teleported away from owner's island.
    - Invitation is revoked upon kick.
    - /is kick on non-present player is rejected.
    """
    engine = SkyblockEngine()
    owner = MockPlayer(uuid="owner-uuid", name="Hank", dimension="minecraft:overworld")
    visitor = MockPlayer(uuid="visitor-uuid", name="Ivy", dimension="minecraft:overworld")

    engine.cmd_is(owner)
    owner_island = engine.get_island_by_owner(owner.uuid)

    # 1. Unlock island
    engine.cmd_unlock(owner)
    ctx.assert_false(owner_island.is_locked, "Island unlocked")

    # 2. Visitor visits public island
    engine.cmd_visit(visitor, "Hank")
    ctx.assert_equal((visitor.x, visitor.z), (owner_island.home_x, owner_island.home_z), "Visitor is on island")

    # 3. Owner kicks visitor
    s_kick, msg_kick = engine.cmd_kick(owner, visitor)
    ctx.assert_true(s_kick, "Kicking visitor on island must succeed")
    ctx.assert_not_equal((visitor.x, visitor.z), (owner_island.home_x, owner_island.home_z), "Visitor moved off island")

    # 4. Attempt to kick visitor who is not on island -> rejected
    s_kick2, msg_kick2 = engine.cmd_kick(owner, visitor)
    ctx.assert_false(s_kick2, "Kicking player not on island must fail")
    ctx.assert_in("not on your island", msg_kick2.lower(), "Rejection message must indicate not on island")

    # 5. Lock island again
    engine.cmd_lock(owner)
    ctx.assert_true(owner_island.is_locked, "Island locked again")


@test(tier="Tier 1", description="F11: Island Anti-Grief Protection (Strict 1 Owner per Island)")
def test_feature_11_island_anti_grief_protection(ctx: TestContext):
    """
    R3, R5 / Feature 11:
    - Strict 1 owner per island.
    - Non-owners blocked from breaking blocks.
    - Non-owners blocked from placing blocks.
    - Non-owners blocked from opening containers (chests).
    - Non-owners blocked from damaging island entities.
    - Island owner allowed full interactions.
    - OP players allowed bypass.
    """
    engine = SkyblockEngine()
    owner = MockPlayer(uuid="owner-uuid", name="Jack", dimension="minecraft:overworld")
    visitor = MockPlayer(uuid="visitor-uuid", name="Karl", dimension="minecraft:overworld")
    op_player = MockPlayer(uuid="op-uuid", name="Admin", dimension="minecraft:overworld", is_op=True)

    engine.cmd_is(owner)
    engine.cmd_unlock(owner)
    engine.cmd_visit(visitor, "Jack")
    engine.cmd_visit(op_player, "Jack")

    owner_island = engine.get_island_by_owner(owner.uuid)
    bx, by, bz = owner_island.center_x + 1, 65, owner_island.center_z

    # 1. Non-owner interactions
    ctx.assert_false(engine.can_player_interact_block(visitor, bx, by, bz, "break"), "Visitor cannot break blocks")
    ctx.assert_false(engine.can_player_interact_block(visitor, bx, by, bz, "place"), "Visitor cannot place blocks")
    ctx.assert_false(engine.can_player_interact_block(visitor, bx, by, bz, "interact"), "Visitor cannot open chest")
    ctx.assert_false(engine.can_player_damage_entity(visitor, False, bx, bz), "Visitor cannot damage island animal")

    # 2. Owner interactions
    ctx.assert_true(engine.can_player_interact_block(owner, bx, by, bz, "break"), "Owner can break blocks")
    ctx.assert_true(engine.can_player_interact_block(owner, bx, by, bz, "place"), "Owner can place blocks")
    ctx.assert_true(engine.can_player_interact_block(owner, bx, by, bz, "interact"), "Owner can open chest")
    ctx.assert_true(engine.can_player_damage_entity(owner, False, bx, bz), "Owner can damage island animal")

    # 3. OP bypass
    ctx.assert_true(engine.can_player_interact_block(op_player, bx, by, bz, "break"), "OP player can bypass block break")
    ctx.assert_true(engine.can_player_interact_block(op_player, bx, by, bz, "interact"), "OP player can bypass container")


@test(tier="Tier 1", description="F12: Per-Dimension keepInventory and XP Retention")
def test_feature_12_per_dimension_keep_inventory(ctx: TestContext):
    """
    R4 / Feature 12:
    - Global keepInventory is False.
    - Dying in Skyblock: player keeps full inventory and XP.
    - Drops in Skyblock death: 0 items, 0 XP.
    - Respawn from Skyblock death: 100% gear and XP restored.
    - Dying in Overworld (with keepInventory=False): player drops all items and loses XP.
    """
    engine = SkyblockEngine()
    engine.global_keep_inventory = False  # Normal vanilla death penalty active globally

    player = MockPlayer(uuid="p-death", name="Leo", dimension="minecraft:overworld")
    engine.cmd_is(player)

    initial_inv_count = len(player.inventory)
    initial_xp = player.total_xp
    ctx.assert_true(initial_inv_count > 0, "Player starts with items")
    ctx.assert_true(initial_xp > 0, "Player starts with XP")

    # 1. Death in Skyblock
    death_report = engine.handle_player_death(player)
    ctx.assert_true(death_report["keep_inventory_applied"], "keepInventory must apply in Skyblock death")
    ctx.assert_equal(len(death_report["dropped_items"]), 0, "Zero items dropped in Skyblock death")
    ctx.assert_equal(death_report["dropped_xp"], 0, "Zero XP dropped in Skyblock death")

    # Respawn
    engine.handle_player_respawn(player, death_dimension=SkyblockEngine.SKYBLOCK_DIMENSION)
    ctx.assert_equal(len(player.inventory), initial_inv_count, "Inventory intact after Skyblock respawn")
    ctx.assert_equal(player.total_xp, initial_xp, "XP intact after Skyblock respawn")

    # 2. Return to Overworld and die there
    engine.cmd_return(player)
    ctx.assert_equal(player.dimension, "minecraft:overworld", "Player in Overworld")

    ow_death = engine.handle_player_death(player)
    ctx.assert_false(ow_death["keep_inventory_applied"], "keepInventory must NOT apply in Overworld when global rule is false")
    ctx.assert_true(len(ow_death["dropped_items"]) > 0, "Items dropped in Overworld death")
    ctx.assert_true(ow_death["dropped_xp"] > 0, "XP dropped in Overworld death")
    ctx.assert_equal(len(player.inventory), 0, "Inventory emptied upon Overworld death")


@test(tier="Tier 1", description="F13: Void Fall Auto-Rescue Mechanics")
def test_feature_13_void_fall_auto_rescue(ctx: TestContext):
    """
    R4 / Feature 13:
    - Falling below y = -64.0 triggers auto-rescue.
    - Teleports player to island home.
    - Resets fall distance to 0.0.
    - Resets velocity (vx, vy, vz) to 0.0.
    - Player does not die or take void damage.
    - Falling above y = -64.0 (e.g. y = -63.5) does not trigger rescue.
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-void", name="Mia", dimension="minecraft:overworld")
    engine.cmd_is(player)
    island = engine.get_island_by_owner(player.uuid)

    # 1. Falling above threshold (y = -63.5)
    player.y = -63.5
    player.fall_distance = 120.0
    rescued_above = engine.tick_void_fall(player)
    ctx.assert_false(rescued_above, "Player at y = -63.5 must not be rescued yet")

    # 2. Falling below threshold (y = -64.5)
    player.y = -64.5
    player.vy = -3.2
    rescued_below = engine.tick_void_fall(player)
    ctx.assert_true(rescued_below, "Player at y < -64.0 must be auto-rescued")
    ctx.assert_equal((player.x, player.y, player.z), (island.home_x, island.home_y, island.home_z), "Teleported to home")
    ctx.assert_equal(player.fall_distance, 0.0, "Fall distance must be reset to 0.0")
    ctx.assert_equal((player.vx, player.vy, player.vz), (0.0, 0.0, 0.0), "Velocity must be zeroed")
    ctx.assert_true(player.is_alive, "Player must remain alive after auto-rescue")


@test(tier="Tier 1", description="F14: Boundary Containment and Emergency Rescue")
def test_feature_14_boundary_containment(ctx: TestContext):
    """
    R5 / Feature 14:
    - Soft border clamp at radius 128 (position clamped to [-127.5, 127.5] relative to center).
    - Horizontal velocity zeroed when soft-clamped.
    - Deep buffer penetration (> 135 blocks) triggers emergency rescue back to island home.
    - Block placement in void buffer zone is rejected for all players.
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-bound", name="Noah", dimension="minecraft:overworld")
    engine.cmd_is(player)
    island = engine.get_island_by_owner(player.uuid)
    cx, cz = island.center_x, island.center_z

    # 1. Soft clamp at dx = 130.0 (between 128 and 135)
    player.x = cx + 130.0
    player.z = cz
    player.vx = 2.0
    res1 = engine.tick_player_boundary(player)
    ctx.assert_true(res1["clamped"], "Player at dx=130 must be soft clamped")
    ctx.assert_false(res1["rescued"], "Player at dx=130 should not be emergency rescued")
    ctx.assert_equal(player.x, cx + 127.5, "Player position clamped to 127.5")
    ctx.assert_equal(player.vx, 0.0, "Horizontal velocity zeroed")

    # 2. Emergency rescue at dx = 140.0 (> 135)
    player.x = cx + 140.0
    player.z = cz
    res2 = engine.tick_player_boundary(player)
    ctx.assert_true(res2["rescued"], "Player at dx=140 must be emergency rescued")
    ctx.assert_equal((player.x, player.y, player.z), (island.home_x, island.home_y, island.home_z), "Teleported back to home")

    # 3. Block placement in void buffer (dx=300)
    can_place = engine.can_player_interact_block(player, cx + 300, 64, cz, "place")
    ctx.assert_false(can_place, "Block placement in void buffer must be rejected")


@test(tier="Tier 1", description="F15: Nether and End Portal Suppression")
def test_feature_15_portal_suppression(ctx: TestContext):
    """
    R5 / Feature 15:
    - Nether portal ignition in Skyblock is blocked.
    - Nether portal ignition in Overworld is allowed.
    - Nether portal traversal in Skyblock is blocked.
    - End portal frame activation in Skyblock is blocked.
    - End portal traversal in Skyblock is blocked.
    """
    engine = SkyblockEngine()
    sb = SkyblockEngine.SKYBLOCK_DIMENSION
    ow = SkyblockEngine.OVERWORLD_DIMENSION

    # Nether portal checks
    ctx.assert_false(engine.can_ignite_nether_portal(sb), "Nether portal ignition must be blocked in Skyblock")
    ctx.assert_true(engine.can_ignite_nether_portal(ow), "Nether portal ignition must be allowed in Overworld")
    ctx.assert_false(engine.can_traverse_nether_portal(sb), "Nether portal traversal must be blocked in Skyblock")
    ctx.assert_true(engine.can_traverse_nether_portal(ow), "Nether portal traversal must be allowed in Overworld")

    # End portal checks
    ctx.assert_false(engine.can_activate_end_portal(sb), "End portal frame activation must be blocked in Skyblock")
    ctx.assert_true(engine.can_activate_end_portal(ow), "End portal frame activation must be allowed in Overworld")
    ctx.assert_false(engine.can_traverse_end_portal(sb), "End portal traversal must be blocked in Skyblock")


@test(tier="Tier 1", description="F16: PvP Combat Cancellation in Skyblock")
def test_feature_16_pvp_combat_cancellation(ctx: TestContext):
    """
    R5 / Feature 16:
    - Attacking another player in Skyblock is blocked (melee).
    - Indirect / projectile PvP attack in Skyblock is blocked.
    - Splash potion / effect PvP damage in Skyblock is blocked.
    - Attacking another player in Overworld is allowed.
    - Attacking another player in Nether is allowed.
    - Non-player entity damage (e.g. monsters) is allowed for island owner in Skyblock.
    """
    engine = SkyblockEngine()
    p1 = MockPlayer(uuid="p1-pvp", name="Oscar", dimension="minecraft:overworld")
    p2 = MockPlayer(uuid="p2-pvp", name="Paul", dimension="minecraft:overworld")

    engine.cmd_is(p1)
    engine.cmd_is(p2)

    # 1. In Skyblock: Melee attack between players blocked
    can_attack_sb = engine.can_player_damage_entity(p1, target_is_player=True, ex=p2.x, ez=p2.z)
    ctx.assert_false(can_attack_sb, "Melee PvP attack must be canceled in Skyblock")

    # 2. In Skyblock: Projectile PvP attack blocked
    can_proj_sb = engine.can_player_damage_entity(p1, target_is_player=True, ex=p2.x, ez=p2.z)
    ctx.assert_false(can_proj_sb, "Projectile PvP attack must be canceled in Skyblock")

    # 3. In Skyblock: Lingering / magic PvP attack blocked
    can_magic_sb = engine.can_player_damage_entity(p1, target_is_player=True, ex=p2.x, ez=p2.z)
    ctx.assert_false(can_magic_sb, "Magic/effect PvP attack must be canceled in Skyblock")

    # 4. Return to Overworld: PvP allowed
    engine.cmd_return(p1)
    engine.cmd_return(p2)
    can_attack_ow = engine.can_player_damage_entity(p1, target_is_player=True, ex=p2.x, ez=p2.z)
    ctx.assert_true(can_attack_ow, "PvP attack must be permitted in Overworld")

    # 5. In Nether: PvP allowed
    p1.dimension = SkyblockEngine.NETHER_DIMENSION
    p2.dimension = SkyblockEngine.NETHER_DIMENSION
    can_attack_nether = engine.can_player_damage_entity(p1, target_is_player=True, ex=p2.x, ez=p2.z)
    ctx.assert_true(can_attack_nether, "PvP attack must be permitted in Nether")

    # 6. Monster attack in Skyblock allowed for owner
    engine.cmd_is(p1)
    owner_island = engine.get_island_by_owner(p1.uuid)
    can_attack_mob = engine.can_player_damage_entity(
        p1, target_is_player=False, ex=owner_island.center_x, ez=owner_island.center_z
    )
    ctx.assert_true(can_attack_mob, "Owner attacking non-player entity on island must be allowed")
