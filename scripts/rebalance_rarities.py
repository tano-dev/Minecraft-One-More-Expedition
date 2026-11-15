"""
Rebalance enchantment rarity tiers into a clean RPG pyramid:
Common > Rare > Epic > Ultimate (with Curses separated cleanly),
while keeping Enchanting Table Rare rates calibrated at ~1-5%.
"""

import json
import os

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG_PATH = os.path.join(REPO_ROOT, "enchantments_config.json")
BUILD_EN_PATH = os.path.join(REPO_ROOT, "scripts", "build_english_database.py")

# True Curses (34 total)
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

# Ultimate Tier (~40): Build-defining / massive power / Max 1 per item
ULTIMATE_IDS = {
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

# Epic Tier (~73): High-impact combat, mobility, Mending-tier repair, unique relics
EPIC_IDS = {
    "enchantencore:adrenaline",
    "enchantencore:bear_spirit",
    "enchantencore:dolphin_blessing",
    "enchantencore:emberweave",
    "enchantencore:frenzy",
    "enchantencore:ghost_spirit",
    "enchantencore:haste",
    "enchantencore:invisibility_cloak",
    "enchantencore:leech",
    "enchantencore:lightning_bolt",
    "enchantencore:moonweave",
    "enchantencore:pegasus",
    "enchantencore:refuel",
    "enchantencore:sunweave",
    "enchantencore:vitality",
    "enchantencore:voidweave",
    "enchantplus:boots/agility",
    "enchantplus:mace/striker",
    "enchantplus:sword/critical",
    "enchantplus:sword/xp_boost",
    "enchantsplus:pyrolysis",
    "enchantsplus:swift_strike",
    "enchantsplus:vitality",
    "minecraft:breach",
    "minecraft:density",
    "minecraft:frost_walker",
    "minecraft:mending",
    "minecraft:soul_speed",
    "minecraft:swift_sneak",
    "nova_structures:ghasted",
    "nova_structures:multishot",
    "nova_structures:photosynthesis",
    "stellarity:ambush",
    "stellarity:plated",
}

# Rare Tier: Strong situational upgrades, dimensional protection, XP multipliers, aspects, mid-tier loot
RARE_IDS = {
    # Promoted from Common
    "enchantencore:bane_of_bosses",
    "enchantencore:day_protection",
    "enchantencore:end_protection",
    "enchantencore:nether_protection",
    "enchantencore:night_protection",
    "enchantencore:scholar_of_blocks",
    "enchantencore:scholar_of_fishing",
    "enchantencore:scholar_of_mobs",
    "enchantsplus:retrieval",
    # Kept in Rare
    "enchantencore:armor_piercing",
    "enchantencore:boltbringer",
    "enchantencore:bulletproof",
    "enchantencore:ender_shot",
    "enchantencore:enderscape",
    "enchantencore:evokers_wrath",
    "enchantencore:hurricane",
    "enchantencore:implosion",
    "enchantencore:jousting",
    "enchantencore:levitation_aspect",
    "enchantencore:meteor",
    "enchantencore:pacifier",
    "enchantencore:poison_aspect",
    "enchantencore:poison_infused",
    "enchantencore:rejuvenation",
    "enchantencore:replenish",
    "enchantencore:web_infused",
    "enchantencore:wither_aspect",
    "enchantencore:wither_infused",
    "enchantplus:armor/fury",
    "enchantplus:bow/accuracy_shot",
    "enchantplus:bow/echo_shot",
    "enchantplus:bow/eternal_frost",
    "enchantplus:bow/explosive_arrow",
    "enchantplus:bow/rebound",
    "enchantplus:bow/storm_arrow",
    "enchantplus:elytra/armored",
    "enchantplus:elytra/propulsion",
    "enchantplus:leggings/fast_swim",
    "enchantplus:mace/wind_propulsion",
    "enchantplus:midas_touch",
    "enchantplus:mounted/ethereal_leap",
    "enchantplus:mounted/happy_boost",
    "enchantplus:mounted/velocity",
    "enchantplus:sword/dimensional_hit",
    "enchantplus:sword/reach",
    "enchantplus:sword/runic_despair",
    "enchantplus:sword/tears_of_asflors",
    "enchantsplus:breeze_burst",
    "enchantsplus:ice_aspect",
    "enchantsplus:kickback",
    "enchantsplus:scorch_walker",
    "minecraft:channeling",
    "minecraft:feather_falling",
    "minecraft:flame",
    "minecraft:fortune",
    "minecraft:infinity",
    "minecraft:looting",
    "minecraft:multishot",
    "minecraft:riptide",
    "minecraft:silk_touch",
    "nova_structures:gravity",
    "nova_structures:piercing",
    "nova_structures:power",
    "nova_structures:spiteful",
    "nova_structures:wither_coated",
    # Moved from Epic -> Rare (solid upgrades, niche immunities, or terrain speeds)
    "enchantencore:boar_spirit",
    "enchantencore:crab_claw",
    "enchantencore:end_speed",
    "enchantencore:fetch",
    "enchantencore:flare",
    "enchantencore:habanero",
    "enchantencore:heroes_crown",
    "enchantencore:incognito",
    "enchantencore:moonwalk",
    "enchantencore:night_vision",
    "enchantencore:path_speed",
    "enchantencore:regrowth",
    "enchantencore:sand_speed",
    "enchantencore:scorpion_spirit",
    "enchantencore:shulker_shell",
    "enchantencore:sonic",
    "enchantencore:trial_runner",
    "enchantencore:velocity",
    "enchantplus:sword/death_touch",
    "enchantplus:sword/pull",
    "enchantsplus:crabs_touch",
    "incendium:poison_protection",
    "kattersstructures:blunt",
    "kattersstructures:enlightning",
    "kattersstructures:light",
    "kattersstructures:ocean_heart",
    "kattersstructures:raj_ambient",
    "kattersstructures:raj_lightning",
    "kattersstructures:raj_minion",
    "kattersstructures:rusta_shot",
    "kattersstructures:web_walker",
    "nova_structures:aerials_bane",
    "nova_structures:hydro_veil",
    "nova_structures:swift_soar",
    "nova_structures:wax_wings",
    "stellarity:dune_speed",
    "stellarity:levitation_shot",
    "stellarity:soaring",
}

# Common Tier: Basic utility, cosmetic auras, minor aspects, technical triggers, core vanilla
COMMON_IDS = {
    "enchantencore:anchor",
    "enchantencore:angry_aura",
    "enchantencore:aquatic_aura",
    "enchantencore:bounce",
    "enchantencore:bunny_hop",
    "enchantencore:cleansing",
    "enchantencore:crispbread",
    "enchantencore:crop_dance",
    "enchantencore:dark_infused",
    "enchantencore:darkness_alloy",
    "enchantencore:dismount",
    "enchantencore:echoing_aura",
    "enchantencore:electric_walker",
    "enchantencore:enchanted_aura",
    "enchantencore:ender_aura",
    "enchantencore:flaming_aura",
    "enchantencore:forge_shot",
    "enchantencore:gallop",
    "enchantencore:gluttony",
    "enchantencore:grass_walker",
    "enchantencore:growth",
    "enchantencore:guard",
    "enchantencore:handyman",
    "enchantencore:hardened",
    "enchantencore:harpoon",
    "enchantencore:heavenly_aura",
    "enchantencore:hunger_aspect",
    "enchantencore:ice_alloy",
    "enchantencore:illumination",
    "enchantencore:impale_rush",
    "enchantencore:infested_aspect",
    "enchantencore:leaf_jumper",
    "enchantencore:lingering",
    "enchantencore:lovely_aura",
    "enchantencore:lush_aura",
    "enchantencore:magma_walker",
    "enchantencore:muddy_aura",
    "enchantencore:ominous_aura",
    "enchantencore:ominous_healing",
    "enchantencore:ominous_rain",
    "enchantencore:oozing_aspect",
    "enchantencore:pale_aura",
    "enchantencore:pickpocket",
    "enchantencore:ping_tag",
    "enchantencore:poppy_peace",
    "enchantencore:propeller",
    "enchantencore:purify_water",
    "enchantencore:rainweave",
    "enchantencore:sacrifice",
    "enchantencore:scent",
    "enchantencore:seeker",
    "enchantencore:shrinkage",
    "enchantencore:slowness_aspect",
    "enchantencore:sparking_aura",
    "enchantencore:spectral_form",
    "enchantencore:sticky_aura",
    "enchantencore:sting",
    "enchantencore:strafe",
    "enchantencore:summer_walker",
    "enchantencore:surfer",
    "enchantencore:switch",
    "enchantencore:thorns_protection",
    "enchantencore:tideweave",
    "enchantencore:trial_aura",
    "enchantencore:updraft",
    "enchantencore:weakness_aspect",
    "enchantencore:weaving_aspect",
    "enchantencore:wind_aspect",
    "enchantencore:wither_protection",
    "enchantencore:yoink",
    "enchantplus:armor/venom_protection",
    "enchantplus:helmet/auto_feed",
    "enchantplus:helmet/bright_vision",
    "enchantplus:hoe/seiors_oblivion",
    "enchantplus:mounted/cavalier_egis",
    "enchantplus:mounted/steel_fang",
    "enchantplus:sword/fear",
    "enchantsplus:harvest",
    "enchantsplus:precision",
    "enchantsplus:stride",
    "enchantsplus:toxic",
    "enchantsplus:websnare",
    "kattersstructures:heavy",
    "nova_structures:antidote",
    "nova_structures:function_only",
    "nova_structures:illagers_bane",
    "nova_structures:outreach",
    "nova_structures:traveler",
}

# Preserve existing Enchanting Table Rare enchants (weight 1) so table stays 2-5%
CALIBRATED_TABLE_RARES = {
    "enchantencore:day_protection",
    "enchantencore:night_protection",
    "minecraft:looting",
    "minecraft:fortune",
    "minecraft:silk_touch",
    "minecraft:feather_falling",
    "minecraft:flame",
    "minecraft:infinity",
    "minecraft:multishot",
    "minecraft:riptide",
    "enchantplus:sword/dimensional_hit",
    "enchantplus:sword/reach",
    "enchantsplus:ice_aspect",
    "nova_structures:spiteful",
}


def main():
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg = json.load(f)

    changed = 0

    for eid, entry in cfg["enchantments"].items():
        if not entry.get("enabled", True):
            continue

        old_tier = entry.get("tier")
        old_curse = entry.get("curse", False)

        is_curse = eid in TRUE_CURSES
        entry["curse"] = is_curse

        if is_curse:
            new_tier = "common"
        elif eid in ULTIMATE_IDS:
            new_tier = "ultimate"
        elif eid in EPIC_IDS or eid.startswith("mt:armor/") or eid.startswith("mt:weapons/"):
            new_tier = "epic"
        elif eid in RARE_IDS:
            new_tier = "rare"
        elif eid in COMMON_IDS or eid.startswith("mt:technical/") or eid.startswith("mt:treasure/") or eid.startswith("stellarity:_technical/"):
            new_tier = "common"
        else:
            new_tier = old_tier

        entry["tier"] = new_tier

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
            if eid in CALIBRATED_TABLE_RARES:
                entry["table"] = True
                entry["table_weight"] = 1
            else:
                entry["table"] = False
                entry["tradeable"] = False
                entry["table_weight"] = 1
        elif new_tier == "common":
            entry["anvil_cost"] = 2
            entry["loot"] = True
            entry["loot_weight"] = 10
            if not entry.get("vanilla", False) and old_tier != "common":
                entry["table"] = False
                entry["tradeable"] = False
                entry["table_weight"] = 1

        if old_tier != new_tier or old_curse != is_curse:
            changed += 1

        desc = entry.get("description", "")
        clean_desc = desc.replace(" [ULTIMATE - Max 1/item]", "").replace(" [ULTIMATE]", "").strip()
        if new_tier == "ultimate":
            entry["description"] = f"{clean_desc} [ULTIMATE]"
        else:
            entry["description"] = clean_desc

    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)

    print(f"Rebalanced {changed} enchantments in enchantments_config.json.")


if __name__ == "__main__":
    main()
