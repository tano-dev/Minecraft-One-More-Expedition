# `expedition-dragon` Opaque-Box E2E Test Suite

Strictly opaque-box E2E test infrastructure for `expedition-dragon` (`MC 26.3`, `Java 25`, `Fabric Loader 0.20.1`, `Fabric API 0.166.2+26.3`).
Zero compile-time or source dependency on `mods_src/expedition-dragon/`.

## Running Tests

```powershell
# Verify harness self-check (JAR/bytecode/config/datapack/RCON/registry + JUnit 5 + Fabric GameTest)
python d:\Minecraft-One-More-Expedition\tests\expedition-dragon-e2e\run_e2e.py --self-check

# Run all tiers or a single tier
python d:\Minecraft-One-More-Expedition\tests\expedition-dragon-e2e\run_e2e.py --tier all --json-out report.json
python d:\Minecraft-One-More-Expedition\tests\expedition-dragon-e2e\run_e2e.py --tier 1 --feature F5

# Enforce full 253+ registry coverage (for E2E-M5 / TEST_READY.md gate)
python d:\Minecraft-One-More-Expedition\tests\expedition-dragon-e2e\run_e2e.py --tier all --require-full-registry
```

## Extension Guidelines for E2E-M2 / E2E-M3 / E2E-M4
- **Python tests**: Place `test_*.py` modules under `tier1/`, `tier2/`, `tier3/`, `tier4/` using stdlib `unittest` and `harness_py.*` helpers (`jar_inspector`, `config_inspector`, `datapack_inspector`, `mc_server_runner`, `java_runner`). Python wrappers in `tier*/test_*.py` can also invoke `java_runner.run_gametest(...)` or `mc_server_runner.SandboxServer` directly when testing custom CLI/config/profile scenarios.
- **Java JUnit 5 tests**: Place `@Tag("tier1".."tier4")` test classes under `harness-java/src/test/java/net/onemoreexpedition/e2e/tier{1..4}/` using `BootstrapSupport`, `ModJarBridge`, `E2EFixtures`, and `E2EOracles`.
- **Fabric GameTests**: Add `@GameTest(dimension = "minecraft:the_end", ...)` test classes under `harness-java/src/gametest/java/net/onemoreexpedition/e2e/tier{1..4}/` (keeping `SelfCheckGameTest.java` in `gametest/` root), register their entrypoints in `harness-java/src/gametest/resources/fabric.mod.json`, and prefix `@GameTest` method names with lowercase registry IDs (e.g., `t1_f05_01_...`, `t3_01_...`, `t4_01_...`). `run_e2e.py` automatically invokes `java_runner.run_gametest(...)` when `.java` files exist under `gametest/.../tier{N}/`.
- **Registry IDs**: Every implemented test must match its canonical ID in `registry.json` (`T1-F01-01..T1-F22-05`, `T2-F01-01..T2-F22-05`, `T3-01..T3-22`, `T4-01..T4-11`; underscores such as `test_T1_F01_01` or `t1_f05_01` are normalized automatically to hyphenated IDs).
