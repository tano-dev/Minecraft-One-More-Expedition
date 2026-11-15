# Mod & Datapack Registry Knowledge Base

> **Standardized Architecture & Diagnostics Catalog** designed for the **AllayOps Agentic Self-Healing Platform** and server administrators of Minecraft: One More Expedition.

- **Minecraft Version:** `26.3`
- **Fabric Loader:** `0.20.1`
- **Java Runtime Environment:** OpenJDK 25 (G1GC enterprise-tuned)
- **Active Mods Count:** `60` (plus `3` optional)
- **Active Datapacks Count:** `49`
- **Machine-Readable Metadata:** [`mods_registry.json`](mods_registry.json)

---

## 1. Architectural Categories

| Category ID | Scope & Architectural Purpose |
| :--- | :--- |
| `core_library` | Foundational abstraction layers, APIs, and declarative configuration libraries required by dependent mods. |
| `server_performance` | Main tick loop optimization, collision detection algorithms, lighting engine, and Redstone BFS engine. |
| `memory_optimization` | JVM heap reduction, model/blockstate footprint compression, and dynamic class-loading optimizations. |
| `network_and_players` | Netty pipeline streamlining, packet batching, and high-concurrency entity tracking for multi-player environments. |
| `worldgen_and_chunks` | Parallel chunk I/O, SIMD-accelerated noise calculation, structure layout optimization, and pre-generation tools. |
| `bugfix_and_security` | Backported security patches against malformed NBT exploits, packet buffer limits, and anti-duplication. |
| `entity_mechanics` | Entity despawn persistence cleanup, orb clustering, and combat AI tuning to prevent unintended mob accumulation. |
| `server_features` | Custom server features: Expedition Sidebar, Expedition Skyblock, ItemFlexer, EconomyCraft economy engine. |
| `player_utilities` | VoiceChat proximity audio, inventory inspection GUI, player skin restoration, and Spark live profiler. |
| `worldgen_structures` | Content mods adding custom biomes, structures, villages, temples, and natural roads. |
| `experimental_threading` | **Multi-threaded entity ticking (Alpha)** — High risk; isolated in `mods_optional/`. |
| `optional_client_side` | Client-only visual enhancers (LOD streaming) or redundant mods superseded by native server settings. |

---

## 2. Complete Fabric Mod Classification Matrix (63 mods)

| Mod ID | Jar Filename | Category | Side | Status | Risk | Diagnostics & Remediation Rule |
| :--- | :--- | :--- | :---: | :---: | :---: | :--- |
| `adorabuild_structures` | `adorabuild-structures-2.12.0-fabric-26.3.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `almanac` | `almanac-fabric-26.3-1.26.11.1.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `alternate-current` | `alternate-current-mc26.3-1.9.0.jar` | `server_performance` | Server | 🟢 Active | 🟢 Low | Essential server tick, lighting, and redstone optimization. |
| `async` | `async-fabric-0.2.5+alpha-26.3.jar` | `experimental_threading` | Server | ⚪ Optional | 🔴 **High** | Isolated in mods_optional/. Multi-threaded entity ticking (Alpha); high risk of race conditions. |
| `asynclogger` | `asynclogger-2.2.3+26.1.2-fabric.jar` | `network_and_players` | Server | 🟢 Active | 🟢 Low | Networking pipeline and high-concurrency player tracking optimization. |
| `balm` | `balm-fabric-26.3-26.3.0.3.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `c2me` | `c2me-fabric-mc26.3-0.4.2-alpha.0.119.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟡 Medium | Parallel chunk I/O and terrain noise calculation engine. |
| `carpet` | `fabric-carpet-26.3+v260915.jar` | `server_performance` | Server | 🟢 Active | 🟢 Low | Essential server tick, lighting, and redstone optimization. |
| `chunky` | `Chunky-Fabric-1.5.66.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟢 Low | Parallel chunk I/O and terrain noise calculation engine. |
| `cloth-config` | `cloth-config-fabric-26.3.159.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `clumps` | `Clumps-fabric-26.3-26.3.2.jar` | `entity_mechanics` | Both | 🟢 Active | 🟢 Low | Entity despawn persistence, item merging, and mob AI stability. |
| `crashexploitfixer` | `crashexploitfixer-fabric-2.0.0+26.3.jar` | `bugfix_and_security` | Both | 🟢 Active | 🟢 Low | Backported security hardening and vanilla bug fixes. |
| `ctov` | `[fabric]ctov-26-3-4.2.2a.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `debugify` | `debugify-26.3.0.0.jar` | `bugfix_and_security` | Both | 🟢 Active | 🟢 Low | Backported security hardening and vanilla bug fixes. |
| `distanthorizons` | `DistantHorizons-3.3.4-26.3-fabric-neoforge.jar` | `optional_client_side` | Client | ⚪ Optional | 🟡 Medium | Isolated in mods_optional/. LOD chunk renderer with heavy SQLite I/O; recommended client-side only. |
| `economycraft` | `economycraft-fabric-1.11.0_26.3.jar` | `server_features` | Server | 🟢 Active | 🟢 Low | Core server custom gameplay feature (Expedition system). |
| `expedition-sidebar` | `expedition-sidebar-1.0.0.jar` | `server_features` | Server | 🟢 Active | 🟢 Low | Core server custom gameplay feature (Expedition system). |
| `expedition-skyblock` | `expedition-skyblock-1.0.0.jar` | `server_features` | Server | 🟢 Active | 🟢 Low | Core server custom gameplay feature (Expedition system). |
| `fabric-api` | `fabric-api-0.166.2+26.3.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `ferritecore` | `ferritecore-9.0.0-fabric.jar` | `memory_optimization` | Both | 🟢 Active | 🟢 Low | Heap reduction, blockstate compression, and dynamic class loading optimization. |
| `forgeconfigapiport` | `ForgeConfigAPIPort-v26.3.1-mc26.3.x-Fabric.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `formations` | `formations-1.0.4-fabric-mc26.3.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `getittogetherdrops` | `getittogetherdrops-fabric-26.1.1-1.5.1.jar` | `entity_mechanics` | Both | 🟢 Active | 🟢 Low | Entity despawn persistence, item merging, and mob AI stability. |
| `invview` | `InvView-1.4.22-26.3+.jar` | `player_utilities` | Both | 🟢 Active | 🟢 Low | Player utility, audio networking, and diagnostics. |
| `itemflexer` | `itemflexer-2.0.0.jar` | `server_features` | Server | 🟢 Active | 🟢 Low | Core server custom gameplay feature (Expedition system). |
| `krypton` | `krypton-0.3.2.jar` | `network_and_players` | Server | 🟢 Active | 🟢 Low | Networking pipeline and high-concurrency player tracking optimization. |
| `ksyxis` | `Ksyxis-1.4.6.jar` | `optional_client_side` | Both | ⚪ Optional | 🟡 Medium | Isolated in mods_optional/. Skips spawn chunk loading; superseded natively by spawn-chunk-radius=1. |
| `letmedespawn` | `letmedespawn-fabric-26.3-1.26.11.1.jar` | `entity_mechanics` | Both | 🟢 Active | 🟢 Low | Entity despawn persistence, item merging, and mob AI stability. |
| `lithium` | `lithium-fabric-0.26.3+mc26.3.jar` | `server_performance` | Server | 🟢 Active | 🟢 Low | Essential server tick, lighting, and redstone optimization. |
| `lithostitched` | `lithostitched-2.0.6-fabric-26.3.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟢 Low | Parallel chunk I/O and terrain noise calculation engine. |
| `mes` | `MoogsEndStructures-universal-1.21-2.1.1.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `mmr` | `MoogsMineshaftsReimagined-universal-1.21-1.1.0.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `mmv` | `MoogsMissingVillages-universal-1.21-2.2.0.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `mns` | `MoogsNetherStructures-universal-1.21-3.1.1.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `modernfix` | `modernfix-5.27.20-build.2.jar` | `memory_optimization` | Both | 🟢 Active | 🟢 Low | Heap reduction, blockstate compression, and dynamic class loading optimization. |
| `moogs_paths` | `MoogsPaths-fabric-26.3-1.2.0.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `moogs_structures` | `MoogsStructureLib-fabric-26.3-3.4.2.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `mr_vanilla_fireflies` | `vanilla-fireflies-5.1.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `mss` | `MoogsSoaringStructures-universal-1.21-2.2.0.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `mtr` | `MoogsTemplesReimagined-universal-1.21-2.0.4.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `mvs` | `MoogsVoyagerStructures-universal-1.21-5.1.3.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `netherportalfix` | `netherportalfix-fabric-26.3-26.3.0.1.jar` | `bugfix_and_security` | Both | 🟢 Active | 🟢 Low | Backported security hardening and vanilla bug fixes. |
| `nochatreports` | `NoChatReports-FABRIC-26.3-v2.21.0.jar` | `bugfix_and_security` | Both | 🟢 Active | 🟢 Low | Backported security hardening and vanilla bug fixes. |
| `packetfixer` | `PacketFixer-fabric-3.3.6.jar` | `bugfix_and_security` | Both | 🟢 Active | 🟢 Low | Backported security hardening and vanilla bug fixes. |
| `panda-anti-dupe` | `panda-anti-dupe-1.1.4+26.3.jar` | `bugfix_and_security` | Both | 🟢 Active | 🟢 Low | Backported security hardening and vanilla bug fixes. |
| `puzzleslib` | `puzzleslib-v26.3.18-mc26.3.x+fabric.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `quick-pack` | `quick-pack-fabric-1.5.1+26.3.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟢 Low | Parallel chunk I/O and terrain noise calculation engine. |
| `resourcefulconfig` | `ResourcefulConfig-6.0.2.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `scalablelux` | `ScalableLux-fabric-mc26.3-0.3.0-alpha.0.6-all.jar` | `server_performance` | Server | 🟢 Active | 🟢 Low | Essential server tick, lighting, and redstone optimization. |
| `servercore` | `servercore-fabric-1.5.20+26.3.jar` | `network_and_players` | Server | 🟢 Active | 🟢 Low | Networking pipeline and high-concurrency player tracking optimization. |
| `skeletonaifix` | `skeletonaifix-v26.3.0-mc26.3.x+fabric.jar` | `entity_mechanics` | Both | 🟢 Active | 🟢 Low | Entity despawn persistence, item merging, and mob AI stability. |
| `skinrestorer` | `skinrestorer-2.11.1+26.3-fabric.jar` | `player_utilities` | Both | 🟢 Active | 🟢 Low | Player utility, audio networking, and diagnostics. |
| `spark` | `spark-1.10.187-fabric.jar` | `player_utilities` | Both | 🟢 Active | 🟢 Low | Player utility, audio networking, and diagnostics. |
| `sparsestructures` | `sparsestructures-fabric-26.3-3.1.5.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `spiral_tower_village` | `spiral_tower_village-0.0.4.jar` | `worldgen_structures` | Both | 🟢 Active | 🟢 Low | World generation structure and village overhaul. |
| `structure_layout_optimizer` | `structure_layout_optimizer-1.1.4+26.3-fabric.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟢 Low | Parallel chunk I/O and terrain noise calculation engine. |
| `tectonic` | `tectonic-3.0.32-fabric-26.3.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟢 Low | Parallel chunk I/O and terrain noise calculation engine. |
| `unloadedactivity` | `unloadedactivity-0.7.3+mc26.3.jar` | `entity_mechanics` | Both | 🟢 Active | 🟢 Low | Entity despawn persistence, item merging, and mob AI stability. |
| `vmp` | `vmp-fabric-mc26.3-0.2.0+beta.7.238-all.jar` | `network_and_players` | Server | 🟢 Active | 🟢 Low | Networking pipeline and high-concurrency player tracking optimization. |
| `voicechat` | `voicechat-fabric-2.6.27+26.3.jar` | `player_utilities` | Both | 🟢 Active | 🟢 Low | Player utility, audio networking, and diagnostics. |
| `wits` | `wits-1.4.3+26.3-fabric.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `zconfig` | `zconfig-1.0.0+26.x.jar` | `core_library` | Both | 🟢 Active | 🟢 Low | Foundational API / config library. Do not remove. |
| `zfastnoise` | `zfastnoise-1.1.3+26.3.jar` | `worldgen_and_chunks` | Server | 🟢 Active | 🟢 Low | Parallel chunk I/O and terrain noise calculation engine. |

---

## 3. Complete World Datapacks Matrix (49 datapacks)

All datapacks in this repository have been normalized and validated against Minecraft 26.3 schema standards (`pack_format: 94`, `min_format: 94`, `max_format: 94`).

| Datapack ID | Version / Details | Pack Format | Architectural Role / Scope |
| :--- | :--- | :---: | :--- |
| `[26.3-26.4]witherswrath-1.17` | The Wither Revamp! Boss Overhaul | `94` | Worldgen & Content Overhaul |
| `ATi Structure V1.4.7 (26.3)` | Adds more Witch Huts | `94` | Worldgen & Content Overhaul |
| `ATi Structures Vanilla V1.4.7 (26.3)` | Adds more Witch Huts | `94` | Worldgen & Content Overhaul |
| `Decubed Dungeons V1.13.1 [26.3]` | Decubed Dungeons V1.13.1 [26.3] | `94` | Worldgen & Content Overhaul |
| `DnT Ancient City Overhaul v4` | Structure datapack overhauling the Ancient City | `94` | Worldgen & Content Overhaul |
| `DnT Desert Temple Overhaul v2.2` | Structure datapack overhauling the Desert Temple | `94` | Worldgen & Content Overhaul |
| `DnT Jungle Temple Overhaul v2.2` | Structure datapack overhauling the Jungle Temple | `94` | Worldgen & Content Overhaul |
| `DnT Mineshaft Overhaul 2.0.1` | Structure datapack overhauling the Mineshafts | `94` | Worldgen & Content Overhaul |
| `DnT Nether Fortress Overhaul 3.2.1` | Structure datapack overhauling the Nether Fortress | `94` | Worldgen & Content Overhaul |
| `DnT Ocean Monument Overhaul v2.3` | Structure datapack overhauling the Ocean Monument | `94` | Worldgen & Content Overhaul |
| `DnT Swamp Hut Overhaul v3` | Structure datapack overhauling the Swamp Hut | `94` | Worldgen & Content Overhaul |
| `Dungeons and Taverns v6.1.0` | Structure datapack adding unique new structures in the vanilla style | `94` | Worldgen & Content Overhaul |
| `Epic Dungeons v1.2.7 (26.3)` | Adds more Witch Huts | `94` | Worldgen & Content Overhaul |
| `Epic Jungle Temples v1.0.4 (26.3)` | Adds more Witch Huts | `94` | Worldgen & Content Overhaul |
| `Epic Villages 2.0.0` | Makes Default Villages Beautiful | `94` | Worldgen & Content Overhaul |
| `Epic Villages Standalone 1.0.0.jar` | Adds Beautiful Villages | `94` | Worldgen & Content Overhaul |
| `Epic Witch Huts v1.3.2 (26.3)` | Adds more Witch Huts | `94` | Worldgen & Content Overhaul |
| `Explorify v1.6.6.dp` | Explorify v1.6.6 A structure data pack | `94` | Worldgen & Content Overhaul |
| `Grim kingdoms (1.21+ 26.2+)` | This mod adds wonderful castles | `94` | Worldgen & Content Overhaul |
| `Incendium_Legacy_v5.5.4+26.3` | Incendium - Legacy Edition - Nether Expansion Created by Stardust Labs | `94` | Worldgen & Content Overhaul |
| `Katters Structures v2.9` | Katters Structures Adds brand new vanilla like Structures Created by Katter 21 | `94` | Worldgen & Content Overhaul |
| `Nullscape_v2.0.1+26.3` | Nullscape - End Reborn Created by Stardust Labs | `94` | Worldgen & Content Overhaul |
| `Stellarity-6.0.1` | This ender expansion is kinda beyond hardcore :drum: | `94` | Worldgen & Content Overhaul |
| `Structory_Towers_v1.0.19+26.3` | Structory: Towers Created by Stardust Labs | `94` | Worldgen & Content Overhaul |
| `Structory_v1.3.18+26.3` | Structory Created by Stardust Labs | `94` | Worldgen & Content Overhaul |
| `Terralith_v2.6.5+26.3` | Terralith - Overworld Evolved Created by Stardust Labs | `94` | Worldgen & Content Overhaul |
| `Too Expensive Extractor v1.0.0 [26.3]` | Too Expensive Extractor -  By github.com/lullaby6 | `94` | Worldgen & Content Overhaul |
| `Trek 26.3 B0.6.2.1` | Trek 26.3 | `94` | Worldgen & Content Overhaul |
| `Wabi-Sabi Structures-3.0.5-1.21.11 Datapack` | A collection of unique, quality, imperfect structures | `94` | Worldgen & Content Overhaul |
| `Yggdrasil-7.1.0` | Yggdrasil-7.1.0 | `94` | Worldgen & Content Overhaul |
| `ddd-v.1.0.6-MC-26.3` | Nightmare fuel | `94` | Worldgen & Content Overhaul |
| `enchantments_encore` | Enchantments Encore by Niceron | `94` | Worldgen & Content Overhaul |
| `expedition_scarce_ores` | Expedition Scarce Ores - Reduces natural diamond, ancient debris, iron, gold, and emera... | `94` | Worldgen & Content Overhaul |
| `hopobettermineshaft-26-3-1-3-8` | More and better mineshafts to explore | `94` | Worldgen & Content Overhaul |
| `hopobetterruinedportals-26-3-1-5-2` | Improve how the portals look in your world | `94` | Worldgen & Content Overhaul |
| `hopobetterunderwaterruins-26-3-1-2-9` | Explore new ocean ruins to your world | `94` | Worldgen & Content Overhaul |
| `medieval_buildings-1.2.0` | Medieval Buildings for Minecraft 26.3 | `94` | Worldgen & Content Overhaul |
| `medieval_end-1.2.1` | Medieval Buildings [The End Edition] for Minecraft 26.3 | `94` | Worldgen & Content Overhaul |
| `medieval_nether-1.2.1` | Medieval Buildings [The Nether Edition] for Minecraft 26.3 | `94` | Worldgen & Content Overhaul |
| `mine-treasure-1.6.5` | Mine Treasure v1.6.5 by Frozytime | `94` | Worldgen & Content Overhaul |
| `more_mobs-v1.5.11-mc1.14-26.3.9-datapack` | Tschipcraft's More Mobs  └ v1.5.11 ● mc1.14-26.3.9 | `94` | Worldgen & Content Overhaul |
| `sunken_spires_1.20` | Remnants of an advanced prismarine civilization | `94` | Worldgen & Content Overhaul |
| `t_and_t-datapack-26.3` | T&T 26.3 | `94` | Worldgen & Content Overhaul |
| `tectonic-datapack-3.0.29` | <3Apollo | `94` | Worldgen & Content Overhaul |
| `terratonic-datapack-3.0.31` | <3Apollo | `94` | Worldgen & Content Overhaul |
| `tidal_towns_2.0_1.20-26.3` | tidal_towns_2.0_1.20-26.3 | `94` | Worldgen & Content Overhaul |
| `tru.e-ending-1.1.4d` |  | `94` | Worldgen & Content Overhaul |
| `u_sea-1.2.5` | Additional structures for the sea and beach biomes, focusing on an immersive expansion ... | `94` | Worldgen & Content Overhaul |
| `veinminer-enchantment-1.3.6` | §6§lVeinminer Enchantment §7/ §626.3+ §eBy Miraculixx | `94` | Worldgen & Content Overhaul |

---

## 4. Automated Heuristics for AllayOps Agentic Plane

When the Self-Healing sub-systems (`ParryEngine`, `ToTPlanner`, `CanaryGate`) detect server telemetry anomalies or crash events, they query this knowledge base using the following triage rules:

1. **Trigger: `ConcurrentModificationException` or Thread Deadlock during Entity Tick:**
   - **Target:** `experimental_threading` (`async`).
   - **Remediation:** Verify `async-fabric` remains quarantined in `mods_optional/`. Never activate on production multiplayer.

2. **Trigger: Out-Of-Memory (OOM) or Native Memory Allocation Failure (`malloc` failed / `The paging file is too small`):**
   - **Target:** JVM Garbage Collector and Virtual Memory.
   - **Remediation:** Enforce G1GC (`MC_GC=G1GC`) with `-Xms2G -Xmx3584M` in `D:\AllayOps\.env`. Verify 32GB pagefile active on `D:\pagefile.sys`.

3. **Trigger: High Tick Lag / Worldgen Choke during Chunk Generation:**
   - **Target:** `c2me` / `sparsestructures`.
   - **Remediation:** Adjust worker thread count in `config/c2me.toml`. Pre-generate chunks with `/chunky start`.

4. **Trigger: Missing Structure / Tag Validation Errors:**
   - **Target:** Datapack schema mismatch.
   - **Remediation:** Check `pack.mcmeta` files for `pack_format: 94`. Ensure custom structures register required template pools.
