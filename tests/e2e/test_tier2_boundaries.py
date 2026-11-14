"""
Tier 2: Boundary & Corner Cases Test Suite for expedition-skyblock.
Covers exact boundary limits:
- y = -64.0 (void rescue threshold: -63.99 vs -64.0 vs -64.01 vs -128.0)
- radius = 128 (island border: 128.0 inside vs 128.01 / 129 outside)
- dx/dz = 135 (emergency rescue buffer: 134.9 clamped vs 135.1 rescued)
- 5.0-second combat cooldown timer (4.9s vs 5.0s vs 5.1s)
- Spiral math index extremes (index 0 vs 1 vs 8 vs 9 vs 1,000 vs 1,000,000)
- /is sethome at exact boundary (128) vs 1 block outside (129)
"""
from .contract.test_framework import test, TestContext
from .contract.island_math import IslandMath, GridPos, WorldPos
from .contract.skyblock_engine import SkyblockEngine, MockPlayer


@test(tier="Tier 2", description="B1: Void Rescue Threshold Boundary (y = -64.0)")
def test_boundary_01_void_rescue_threshold_y(ctx: TestContext):
    """
    Validates void rescue trigger behavior around exact threshold y = -64.0.
    Condition in server logic: player.getY() < -64.0.
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-b1", name="VoidBoundaryPlayer", dimension="minecraft:overworld")
    engine.cmd_is(player)
    island = engine.get_island_by_owner(player.uuid)

    # Point 1: y = -63.99 (just above threshold) -> NOT rescued
    player.y = -63.99
    player.fall_distance = 127.99
    r1 = engine.tick_void_fall(player)
    ctx.assert_false(r1, "y = -63.99 must not trigger void rescue")
    ctx.assert_almost_equal(player.y, -63.99, delta=1e-4, message="Player remains at y = -63.99")

    # Point 2: y = -64.00 (exact threshold) -> NOT rescued (condition is strictly < -64.0)
    player.y = -64.00
    r2 = engine.tick_void_fall(player)
    ctx.assert_false(r2, "y = -64.00 must not trigger void rescue (condition is y < -64.0)")

    # Point 3: y = -64.01 (just below threshold) -> RESCUED
    player.y = -64.01
    player.vy = -2.5
    r3 = engine.tick_void_fall(player)
    ctx.assert_true(r3, "y = -64.01 must trigger void rescue")
    ctx.assert_equal(player.y, island.home_y, "Player teleported to island home Y")
    ctx.assert_equal(player.fall_distance, 0.0, "Fall distance reset to 0.0")
    ctx.assert_equal(player.vy, 0.0, "Vertical velocity reset to 0.0")

    # Point 4: y = -128.0 (vanilla out-of-world damage point) -> RESCUED safely
    player.y = -128.0
    r4 = engine.tick_void_fall(player)
    ctx.assert_true(r4, "y = -128.0 must trigger void rescue safely")
    ctx.assert_true(player.is_alive, "Player must remain alive")

    # Point 5: y = -500.0 (extreme void fall) -> RESCUED safely
    player.y = -500.0
    r5 = engine.tick_void_fall(player)
    ctx.assert_true(r5, "y = -500.0 must trigger void rescue safely")
    ctx.assert_equal(player.y, island.home_y, "Player returned to island home Y")


@test(tier="Tier 2", description="B2: Island Border Radius Boundary (radius = 128)")
def test_boundary_02_island_border_radius_128(ctx: TestContext):
    """
    Validates 256x256 island border boundary (radius = 128).
    Boundary condition: |x - cx| <= 128 and |z - cz| <= 128.
    """
    cx, cz = 1024, -2048

    # Points on the exact boundary (radius = 128)
    ctx.assert_true(IslandMath.is_within_island_bounds(cx + 128.0, cz, cx, cz), "+X border (cx+128, cz) is in bounds")
    ctx.assert_true(IslandMath.is_within_island_bounds(cx - 128.0, cz, cx, cz), "-X border (cx-128, cz) is in bounds")
    ctx.assert_true(IslandMath.is_within_island_bounds(cx, cz + 128.0, cx, cz), "+Z border (cx, cz+128) is in bounds")
    ctx.assert_true(IslandMath.is_within_island_bounds(cx, cz - 128.0, cx, cz), "-Z border (cx, cz-128) is in bounds")
    ctx.assert_true(IslandMath.is_within_island_bounds(cx + 128.0, cz + 128.0, cx, cz), "Corner (+128, +128) is in bounds")
    ctx.assert_true(IslandMath.is_within_island_bounds(cx - 128.0, cz - 128.0, cx, cz), "Corner (-128, -128) is in bounds")

    # Points just outside the boundary
    ctx.assert_false(IslandMath.is_within_island_bounds(cx + 128.01, cz, cx, cz), "+X outside (cx+128.01) is out of bounds")
    ctx.assert_false(IslandMath.is_within_island_bounds(cx - 128.01, cz, cx, cz), "-X outside (cx-128.01) is out of bounds")
    ctx.assert_false(IslandMath.is_within_island_bounds(cx, cz + 128.01, cx, cz), "+Z outside (cz+128.01) is out of bounds")
    ctx.assert_false(IslandMath.is_within_island_bounds(cx, cz - 128.01, cx, cz), "-Z outside (cz-128.01) is out of bounds")
    ctx.assert_false(IslandMath.is_within_island_bounds(cx + 129.0, cz, cx, cz), "+1 block outside (cx+129) is out of bounds")
    ctx.assert_false(IslandMath.is_within_island_bounds(cx, cz + 129.0, cx, cz), "+1 block outside (cz+129) is out of bounds")


@test(tier="Tier 2", description="B3: Emergency Rescue Buffer Boundary (dx/dz = 135)")
def test_boundary_03_buffer_emergency_rescue_135(ctx: TestContext):
    """
    Validates soft clamping vs emergency rescue threshold at buffer penetration = 135.
    - Up to 135.0: soft clamp to 127.5.
    - Exceeding 135.0: emergency rescue back to home.
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-b3", name="BufferPlayer", dimension="minecraft:overworld")
    engine.cmd_is(player)
    island = engine.get_island_by_owner(player.uuid)
    cx, cz = island.center_x, island.center_z

    # 1. dx = 134.9 (just below emergency threshold 135) -> CLAMPED, NOT RESCUED
    player.x = cx + 134.9
    player.z = cz
    player.vx = 1.5
    res1 = engine.tick_player_boundary(player)
    ctx.assert_true(res1["clamped"], "dx = 134.9 must be soft-clamped")
    ctx.assert_false(res1["rescued"], "dx = 134.9 must not trigger emergency rescue")
    ctx.assert_equal(player.x, cx + 127.5, "Position clamped to cx + 127.5")

    # 2. dx = 135.0 (exact threshold) -> CLAMPED, NOT RESCUED
    player.x = cx + 135.0
    player.z = cz
    res2 = engine.tick_player_boundary(player)
    ctx.assert_true(res2["clamped"], "dx = 135.0 must be soft-clamped")
    ctx.assert_false(res2["rescued"], "dx = 135.0 must not trigger emergency rescue")

    # 3. dx = 135.01 (just above emergency threshold 135) -> EMERGENCY RESCUED
    player.x = cx + 135.01
    player.z = cz
    res3 = engine.tick_player_boundary(player)
    ctx.assert_true(res3["rescued"], "dx = 135.01 must trigger emergency rescue")
    ctx.assert_equal((player.x, player.z), (island.home_x, island.home_z), "Returned to home")

    # 4. dz = 136.0 (Z-axis emergency penetration) -> EMERGENCY RESCUED
    player.x = cx
    player.z = cz + 136.0
    res4 = engine.tick_player_boundary(player)
    ctx.assert_true(res4["rescued"], "dz = 136.0 must trigger emergency rescue")
    ctx.assert_equal((player.x, player.z), (island.home_x, island.home_z), "Returned to home")


@test(tier="Tier 2", description="B4: Combat Cooldown Boundary (4.9s vs 5.0s vs 5.1s)")
def test_boundary_04_combat_cooldown_timer_boundary(ctx: TestContext):
    """
    Validates exact combat cooldown duration of 5.0 seconds.
    - t = 4.90s: rejected
    - t = 4.99s: rejected
    - t = 5.00s: accepted (cooldown expired)
    - t = 5.10s: accepted
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-b4", name="CooldownPlayer", dimension="minecraft:overworld", x=10.0, y=64.0, z=10.0)

    # Damage taken at t = 1000.0s
    player.take_damage(2.0, current_time=1000.0)
    engine.current_time_seconds = 1000.0

    # 1. Elapsed 4.90s -> t = 1004.90s -> REJECTED
    engine.current_time_seconds = 1004.90
    s_490, _ = engine.cmd_is(player)
    ctx.assert_false(s_490, "4.90s elapsed: combat cooldown must reject /is")
    ctx.assert_equal(player.dimension, "minecraft:overworld", "Player remains in Overworld")

    # 2. Elapsed 4.99s -> t = 1004.99s -> REJECTED
    engine.current_time_seconds = 1004.99
    s_499, _ = engine.cmd_is(player)
    ctx.assert_false(s_499, "4.99s elapsed: combat cooldown must reject /is")

    # 3. Elapsed 5.00s -> t = 1005.00s -> ACCEPTED
    engine.current_time_seconds = 1005.00
    s_500, _ = engine.cmd_is(player)
    ctx.assert_true(s_500, "5.00s elapsed: cooldown reached, /is must succeed")
    ctx.assert_equal(player.dimension, "expedition:skyblock", "Player entered Skyblock")

    # Return player to Overworld and re-test with 5.1s
    engine.cmd_return(player)
    player.take_damage(2.0, current_time=2000.0)
    engine.current_time_seconds = 2005.10
    s_510, _ = engine.cmd_is(player)
    ctx.assert_true(s_510, "5.10s elapsed: cooldown expired, /is must succeed")


@test(tier="Tier 2", description="B5: Spiral Math Extreme Indices (0, 1, 8, 9, 1,000, 1,000,000)")
def test_boundary_05_spiral_math_index_0_and_large_indices(ctx: TestContext):
    """
    Validates spiral math at corner indices:
    - Index 0: center (0, 0)
    - Index 1: center (1024, 0)
    - Index 8: ring 1 corner (1024, -1024)
    - Index 9: ring 2 start (2048, -1024)
    - Index 1,000: intermediate large index
    - Index 1,000,000: extreme large index without numeric overflow or collision
    """
    # Index 0
    g0 = IslandMath.index_to_grid_pos(0)
    c0 = IslandMath.index_to_center_pos(0)
    ctx.assert_equal((g0.u, g0.v), (0, 0), "Index 0 grid is (0, 0)")
    ctx.assert_equal((c0.x, c0.z), (0, 0), "Index 0 center is (0, 0)")

    # Index 1
    g1 = IslandMath.index_to_grid_pos(1)
    c1 = IslandMath.index_to_center_pos(1)
    ctx.assert_equal((g1.u, g1.v), (1, 0), "Index 1 grid is (1, 0)")
    ctx.assert_equal((c1.x, c1.z), (1024, 0), "Index 1 center is (1024, 0)")

    # Index 8 (ring 1 end)
    g8 = IslandMath.index_to_grid_pos(8)
    c8 = IslandMath.index_to_center_pos(8)
    ctx.assert_equal((g8.u, g8.v), (1, -1), "Index 8 grid is (1, -1)")
    ctx.assert_equal((c8.x, c8.z), (1024, -1024), "Index 8 center is (1024, -1024)")

    # Index 9 (ring 2 start)
    g9 = IslandMath.index_to_grid_pos(9)
    c9 = IslandMath.index_to_center_pos(9)
    ctx.assert_equal((g9.u, g9.v), (2, -1), "Index 9 grid is (2, -1)")
    ctx.assert_equal((c9.x, c9.z), (2048, -1024), "Index 9 center is (2048, -1024)")

    # Index 1,000
    g1000 = IslandMath.index_to_grid_pos(1000)
    c1000 = IslandMath.index_to_center_pos(1000)
    ctx.assert_equal(c1000.x, g1000.u * 1024, "Center X is grid.u * 1024")
    ctx.assert_equal(c1000.z, g1000.v * 1024, "Center Z is grid.v * 1024")

    # Index 1,000,000
    g1m = IslandMath.index_to_grid_pos(1000000)
    c1m = IslandMath.index_to_center_pos(1000000)
    ctx.assert_not_equal(c1m.x, 0, "Extreme index center X must be non-zero")
    ctx.assert_not_equal(c1m.z, 0, "Extreme index center Z must be non-zero")

    # Collision test between 1,000,000 and 1,000,001
    c1m_plus = IslandMath.index_to_center_pos(1000001)
    ctx.assert_not_equal((c1m.x, c1m.z), (c1m_plus.x, c1m_plus.z), "Adjacent extreme indices must not collide")


@test(tier="Tier 2", description="B6: /is sethome Exact Boundary vs 1 Block Outside")
def test_boundary_06_sethome_boundary_vs_outside(ctx: TestContext):
    """
    Validates /is sethome at exact border limits:
    - dx = 128, dz = 128: accepted
    - dx = 129, dz = 0: rejected (1 block outside)
    - dx = 0, dz = 129: rejected (1 block outside)
    - dx = -128, dz = -128: accepted
    - dx = -129, dz = 0: rejected
    """
    engine = SkyblockEngine()
    player = MockPlayer(uuid="p-b6", name="SethomeBoundaryPlayer", dimension="minecraft:overworld")
    engine.cmd_is(player)
    island = engine.get_island_by_owner(player.uuid)
    cx, cz = island.center_x, island.center_z

    # 1. dx = 128, dz = 128 -> ACCEPTED
    player.x = cx + 128.0
    player.z = cz + 128.0
    s1, _ = engine.cmd_sethome(player)
    ctx.assert_true(s1, "Sethome at exact corner (+128, +128) must succeed")
    ctx.assert_equal(island.home_x, cx + 128.0, "Home X updated to boundary")
    ctx.assert_equal(island.home_z, cz + 128.0, "Home Z updated to boundary")

    # 2. dx = 129, dz = 0 -> REJECTED
    player.x = cx + 129.0
    player.z = cz
    s2, msg2 = engine.cmd_sethome(player)
    ctx.assert_false(s2, "Sethome at dx = 129 must be rejected")
    ctx.assert_equal(island.home_x, cx + 128.0, "Home X remains at previous valid location")

    # 3. dx = 0, dz = 129 -> REJECTED
    player.x = cx
    player.z = cz + 129.0
    s3, _ = engine.cmd_sethome(player)
    ctx.assert_false(s3, "Sethome at dz = 129 must be rejected")

    # 4. dx = -128, dz = -128 -> ACCEPTED
    player.x = cx - 128.0
    player.z = cz - 128.0
    s4, _ = engine.cmd_sethome(player)
    ctx.assert_true(s4, "Sethome at opposite corner (-128, -128) must succeed")
    ctx.assert_equal(island.home_x, cx - 128.0, "Home X updated to negative boundary")

    # 5. dx = -129, dz = 0 -> REJECTED
    player.x = cx - 129.0
    player.z = cz
    s5, _ = engine.cmd_sethome(player)
    ctx.assert_false(s5, "Sethome at dx = -129 must be rejected")
