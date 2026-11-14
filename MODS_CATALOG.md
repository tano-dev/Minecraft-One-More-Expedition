# Mod Registry & Diagnostics Knowledge Base

> **Standardized Architecture & Diagnostics Catalog** designed for the **AllayOps Agentic Self-Healing Platform** and server administrators of Minecraft: One More Expedition.

- **Minecraft Version:** `26.3`
- **Fabric Loader:** `0.20.1`
- **Java Runtime Environment:** OpenJDK 25 (with ZGC generational collector)
- **Machine-Readable Metadata:** [`mods_registry.json`](file:///d:/Minecraft-One-More-Expedition/mods_registry.json)

---

## 1. Mod Categories

| Category ID | Scope & Architectural Purpose |
| :--- | :--- |
| `core_library` | Foundational abstraction layers, APIs, and declarative configuration libraries required by dependent mods. |
| `server_performance` | Main tick loop optimization, collision detection algorithms, pathfinding AI caching, and Redstone BFS engine. |
| `memory_optimization` | JVM heap reduction, model/blockstate footprint compression, and dynamic class-loading optimizations. |
| `network_and_players` | Netty pipeline streamlining, packet batching, and high-concurrency entity tracking for multi-player environments. |
| `worldgen_and_chunks` | Parallel chunk I/O, SIMD-accelerated noise calculation, structure bounding box optimization, and pre-generation tools. |
| `bugfix_and_security` | Backported security patches against malformed NBT exploits and oversized network packet buffer adjustments. |
| `entity_mechanics` | Entity despawn persistence cleanup, orb clustering, and combat AI tuning to prevent unintended mob accumulation. |
| `experimental_threading` | **Multi-threaded entity ticking (Alpha)** — High risk; isolated in `mods_optional/`. |
| `optional_client_side` | Client-only visual enhancers (LOD streaming) or redundant mods superseded by native server settings. |

---

## 2. Complete Mod Classification Matrix

| Mod ID | Jar Filename | Category | Side | Status | Risk Level | Diagnostics & Agentic Remediation Rule |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `fabric-api` | `fabric-api-0.166.2+26.3.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Fundamental Fabric API layer. Do not remove. Check loader version compatibility on mixin crashes. |
| `forgeconfigapiport` | `ForgeConfigAPIPort-v26.3.1-mc26.3.x-Fabric.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Bridge for Forge configuration specs on Fabric; required dependency for `skeletonaifix` & `getittogetherdrops`. |
| `puzzleslib` | `puzzleslib-v26.3.18-mc26.3.x+fabric.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Common abstraction library by Fuzs; required dependency for `skeletonaifix`. |
| `resourcefulconfig` | `ResourcefulConfig-6.0.2.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Declarative config framework; required dependency for `structure_layout_optimizer`. |
| `almanac` | `almanac-fabric-26.3-1.26.11.1.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Library framework by frikinjay; embeds DungeonConfig; required for `letmedespawn`. |
| `zconfig` | `zconfig-1.0.0+26.x.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Lightweight configuration framework; required dependency for `zfastnoise`. |
| `lithium` | `lithium-fabric-0.26.3+mc26.3.jar` | `server_performance` | Server | 🟢 Active | 🟢 Low | Essential server tick optimization (AI, physics, chunk ticking). Modules can be toggled via `config/lithium.properties`. |
| `carpet` | `fabric-carpet-26.3+v260915.jar` | `server_performance` | Server | 🟢 Active | 🟢 Low | Technical server mechanics & TNT optimization engine. Configure `/carpet optimizedTNT true` and `/carpet tntDoCollision false` to prevent entity collision freezing during mass detonations. |
| `alternate-current` | `alternate-current-mc26.3-1.9.0.jar` | `server_performance` | Server | 🟢 Active | 🟢 Low | Replaces vanilla redstone lag with high-performance BFS algorithm. Lithium automatically yields redstone calculation. |
| `scalablelux` | `ScalableLux-fabric-mc26.3-0.3.0-alpha.0.6-all.jar` | `server_performance` | Server | 🟢 Active | 🟢 Low | Optimizes vanilla 26.x lighting engine; fully integrated and concurrent with C2ME. |
| `ferritecore` | `ferritecore-9.0.0-fabric.jar` | `memory_optimization` | Both | 🟢 Active | 🟢 Low | Drastically reduces memory footprint for blockstates and models. Highly stable. |
| `modernfix` | `modernfix-5.27.20-build.2.jar` | `memory_optimization` | Both | 🟢 Active | 🟢 Low | Optimizes class loading, dynamic resource leaks, and overall RAM usage. |
| `krypton` | `krypton-0.3.2.jar` | `network_and_players` | Both | 🟢 Active | 🟢 Low | Optimizes Netty networking pipeline, minimizing CPU overhead during heavy client traffic. |
| `vmp` | `vmp-fabric-mc26.3-0.2.0+beta.7.238-all.jar` | `network_and_players` | Server | 🟢 Active | 🟢 Low | Very Many Players: entity tracking and packet dispatch optimization for multi-player loads. |
| `servercore` | `servercore-fabric-1.5.20+26.3.jar` | `network_and_players` | Server | 🟢 Active | 🟢 Low | Dynamic view/simulation distance and entity activation range limits during tick spikes. |
| `c2me` | `c2me-fabric-mc26.3-0.4.2-alpha.0.119.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟡 Medium | Concurrent Chunk Management Engine (parallel chunk I/O and terrain gen). Adjust worker threads in `config/c2me.toml` if high CPU/OOM occurs. |
| `zfastnoise` | `zfastnoise-1.1.3+26.3.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟢 Low | Replaces vanilla noise generation with vectorized FastNoise SIMD calculations. |
| `structure_layout_optimizer` | `structure_layout_optimizer-1.1.4+26.3-fabric.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟢 Low | Optimizes structure piece bounding box calculation during terrain generation. |
| `chunky` | `Chunky-Fabric-1.5.66.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟢 Low | Execute `/chunky start` to pre-generate world chunks before launching server to players. |
| `quick-pack` | `quick-pack-fabric-1.5.1+26.3.jar` | `worldgen_and_chunks` | Both | 🟢 Active | 🟢 Low | Accelerates zip file decompression for datapacks and resourcepacks during server boot. |
| `crashexploitfixer` | `crashexploitfixer-fabric-2.0.0+26.3.jar` | `bugfix_and_security` | Server | 🟢 Active | 🟢 Low | Hardens server against malicious NBT tag crash exploits (Paper backport). |
| `packetfixer` | `PacketFixer-fabric-3.3.6.jar` | `bugfix_and_security` | Both | 🟢 Active | 🟢 Low | Removes 2MB payload buffer limit; prevents player kick/disconnect on oversized NBT/inventory packets. |
| `debugify` | `debugify-26.3.0.0.jar` | `bugfix_and_security` | Both | 🟢 Active | 🟢 Low | Patches dozens of vanilla bugs; individual fixes configurable via `debugify.json`. |
| `letmedespawn` | `letmedespawn-fabric-26.3-1.26.11.1.jar` | `entity_mechanics` | Server | 🟢 Active | 🟢 Low | Allows mobs holding dropped items to despawn naturally, preventing cave/dungeon entity accumulation. |
| `clumps` | `Clumps-fabric-26.3-26.3.2.jar` | `entity_mechanics` | Both | 🟢 Active | 🟢 Low | Aggregates multiple XP orbs into a single clustered orb entity, eliminating XP-induced tick stalls. |
| `getittogetherdrops` | `getittogetherdrops-fabric-26.1.1-1.5.1.jar` | `entity_mechanics` | Both | 🟢 Active | 🟢 Low | Widens gravitational radius and accelerates item merging, preventing thousands of floating block drops from choking entity ticks. |
| `skeletonaifix` | `skeletonaifix-v26.3.0-mc26.3.x+fabric.jar` | `entity_mechanics` | Server | 🟢 Active | 🟢 Low | Fixes awkward skeleton strafing behavior to ensure smooth combat AI. |
| `asynclogger` | `asynclogger-2.2.3+26.1.2-fabric.jar` | `network_and_players` | Both | 🟢 Active | 🟢 Low | Offloads console and file logging I/O to background thread, preventing log-spam tick hitches. |
| `async` | `async-fabric-0.2.5+alpha-26.3.jar` | `experimental_threading` | Server | ⚪ Optional | 🔴 **High** | *(Moved to `mods_optional/`)*. Multi-threaded entity ticking (Alpha). Isolated to prevent `ConcurrentModificationException` and race conditions in dense mob encounters. |
| `distanthorizons` | `DistantHorizons-3.3.4-26.3-fabric-neoforge.jar` | `optional_client_side` | Client | ⚪ Optional | 🟡 Medium | *(Moved to `mods_optional/`)*. Level-of-Detail (LOD) generator. Isolated due to high SQLite I/O and CPU overhead on dedicated servers; recommend client-side deployment. |
| `ksyxis` | `Ksyxis-1.4.6.jar` | `optional_client_side` | Both | ⚪ Optional | 🟡 Medium | *(Moved to `mods_optional/`)*. Skips spawn chunks. Superseded natively by setting `spawn-chunk-radius=1` in `server.properties`. |

---

## 3. Automated Heuristics for AllayOps Agentic Plane

When the Self-Healing sub-systems (`ParryEngine`, `ToTPlanner`, `CanaryGate`) detect server telemetry anomalies or crash events, they query this knowledge base using the following triage rules:

1. **Trigger: `ConcurrentModificationException` or Thread Deadlock during Entity Tick:**
   - **Primary Suspect:** `async-fabric` (`id: async`).
   - **Remediation Action:** Verify `async-fabric-*.jar` is isolated in `mods_optional/`. If manually reactivated, evacuate it back to `mods_optional/` and restart the server instance.
2. **Trigger: Mass Explosion Freeze / Watchdog Alert:**
   - **Verification Target:** Verify `fabric-carpet` is active and rules `optimizedTNT=true` and `tntDoCollision=false` are enabled in `carpet.conf`.
3. **Trigger: `OutOfMemoryError` or Sustained 100% CPU Utilization during World Generation:**
   - **Primary Suspect:** `c2me` (`id: c2me`).
   - **Remediation Action:** Inspect `config/c2me.toml`, throttle `asyncCombinedChunkLoading`, and decrease `workerThreads` allocation.
4. **Trigger: Client Disconnection Event with Reason `"Packet buffer overflow"` or `"Payload too large":**
   - **Verification Target:** Ensure `packetfixer` is present and active in the runtime `mods/` directory.
5. **Trigger: Server TPS Drop caused by High Mob Density in Dungeons/Spawners:**
   - **Verification Target:** Verify `letmedespawn` and `clumps` are active, and tune `entity-activation-range` inside `config/servercore.toml`.
