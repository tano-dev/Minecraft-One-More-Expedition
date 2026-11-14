# Optional & Isolated Mods (`mods_optional`)

This directory houses mods that are intentionally excluded from the production server runtime (`mods/`) during synchronization to `AllayOps`.

### Current Contents:

1. **`async-fabric-0.2.5+alpha-26.3.jar`**
   - **Reason for Isolation:** Multi-threaded entity ticking (Alpha state). Prone to `ConcurrentModificationException` (CME) and race conditions when large mob groups (100+) congregate in dungeons and spawners. The server relies on the proven, stable combination of `Lithium` + `ServerCore` + `LetMeDespawn` instead.
2. **`DistantHorizons-3.3.4-26.3-fabric-neoforge.jar`**
   - **Reason for Isolation:** Client-side visual Level-of-Detail (LOD) mod. Running it on a dedicated server introduces unnecessary CPU and SQLite database disk I/O overhead unless specifically streaming to players with DH clients. Recommended for client installations only.
3. **`Ksyxis-1.4.6.jar`**
   - **Reason for Isolation:** Skips spawn chunks. Superseded natively by configuring `spawn-chunk-radius=1` in `server.properties` without injecting Mixins into chunk loading pipelines.
