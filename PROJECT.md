# Project: expedition-skyblock Fabric Mod for Minecraft 26.3

## Architecture
`expedition-skyblock` is a dedicated server-side Fabric mod designed for Minecraft 26.3 (Fabric Loader 0.20.1, Java 25, Mojang official mappings). It resides in `mods_src/expedition-skyblock/` as an independent Gradle project with a build task outputting `build/libs/expedition-skyblock-1.0.0.jar` to `mods/`, registered in `mods_registry.json`, and synced to `D:\AllayOps` via `sync_content.py`.

The system comprises 5 interconnected components:
1. **Dimension & Void Generation**:
   - `expedition:skyblock` dimension and dimension_type registered via data pack JSONs (`data/expedition/dimension_type/skyblock.json` and `data/expedition/dimension/skyblock.json`).
   - Uses vanilla flat level source with 1 layer of air and disabled features (`the_void` pattern) for zero chunk generation overhead.
2. **Island Allocation & Starter Island**:
   - Closed-form square spiral coordinate mapping (`IslandMath`) providing 0-collision $(cx, cz)$ centers spaced 1,024 blocks apart.
   - 256x256 buildable boundary ($[-128, 128]$ from center) with 768-block void buffers.
   - Procedural starter island generator creating dirt/grass platform, bedrock anchor at $y=62$, oak tree, and starter chest (lava bucket, ice, sapling, seeds).
3. **Teleportation & State Persistence**:
   - Brigadier commands: `/is`, `/return` (or `/is return`), `/is sethome`, `/is lock`, `/is unlock`, `/is kick <player>`, `/visit <player>`, `/invite <player>`.
   - Main world dimension and exact coordinates saved upon entering Skyblock from outside.
   - Executing `/is` within Skyblock preserves the original main world return coordinates.
   - 5-second combat/falling cooldown gating `/is` from outside Skyblock.
   - Persistent storage in `world/data/expedition_skyblock.json` with atomic writes (`ATOMIC_MOVE`) and server lifecycle synchronization (`SERVER_STARTED` / `SERVER_STOPPING`).
4. **Social & Anti-Grief Protection**:
   - Strict 1 owner per island. Non-owners are denied block breaking, block placing, container/block interactions, entity interactions, and entity damage on visited islands.
   - Public visit vs invite-only lock state.
   - Horizontal boundary containment (soft clamp at 128 blocks, emergency rescue at 135 blocks).
   - Nether & End portal ignition and traversal disabled inside `expedition:skyblock`.
   - PvP combat completely disabled inside `expedition:skyblock`.
5. **Per-Dimension keepInventory & Void Rescue**:
   - KeepInventory and XP retention always active in `expedition:skyblock` via mixins on `Player.dropEquipment`, `Player.getBaseExperienceReward`, and `ServerPlayer.restoreFrom` (guaranteeing cross-dimension respawn preservation while keeping Overworld/Nether/End vanilla).
   - Void fall auto-rescue below $y = -64$ in `ServerTickEvents.END_LEVEL_TICK` resetting fall distance, velocity, and teleporting safely to island spawn point without death.

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Void Dimension Registration | Data pack `skyblock.json` dimension and dimension_type for `expedition:skyblock` | M1 | R1 / Survey |
| 2 | Spiral Island Math & Spacing | Closed-form spiral math: 1,024-block spacing, 256x256 build area, 768-block buffer | M1 | R1 / Survey |
| 3 | Starter Island Generation | Procedural placement of dirt, bedrock anchor, oak tree, and chest with resources | M1 | R1 / Survey |
| 4 | Island State Persistence | JSON storage in `world/data/expedition_skyblock.json` with atomic writes | M2 | R2 / Survey |
| 5 | Island Teleportation (`/is`) | Teleport to island home; saves prior dimension/coords when outside; preserves return state when inside | M2 | R2 / Survey |
| 6 | Combat & Falling Cooldown | 5s cooldown after damage or falling blocking `/is` from main world | M2 | R2 / Survey |
| 7 | Main World Return (`/return`) | Returns player from Skyblock to saved main world coordinates | M2 | R2 / Survey |
| 8 | Island Home Management (`/is sethome`) | Sets custom island spawn within 256x256 bounds | M2 | R2 / Survey |
| 9 | Social & Visiting (`/visit`, `/invite`) | Visit other islands; invite players to locked islands; offline player support | M3 | R3 / Survey |
| 10 | Island Access Control (`/is lock`, `/is kick`) | Toggle lock status; kick visitors within island bounds | M3 | R3 / Survey |
| 11 | Island Anti-Grief Protection | Deny non-owners block break, place, container access, entity damage | M3 | R3, R5 / Survey |
| 12 | Per-Dimension keepInventory | Inventory & XP kept on death in Skyblock; vanilla behavior elsewhere | M4 | R4 / Survey |
| 13 | Void Fall Auto-Rescue | Detect $y < -64$, reset fall distance/velocity, teleport safely to island spawn | M4 | R4 / Survey |
| 14 | Boundary Containment | Clamp movement at 128-block radius; rescue at 135-block buffer penetration | M5 | R5 / Survey |
| 15 | Portal Suppression | Block Nether portal ignition/traversal and End portal traversal in Skyblock | M5 | R5 / Survey |
| 16 | PvP Combat Cancellation | Disable player-vs-player damage in Skyblock dimension | M5 | R5 / Survey |
| 17 | Build, Packaging & AllayOps Sync | `./gradlew build`, `mods_registry.json` registration, `sync_content.py` verification | M6 | Ops / Survey |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Core Implementation | Full `expedition-skyblock` mod implementation (all features 1-16) | none | DONE |
| M2 | E2E Testing Suite | Dual Track Opaque-box E2E test suite (Tiers 1-4) published in `TEST_READY.md` | none | DONE |
| M3 | E2E Verification & Hardening | Pass 100% E2E tests + Tier 5 Adversarial Hardening | M1, M2 | DONE |
| M4 | Packaging & AllayOps Sync | `./gradlew build`, `mods_registry.json` entry, `sync_content.py` validation | M3 | DONE |

## Interface Contracts
### `IslandMath`
- `static GridPos indexToGridPos(int index)` -> `(gridX, gridZ)`
- `static WorldPos indexToCenterPos(int index)` -> `(gridX * 1024, gridZ * 1024)`
- `static boolean isWithinIslandBounds(int blockX, int blockZ, int centerX, int centerZ)` -> `Math.abs(blockX - centerX) <= 128 && Math.abs(blockZ - centerZ) <= 128`
- `static boolean isBufferZone(int blockX, int blockZ)` -> returns true if in 768-block void buffer

### `SkyblockDataManager`
- `SkyblockIsland getOrCreateIsland(ServerPlayer player)`
- `SkyblockIsland getIslandByOwner(UUID ownerUUID)`
- `void saveReturnLocation(ServerPlayer player)` (only if not in Skyblock)
- `PlayerReturnLocation getReturnLocation(UUID playerUUID)`
- `void save()` (atomic JSON flush)

### `SkyblockProtectionHandler`
- `boolean canPlayerInteract(ServerPlayer player, BlockPos pos)`
- `boolean canPlayerDamageEntity(ServerPlayer attacker, Entity target)`
- `void handleVoidFall(ServerPlayer player)`

## Code Layout
```text
mods_src/expedition-skyblock/
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── java/net/onemoreexpedition/skyblock/
    │   │   ├── ExpeditionSkyblockMod.java
    │   │   ├── command/
    │   │   │   └── SkyblockCommands.java
    │   │   ├── data/
    │   │   │   ├── PlayerReturnLocation.java
    │   │   │   ├── SkyblockDataManager.java
    │   │   │   └── SkyblockIsland.java
    │   │   ├── island/
    │   │   │   ├── IslandGenerator.java
    │   │   │   └── IslandMath.java
    │   │   ├── listener/
    │   │   │   └── SkyblockEventListener.java
    │   │   └── mixin/
    │   │       ├── EndPortalMixin.java
    │   │       ├── NetherPortalMixin.java
    │   │       ├── PlayerKeepInventoryMixin.java
    │   │       ├── PortalShapeMixin.java
    │   │       └── ServerPlayerKeepInventoryMixin.java
    │   └── resources/
    │       ├── fabric.mod.json
    │       ├── expedition-skyblock.mixins.json
    │       └── data/
    │           └── expedition/
    │               ├── dimension/
    │               │   └── skyblock.json
    │               └── dimension_type/
    │                   └── skyblock.json
    └── test/
        └── java/net/onemoreexpedition/skyblock/
            └── IslandMathTest.java
```
