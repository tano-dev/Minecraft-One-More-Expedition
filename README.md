# Minecraft - One More Expedition

> **Dedicated Modding & Datapacks Repository for [AllayOps](https://github.com/tano-dev/AllayOps)**

This repository independently maintains all **Fabric Mods (`mods/`, `mods_src/`)** and **World Datapacks (`datapacks/`)** for the **Minecraft - One More Expedition** server runtime (MC 26.3 / Fabric Loader 0.20.1 / Java 25 with G1GC), isolated from the Agentic Self-Healing control plane in `AllayOps`.

---

## 📊 Repository Summary

- **Minecraft Version:** `26.3`
- **Fabric Loader:** `0.20.1`
- **Java Runtime:** Eclipse Adoptium OpenJDK 25 (with enterprise G1GC)
- **Active Fabric Mods:** `60` mods in `mods/` (plus 3 optional mods in `mods_optional/`)
- **Active World Datapacks:** `49` normalized datapacks in `datapacks/` (Schema format 94)
- **Custom Native Fabric Mods:** `expedition-sidebar` (1.0.0), `expedition-skyblock` (1.0.0), `itemflexer` (2.0.0)

---

## 📂 Directory Structure

```text
Minecraft-One-More-Expedition/
├── mods/                 # 60 verified, production-grade Fabric server mods
├── mods_optional/        # Client-only or isolated experimental mods (Async, DH, Ksyxis)
├── config/               # Version-controlled configuration files for all mods
├── mods_src/             # Fabric mod source code (Gradle / Java 25)
│   ├── expedition-sidebar/
│   ├── expedition-skyblock/
│   └── itemflexer/
├── datapacks/            # 49 custom worldgen & overhaul datapacks (synced to world/datapacks/)
├── mods_registry.json    # Machine-readable registry for AI Agentic Self-Healing (v2.0.0)
├── MODS_CATALOG.md       # Full architectural classification & diagnostics matrix
└── README.md
```

---

## 🔄 Synchronization & Management

All runtime orchestration, server execution, chaos benchmarking, and automated world wipe/reset are centrally managed inside the **[AllayOps](https://github.com/tano-dev/AllayOps)** enterprise control plane:

- **Launch Server:**
  ```bash
  make server
  ```
  *(run inside `D:\AllayOps`)*

- **Reset World & Progress (Safe Confirmation):**
  ```bash
  make reset-world
  ```
  *(run inside `D:\AllayOps`; requires typing `reset` in terminal)*

- **Synchronize Mods & Datapacks:**
  ```bash
  make sync-local
  ```
  *(run inside `D:\AllayOps`)*

---

## 🧠 Mod Architecture & Agentic Self-Healing

All server optimization mods and datapacks are cataloged, classified by category, and tagged with risk levels to allow autonomous diagnostic agents in `AllayOps` (`ParryEngine`, `ToTPlanner`, `CanaryGate`) to perform root-cause analysis and automated self-healing.

- 📖 **Comprehensive Catalog & Diagnostics:** [`MODS_CATALOG.md`](MODS_CATALOG.md)
- 🤖 **Machine-Readable Metadata:** [`mods_registry.json`](mods_registry.json)
