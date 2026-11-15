"""
Rebalance enchantment rarity tiers (Common, Rare, Epic, Ultimate) and curse flags
based on actual gameplay power and mechanics, then sync config, datapack, and wiki.
"""

import json
import os

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG_PATH = os.path.join(REPO_ROOT, "enchantments_config.json")
BUILD_EN_PATH = os.path.join(REPO_ROOT, "scripts", "build_english_database.py")

# True Curses (33 total)
TRUE_CURSES = {
    "enchantencore:blindness_curse",
    "enchantencore:breeze_curse",
    "enchantencore:caravan_curse",
    "enchantencore:color_blindness_curse",
    "enchantencore:corrosion_curse",
    "enchantencore:crank_curse",
    "enchantencore:creeper_vision_curse",
    "enchantencore:death_curse",
    "enchantencore:ender_vision_curse",
    "enchantencore:fragility_curse",
    "enchantencore:hiding_curse",
    "enchantencore:mirror_vision_curse",
    "enchantencore:ownership_curse",
    "enchantencore:randomness_curse",
    "enchantencore:reach_curse",
    "enchantencore:shortsight_curse",
    "enchantencore:spider_vision_curse",
    "enchantencore:undying_curse",
    "enchantencore:void_vision_curse",
    "enchantencore:welfare_curse",
    "enchantplus:durability/curse_of_breaking",
    "enchantplus:durability/curse_of_enchant",
    "enchantplus:leggings/dwarfed",
    "enchantplus:leggings/oversize",
    "enchantsplus:breaking_curse",
    "enchantsplus:clumsiness_curse",
    "enchantsplus:compassion_curse",
    "enchantsplus:displacement_curse",
    "enchantsplus:double_edge_curse",
    "kattersstructures:alchemy_curse",
    "kattersstructures:crystal_curse",
    "minecraft:binding_curse",
    "minecraft:vanishing_curse",
    "nova_structures:conductivity_curse",
}

# Ultimate Tier: Build-defining / massive power / Max 1 per item
ULTIMATE_IDS = {
    # Enchantments Encore
    "enchantencore:absolute_unit",
    "enchantencore:archer_barrage",
    "enchantencore:aurora",
    "enchantencore:blaze_blessing",
    "enchantencore:bomber",
    "enchantencore:creeper_blessing",
    "enchantencore:crystalize_arrow",
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
    "enchantencore:warden",
    "enchantencore:warped_arrow",
    "enchantencore:warped_trident",
    "enchantencore:wind_spirit",
    # NeoEnchant / Enchants+ / Vanilla / Stellarity / Veinminer / Wither
    "enchantplus:armor/lifeplus",
    "enchantplus:chestplate/builder_arm",
    "enchantplus:helmet/voidless",
    "enchantplus:mace/teluric_wave",
    "enchantplus:pickaxe/bedrock_breaker",
    "enchantplus:pickaxe/spawner_touch",
    "enchantplus:sword/last_hope",
    "enchantplus:tools/miningplus",
    "enchantplus:trident/gungnir_breath",
    "enchantsplus:graviole",
    "minecraft:wind_burst",
    "stellarity:critical_strike",
    "stellarity:void_shot",
    "stellarity:void_strike",
    "veinminer-enchantment:veinminer",
    "wither:soul_siphon",
    "wither:witherbound",
}

# Epic Tier: High-impact combat, mobility, Mending-tier repair, unique relics
EPIC_IDS = {
    # Demoted from Ultimate to Epic (strong but not Ultimate-exclusive worthy)
    "enchantencore:adrenaline",
    "enchantencore:dolphin_blessing",
    "enchantencore:frenzy",
    "enchantencore:habanero",
    "enchantencore:heroes_crown",
    "enchantencore:vitality",
    "enchantencore:voidweave",
    "enchantsplus:crabs_touch",
    "incendium:poison_protection",
    "nova_structures:photosynthesis",
    "nova_structures:wax_wings",
    "stellarity:dune_speed",
    # Promoted from Rare/Common to Epic
    "enchantencore:crab_claw",
    "enchantencore:emberweave",
    "enchantencore:haste",
    "enchantencore:invisibility_cloak",
    "enchantencore:leech",
    "enchantencore:moonweave",
    "enchantencore:sunweave",
    "enchantplus:boots/agility",
    "enchantplus:sword/critical",
    "enchantplus:sword/xp_boost",
    "enchantsplus:pyrolysis",
    "enchantsplus:swift_strike",
    "enchantsplus:vitality",
    "nova_structures:ghasted",
    "nova_structures:multishot",
}

# Rare Tier: Strong situational upgrades, dimensional protection, XP multipliers, aspects
RARE_PROMOTIONS = {
    "enchantencore:bane_of_bosses",
    "enchantencore:day_protection",
    "enchantencore:end_protection",
    "enchantencore:nether_protection",
    "enchantencore:night_protection",
    "enchantencore:scholar_of_blocks",
    "enchantencore:scholar_of_fishing",
    "enchantencore:scholar_of_mobs",
    "enchantencore:spectral_form",
    "enchantsplus:retrieval",
}

# Common Tier: Cosmetic auras (loot-only, non-table) & basic utility
COMMON_COSMETIC_AURAS = {
    "enchantencore:angry_aura",
    "enchantencore:aquatic_aura",
    "enchantencore:echoing_aura",
    "enchantencore:enchanted_aura",
    "enchantencore:ender_aura",
    "enchantencore:flaming_aura",
    "enchantencore:heavenly_aura",
    "enchantencore:lovely_aura",
    "enchantencore:lush_aura",
    "enchantencore:muddy_aura",
    "enchantencore:ominous_aura",
    "enchantencore:pale_aura",
    "enchantencore:sparking_aura",
    "enchantencore:sticky_aura",
    "enchantencore:trial_aura",
}


def main():
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg = json.load(f)

    tiers_meta = cfg["tiers"]
    changed = 0

    for eid, entry in cfg["enchantments"].items():
        if not entry.get("enabled", True):
            continue

        old_tier = entry.get("tier")
        old_curse = entry.get("curse", False)

        # 1. Fix curse flag
        is_curse = eid in TRUE_CURSES
        entry["curse"] = is_curse

        # 2. Determine target tier
        if is_curse:
            new_tier = "common"
        elif eid in ULTIMATE_IDS:
            new_tier = "ultimate"
        elif eid in EPIC_IDS:
            new_tier = "epic"
        elif eid in RARE_PROMOTIONS:
            new_tier = "rare"
        elif eid in COMMON_COSMETIC_AURAS:
            new_tier = "common"
        elif eid.startswith("mt:armor/") or eid.startswith("mt:weapons/"):
            new_tier = "epic"
        elif eid.startswith("mt:technical/") or eid.startswith("mt:treasure/"):
            new_tier = "rare"
        else:
            new_tier = old_tier

        entry["tier"] = new_tier

        # 3. Sync tier defaults (keeping calibrated table_weight for rare table enchants)
        if is_curse:
            entry["table"] = False
            entry["tradeable"] = False
            entry["loot"] = True
            entry["anvil_cost"] = 8
            entry["table_weight"] = 1
            entry["loot_weight"] = 2
        elif new_tier == "ultimate":
            entry["table"] = False
            entry["tradeable"] = False
            entry["loot"] = True
            entry["anvil_cost"] = 8
            entry["table_weight"] = 1
            entry["loot_weight"] = 1
        elif new_tier == "epic":
            entry["table"] = False
            entry["tradeable"] = False
            entry["loot"] = True
            entry["anvil_cost"] = 6
            entry["table_weight"] = 1
            entry["loot_weight"] = 2
        elif new_tier == "rare":
            entry["anvil_cost"] = 4
            entry["loot"] = True
            entry["loot_weight"] = 5
            # Newly promoted rares are loot-only so Enchanting Table stays calibrated at 2-5%
            if eid in RARE_PROMOTIONS or not entry.get("table", False):
                entry["table"] = False
                entry["tradeable"] = False
                entry["table_weight"] = 1
        elif new_tier == "common":
            entry["anvil_cost"] = 2
            entry["loot"] = True
            entry["loot_weight"] = 10
            if eid in COMMON_COSMETIC_AURAS:
                entry["table"] = False
                entry["tradeable"] = False
                entry["table_weight"] = 1
            elif old_tier != "common" and not entry.get("table", False):
                entry["table"] = False

        if old_tier != new_tier or old_curse != is_curse:
            changed += 1

        # 4. Sync [ULTIMATE] suffix in description
        desc = entry.get("description", "")
        clean_desc = desc.replace(" [ULTIMATE - Max 1/item]", "").replace(" [ULTIMATE]", "").strip()
        if new_tier == "ultimate":
            entry["description"] = f"{clean_desc} [ULTIMATE]"
        else:
            entry["description"] = clean_desc

    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)

    print(f"Rebalanced {changed} enchantments in enchantments_config.json.")

    # Also update build_english_database.py so future exports preserve [ULTIMATE] tags cleanly
    with open(BUILD_EN_PATH, "r", encoding="utf-8") as f:
        py_src = f.read()

    # In build_english_database.py, make description respect cfg's tier for [ULTIMATE] tag!
    old_block = '        if eid in master:\n            max_lv, anvil_cost, gear_cats, gear_display, name_en, desc = master[eid]'
    new_block = (
        '        if eid in master:\n'
        '            max_lv, anvil_cost, gear_cats, gear_display, name_en, desc = master[eid]\n'
        '            desc = desc.replace(" [ULTIMATE - Max 1/item]", "").replace(" [ULTIMATE]", "").strip()\n'
        '            if tier == "ultimate":\n'
        '                desc = f"{desc} [ULTIMATE]"\n'
        '            anvil_cost = entry.get("anvil_cost", anvil_cost)'
    )
    if old_block in py_src and 'desc.replace(" [ULTIMATE - Max 1/item]"' not in py_src:
        py_src = py_src.replace(old_block, new_block)
        with open(BUILD_EN_PATH, "w", encoding="utf-8") as f:
            f.write(py_src)
        print("Updated build_english_database.py to sync [ULTIMATE] tags and anvil_cost from config.")


if __name__ == "__main__":
    main()
