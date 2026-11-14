# Minecraft - One More Expedition

> **Modding & Datapacks Repository for [AllayOps](https://github.com/tano-dev/AllayOps)**

Repository này quản lý độc lập toàn bộ **Fabric Mods (`mods/`, `mods_src/`)** và **World Datapacks (`datapacks/`)** cho server **Minecraft - One More Expedition** (MC 26.3 / Fabric Loader 0.20.1), tách biệt khỏi hệ thống Agentic Self-Healing tại `AllayOps`.

---

## Cấu trúc thư mục

```text
Minecraft-One-More-Expedition/
├── mods_src/       # Mã nguồn Fabric Mod (Gradle / Java 25)
├── mods/           # File .jar mod đã build hoặc mod tuyển chọn
├── datapacks/      # Các datapack tùy chỉnh (đồng bộ vào world/datapacks/)
└── README.md
```

---

## Đồng bộ sang server `AllayOps`

- **Đồng bộ khi đang phát triển local (tại `D:\AllayOps`):**
  ```powershell
  python scripts/sync_content.py --local-source D:\Minecraft-One-More-Expedition
  ```
- **Đồng bộ từ GitHub:**
  ```powershell
  python scripts/sync_content.py
  ```
