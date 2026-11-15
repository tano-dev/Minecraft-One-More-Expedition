"""
AllayOps & OneMoreExpedition - Central Enchantment Manager (A -> Z)
Scans all datapacks & mods (including nested enchantment folders), references
1.21.11 rarity data, deduplicates overlapping enchants, classifies into 4 Tiers
(Common, Rare, Epic, Ultimate), enforces Max 1 Ultimate per item, calibrates
2-5% Rare enchanting table rates, and compiles `expedition_enchantments_control`.
"""

import argparse
import json
import os
import shutil
from collections import defaultdict

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATAPACKS_DIR = os.path.join(REPO_ROOT, "datapacks")
CONTROLLER_NAME = "expedition_enchantments_control"
CONTROLLER_DIR = os.path.join(DATAPACKS_DIR, CONTROLLER_NAME)
CONFIG_PATH = os.path.join(REPO_ROOT, "enchantments_config.json")
CATALOG_PATH = os.path.join(REPO_ROOT, "ENCHANTMENTS_CATALOG.md")
REF_1_21_11_DIR = r"D:\Fabric 1.21.11\Fabric 1.21.11\world\datapacks"

TIER_DEFAULTS = {
    "common": {
        "color": "gray",
        "table_weight": 12,
        "loot_weight": 10,
        "anvil_cost": 2,
        "table": True,
        "tradeable": True,
        "loot": True,
    },
    "rare": {
        "color": "aqua",
        "table_weight": 1,
        "loot_weight": 5,
        "anvil_cost": 4,
        "table": False,
        "tradeable": False,
        "loot": True,
    },
    "epic": {
        "color": "light_purple",
        "table_weight": 1,
        "loot_weight": 2,
        "anvil_cost": 6,
        "table": False,
        "tradeable": False,
        "loot": True,
    },
    "ultimate": {
        "color": "gold",
        "table_weight": 1,
        "loot_weight": 1,
        "anvil_cost": 8,
        "table": False,
        "tradeable": False,
        "loot": True,
        "exclusive_group": "ultimate",
    },
}

# Standard Vanilla 26.3 / 1.21+ Enchantments
VANILLA_ENCHANTS = {
    "minecraft:sharpness": ("common", False, "#minecraft:enchantable/sharp_weapon", 12),
    "minecraft:smite": ("common", False, "#minecraft:enchantable/weapon", 10),
    "minecraft:bane_of_arthropods": ("common", False, "#minecraft:enchantable/weapon", 10),
    "minecraft:knockback": ("common", False, "#minecraft:enchantable/sword", 10),
    "minecraft:fire_aspect": ("common", False, "#minecraft:enchantable/fire_aspect", 8),
    "minecraft:sweeping_edge": ("common", False, "#minecraft:enchantable/sword", 8),
    "minecraft:efficiency": ("common", False, "#minecraft:enchantable/mining", 24),
    "minecraft:unbreaking": ("common", False, "#minecraft:enchantable/durability", 15),
    "minecraft:protection": ("common", False, "#minecraft:enchantable/armor", 15),
    "minecraft:fire_protection": ("common", False, "#minecraft:enchantable/armor", 10),
    "minecraft:blast_protection": ("common", False, "#minecraft:enchantable/armor", 10),
    "minecraft:projectile_protection": ("common", False, "#minecraft:enchantable/armor", 10),
    "minecraft:respiration": ("common", False, "#minecraft:enchantable/head_armor", 10),
    "minecraft:aqua_affinity": ("common", False, "#minecraft:enchantable/head_armor", 10),
    "minecraft:thorns": ("common", False, "#minecraft:enchantable/armor", 8),
    "minecraft:depth_strider": ("common", False, "#minecraft:enchantable/foot_armor", 10),
    "minecraft:power": ("common", False, "#minecraft:enchantable/bow", 22),
    "minecraft:punch": ("common", False, "#minecraft:enchantable/bow", 10),
    "minecraft:quick_charge": ("common", False, "#minecraft:enchantable/crossbow", 12),
    "minecraft:piercing": ("common", False, "#minecraft:enchantable/crossbow", 15),
    "minecraft:loyalty": ("common", False, "#minecraft:enchantable/trident", 12),
    "minecraft:impaling": ("common", False, "#minecraft:enchantable/trident", 12),
    "minecraft:luck_of_the_sea": ("common", False, "#minecraft:enchantable/fishing", 10),
    "minecraft:lure": ("common", False, "#minecraft:enchantable/fishing", 10),
    # Rare (selected ones enchantable at ~2-5%)
    "minecraft:looting": ("rare", True, "#minecraft:enchantable/sword", 1),
    "minecraft:fortune": ("rare", True, "#minecraft:enchantable/mining_loot", 1),
    "minecraft:silk_touch": ("rare", True, "#minecraft:enchantable/mining_loot", 1),
    "minecraft:feather_falling": ("rare", True, "#minecraft:enchantable/foot_armor", 2),
    "minecraft:flame": ("rare", True, "#minecraft:enchantable/bow", 1),
    "minecraft:infinity": ("rare", True, "#minecraft:enchantable/bow", 1),
    "minecraft:multishot": ("rare", True, "#minecraft:enchantable/crossbow", 1),
    "minecraft:riptide": ("rare", True, "#minecraft:enchantable/trident", 2),
    "minecraft:channeling": ("rare", False, "#minecraft:enchantable/trident", 1),
    # Epic
    "minecraft:mending": ("epic", False, "#minecraft:enchantable/durability", 1),
    "minecraft:frost_walker": ("epic", False, "#minecraft:enchantable/foot_armor", 2),
    "minecraft:soul_speed": ("epic", False, "#minecraft:enchantable/foot_armor", 2),
    "minecraft:swift_sneak": ("epic", False, "#minecraft:enchantable/leg_armor", 2),
    "minecraft:breach": ("epic", False, "#minecraft:enchantable/mace", 2),
    "minecraft:density": ("epic", False, "#minecraft:enchantable/mace", 2),
    # Ultimate
    "minecraft:wind_burst": ("ultimate", False, "#minecraft:enchantable/mace", 1),
    # Curses
    "minecraft:binding_curse": ("common", False, "#minecraft:enchantable/equippable", 1),
    "minecraft:vanishing_curse": ("common", False, "#minecraft:enchantable/vanishing", 1),
}

# Explicit duplicate suppression map: secondary_id -> primary_id
KNOWN_DUPLICATES = {
    "enchantsplus:gluttony": "enchantencore:gluttony",
    "enchantsplus:outreach": "nova_structures:outreach",
    "enchantplus:pickaxe/vein_miner": "veinminer-enchantment:veinminer",
    "enchantencore:veinminer": "veinminer-enchantment:veinminer",
    "enchantplus:tools/auto_smelt": "enchantsplus:pyrolysis",
    "enchantplus:axe/timber": "enchantencore:lumberjack",
    "enchantplus:hoe/harvest": "enchantsplus:harvest",
    "enchantplus:sword/curse_of_clumsiness": "enchantsplus:clumsiness_curse",
    "enchantplus:elytra/kinetic_protection": "enchantsplus:kinetic_protection",
    "enchantplus:boots/lava_walker": "enchantsplus:scorch_walker",
    "enchantplus:sword/attack_speed": "enchantsplus:swift_strike",
    "enchantencore:swift_slash": "enchantsplus:swift_strike",
    "enchantplus:bow/breezing_arrow": "enchantsplus:breeze_burst",
    "enchantencore:frost_aspect": "enchantsplus:ice_aspect",
    "enchantplus:sword/poison_aspect": "enchantencore:poison_aspect",
    "enchantplus:sword/wither_aspect": "enchantencore:wither_aspect",
    "enchantplus:sword/vampirism": "enchantencore:leech",
    "enchantplus:sword/life_steal": "enchantencore:leech",
    "enchantplus:leggings/leaping": "enchantencore:leaping",
    "enchantplus:boots/step_assist": "enchantencore:step_up",
}

# Curated Ultimate-tier IDs (Max 1 per item!)
ULTIMATE_IDS = {
    "veinminer-enchantment:veinminer",
    "wither:soul_siphon",
    "wither:witherbound",
    "nova_structures:wax_wings",
    "nova_structures:photosynthesis",
    "incendium:poison_protection",
    "enchantencore:voidweave",
    "enchantencore:dragon_aspect",
    "enchantencore:time_aspect",
    "enchantencore:soulbound",
    "kattersstructures:soulbound",
    "enchantsplus:graviole",
    "enchantsplus:crabs_touch",
    "enchantplus:pickaxe/bedrock_breaker",
    "enchantplus:pickaxe/spawner_touch",
    "enchantplus:tools/miningplus",
    "enchantplus:trident/gungnir_breath",
    "enchantplus:helmet/voidless",
    "enchantplus:chestplate/builder_arm",
    "enchantplus:mace/teluric_wave",
    # Enchantments Encore Custom Ultimates
    "enchantencore:absolute_unit",
    "enchantencore:adrenaline",
    "enchantencore:archer_barrage",
    "enchantencore:aurora",
    "enchantencore:blaze_blessing",
    "enchantencore:bomber",
    "enchantencore:creeper_blessing",
    "enchantencore:crystalize_arrow",
    "enchantencore:dolphin_blessing",
    "enchantencore:frenzy",
    "enchantencore:habanero",
    "enchantencore:heroes_crown",
    "enchantencore:infinity_durability",
    "enchantencore:last_stand",
    "enchantencore:machine_gun",
    "enchantencore:neptune",
    "enchantencore:nightmare",
    "enchantencore:nuclear_arrow",
    "enchantencore:perfect_protection",
    "enchantencore:poseidon",
    "enchantencore:radiant_life",
    "enchantencore:railgun",
    "enchantencore:reflection",
    "enchantencore:sniper",
    "enchantencore:spectral_form",
    "enchantencore:vitality",
    "enchantencore:warden",
    "enchantencore:warped_arrow",
    "enchantencore:warped_trident",
    "enchantencore:wind_spirit",
}

# Curated Rare enchants allowed on Enchanting Table at ~2-5% chance
ENCHANTABLE_RARE_CUSTOM = {
    "enchantsplus:ice_aspect",
    "enchantsplus:swift_strike",
    "enchantsplus:vitality",
    "enchantsplus:stride",
    "enchantsplus:precision",
    "enchantsplus:harvest",
    "enchantplus:armor/lifeplus",
    "enchantplus:armor/fury",
    "enchantplus:boots/agility",
    "enchantplus:helmet/bright_vision",
    "enchantplus:bow/accuracy_shot",
    "enchantplus:sword/critical",
    "nova_structures:illagers_bane",
    "nova_structures:traveler",
    "nova_structures:spiteful",
    "enchantencore:reach",
    "enchantencore:leech",
    "enchantencore:magnetic",
}

EPIC_NAMESPACES = {"stellarity"}


def _extract_desc_text(desc):
    if isinstance(desc, dict):
        return desc.get("text") or desc.get("translate") or ""
    return str(desc or "")


def _read_reference_data():
    """Read weights and treasure/table tags from D:\\Fabric 1.21.11 reference datapacks."""
    ref_weights = {}
    ref_treasure = set()
    ref_table = set()
    if not os.path.exists(REF_1_21_11_DIR):
        return ref_weights, ref_treasure, ref_table

    for root, _, files in os.walk(REF_1_21_11_DIR):
        parts = os.path.normpath(root).split(os.sep)
        if "enchantment" in parts and "tags" not in parts and "data" in parts:
            data_idx = parts.index("data")
            if data_idx + 2 < len(parts) and parts[data_idx + 2] == "enchantment":
                ns = parts[data_idx + 1]
                sub = "/".join(parts[data_idx + 3 :])
                for f in files:
                    if f.endswith(".json"):
                        try:
                            with open(os.path.join(root, f), "r", encoding="utf-8") as jf:
                                d = json.load(jf)
                                name_part = f"{sub}/{f[:-5]}" if sub else f[:-5]
                                eid = f"{ns}:{name_part}"
                                ref_weights[eid] = d.get("weight", 1)
                        except Exception:
                            pass
        elif "tags" in parts and "enchantment" in parts:
            for f in files:
                if f in ("treasure.json", "in_enchanting_table.json"):
                    try:
                        with open(os.path.join(root, f), "r", encoding="utf-8") as jf:
                            d = json.load(jf)
                            for val in d.get("values", []):
                                eid = val["id"] if isinstance(val, dict) else val
                                if isinstance(eid, str) and not eid.startswith("#"):
                                    if f == "treasure.json":
                                        ref_treasure.add(eid)
                                    else:
                                        ref_table.add(eid)
                    except Exception:
                        pass
    return ref_weights, ref_treasure, ref_table


def _scan_current_tags():
    """Scan existing enchantment tags across all active datapacks."""
    tags = defaultdict(set)
    for dp in sorted(os.listdir(DATAPACKS_DIR)):
        dp_path = os.path.join(DATAPACKS_DIR, dp)
        if not os.path.isdir(dp_path) or dp.startswith("_") or dp == CONTROLLER_NAME:
            continue
        tag_dir = os.path.join(dp_path, "data", "minecraft", "tags", "enchantment")
        if not os.path.exists(tag_dir):
            continue
        for fname in ("in_enchanting_table.json", "tradeable.json", "on_random_loot.json", "treasure.json", "curse.json"):
            fpath = os.path.join(tag_dir, fname)
            if os.path.exists(fpath):
                try:
                    with open(fpath, "r", encoding="utf-8") as jf:
                        d = json.load(jf)
                        tname = fname[:-5]
                        for v in d.get("values", []):
                            eid = v["id"] if isinstance(v, dict) else v
                            if isinstance(eid, str) and not eid.startswith("#"):
                                tags[tname].add(eid)
                except Exception:
                    pass
    return tags


def _classify_enchant(eid, raw_data, tags, ref_weights, ref_treasure):
    """Balanced 4-Tier classification using reference weights, treasure tags, and max_level."""
    is_curse = (
        eid in tags["curse"]
        or "curse" in eid
        or eid in ("minecraft:binding_curse", "minecraft:vanishing_curse")
    )
    if is_curse:
        return "common", True, False

    ns, _ = eid.split(":", 1)
    weight = ref_weights.get(eid, raw_data.get("weight", 5))
    max_lv = raw_data.get("max_level", 1)
    is_treasure = eid in tags["treasure"] or eid in ref_treasure

    if eid in ULTIMATE_IDS:
        return "ultimate", False, False
    if ns in EPIC_NAMESPACES:
        return ("ultimate" if weight <= 1 and max_lv >= 2 else "epic"), False, False
    if eid in ENCHANTABLE_RARE_CUSTOM:
        return "rare", False, True
    if is_treasure and weight <= 1:
        return "epic", False, False
    if weight <= 1 and max_lv <= 2:
        return "epic", False, False
    if weight <= 4 or is_treasure:
        return "rare", False, False
    return "common", False, True


def cmd_scan():
    """Scan all datapacks (including nested dirs) and generate/update enchantments_config.json."""
    ref_weights, ref_treasure, _ = _read_reference_data()
    tags = _scan_current_tags()

    existing_cfg = {}
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, "r", encoding="utf-8") as f:
            existing_cfg = json.load(f).get("enchantments", {})

    discovered = {}

    # 1. Vanilla enchantments
    for eid, (tier, ench_rare, items, w) in VANILLA_ENCHANTS.items():
        is_curse = "curse" in eid
        prev = existing_cfg.get(eid, {})
        td = TIER_DEFAULTS[tier]
        discovered[eid] = {
            "source_pack": "minecraft (vanilla)",
            "source_file": None,
            "vanilla": True,
            "enabled": prev.get("enabled", True),
            "tier": prev.get("tier", tier),
            "curse": prev.get("curse", is_curse),
            "enchantable_rare": prev.get("enchantable_rare", tier == "rare" and ench_rare),
            "table": prev.get("table", False if is_curse else (True if tier == "common" else (tier == "rare" and ench_rare))),
            "tradeable": prev.get("tradeable", False if is_curse else td["tradeable"]),
            "loot": prev.get("loot", td["loot"]),
            "table_weight": prev.get("table_weight", w),
            "loot_weight": prev.get("loot_weight", td["loot_weight"]),
            "supported_items": items,
            "description": eid.split(":")[1].replace("_", " ").title(),
        }

    # 2. Scan all Datapacks (supports nested subfolders under data/<ns>/enchantment/...)
    for dp in sorted(os.listdir(DATAPACKS_DIR)):
        dp_path = os.path.join(DATAPACKS_DIR, dp)
        if not os.path.isdir(dp_path) or dp.startswith("_") or dp == CONTROLLER_NAME:
            continue
        for root, _, files in os.walk(dp_path):
            if "overlay_" in root:
                continue
            parts = os.path.normpath(root).split(os.sep)
            if "enchantment" in parts and "tags" not in parts and "data" in parts:
                data_idx = parts.index("data")
                if data_idx + 2 >= len(parts) or parts[data_idx + 2] != "enchantment":
                    continue
                ns = parts[data_idx + 1]
                sub = "/".join(parts[data_idx + 3 :])
                for f in sorted(files):
                    if not f.endswith(".json"):
                        continue
                    fp = os.path.join(root, f)
                    name_part = f"{sub}/{f[:-5]}" if sub else f[:-5]
                    eid = f"{ns}:{name_part}"
                    try:
                        with open(fp, "r", encoding="utf-8") as jf:
                            raw = json.load(jf)
                    except Exception:
                        continue
                    if "max_level" not in raw and "supported_items" not in raw:
                        continue

                    is_vanilla = eid in VANILLA_ENCHANTS
                    tier, is_curse, ench_rare = _classify_enchant(eid, raw, tags, ref_weights, ref_treasure)
                    if is_vanilla:
                        tier, ench_rare_v, _, _ = VANILLA_ENCHANTS[eid]
                        ench_rare = tier == "rare" and ench_rare_v

                    dupe_of = KNOWN_DUPLICATES.get(eid)
                    td = TIER_DEFAULTS[tier]

                    # Only preserve user manual edits if it wasn't a newly-classified item
                    prev = existing_cfg.get(eid) if eid in existing_cfg else None
                    # On fresh scan, use computed tier & defaults
                    use_tier = prev["tier"] if prev and prev.get("manual_override") else tier
                    use_td = TIER_DEFAULTS[use_tier]

                    entry = {
                        "source_pack": dp,
                        "source_file": os.path.relpath(fp, REPO_ROOT),
                        "vanilla": is_vanilla,
                        "enabled": prev["enabled"] if prev and prev.get("manual_override") else (dupe_of is None),
                        "tier": use_tier,
                        "curse": is_curse,
                        "enchantable_rare": ench_rare if use_tier == "rare" else False,
                        "table": False if is_curse else (True if use_tier == "common" else (use_tier == "rare" and ench_rare)),
                        "tradeable": False if is_curse else use_td["tradeable"],
                        "loot": use_td["loot"],
                        "table_weight": 2 if eid == "enchantsplus:vitality" else use_td["table_weight"],
                        "loot_weight": use_td["loot_weight"],
                        "max_level": raw.get("max_level", 1),
                        "anvil_cost": raw.get("anvil_cost", use_td["anvil_cost"]),
                        "supported_items": raw.get("supported_items", ""),
                        "description": _extract_desc_text(raw.get("description", "")),
                    }
                    if dupe_of:
                        entry["duplicate_of"] = dupe_of
                    discovered[eid] = entry

    config_out = {
        "$schema_version": "1.1.0",
        "tiers": TIER_DEFAULTS,
        "statistics": {
            "total": len(discovered),
            "enabled": sum(1 for v in discovered.values() if v["enabled"]),
            "disabled_duplicates": sum(1 for v in discovered.values() if not v["enabled"]),
        },
        "enchantments": dict(sorted(discovered.items())),
    }

    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(config_out, f, indent=2, ensure_ascii=False)

    print(f"[scan] Saved {len(discovered)} enchantments to {CONFIG_PATH}")
    print(f"       Enabled: {config_out['statistics']['enabled']}, Disabled/Duplicates: {config_out['statistics']['disabled_duplicates']}")


def _apply_color_to_desc(desc, color, bold=False):
    if isinstance(desc, dict):
        d = dict(desc)
        d["color"] = color
        if bold:
            d["bold"] = True
        elif "bold" in d and not bold:
            d.pop("bold", None)
        return d
    res = {"text": str(desc), "color": color}
    if bold:
        res["bold"] = True
    return res


# Item-type mapping for accurate real-world Enchanting Table Rare % calculation
ITEM_PROFILES = {
    "Sword": {"#minecraft:enchantable/sword", "#minecraft:enchantable/sharp_weapon", "#minecraft:enchantable/weapon", "#minecraft:enchantable/fire_aspect", "#minecraft:enchantable/durability", "#minecraft:enchantable/vanishing", "#enchantencore:weapons", "#enchantencore:all_tools", "#enchantsplus:toolsnsword", "#enchantsplus:weapons", "#nova_structures:enchantable/combat_weapons", "#nova_structures:enchantable/combat_tools"},
    "Pickaxe": {"#minecraft:enchantable/mining", "#minecraft:enchantable/mining_loot", "#minecraft:enchantable/durability", "#enchantencore:all_tools", "#enchantsplus:toolsnsword", "#enchantsplus:tools"},
    "Axe": {"#minecraft:enchantable/mining", "#minecraft:enchantable/mining_loot", "#minecraft:enchantable/sharp_weapon", "#minecraft:enchantable/weapon", "#minecraft:enchantable/durability", "#enchantencore:weapons", "#enchantencore:all_tools", "#enchantsplus:toolsnsword", "#enchantsplus:tools"},
    "Bow": {"#minecraft:enchantable/bow", "#minecraft:enchantable/durability", "#enchantsplus:bows", "#enchantencore:all_bows"},
    "Crossbow": {"#minecraft:enchantable/crossbow", "#minecraft:enchantable/durability"},
    "Trident": {"#minecraft:enchantable/trident", "#minecraft:enchantable/durability"},
    "Helmet": {"#minecraft:enchantable/armor", "#minecraft:enchantable/head_armor", "#minecraft:head_armor", "#minecraft:enchantable/durability"},
    "Chestplate": {"#minecraft:enchantable/armor", "#minecraft:enchantable/chest_armor", "#minecraft:chest_armor", "#minecraft:enchantable/durability"},
    "Leggings": {"#minecraft:enchantable/armor", "#minecraft:enchantable/leg_armor", "#minecraft:enchantable/durability"},
    "Boots": {"#minecraft:enchantable/armor", "#minecraft:enchantable/foot_armor", "#minecraft:foot_armor", "#minecraft:enchantable/durability"},
}


def _matches_profile(supported_items, profile_tags):
    if isinstance(supported_items, str):
        return supported_items in profile_tags
    if isinstance(supported_items, list):
        return any(s in profile_tags for s in supported_items)
    return False


def _compute_table_probabilities(enchantments):
    """Compute realistic Enchanting Table Rare % per equipment type."""
    report = []
    for item_name, profile_tags in ITEM_PROFILES.items():
        total_w = 0
        rare_w = 0
        total_cnt = 0
        rare_cnt = 0
        for eid, cfg in enchantments.items():
            if not cfg.get("enabled", True) or not cfg.get("table", False) or cfg.get("curse", False):
                continue
            if _matches_profile(cfg.get("supported_items", ""), profile_tags):
                w = cfg.get("table_weight", 1)
                total_w += w
                total_cnt += 1
                if cfg["tier"] == "rare":
                    rare_w += w
                    rare_cnt += 1
        pct = (rare_w / total_w * 100.0) if total_w > 0 else 0.0
        report.append({
            "item": item_name,
            "total_enchants": total_cnt,
            "total_weight": total_w,
            "rare_enchants": rare_cnt,
            "rare_weight": rare_w,
            "rare_pct": round(pct, 2),
        })
    return report


def _apply_protection_nerfs(eid, raw):
    """Apply balanced EPF nerfs to protection enchantments relative to minecraft:protection."""
    dp = raw.get("effects", {}).get("minecraft:damage_protection")
    if not dp:
        return

    # Format: eid -> list of (base, per_level_above_first) or scalar per effect entry
    nerf_map = {
        # Broad / conditional all-damage protections
        "enchantencore:day_protection": [(1.5, 1.5)],
        "enchantencore:night_protection": [(1.5, 1.5)],
        "enchantencore:end_protection": [(1.5, 1.5)],
        "enchantencore:nether_protection": [(1.5, 1.5)],
        "enchantplus:elytra/armored": [6],
        "enchantsplus:skyguard": [(0.75, 0.75)],
        "enchantplus:mounted/cavalier_egis": [(0.75, 0.75)],
        # Niche / single-type protections & Ultimate buffs
        "enchantencore:magic_protection": [(0.4, 0.4)],
        "enchantencore:reflection": [(2, 3)],
        "enchantencore:thorns_protection": [(3.5, 3)],
        "enchantencore:wither_protection": [(3.5, 3)],
        "enchantencore:cushion": [(5, 5)],
        "enchantsplus:kinetic_protection": [(3, 3)],
        "enchantencore:wind_spirit": [15],
        "nova_structures:hydro_veil": [(3.75, 3.75)],
    }

    spec = nerf_map.get(eid)
    if not spec:
        return

    for idx, rule in enumerate(spec):
        if idx >= len(dp):
            break
        eff = dp[idx].get("effect", {})
        if isinstance(rule, tuple):
            base_v, per_lv = rule
            eff["value"] = {"type": "minecraft:linear", "base": base_v, "per_level_above_first": per_lv}
        else:
            eff["value"] = rule


def _apply_custom_overrides(eid, raw, color, bold):
    """Apply custom renames, stat tweaks, and exclusivity rules."""
    if eid == "enchantencore:sunweave":
        raw["description"] = {"text": "Lunar Mend", "color": color, "bold": bold}
        # Switch daylight check to nighttime (13000..23000)
        for eff_entry in raw.get("effects", {}).get("minecraft:tick", []):
            for term in eff_entry.get("requirements", {}).get("terms", []):
                if term.get("type") == "minecraft:time_check":
                    term["value"] = {"min": 13000, "max": 23000}
    elif eid == "enchantplus:armor/lifeplus":
        raw["description"] = {"text": "Walking Fortress", "color": color, "bold": bold}
        for attr in raw.get("effects", {}).get("minecraft:attributes", []):
            if attr.get("attribute") == "minecraft:max_health":
                attr["amount"] = {"type": "minecraft:linear", "base": 1.2, "per_level_above_first": 1.2}
    elif eid == "nova_structures:wither_coated":
        raw["exclusive_set"] = ["enchantencore:wither_aspect"]
    elif eid == "enchantencore:wither_aspect":
        raw["exclusive_set"] = ["nova_structures:wither_coated"]


def cmd_build():
    """Compile enchantments_config.json into expedition_enchantments_control datapack & catalog."""
    if not os.path.exists(CONFIG_PATH):
        cmd_scan()

    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg_data = json.load(f)

    enchantments = cfg_data["enchantments"]

    if os.path.exists(CONTROLLER_DIR):
        shutil.rmtree(CONTROLLER_DIR)
    os.makedirs(CONTROLLER_DIR, exist_ok=True)

    # 1. pack.mcmeta
    pack_mcmeta = {
        "pack": {
            "pack_format": 94,
            "min_format": 94,
            "max_format": 94,
            "description": "§6Expedition Central Enchantment Controller (4-Tier Rarity & Balance)",
        }
    }
    with open(os.path.join(CONTROLLER_DIR, "pack.mcmeta"), "w", encoding="utf-8") as f:
        json.dump(pack_mcmeta, f, indent=2, ensure_ascii=False)

    ultimate_ids = []
    in_table_ids = []
    tradeable_ids = []
    loot_ids = []
    treasure_ids = []
    non_treasure_ids = []
    curse_ids = []

    for eid, cfg in enchantments.items():
        if not cfg.get("enabled", True):
            continue
        tier = cfg.get("tier", "common")
        is_curse = cfg.get("curse", False)

        if tier == "ultimate" and not is_curse:
            ultimate_ids.append(eid)
        if is_curse:
            curse_ids.append(eid)
        if cfg.get("table", False) and not is_curse:
            in_table_ids.append(eid)
        if cfg.get("tradeable", False) and not is_curse:
            tradeable_ids.append(eid)
        if cfg.get("loot", False):
            loot_ids.append(eid)

        if tier in ("epic", "ultimate") or (tier == "rare" and not cfg.get("table", False)) or is_curse:
            treasure_ids.append(eid)
        else:
            non_treasure_ids.append(eid)

    # 2. #expedition:exclusive_set/ultimate
    ult_tag_path = os.path.join(CONTROLLER_DIR, "data", "expedition", "tags", "enchantment", "exclusive_set", "ultimate.json")
    os.makedirs(os.path.dirname(ult_tag_path), exist_ok=True)
    with open(ult_tag_path, "w", encoding="utf-8") as f:
        json.dump({"replace": True, "values": sorted(ultimate_ids)}, f, indent=2)

    # 3. Override each enabled datapack-backed enchantment JSON
    overridden_count = 0
    for eid, cfg in enchantments.items():
        if not cfg.get("enabled", True):
            continue
        src_rel = cfg.get("source_file")
        if not src_rel:
            continue
        src_abs = os.path.join(REPO_ROOT, src_rel)
        if not os.path.exists(src_abs):
            continue

        with open(src_abs, "r", encoding="utf-8") as jf:
            raw = json.load(jf)

        tier = cfg.get("tier", "common")
        is_curse = cfg.get("curse", False)
        color = "red" if is_curse else TIER_DEFAULTS[tier]["color"]
        bold = (tier == "ultimate" and not is_curse)

        if "description" in raw:
            raw["description"] = _apply_color_to_desc(raw["description"], color, bold=bold)

        if "max_level" in cfg:
            raw["max_level"] = cfg["max_level"]
        if "anvil_cost" in cfg:
            raw["anvil_cost"] = cfg["anvil_cost"]

        raw["weight"] = cfg.get("table_weight", 1) if cfg.get("table", False) else cfg.get("loot_weight", 1)

        if tier == "ultimate" and not is_curse:
            ns, name = eid.split(":", 1)
            safe_name = name.replace("/", "_")
            excl_values = [{"id": "#expedition:exclusive_set/ultimate", "required": False}]
            orig_excl = raw.get("exclusive_set")
            if isinstance(orig_excl, str):
                excl_values.append({"id": orig_excl, "required": False})
            elif isinstance(orig_excl, list):
                for item in orig_excl:
                    excl_values.append({"id": item, "required": False} if isinstance(item, str) else item)

            excl_tag_path = os.path.join(
                CONTROLLER_DIR, "data", "expedition", "tags", "enchantment", "exclusive", f"{ns}_{safe_name}.json"
            )
            os.makedirs(os.path.dirname(excl_tag_path), exist_ok=True)
            with open(excl_tag_path, "w", encoding="utf-8") as ef:
                json.dump({"replace": True, "values": excl_values}, ef, indent=2)
            raw["exclusive_set"] = f"#expedition:exclusive/{ns}_{safe_name}"

        _apply_protection_nerfs(eid, raw)
        _apply_custom_overrides(eid, raw, color, bold)

        ns, name = eid.split(":", 1)
        out_path = os.path.join(CONTROLLER_DIR, "data", ns, "enchantment", *name.split("/")) + ".json"
        os.makedirs(os.path.dirname(out_path), exist_ok=True)
        with open(out_path, "w", encoding="utf-8") as of:
            json.dump(raw, of, indent=2, ensure_ascii=False)
        overridden_count += 1

    # 4. Master Vanilla Tags
    mc_tag_dir = os.path.join(CONTROLLER_DIR, "data", "minecraft", "tags", "enchantment")
    os.makedirs(mc_tag_dir, exist_ok=True)
    tag_map = {
        "in_enchanting_table.json": sorted(in_table_ids),
        "tradeable.json": sorted(tradeable_ids),
        "on_random_loot.json": sorted(loot_ids),
        "treasure.json": sorted(treasure_ids),
        "non_treasure.json": sorted(non_treasure_ids),
        "curse.json": sorted(curse_ids),
    }
    for fname, vals in tag_map.items():
        with open(os.path.join(mc_tag_dir, fname), "w", encoding="utf-8") as tf:
            json.dump({"replace": True, "values": vals}, tf, indent=2)

    # 5. Catalog
    prob_report = _compute_table_probabilities(enchantments)
    _write_catalog(enchantments, prob_report)

    print(f"[build] Compiled {CONTROLLER_NAME}:")
    print(f"        Overridden enchantments: {overridden_count}")
    print(f"        Ultimate (Max 1/item): {len(ultimate_ids)}")
    print(f"        In Enchanting Table: {len(in_table_ids)}")
    print(f"        Tradeable (Villagers): {len(tradeable_ids)}")
    print(f"        Dungeon/Treasure Loot: {len(loot_ids)}")
    print(f"        Catalog written to {CATALOG_PATH}")


def _write_catalog(enchantments, prob_report):
    by_tier = defaultdict(list)
    disabled = []
    curses = []
    for eid, cfg in sorted(enchantments.items()):
        if not cfg.get("enabled", True):
            disabled.append((eid, cfg))
        elif cfg.get("curse", False):
            curses.append((eid, cfg))
        else:
            by_tier[cfg.get("tier", "common")].append((eid, cfg))

    lines = [
        "# Enchantments Master Catalog (A → Z)",
        "",
        "> Tự động tổng hợp bởi `scripts/enchant_manager.py` từ `enchantments_config.json`.",
        "",
        "## 1. Tổng Quan Phân Bố 4 Độ Hiếm",
        "",
        "| Tier | Màu sắc | Số lượng | Nguồn kiếm chính | Quy tắc đặc biệt |",
        "| :--- | :--- | :---: | :--- | :--- |",
        f"| **Ultimate** | Gold (`gold`) | **{len(by_tier['ultimate'])}** | Rương kho báu / Hầm ngục hiếm | **Tối đa 1 Ultimate / vật phẩm** |",
        f"| **Epic** | Light Purple (`light_purple`) | **{len(by_tier['epic'])}** | Rương kho báu / Hầm ngục | Không ra ở bàn phù phép / dân làng |",
        f"| **Rare** | Aqua (`aqua`) | **{len(by_tier['rare'])}** | Hầm ngục + 2–5% ở Bàn phù phép | Không bán ở dân làng |",
        f"| **Common** | Gray (`gray`) | **{len(by_tier['common'])}** | Bàn phù phép, Dân làng, Rương | Phù phép cơ bản |",
        f"| **Curses** | Red (`red`) | **{len(curses)}** | Rương kho báu / Curse | Lời nguyền |",
        f"| **Filtered Duplicates** | — | **{len(disabled)}** | Đã tắt | Lọc trùng giữa các pack |",
        "",
        "## 2. Xác Suất Rare Khi Enchant Theo Loại Trang Bị (Mục tiêu 2% – 5%)",
        "",
        "| Loại trang bị | Tổng Enchant | Tổng Weight | Số Rare Enchantable | Tỉ lệ Rare (%) |",
        "| :--- | :---: | :---: | :---: | :---: |",
    ]
    for r in prob_report:
        lines.append(
            f"| **{r['item']}** | {r['total_enchants']} | {r['total_weight']} | {r['rare_enchants']} | **{r['rare_pct']}%** |"
        )

    for tier_key, title in [
        ("ultimate", "3. Ultimate Enchants (Tối đa 1 / vật phẩm)"),
        ("epic", "4. Epic Enchants (Chỉ kiếm từ Kho báu / Hầm ngục)"),
        ("rare", "5. Rare Enchants (Kho báu + 2–5% Bàn phù phép)"),
        ("common", "6. Common Enchants"),
    ]:
        lines.extend([
            "",
            f"## {title} ({len(by_tier[tier_key])})",
            "",
            "| Enchantment ID | Max Lv | Nguồn Pack | Table? | Loot Weight | Items áp dụng |",
            "| :--- | :---: | :--- | :---: | :---: | :--- |",
        ])
        for eid, cfg in by_tier[tier_key]:
            items = cfg.get("supported_items", "")
            if isinstance(items, list):
                items = ", ".join(items[:2]) + ("..." if len(items) > 2 else "")
            t_flag = "Yes" if cfg.get("table") else "No"
            lines.append(
                f"| `{eid}` | {cfg.get('max_level', '-')} | `{cfg['source_pack']}` | {t_flag} | {cfg.get('loot_weight')} | `{items}` |"
            )

    if disabled:
        lines.extend([
            "",
            f"## 7. Enchants Bị Lọc Do Trùng Lặp ({len(disabled)})",
            "",
            "| Enchantment ID (Đã tắt) | Nguồn Pack | Thay thế bởi (`duplicate_of`) |",
            "| :--- | :--- | :--- |",
        ])
        for eid, cfg in disabled:
            lines.append(f"| `{eid}` | `{cfg['source_pack']}` | `{cfg.get('duplicate_of', 'manual')}` |")

    with open(CATALOG_PATH, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + "\n")


def cmd_stats():
    """Print quick statistics and Rare enchanting table probability check."""
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg_data = json.load(f)
    enchantments = cfg_data["enchantments"]
    prob_report = _compute_table_probabilities(enchantments)
    print("=== ENCHANTMENTS STATS ===")
    counts = defaultdict(int)
    for cfg in enchantments.values():
        if not cfg.get("enabled", True):
            counts["disabled_duplicate"] += 1
        elif cfg.get("curse", False):
            counts["curse"] += 1
        else:
            counts[cfg["tier"]] += 1
    for k, v in sorted(counts.items()):
        print(f"  {k}: {v}")
    print("\n=== ENCHANTING TABLE RARE PROBABILITY (PER ITEM) ===")
    for r in prob_report:
        print(f"  {r['item']:12s}: {r['rare_pct']:5.2f}% ({r['rare_weight']}/{r['total_weight']} weight, {r['rare_enchants']} rare enchants)")


def main():
    parser = argparse.ArgumentParser(description="AllayOps Enchantment Manager")
    parser.add_argument("action", choices=["scan", "build", "stats", "all"], help="Action to run")
    args = parser.parse_args()

    if args.action == "scan":
        cmd_scan()
    elif args.action == "build":
        cmd_build()
    elif args.action == "stats":
        cmd_stats()
    elif args.action == "all":
        cmd_scan()
        cmd_build()
        cmd_stats()


if __name__ == "__main__":
    main()
