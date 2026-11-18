"""Built-in --self-check suite validating all harness inspectors, registry, RCON, and harness-java."""

from __future__ import annotations

import gzip
import json
import socket
import struct
import sys
import tempfile
import threading
import time
import unittest
from unittest import mock
import zipfile
from pathlib import Path
from typing import Any

from harness_py import (
    config_inspector,
    datapack_inspector,
    jar_inspector,
    java_runner,
    mc_server_runner,
)

SUITE_ROOT = Path(__file__).resolve().parent.parent
REGISTRY_PATH = SUITE_ROOT / "registry.json"


def _build_minimal_class_bytes(utf8_strings: list[str], major_version: int = 69) -> bytes:
    """Synthesize a valid minimal JVM classfile (with Constant_Utf8 + Constant_String entries)."""
    # CP layout:
    # 1: Utf8 "test/Synthetic"
    # 2: Class #1
    # 3: Utf8 "java/lang/Object"
    # 4: Class #3
    # For each s in utf8_strings: Utf8(s) + String(#utf8)
    cp_chunks: list[bytes] = []

    def u8_entry(s: str) -> bytes:
        b = s.encode("utf-8")
        return struct.pack(">BH", 1, len(b)) + b

    cp_chunks.append(u8_entry("test/Synthetic"))  # 1
    cp_chunks.append(struct.pack(">BH", 7, 1))    # 2
    cp_chunks.append(u8_entry("java/lang/Object"))  # 3
    cp_chunks.append(struct.pack(">BH", 7, 3))    # 4

    idx = 5
    for s in utf8_strings:
        cp_chunks.append(u8_entry(s))
        cp_chunks.append(struct.pack(">BH", 8, idx))
        idx += 2

    cp_count = idx
    header = struct.pack(">IHHH", 0xCAFEBABE, 0, major_version, cp_count)
    # access_flags, this_class=2, super_class=4, interfaces=0, fields=0, methods=0, attrs=0
    tail = struct.pack(">HHHHHHH", 0x0021, 2, 4, 0, 0, 0, 0)
    return header + b"".join(cp_chunks) + tail


def _build_synthetic_jar(
    path: Path,
    mod_id: str = "expedition-dragon",
    depends: dict[str, str] | None = None,
    mixin_compat: str = "JAVA_25",
    include_mixin_class: bool = True,
    string_literals: list[str] | None = None,
    major_version: int = 69,
) -> None:
    if depends is None:
        depends = {
            "fabricloader": ">=0.20.1",
            "minecraft": "~26.3",
            "java": ">=25",
            "fabric-api": "*",
        }
    mod_json = {
        "schemaVersion": 1,
        "id": mod_id,
        "version": "1.0.0",
        "entrypoints": {"main": ["net.onemoreexpedition.dragon.ExpeditionDragonMod"]},
        "mixins": ["expedition-dragon.mixins.json"],
        "depends": depends,
    }
    mixin_json = {
        "required": True,
        "minVersion": "0.8",
        "package": "net.onemoreexpedition.dragon.mixin",
        "compatibilityLevel": mixin_compat,
        "mixins": ["EnderDragonMixin"],
        "injectors": {"defaultRequire": 1},
    }
    main_cls = _build_minimal_class_bytes(string_literals or ["Dragon awakened!"], major_version=major_version)
    mixin_cls = _build_minimal_class_bytes(["mixin"], major_version=major_version)
    with zipfile.ZipFile(path, "w") as zf:
        zf.writestr("fabric.mod.json", json.dumps(mod_json))
        zf.writestr("expedition-dragon.mixins.json", json.dumps(mixin_json))
        zf.writestr("net/onemoreexpedition/dragon/ExpeditionDragonMod.class", main_cls)
        zf.writestr("assets/expedition-dragon/padding.bin", b"\x00" * 11_000)
        if include_mixin_class:
            zf.writestr("net/onemoreexpedition/dragon/mixin/EnderDragonMixin.class", mixin_cls)


def _encode_nbt_level_dat(enabled: list[str], disabled: list[str]) -> bytes:
    def nbt_str(s: str) -> bytes:
        b = s.encode("utf-8")
        return struct.pack(">H", len(b)) + b

    def nbt_str_list(name: str, items: list[str]) -> bytes:
        body = b"\x08" + struct.pack(">i", len(items)) + b"".join(nbt_str(x) for x in items)
        return b"\x09" + nbt_str(name) + body

    dp_compound = nbt_str_list("Enabled", enabled) + nbt_str_list("Disabled", disabled) + b"\x00"
    data_compound = b"\x0a" + nbt_str("DataPacks") + dp_compound + b"\x00"
    root = b"\x0a" + nbt_str("") + (b"\x0a" + nbt_str("Data") + data_compound) + b"\x00"
    return gzip.compress(root)


def _build_synthetic_datapacks(root: Path, deconflicted: bool = True, intact: bool = True) -> tuple[Path, Path]:
    dp = root / "datapacks"
    te = dp / "tru.e-ending-1.1.4d"
    (te / "data/minecraft/tags/function").mkdir(parents=True)
    (te / "data/true_ending/function").mkdir(parents=True)
    (te / "pack.mcmeta").write_text('{"pack":{"pack_format":94,"description":"te"}}', encoding="utf-8")
    if deconflicted:
        (te / "data/minecraft/tags/function/load.json").write_text('{"replace":false,"values":[]}', encoding="utf-8")
        (te / "data/minecraft/tags/function/tick.json").write_text('{"replace":false,"values":[]}', encoding="utf-8")
        (te / "data/true_ending/function/load.mcfunction").write_text("# disabled\n", encoding="utf-8")
        (te / "data/true_ending/function/tick.mcfunction").write_text("# disabled\n", encoding="utf-8")
        level_bytes = _encode_nbt_level_dat(["vanilla", "file/Stellarity-6.0.1"], ["file/tru.e-ending-1.1.4d"])
    else:
        (te / "data/minecraft/tags/function/load.json").write_text('{"replace":false,"values":["true_ending:load"]}', encoding="utf-8")
        (te / "data/minecraft/tags/function/tick.json").write_text('{"replace":false,"values":["true_ending:tick"]}', encoding="utf-8")
        (te / "data/true_ending/function/load.mcfunction").write_text("scoreboard objectives add trueEnding_x dummy\n", encoding="utf-8")
        (te / "data/true_ending/function/tick.mcfunction").write_text("execute as @e[type=ender_dragon] run function true_ending:boss/a_main\n", encoding="utf-8")
        level_bytes = _encode_nbt_level_dat(["vanilla", "file/Stellarity-6.0.1", "file/tru.e-ending-1.1.4d"], [])

    level_dat = root / "level.dat"
    level_dat.write_bytes(level_bytes)

    st = dp / "Stellarity-6.0.1"
    st.mkdir(parents=True, exist_ok=True)
    (st / "pack.mcmeta").write_text('{"id":"stellarity","pack":{"pack_format":94}}', encoding="utf-8")
    (st / "data/minecraft/tags/function").mkdir(parents=True)
    (st / "data/minecraft/tags/function/load.json").write_text('{"values":["kohara:load","stellarity:init"]}', encoding="utf-8")
    (st / "data/minecraft/tags/function/tick.json").write_text('{"values":["kohara:main","stellarity:main"]}', encoding="utf-8")

    (st / "data/stellarity/function/config").mkdir(parents=True)
    ed_val = "0" if deconflicted else "1"
    (st / "data/stellarity/function/config/init.mcfunction").write_text(
        f"scoreboard players set #stellarity.config stellarity.config.enable_ender_dragon {ed_val}\n",
        encoding="utf-8",
    )

    main_lines = [
        "function stellarity:structure/exit_portal/replace",
        "function stellarity:entity/end_city/crystal/main",
        "function stellarity:entity/end_city/marker_init",
    ]
    if deconflicted:
        main_lines.append("execute if score #stellarity.config stellarity.config.enable_ender_dragon matches 1 run schedule function stellarity:entity/dragon/butcher/scheduled 1t append")
    else:
        main_lines.append("execute if entity @e[type=ender_dragon] run schedule function stellarity:entity/dragon/butcher/scheduled 1t append")
    (st / "data/stellarity/function/main.mcfunction").write_text("\n".join(main_lines) + "\n", encoding="utf-8")

    # End Altar
    (st / "data/stellarity/structure/exit_portal").mkdir(parents=True)
    (st / "data/stellarity/structure/exit_portal/activated.nbt").write_bytes(b"nbt")
    (st / "data/stellarity/structure/exit_portal/deactivated.nbt").write_bytes(b"nbt")
    for fn_rel in (
        "data/stellarity/function/structure/exit_portal/main.mcfunction",
        "data/stellarity/function/structure/exit_portal/replace.mcfunction",
        "data/stellarity/function/structure/exit_portal/activated/generate.mcfunction",
        "data/stellarity/function/structure/exit_portal/deactivated/generate.mcfunction",
        "data/stellarity/function/structure/exit_portal/gateway/choose_location.mcfunction",
    ):
        p = st / fn_rel
        p.parent.mkdir(parents=True, exist_ok=True)
        txt = "function stellarity:post_gen/initialize\n" if fn_rel.endswith("exit_portal/main.mcfunction") else "# ok\n"
        p.write_text(txt, encoding="utf-8")

    # Custom items (491 fns, 156 loot tables, ender_dragon loot)
    (st / "data/stellarity/function/item").mkdir(parents=True)
    (st / "data/stellarity/function/item/main.mcfunction").write_text("# item main\n", encoding="utf-8")
    for i in range(490):
        (st / f"data/stellarity/function/item/i_{i}.mcfunction").write_text("# item\n", encoding="utf-8")
    (st / "data/stellarity/loot_table/item").mkdir(parents=True)
    for i in range(156):
        (st / f"data/stellarity/loot_table/item/i_{i}.json").write_text('{"pools":[]}', encoding="utf-8")
    (st / "data/stellarity/loot_table/entity").mkdir(parents=True)
    (st / "data/stellarity/loot_table/entity/ender_dragon.json").write_text(
        '{"pools":[{},{},{},{}]}', encoding="utf-8"
    )

    # 24 biomes
    (st / "data/stellarity/worldgen/biome").mkdir(parents=True)
    biome_count = 24 if intact else 23
    for i in range(biome_count):
        (st / f"data/stellarity/worldgen/biome/b_{i}.json").write_text('{"has_precipitation":false}', encoding="utf-8")

    # End Cities
    (st / "data/stellarity/loot_table/end_city").mkdir(parents=True)
    for i in range(39):
        (st / f"data/stellarity/loot_table/end_city/ec_{i}.json").write_text('{"pools":[]}', encoding="utf-8")
    (st / "data/stellarity/structure/end_city").mkdir(parents=True)
    for i in range(58):
        (st / f"data/stellarity/structure/end_city/s_{i}.nbt").write_bytes(b"nbt")
    (st / "data/minecraft/structure/end_city").mkdir(parents=True)
    for i in range(20):
        (st / f"data/minecraft/structure/end_city/m_{i}.nbt").write_bytes(b"nbt")

    # Altar of the Accursed
    (st / "data/stellarity/structure/altar_of_the_accursed.nbt").write_bytes(b"nbt")
    (st / "data/stellarity/function/post_gen").mkdir(parents=True)
    (st / "data/stellarity/function/post_gen/initialize.mcfunction").write_text(
        "place template stellarity:altar_of_the_accursed 42 67 -48\n", encoding="utf-8"
    )
    (st / "data/stellarity/function/mechanic/altar_of_the_accursed/crafting").mkdir(parents=True)
    for name in ("main.mcfunction", "main_activated.mcfunction", "place.mcfunction", "crafting/checks.mcfunction"):
        (st / "data/stellarity/function/mechanic/altar_of_the_accursed" / name).write_text("# altar\n", encoding="utf-8")
    (st / "data/stellarity/advancement/altar_of_the_accursed").mkdir(parents=True)
    for i in range(6):
        (st / f"data/stellarity/advancement/altar_of_the_accursed/a_{i}.json").write_text("{}", encoding="utf-8")
    (st / "data/stellarity/advancement/event").mkdir(parents=True)
    (st / "data/stellarity/advancement/event/place_altar_of_the_accursed.json").write_text("{}", encoding="utf-8")

    # left_click crystal
    (st / "data/stellarity/function/mechanic/end_crystal/action").mkdir(parents=True)
    (st / "data/stellarity/function/mechanic/end_crystal/action/left_click.mcfunction").write_text(
        "# safe\n", encoding="utf-8"
    )
    return dp, level_dat


class HarnessSelfCheckTests(unittest.TestCase):
    def test_01_registry_completeness(self) -> None:
        self.assertTrue(REGISTRY_PATH.is_file(), "registry.json must exist")
        reg = json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))
        tests = reg.get("tests", [])
        self.assertGreaterEqual(len(tests), 253)

        ids = [t["id"] for t in tests]
        self.assertEqual(len(ids), len(set(ids)), "Duplicate IDs in registry.json")

        for f_num in range(1, 23):
            feat = f"F{f_num}"
            t1 = [t for t in tests if t["tier"] == 1 and t["feature"] == feat]
            t2 = [t for t in tests if t["tier"] == 2 and t["feature"] == feat]
            self.assertGreaterEqual(len(t1), 5, f"{feat} Tier 1 count < 5")
            self.assertGreaterEqual(len(t2), 5, f"{feat} Tier 2 count < 5")

        self.assertGreaterEqual(sum(1 for t in tests if t["tier"] == 3), 22)
        self.assertGreaterEqual(sum(1 for t in tests if t["tier"] == 4), 11)

    def test_02_jar_inspector_accepts_valid_and_rejects_violations(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            valid_jar = tmp_dir / "valid.jar"
            _build_synthetic_jar(valid_jar)
            res = jar_inspector.inspect_jar_full(valid_jar)
            self.assertTrue(res["ok"], f"Valid synthetic JAR failed: {res['errors']}")

            # Dual deploy parity positive & negative
            copy_jar = tmp_dir / "valid_copy.jar"
            copy_jar.write_bytes(valid_jar.read_bytes())
            self.assertTrue(jar_inspector.check_dual_deploy_parity(valid_jar, copy_jar)["ok"])

            mismatch_jar = tmp_dir / "mismatch.jar"
            _build_synthetic_jar(mismatch_jar, string_literals=["different"])
            parity_bad = jar_inspector.check_dual_deploy_parity(valid_jar, mismatch_jar)
            self.assertFalse(parity_bad["ok"])

            # @e[ selector detection
            poll_jar = tmp_dir / "poll.jar"
            _build_synthetic_jar(poll_jar, string_literals=["execute as @e[type=ender_dragon] run say hi"])
            poll_res = jar_inspector.inspect_jar_full(poll_jar)
            self.assertFalse(poll_res["ok"])
            self.assertTrue(any("@e" in e for e in poll_res["errors"]))

            # Hard dep on economycraft rejected
            harddep_jar = tmp_dir / "harddep.jar"
            _build_synthetic_jar(harddep_jar, depends={
                "fabricloader": ">=0.20.1",
                "minecraft": "~26.3",
                "java": ">=25",
                "fabric-api": "*",
                "economycraft": "*",
            })
            hd_res = jar_inspector.inspect_jar_full(harddep_jar)
            self.assertFalse(hd_res["ok"])
            self.assertTrue(any("economycraft" in e for e in hd_res["errors"]))

            # Missing mixin class rejected
            missing_mixin_jar = tmp_dir / "nomixin.jar"
            _build_synthetic_jar(missing_mixin_jar, include_mixin_class=False)
            mm_res = jar_inspector.inspect_jar_full(missing_mixin_jar)
            self.assertFalse(mm_res["ok"])

            # Java 25 invokedynamic makeConcatWithConstants \x01@e[...] and \x02@e[...] caught
            for marker in ("\x01", "\x02"):
                concat_jar = tmp_dir / f"concat_{ord(marker)}.jar"
                _build_synthetic_jar(concat_jar, string_literals=[f"{marker}@e[type=ender_dragon] run say hi"])
                concat_res = jar_inspector.inspect_jar_full(concat_jar)
                self.assertFalse(concat_res["ok"], f"Missed concat marker {marker!r}@e")
                self.assertTrue(any("@e" in e for e in concat_res["errors"]))

            # Crash resilience: corrupt .class bytes, missing fabric.mod.json, malformed JSON
            corrupt_cls_jar = tmp_dir / "corrupt_cls.jar"
            with zipfile.ZipFile(corrupt_cls_jar, "w") as zf:
                zf.writestr("fabric.mod.json", json.dumps({
                    "schemaVersion": 1, "id": "expedition-dragon", "version": "1.0.0",
                    "entrypoints": {"main": ["a.B"]}, "mixins": ["m.json"],
                    "depends": {"fabricloader": ">=0.20.1", "minecraft": "~26.3", "java": ">=25", "fabric-api": "*"},
                }))
                zf.writestr("m.json", json.dumps({"package": "a.mixin", "compatibilityLevel": "JAVA_25", "mixins": ["M"]}))
                zf.writestr("a/mixin/M.class", b"\xca\xfe\xba\xbe\x00\x00\x00\x45\x00\x05\x01")
                zf.writestr("assets/pad.bin", b"\x00" * 11_000)
            cc_res = jar_inspector.inspect_jar_full(corrupt_cls_jar)
            self.assertFalse(cc_res["ok"])
            self.assertTrue(cc_res["errors"])

            no_mod_json_jar = tmp_dir / "no_mod_json.jar"
            with zipfile.ZipFile(no_mod_json_jar, "w") as zf:
                zf.writestr("a/B.class", _build_minimal_class_bytes(["ok"]))
            nmj_res = jar_inspector.inspect_jar_full(no_mod_json_jar)
            self.assertFalse(nmj_res["ok"])
            self.assertTrue(any("fabric.mod.json" in e for e in nmj_res["errors"]))

            bad_json_jar = tmp_dir / "bad_json.jar"
            with zipfile.ZipFile(bad_json_jar, "w") as zf:
                zf.writestr("fabric.mod.json", "{not-json")
                zf.writestr("a/B.class", _build_minimal_class_bytes(["ok"]))
            bj_res = jar_inspector.inspect_jar_full(bad_json_jar)
            self.assertFalse(bj_res["ok"])
            self.assertTrue(bj_res["errors"])

            # List-typed entrypoints returns error cleanly without AttributeError
            list_ep_jar = tmp_dir / "list_ep.jar"
            with zipfile.ZipFile(list_ep_jar, "w") as zf:
                zf.writestr("fabric.mod.json", json.dumps({
                    "schemaVersion": 1, "id": "expedition-dragon", "version": "1.0.0",
                    "entrypoints": ["test.Dummy"], "mixins": ["expedition-dragon.mixins.json"],
                    "depends": {"fabricloader": ">=0.20.1", "minecraft": "~26.3", "java": ">=25", "fabric-api": "*"},
                }))
                zf.writestr("expedition-dragon.mixins.json", json.dumps({
                    "package": "a.mixin", "compatibilityLevel": "JAVA_25", "mixins": ["M"],
                }))
                zf.writestr("a/mixin/M.class", _build_minimal_class_bytes(["ok"]))
                zf.writestr("assets/pad.bin", b"\x00" * 11_000)
            lep_res = jar_inspector.inspect_jar_full(list_ep_jar)
            self.assertFalse(lep_res["ok"])
            self.assertTrue(any("entrypoints" in e for e in lep_res["errors"]))

    def test_03_config_inspector_valid_and_invalid(self) -> None:
        valid_cfg = {
            "variants": {
                "infernal": {"weight": 0.20, "baseHp": 1500, "damageMultiplier": 1.0, "cooldownMultiplier": 1.0, "bossBarColor": "RED"},
                "voidgloom": {"weight": 0.20, "baseHp": 1400, "damageMultiplier": 1.0, "cooldownMultiplier": 1.0, "bossBarColor": "PURPLE"},
                "revenant": {"weight": 0.20, "baseHp": 1600, "damageMultiplier": 1.0, "cooldownMultiplier": 1.0, "bossBarColor": "GREEN"},
                "enlightened": {"weight": 0.20, "baseHp": 1800, "damageMultiplier": 1.0, "cooldownMultiplier": 1.0, "bossBarColor": "YELLOW"},
                "chronos": {"weight": 0.20, "baseHp": 2000, "damageMultiplier": 1.0, "cooldownMultiplier": 1.0, "bossBarColor": "BLUE"},
            },
            "cooldowns": {"diveTicks": 200, "laserTicks": 300},
            "rewards": {"minDamagePercent": 0.01, "minDamageFlat": 20.0},
        }
        self.assertEqual(config_inspector.validate_config_dict(valid_cfg, require_default_tuning=True), [])

        bad_cfg = json.loads(json.dumps(valid_cfg))
        bad_cfg["variants"]["chronos"]["baseHp"] = 2500
        bad_cfg["variants"]["infernal"]["weight"] = 0.50
        errs = config_inspector.validate_config_dict(bad_cfg, require_default_tuning=True)
        self.assertGreaterEqual(len(errs), 2)

        # Empty rewards, missing thresholds, empty cooldowns
        empty_rewards = json.loads(json.dumps(valid_cfg))
        empty_rewards["rewards"] = {}
        self.assertTrue(config_inspector.validate_config_dict(empty_rewards))

        missing_flat = json.loads(json.dumps(valid_cfg))
        missing_flat["rewards"] = {"minDamagePercent": 0.01}
        self.assertTrue(config_inspector.validate_config_dict(missing_flat))

        empty_cooldowns = json.loads(json.dumps(valid_cfg))
        empty_cooldowns["cooldowns"] = {}
        self.assertTrue(config_inspector.validate_config_dict(empty_cooldowns))

        # bool / NaN / Inf rejection
        bool_weight = json.loads(json.dumps(valid_cfg))
        bool_weight["variants"]["infernal"]["weight"] = True
        self.assertTrue(config_inspector.validate_config_dict(bool_weight))

        nan_hp = json.loads(json.dumps(valid_cfg))
        nan_hp["variants"]["chronos"]["baseHp"] = float("nan")
        nan_hp["variants"]["voidgloom"]["damageMultiplier"] = float("inf")
        self.assertGreaterEqual(len(config_inspector.validate_config_dict(nan_hp)), 2)

        bool_cd = json.loads(json.dumps(valid_cfg))
        bool_cd["cooldowns"]["diveTicks"] = True
        self.assertTrue(config_inspector.validate_config_dict(bool_cd))

    def test_04_datapack_inspector_synthetic_and_junction(self) -> None:
        junction = datapack_inspector.verify_datapacks_junction()
        self.assertTrue(junction["ok"], f"Junction check failed: {junction['errors']}")

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            dp_ok, lvl_ok = _build_synthetic_datapacks(root / "ok", deconflicted=True, intact=True)
            res_ok = datapack_inspector.inspect_datapacks_full(dp_ok, dp_ok, lvl_ok)
            self.assertTrue(res_ok["ok"], f"Deconflicted synthetic datapack failed: {res_ok['errors']}")

            dp_bad, lvl_bad = _build_synthetic_datapacks(root / "bad", deconflicted=False, intact=False)
            res_bad = datapack_inspector.inspect_datapacks_full(dp_bad, dp_bad, lvl_bad)
            self.assertFalse(res_bad["ok"])
            self.assertTrue(any("tru.e-ending" in e for e in res_bad["errors"]))
            self.assertTrue(any("enable_ender_dragon" in e for e in res_bad["errors"]))
            self.assertTrue(any("dragon/butcher" in e for e in res_bad["errors"]))
            self.assertTrue(any("24 Stellarity End biomes" in e for e in res_bad["errors"]))

            # Backslash line-continuation in left_click.mcfunction: gated passes, ungated fails
            lc_path = dp_ok / "Stellarity-6.0.1/data/stellarity/function/mechanic/end_crystal/action/left_click.mcfunction"
            lc_path.write_text(
                "execute if score #stellarity.config stellarity.config.enable_ender_dragon matches 1 run \\\n"
                "  function stellarity:mechanic/end_crystal/drop\n",
                encoding="utf-8",
            )
            self.assertTrue(datapack_inspector.check_stellarity_deconflicted(dp_ok, lvl_ok)["ok"])

            lc_path.write_text(
                "execute unless entity @p[predicate=kohara:player/is_sneaking] run \\\n"
                "  execute if score #stellarity.config stellarity.config.enable_end_crystal_drop matches 1 run \\\n"
                "  function stellarity:mechanic/end_crystal/drop\n",
                encoding="utf-8",
            )
            ungated_res = datapack_inspector.check_stellarity_deconflicted(dp_ok, lvl_ok)
            self.assertFalse(ungated_res["ok"])
            self.assertTrue(ungated_res["ungated_crystal_drop_lines"])
            self.assertTrue(any("end_crystal/drop" in e for e in ungated_res["errors"]))

            # Guard in end_crystal/main.mcfunction before action/left_click satisfies altar crystal protection
            ec_main = dp_ok / "Stellarity-6.0.1/data/stellarity/function/mechanic/end_crystal/main.mcfunction"
            ec_main.write_text(
                "execute if predicate stellarity:location/in_the_end positioned 0 ~ 0 if entity @s[distance=..6] run return run kill @s\n"
                "execute if data storage stellarity:temp end_crystal.attack run function stellarity:mechanic/end_crystal/action/left_click\n",
                encoding="utf-8",
            )
            guarded_res = datapack_inspector.check_stellarity_deconflicted(dp_ok, lvl_ok)
            self.assertTrue(guarded_res["ok"], f"Expected guarded end_crystal/main to pass: {guarded_res['errors']}")

            # Corrupt level.dat returns ok=False without crashing
            corrupt_lvl = root / "corrupt_level.dat"
            corrupt_lvl.write_bytes(b"not-a-gzip-file")
            te_corrupt = datapack_inspector.check_true_ending_disabled(dp_ok, corrupt_lvl)
            self.assertFalse(te_corrupt["ok"])
            self.assertTrue(any("level.dat" in e for e in te_corrupt["errors"]))
            st_corrupt = datapack_inspector.check_stellarity_deconflicted(dp_ok, corrupt_lvl)
            self.assertFalse(st_corrupt["ok"])
            self.assertTrue(any("level.dat" in e for e in st_corrupt["errors"]))

            # Non-compound Data in level.dat & corrupt load.json return ok=False cleanly
            bad_nbt_lvl = root / "bad_data_level.dat"
            bad_nbt_lvl.write_bytes(gzip.compress(b"\x0a\x00\x00\x08\x00\x04Data\x00\x03bad\x00"))
            self.assertFalse(datapack_inspector.check_true_ending_disabled(dp_ok, bad_nbt_lvl)["ok"])
            self.assertFalse(datapack_inspector.check_stellarity_deconflicted(dp_ok, bad_nbt_lvl)["ok"])

            bad_load_json = dp_ok / "Stellarity-6.0.1/data/minecraft/tags/function/load.json"
            bad_load_json.write_text("{corrupt", encoding="utf-8")
            pres_corrupt = datapack_inspector.check_stellarity_preserved(dp_ok)
            self.assertFalse(pres_corrupt["ok"])
            self.assertTrue(any("Corrupt tag JSON" in e for e in pres_corrupt["errors"]))

    def test_05_rcon_protocol_round_trip(self) -> None:
        received_cmds: list[str] = []

        def _server(srv_sock: socket.socket) -> None:
            conn, _ = srv_sock.accept()
            with conn:
                for _ in range(2):
                    hdr = conn.recv(4)
                    (length,) = struct.unpack("<i", hdr)
                    body = conn.recv(length)
                    req_id, pkt_type, payload = mc_server_runner.decode_rcon_packet(hdr + body)
                    if pkt_type == mc_server_runner.RCON_TYPE_AUTH:
                        reply_id = req_id if payload == "secret" else -1
                        conn.sendall(mc_server_runner.encode_rcon_packet(reply_id, mc_server_runner.RCON_TYPE_AUTH_RESPONSE, ""))
                    else:
                        received_cmds.append(payload)
                        conn.sendall(mc_server_runner.encode_rcon_packet(req_id, mc_server_runner.RCON_TYPE_RESPONSE, f"ack:{payload}"))

        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as srv:
            srv.bind(("127.0.0.1", 0))
            srv.listen(1)
            port = int(srv.getsockname()[1])
            t = threading.Thread(target=_server, args=(srv,), daemon=True)
            t.start()

            client = mc_server_runner.RconClient("127.0.0.1", port, "secret", timeout=5.0)
            client.connect()
            resp = client.command("dragon spawn infernal")
            client.close()
            t.join(timeout=2)

            self.assertEqual(resp, "ack:dragon spawn infernal")
            self.assertEqual(received_cmds, ["dragon spawn infernal"])

        # Bad RCON password closes socket cleanly
        def _bad_auth_server(srv_sock: socket.socket) -> None:
            conn, _ = srv_sock.accept()
            with conn:
                hdr = conn.recv(4)
                (length,) = struct.unpack("<i", hdr)
                body = conn.recv(length)
                _req_id, _pkt_type, _payload = mc_server_runner.decode_rcon_packet(hdr + body)
                conn.sendall(mc_server_runner.encode_rcon_packet(-1, mc_server_runner.RCON_TYPE_AUTH_RESPONSE, ""))

        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as srv:
            srv.bind(("127.0.0.1", 0))
            srv.listen(1)
            port = int(srv.getsockname()[1])
            t = threading.Thread(target=_bad_auth_server, args=(srv,), daemon=True)
            t.start()
            bad_client = mc_server_runner.RconClient("127.0.0.1", port, "wrong", timeout=5.0)
            with self.assertRaises(PermissionError):
                bad_client.connect()
            self.assertIsNone(bad_client._sock)
            t.join(timeout=2)

        # Multi-packet RCON response >4KB drained completely
        chunk1 = "A" * 4096
        chunk2 = "B" * 2048

        def _multipkt_server(srv_sock: socket.socket) -> None:
            conn, _ = srv_sock.accept()
            with conn:
                for _ in range(2):
                    hdr = conn.recv(4)
                    (length,) = struct.unpack("<i", hdr)
                    body = conn.recv(length)
                    req_id, pkt_type, _payload = mc_server_runner.decode_rcon_packet(hdr + body)
                    if pkt_type == mc_server_runner.RCON_TYPE_AUTH:
                        conn.sendall(mc_server_runner.encode_rcon_packet(req_id, mc_server_runner.RCON_TYPE_AUTH_RESPONSE, ""))
                    else:
                        conn.sendall(mc_server_runner.encode_rcon_packet(req_id, mc_server_runner.RCON_TYPE_RESPONSE, chunk1))
                        time.sleep(0.08)
                        conn.sendall(mc_server_runner.encode_rcon_packet(req_id, mc_server_runner.RCON_TYPE_RESPONSE, chunk2))

        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as srv:
            srv.bind(("127.0.0.1", 0))
            srv.listen(1)
            port = int(srv.getsockname()[1])
            t = threading.Thread(target=_multipkt_server, args=(srv,), daemon=True)
            t.start()
            with mc_server_runner.RconClient("127.0.0.1", port, "secret", timeout=5.0) as mp_client:
                mp_resp = mp_client.command("big")
            t.join(timeout=2)
            self.assertEqual(mp_resp, chunk1 + chunk2)

        # SandboxServer.__enter__ cleanup on start() failure & provision(require_dragon_mod=True)
        with tempfile.TemporaryDirectory() as tmp:
            fake_ao = Path(tmp) / "allayops"
            fake_ws_mods = Path(tmp) / "ws_mods"
            fake_dp = Path(tmp) / "datapacks"
            (fake_ao / "libraries").mkdir(parents=True)
            (fake_ao / "versions").mkdir(parents=True)
            (fake_ao / "mods").mkdir(parents=True)
            fake_ws_mods.mkdir(parents=True)
            fake_dp.mkdir(parents=True)
            (fake_ao / mc_server_runner.SERVER_LAUNCHER_NAME).write_bytes(b"launcher")

            srv_missing = mc_server_runner.SandboxServer(
                allayops_root=fake_ao,
                workspace_mods=fake_ws_mods,
                workspace_datapacks=fake_dp,
            )
            try:
                with self.assertRaises(FileNotFoundError):
                    srv_missing.provision(require_dragon_mod=True)
                srv_missing.provision(require_dragon_mod=False)
                self.assertTrue((srv_missing.sandbox_dir / "eula.txt").is_file())
            finally:
                srv_missing.cleanup()

            srv_enter = mc_server_runner.SandboxServer(
                allayops_root=fake_ao,
                workspace_mods=fake_ws_mods,
                workspace_datapacks=fake_dp,
            )
            sandbox_path = srv_enter.sandbox_dir
            with mock.patch.object(srv_enter, "start", side_effect=RuntimeError("boom")):
                with self.assertRaises(RuntimeError):
                    with srv_enter:
                        pass
            self.assertFalse(sandbox_path.exists())

    def test_06_harness_java_junit_and_gametest_smoke(self) -> None:
        junit_res = java_runner.run_junit_tag("selfcheck")
        self.assertEqual(junit_res["returncode"], 0, f"gradlew test failed:\n{junit_res['stdout']}\n{junit_res['stderr']}")
        self.assertGreaterEqual(len(junit_res["cases"]), 3)
        self.assertTrue(all(c.status == "passed" for c in junit_res["cases"]), f"Failed cases: {junit_res['cases']}")

        gt_res = java_runner.run_gametest(
            add_mods="",
            test_filter="expedition-dragon-e2e-gametest:self_check_game_test_*",
        )
        self.assertEqual(gt_res["returncode"], 0, f"gradlew runGameTest failed:\n{gt_res['stdout']}\n{gt_res['stderr']}")
        self.assertGreaterEqual(len(gt_res["cases"]), 1, "Expected at least 1 GameTest XML result")
        self.assertTrue(any("self_check_game_test" in c.name for c in gt_res["cases"]), f"Unexpected cases: {gt_res['cases']}")
        self.assertTrue(all(c.status == "passed" for c in gt_res["cases"]), f"Failed GameTests: {gt_res['cases']}")

    def test_07_run_e2e_discovery_feature_filter_and_registry(self) -> None:
        if str(SUITE_ROOT) not in sys.path:
            sys.path.insert(0, str(SUITE_ROOT))
        import run_e2e  # noqa: E402

        self.assertEqual(run_e2e.normalize_feature("f01"), "F1")
        self.assertEqual(run_e2e.normalize_feature("F22"), "F22")
        for bad_feat in ("F0", "F23", "F99", "invalid"):
            with self.assertRaises(ValueError):
                run_e2e.normalize_feature(bad_feat)

        self.assertEqual(run_e2e.extract_canonical_ids("test_T1_F01_01"), ["T1-F01-01"])
        self.assertEqual(run_e2e.extract_canonical_ids("test_T3_01"), ["T3-01"])
        self.assertEqual(run_e2e.extract_canonical_ids("test_T3_010"), [])

        with tempfile.TemporaryDirectory() as tmp:
            tmp_root = Path(tmp)
            t1 = tmp_root / "tier1"
            t1.mkdir(parents=True)
            (t1 / "test_sample.py").write_text(
                "import unittest\n"
                "class SampleT1(unittest.TestCase):\n"
                "    def test_T1_F01_01(self):\n"
                "        self.assertTrue(True)\n"
                "    def test_T1_F01_02(self):\n"
                "        for i in (0, 1):\n"
                "            with self.subTest(i=i):\n"
                "                self.assertEqual(i, 0)\n"
                "    def test_T1_F10_01(self):\n"
                "        self.assertTrue(True)\n",
                encoding="utf-8",
            )
            (t1 / "test_broken_import.py").write_text("import nonexistent_mod_xyz_999\n", encoding="utf-8")

            discovered = run_e2e.discover_python_tier(1, suite_root=tmp_root)
            by_id = {d["id"]: d for d in discovered}
            self.assertEqual(by_id["test_sample.SampleT1.test_T1_F01_01"]["status"], "passed")
            self.assertEqual(by_id["test_sample.SampleT1.test_T1_F01_02"]["status"], "failed")
            self.assertEqual(by_id["test_sample.SampleT1.test_T1_F10_01"]["status"], "passed")
            self.assertTrue(any("test_broken_import" in k and v["status"] == "failed" for k, v in by_id.items()))

            # Clean F1 vs F10 & --require-full-registry check
            (t1 / "test_broken_import.py").unlink()
            (t1 / "test_sample.py").write_text(
                "import unittest\n"
                "class CleanT1(unittest.TestCase):\n"
                "    def test_T1_F01_01(self): pass\n"
                "    def test_T1_F01_02(self): pass\n"
                "    def test_T1_F01_03(self): pass\n"
                "    def test_T1_F01_04(self): pass\n"
                "    def test_T1_F01_05(self): pass\n"
                "    def test_T1_F10_01(self): pass\n",
                encoding="utf-8",
            )
            f1_rep = run_e2e.run_tiers([1], feature_filter="F1", require_full_registry=True, suite_root=tmp_root)
            self.assertEqual(f1_rep["total"], 5)
            self.assertEqual(f1_rep["passed"], 5)
            self.assertEqual(f1_rep["failed"], 0)
            self.assertFalse(any("F10" in r["id"] for r in f1_rep["results"]))

            # Tier 3/4 feature matching via registry.json
            reg_by_id = {t["id"]: t for t in run_e2e.load_registry()["tests"]}
            self.assertTrue(run_e2e._matches_feature_filter({"id": "test_T3_01"}, "F5", reg_by_id))
            self.assertFalse(run_e2e._matches_feature_filter({"id": "test_T3_01"}, "F1", reg_by_id))

            # GameTest tier discovery wiring (Fabric camelToSnake: only [a-z][A-Z] gets '_')
            gt_t1 = tmp_root / "harness-java/src/gametest/java/net/onemoreexpedition/e2e/tier1"
            gt_t1.mkdir(parents=True)
            (gt_t1 / "Tier1SampleGameTest.java").write_text(
                "public class Tier1SampleGameTest { @GameTest public void t1_f05_01_demo(Object h) {} }",
                encoding="utf-8",
            )
            fake_gt = {
                "returncode": 0,
                "cases": [
                    java_runner.JavaCaseResult(
                        name="expedition-dragon-e2e-gametest:tier1sample_game_test_t1_f05_01_demo",
                        classname="fabric-gametest-api-v1:empty",
                        status="passed",
                        time_seconds=0.1,
                    ),
                    java_runner.JavaCaseResult(
                        name="minecraft:always_pass",
                        classname="minecraft:empty",
                        status="passed",
                        time_seconds=0.0,
                    ),
                ],
            }
            with mock.patch.object(java_runner, "run_gametest", return_value=fake_gt) as mocked_gt:
                j_res = run_e2e.discover_java_tier(1, suite_root=tmp_root)
            mocked_gt.assert_called_once_with(test_filter="expedition-dragon-e2e-gametest:tier1sample_game_test_*")
            self.assertEqual(len(j_res), 1)
            self.assertEqual(j_res[0]["engine"], "gametest")
            self.assertEqual(j_res[0]["status"], "passed")

            # Tier1AltarTest -> tier1altar_test_t1_f05_01_crystal
            (gt_t1 / "Tier1SampleGameTest.java").unlink()
            (gt_t1 / "Tier1AltarTest.java").write_text(
                "public class Tier1AltarTest { @GameTest public void t1_f05_01_crystal(Object h) {} }",
                encoding="utf-8",
            )
            fake_altar = {
                "returncode": 0,
                "cases": [
                    java_runner.JavaCaseResult(
                        name="expedition-dragon-e2e-gametest:tier1altar_test_t1_f05_01_crystal",
                        classname="fabric-gametest-api-v1:empty",
                        status="passed",
                        time_seconds=0.1,
                    ),
                ],
            }
            with mock.patch.object(java_runner, "run_gametest", return_value=fake_altar) as mocked_altar:
                altar_res = run_e2e.discover_java_tier(1, suite_root=tmp_root)
            mocked_altar.assert_called_once_with(test_filter="expedition-dragon-e2e-gametest:tier1altar_test_*")
            self.assertEqual(len(altar_res), 1)
            self.assertEqual(altar_res[0]["status"], "passed")

            # F05AltarTest -> f05altar_test_*
            (gt_t1 / "Tier1AltarTest.java").unlink()
            (gt_t1 / "F05AltarTest.java").write_text(
                "public class F05AltarTest { @GameTest public void t1_f05_02_lock(Object h) {} }",
                encoding="utf-8",
            )
            fake_f05 = {
                "returncode": 0,
                "cases": [
                    java_runner.JavaCaseResult(
                        name="expedition-dragon-e2e-gametest:f05altar_test_t1_f05_02_lock",
                        classname="fabric-gametest-api-v1:empty",
                        status="passed",
                        time_seconds=0.1,
                    ),
                ],
            }
            with mock.patch.object(java_runner, "run_gametest", return_value=fake_f05) as mocked_f05:
                f05_res = run_e2e.discover_java_tier(1, suite_root=tmp_root)
            mocked_f05.assert_called_once_with(test_filter="expedition-dragon-e2e-gametest:f05altar_test_*")
            self.assertEqual(len(f05_res), 1)

            # Multiple Tier1* classes -> tier1* filter & exact class-prefix filtering (excludes tier10)
            (gt_t1 / "F05AltarTest.java").unlink()
            (gt_t1 / "Tier1AltarTest.java").write_text(
                "public class Tier1AltarTest { @GameTest public void t1_f05_01_crystal(Object h) {} }",
                encoding="utf-8",
            )
            (gt_t1 / "Tier1CombatTest.java").write_text(
                "public class Tier1CombatTest { @GameTest public void t1_f08_01_bossbar(Object h) {} }",
                encoding="utf-8",
            )
            fake_multi = {
                "returncode": 0,
                "cases": [
                    java_runner.JavaCaseResult(
                        name="expedition-dragon-e2e-gametest:tier1altar_test_t1_f05_01_crystal",
                        classname="fabric-gametest-api-v1:empty",
                        status="passed",
                        time_seconds=0.1,
                    ),
                    java_runner.JavaCaseResult(
                        name="expedition-dragon-e2e-gametest:tier10other_test_x",
                        classname="fabric-gametest-api-v1:empty",
                        status="passed",
                        time_seconds=0.1,
                    ),
                ],
            }
            with mock.patch.object(java_runner, "run_gametest", return_value=fake_multi) as mocked_multi:
                multi_res = run_e2e.discover_java_tier(1, suite_root=tmp_root)
            mocked_multi.assert_called_once_with(test_filter="expedition-dragon-e2e-gametest:tier1*")
            self.assertEqual([r["id"] for r in multi_res], [
                "expedition-dragon-e2e-gametest:tier1altar_test_t1_f05_01_crystal"
            ])

            # setUpClass & setUpModule failures must mark affected tests failed, never passed
            t1_setup = tmp_root / "tier1"
            (t1_setup / "test_sample.py").unlink()
            (t1_setup / "test_setup_boom.py").write_text(
                "import unittest\n"
                "class SetupBoom(unittest.TestCase):\n"
                "    @classmethod\n"
                "    def setUpClass(cls):\n"
                "        raise RuntimeError('setup boom')\n"
                "    def test_T1_F01_01(self):\n"
                "        pass\n"
                "class HealthyOther(unittest.TestCase):\n"
                "    def test_T1_F10_01(self):\n"
                "        pass\n",
                encoding="utf-8",
            )
            setup_rep = run_e2e.run_tiers([1], feature_filter="F1", suite_root=tmp_root)
            self.assertEqual(setup_rep["total"], 1)
            self.assertEqual(setup_rep["passed"], 0)
            self.assertEqual(setup_rep["failed"], 1)
            self.assertEqual(setup_rep["results"][0]["status"], "failed")
            self.assertIn("setup boom", setup_rep["results"][0]["message"])

            (t1_setup / "test_setup_boom.py").unlink()
            (t1_setup / "test_mod_boom.py").write_text(
                "import unittest\n"
                "def setUpModule():\n"
                "    raise RuntimeError('module boom')\n"
                "class ModBoom(unittest.TestCase):\n"
                "    def test_T1_F01_01(self):\n"
                "        pass\n",
                encoding="utf-8",
            )
            mod_rep = run_e2e.run_tiers([1], feature_filter="F1", suite_root=tmp_root)
            self.assertEqual(mod_rep["total"], 1)
            self.assertEqual(mod_rep["passed"], 0)
            self.assertEqual(mod_rep["failed"], 1)
            self.assertIn("module boom", mod_rep["results"][0]["message"])

    def test_08_java_runner_xml_resilience(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            tmp_dir = Path(tmp)
            empty_xml = tmp_dir / "TEST-empty.xml"
            empty_xml.write_bytes(b"")
            res_empty = java_runner.parse_junit_xml_file(empty_xml)
            self.assertEqual(len(res_empty), 1)
            self.assertEqual(res_empty[0].status, "failed")

            trunc_xml = tmp_dir / "TEST-trunc.xml"
            trunc_xml.write_text('<testsuite><testcase name="x"><failure', encoding="utf-8")
            res_trunc = java_runner.parse_junit_xml_file(trunc_xml)
            self.assertEqual(len(res_trunc), 1)
            self.assertEqual(res_trunc[0].status, "failed")

            bad_time_xml = tmp_dir / "TEST-badtime.xml"
            bad_time_xml.write_text(
                '<testsuite><testcase name="T1_F01_01" classname="C" time="not_a_float"/></testsuite>',
                encoding="utf-8",
            )
            res_bad_time = java_runner.parse_junit_xml_file(bad_time_xml)
            self.assertEqual(len(res_bad_time), 1)
            self.assertEqual(res_bad_time[0].status, "failed")


def run_self_check() -> dict[str, Any]:
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(HarnessSelfCheckTests)
    runner = unittest.TextTestRunner(verbosity=2)
    result = runner.run(suite)

    total = result.testsRun
    failed = len(result.failures)
    errors = len(result.errors)
    passed = total - failed - errors
    details = []
    for test, tb in result.failures + result.errors:
        tb_lines = [ln.strip() for ln in tb.splitlines() if ln.strip()]
        details.append({"id": test.id(), "status": "failed", "message": tb_lines[-1] if tb_lines else ""})

    return {
        "mode": "self-check",
        "total": total,
        "passed": passed,
        "failed": failed,
        "errors": errors,
        "ok": result.wasSuccessful(),
        "failures": details,
    }
