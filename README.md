# Minecraft - One More Expedition

> **Dedicated Modding & Datapacks Repository for [AllayOps](https://github.com/tano-dev/AllayOps)**

This repository independently maintains all **Fabric Mods (`mods/`, `mods_src/`)** and **World Datapacks (`datapacks/`)** for the **Minecraft - One More Expedition** server runtime (MC 26.3 / Fabric Loader 0.20.1 / Java 25 with ZGC), isolated from the Agentic Self-Healing control plane in `AllayOps`.

---

## Directory Structure

```text
Minecraft-One-More-Expedition/
├── mods/                 # Verified, production-grade Fabric server mods
├── mods_optional/        # Client-only, redundant, or isolated experimental mods
├── mods_src/             # Fabric mod source code (Gradle / Java 25)
├── datapacks/            # Custom world datapacks (synced to world/datapacks/)
├── mods_registry.json    # Machine-readable mod registry for AI Agentic Self-Healing
├── MODS_CATALOG.md       # Diagnostic matrix and categorization documentation
└── README.md
```

---

## Mod Architecture & Diagnostics

All server optimization mods are cataloged, classified by tier, and tagged with risk levels to allow autonomous diagnostic agents in `AllayOps` to perform root-cause analysis and automated self-healing.

- Detailed documentation: [`MODS_CATALOG.md`](MODS_CATALOG.md)
- Machine-readable registry: [`mods_registry.json`](mods_registry.json)

---

## Synchronization to `AllayOps` Server Runtime

- **Local Development Sync (from `D:\AllayOps`):**
  ```powershell
  python scripts/sync_content.py --local-source D:\Minecraft-One-More-Expedition
  ```

- **Remote Synchronization (from GitHub):**
  ```powershell
  python scripts/sync_content.py
  ```
