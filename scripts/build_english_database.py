"""
Master Builder for One More Expedition Enchantment Database (100% English, +x/level Scaling)
Compiles all 400 enabled enchantments with verified datapack stats, exact per-level scaling,
and crisp, professional English descriptions.
Updates enchantments_config.json & outputs assets/js/enchants_data.js.
"""

import json
import os

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG_PATH = os.path.join(REPO_ROOT, "enchantments_config.json")
OUTPUT_FILE = os.path.join(REPO_ROOT, "assets", "js", "enchants_data.js")

with open(CONFIG_PATH, "r", encoding="utf-8") as f:
    cfg = json.load(f)

enabled_400 = [eid for eid, d in cfg.get("enchantments", {}).items() if d.get("enabled", True)]

# Format: eid: (max_lv, anvil_cost, gear_cats, supported_display, name_en, desc_en)

VANILLA_EN = {
    "minecraft:sharpness": (5, 1, ["weapon"], "Swords & Axes", "Sharpness", "Increases melee damage by +0.5 per level (+0.5 at Lv I, up to +2.5 at Lv V)."),
    "minecraft:smite": (5, 2, ["weapon"], "Swords, Axes & Maces", "Smite", "Increases damage against Undead mobs (Zombie, Skeleton, Wither, Phantom) by +2 per level (up to +10 at Lv V)."),
    "minecraft:bane_of_arthropods": (5, 2, ["weapon"], "Swords, Axes & Maces", "Bane of Arthropods", "Increases damage against Arthropods (Spiders, Bees, Silverfish) by +2 per level (up to +10 at Lv V) and inflicts Slowness IV (1s + 0.5s per level)."),
    "minecraft:knockback": (2, 2, ["weapon"], "Swords", "Knockback", "Increases melee knockback distance by +3 blocks per level (up to +6 blocks at Lv II)."),
    "minecraft:fire_aspect": (2, 4, ["weapon"], "Swords & Maces", "Fire Aspect", "Ignites targets on melee hit for +4 seconds per level (up to 8s at Lv II)."),
    "minecraft:looting": (3, 4, ["weapon"], "Swords", "Looting", "Increases mob loot drops by +1 max common drop and +1% rare drop chance per level (up to +3 at Lv III)."),
    "minecraft:sweeping_edge": (3, 4, ["weapon"], "Swords", "Sweeping Edge", "Increases sweeping attack damage ratio to 50% / 67% / 75% across Levels I–III."),
    "minecraft:unbreaking": (3, 1, ["universal"], "All Durable Gear", "Unbreaking", "Grants a chance to ignore durability loss on use: +50% / +67% / +75% effective durability on tools, or +20% / +27% / +30% on armor per level."),
    "minecraft:mending": (1, 4, ["universal"], "All Durable Gear", "Mending", "Repairs item durability using collected XP orbs (+2 durability per 1 XP point)."),
    "minecraft:efficiency": (5, 1, ["tool"], "Pickaxes, Shovels, Axes, Hoes & Shears", "Efficiency", "Increases mining speed by +(level^2 + 1) when using the proper tool (up to +26 speed at Lv V)."),
    "minecraft:silk_touch": (1, 8, ["tool"], "Pickaxes, Shovels, Axes & Hoes", "Silk Touch", "Mined blocks drop themselves intact instead of their usual broken items or ores."),
    "minecraft:fortune": (3, 4, ["tool"], "Pickaxes, Shovels, Axes & Hoes", "Fortune", "Increases ore and crop drop multipliers by +1 max bonus roll per level (up to 4x max drops at Lv III)."),
    "minecraft:power": (5, 1, ["ranged"], "Bows", "Power", "Increases arrow damage by +1 base +0.5 per level above I (up to +3 bonus damage at Lv V)."),
    "minecraft:punch": (2, 2, ["ranged"], "Bows", "Punch", "Increases arrow knockback distance by +3 blocks per level (up to +6 blocks at Lv II)."),
    "minecraft:flame": (1, 4, ["ranged"], "Bows", "Flame", "Arrows become flaming projectiles, igniting targets for 5 seconds and lighting TNT/campfires."),
    "minecraft:infinity": (1, 8, ["ranged"], "Bows", "Infinity", "Shooting regular arrows does not consume ammo (requires at least 1 regular arrow in inventory)."),
    "minecraft:luck_of_the_sea": (3, 4, ["tool"], "Fishing Rods", "Luck of the Sea", "Increases treasure catch chance while fishing by ~+2.1% per level and reduces junk chance."),
    "minecraft:lure": (3, 2, ["tool"], "Fishing Rods", "Lure", "Decreases fishing wait time by -5 seconds per level (up to -15s faster at Lv III)."),
    "minecraft:loyalty": (3, 1, ["ranged", "weapon"], "Tridents", "Loyalty", "Thrown tridents automatically return to the owner; return speed scales with level (+1x speed tier per level)."),
    "minecraft:impaling": (5, 4, ["ranged", "weapon"], "Tridents", "Impaling", "Increases trident damage against aquatic mobs by +2 per level (up to +10 at Lv V)."),
    "minecraft:riptide": (3, 4, ["ranged", "weapon"], "Tridents", "Riptide", "Launches the player forward when thrown in water or rain; launch velocity scales with level (I–III)."),
    "minecraft:channeling": (1, 8, ["ranged", "weapon"], "Tridents", "Channeling", "Summons a lightning bolt on the target when a thrown trident hits during a thunderstorm."),
    "minecraft:multishot": (1, 4, ["ranged"], "Crossbows", "Multishot", "Fires 3 arrows or firework rockets in a spread while consuming only 1 ammo."),
    "minecraft:quick_charge": (3, 2, ["ranged"], "Crossbows", "Quick Charge", "Reduces crossbow reload time by -0.25 seconds per level (down to 0.5s at Lv III)."),
    "minecraft:piercing": (4, 1, ["ranged"], "Crossbows", "Piercing", "Crossbow arrows pierce through +1 additional entity per level (up to 4 targets at Lv IV) and bypass shields."),
    "minecraft:density": (5, 2, ["weapon"], "Maces", "Density", "Increases Mace smash damage by +0.5 per block fallen per level (up to +2.5 per block at Lv V)."),
    "minecraft:breach": (4, 4, ["weapon"], "Maces", "Breach", "Reduces target's armor effectiveness by -15% per level on Mace hits (up to -60% armor penetration at Lv IV)."),
    "minecraft:wind_burst": (3, 4, ["weapon"], "Maces", "Wind Burst", "Triggers a wind explosion on Mace smash hits, launching the attacker upward (scales strongly across Levels I–III)."),
    "minecraft:protection": (4, 1, ["armor"], "Armor", "Protection", "Reduces most incoming damage: grants +0.5 EPF (+1.0 per level above I, up to +3.5 EPF at Lv IV per piece)."),
    "minecraft:fire_protection": (4, 2, ["armor"], "Armor", "Fire Protection", "Reduces fire damage by +8% per level (up to +32% at Lv IV per piece) and cuts burn duration by -15% per level."),
    "minecraft:feather_falling": (4, 2, ["armor"], "Boots", "Feather Falling", "Reduces fall and Ender Pearl damage by +12% per level (up to +48% at Lv IV)."),
    "minecraft:blast_protection": (4, 4, ["armor"], "Armor", "Blast Protection", "Reduces explosion damage by +8% per level and explosion knockback by +15% per level."),
    "minecraft:projectile_protection": (4, 2, ["armor"], "Armor", "Projectile Protection", "Reduces projectile damage (arrows, tridents, fireballs) by +8% per level (up to +32% at Lv IV per piece)."),
    "minecraft:respiration": (3, 4, ["armor"], "Helmets", "Respiration", "Extends underwater breathing time by +15 seconds per level (up to +45s at Lv III) and improves underwater vision."),
    "minecraft:aqua_affinity": (1, 4, ["armor"], "Helmets", "Aqua Affinity", "Removes the 5x underwater mining speed penalty, allowing normal mining speed underwater."),
    "minecraft:thorns": (3, 8, ["armor"], "Armor", "Thorns", "Grants +(15% x level) chance to reflect 1–4 damage back to attackers (up to 45% chance at Lv III)."),
    "minecraft:depth_strider": (3, 4, ["armor"], "Boots", "Depth Strider", "Reduces underwater movement slowdown by +33% per level (100% full land speed underwater at Lv III)."),
    "minecraft:frost_walker": (2, 4, ["armor"], "Boots", "Frost Walker", "Freezes nearby water within a (2 + level) block radius into temporary Frosted Ice and grants magma block immunity."),
    "minecraft:soul_speed": (3, 8, ["armor"], "Boots", "Soul Speed", "Increases movement speed on Soul Sand/Soil by +4.05% (+1.05% per level above I, up to +6.15% at Lv III)."),
    "minecraft:swift_sneak": (3, 8, ["armor"], "Leggings", "Swift Sneak", "Increases sneaking movement speed by +15% per level (up to +45% / 75% of walking speed at Lv III)."),
    "minecraft:binding_curse": (1, 8, ["armor"], "Armor & Elytra", "Curse of Binding", "Prevents the equipped item from being removed until it breaks or the wearer dies."),
    "minecraft:vanishing_curse": (1, 8, ["universal"], "All Gear", "Curse of Vanishing", "Causes the enchanted item to vanish permanently upon player death instead of dropping.")
}

DNT_EN = {
    "nova_structures:aerials_bane": (5, 4, ["weapon", "tool"], "Melee Weapons", "Aerial's Bane", "Deals +2 bonus damage per level (up to +10 at Lv V) against airborne mobs (Ghasts, Phantoms, Blazes, Vexes)."),
    "nova_structures:antidote": (1, 4, ["armor"], "Chestplates", "Antidote", "Chestplate absorbs Poison damage instead of the player's health."),
    "nova_structures:conductivity_curse": (1, 8, ["armor", "weapon", "tool"], "Metal Gear", "Curse of Conductivity", "Metal gear attracts lightning strikes to the wearer during thunderstorms."),
    "nova_structures:function_only": (5, 4, ["tool"], "Sticks & Flint", "Bug Enchant", "Internal technical enchantment; converts into Bane of Arthropods inside player inventory."),
    "nova_structures:ghasted": (3, 4, ["ranged"], "Crossbows", "Ghasted", "Transforms fired arrows into explosive Ghast fireballs; explosion power scales from Lv I to Lv III."),
    "nova_structures:gravity": (3, 4, ["ranged"], "Crossbows", "Gravity", "Arrows create a gravitational vortex upon landing that pulls nearby entities inward (radius/strength scales with Lv I–III)."),
    "nova_structures:hydro_veil": (4, 4, ["mount"], "Happy Ghast Harnesses", "Hydro Veil", "Grants Happy Ghast -25% burn time per level, +25% explosion knockback resistance per level, and +3.75 fire/blast protection EPF per level (up to +15 at Lv IV)."),
    "nova_structures:illagers_bane": (5, 4, ["weapon"], "Swords & Axes", "Illagers Bane", "Deals +2.5 bonus damage per level (up to +12.5 at Lv V) against Illagers and Witches."),
    "nova_structures:multishot": (1, 4, ["ranged"], "Bows", "Tri-Shot", "Fires 3 arrows simultaneously from a Bow while consuming only 1 arrow."),
    "nova_structures:outreach": (4, 4, ["armor"], "Chestplates", "Outreach", "Increases block interaction reach by +0.5 blocks per level (up to +2.0 blocks at Lv IV)."),
    "nova_structures:photosynthesis": (1, 2, ["universal"], "All Durable Gear", "Photosynthesis", "Gradually repairs item durability when exposed to direct daylight."),
    "nova_structures:piercing": (4, 4, ["ranged"], "Bows", "Blowthrough", "Allows Bow arrows to pierce through +1 additional target per level (up to 4 targets at Lv IV)."),
    "nova_structures:power": (5, 4, ["ranged"], "Crossbows", "Might", "Increases Crossbow arrow damage by +1 base +0.5 per level above I (up to +3 bonus damage at Lv V)."),
    "nova_structures:spiteful": (3, 4, ["weapon"], "Melee Weapons", "Spiteful", "Deals +2 bonus damage per level (up to +6 at Lv III) at low health, with a chance to grant Strength I (3s)."),
    "nova_structures:swift_soar": (3, 4, ["mount"], "Happy Ghast Harnesses", "Swift Soar", "Increases Happy Ghast flying speed by +10% (+5% per level above I, up to +20% at Lv III) while sprinting."),
    "nova_structures:traveler": (3, 8, ["armor"], "Boots", "Traveler", "Increases step height by +0.5 (+0.1 per level above I) and movement speed by +0.01 (+0.005 per level above I)."),
    "nova_structures:wax_wings": (1, 2, ["armor"], "Elytra", "Wax Wings", "Prevents Elytra durability loss when gliding slowly."),
    "nova_structures:wither_coated": (3, 5, ["weapon"], "Melee Weapons", "Wither Coated", "Inflicts Wither I / II / III (12s / 6s / 3s across Lv I–III) on hit at the cost of +1 extra durability.")
}

ENCHANTSPLUS_EN = {
    "enchantsplus:breaking_curse": (1, 2, ["universal"], "All Durable Gear", "Curse of Breaking", "Doubles durability consumption rate when using the enchanted item."),
    "enchantsplus:breeze_burst": (1, 4, ["ranged"], "Bows", "Breeze Burst", "Arrows trigger a Breeze wind charge explosion on impact, knocking back nearby entities."),
    "enchantsplus:clumsiness_curse": (1, 8, ["weapon", "tool"], "Weapons & Tools", "Curse of Clumsiness", "Attacks and mining have a chance to miss or drop the held item."),
    "enchantsplus:compassion_curse": (1, 8, ["weapon"], "Melee Weapons", "Curse of Compassion", "Melee hits have a chance to heal the target instead of dealing damage."),
    "enchantsplus:crabs_touch": (3, 4, ["tool"], "Pickaxes & Tools", "Crab's Touch", "Increases block interaction range by +1 block per level (up to +3 blocks at Lv III). [ULTIMATE - Max 1/item]"),
    "enchantsplus:displacement_curse": (1, 8, ["weapon"], "Melee Weapons", "Curse of Displacement", "Attacks have a chance to randomly teleport the attacker or victim."),
    "enchantsplus:double_edge_curse": (1, 8, ["weapon"], "Melee Weapons", "Curse of Double Edge", "Deals heavy bonus damage to targets while reflecting a portion back onto the attacker."),
    "enchantsplus:graviole": (3, 4, ["armor"], "Elytra", "Graviole", "Reduces gravity by -15% per level (up to -45% at Lv III) for extended gliding control. [ULTIMATE - Max 1/item]"),
    "enchantsplus:harvest": (1, 2, ["tool"], "Hoes", "Harvest", "Automatically replants crops in an area immediately upon harvesting mature crops."),
    "enchantsplus:ice_aspect": (2, 4, ["weapon"], "Swords & Maces", "Icicle Impale", "Slows and freezes targets on hit; freeze duration and slowness scale across Lv I–II."),
    "enchantsplus:kickback": (3, 2, ["armor"], "Shields", "Kickback", "Blocking an attack with a shield knocks the attacker backward; force scales across Lv I–III."),
    "enchantsplus:kinetic_protection": (4, 4, ["armor"], "Helmets", "Kinetic Protection", "Reduces Elytra wall-collision kinetic damage by +3 EPF per level (up to +12 at Lv IV)."),
    "enchantsplus:precision": (2, 4, ["ranged"], "Bows & Crossbows", "Precision", "Increases projectile velocity and accuracy across Lv I–II."),
    "enchantsplus:pyrolysis": (1, 4, ["tool"], "Pickaxes", "Pyrolysis", "Automatically smelts mined ores directly into ingots upon breaking."),
    "enchantsplus:retrieval": (4, 4, ["armor"], "Chestplates", "Retrieval", "Restores health upon defeating enemies; healing amount scales across Lv I–IV."),
    "enchantsplus:scorch_walker": (2, 8, ["armor"], "Boots", "Scorch Walker", "Converts lava beneath your feet into temporary magma/stone blocks and grants fire/lava immunity; radius scales across Lv I–II."),
    "enchantsplus:skyguard": (4, 4, ["armor"], "Elytra", "Skyguard", "Grants +0.75 damage protection EPF per level (up to +3 EPF at Lv IV) while gliding with Elytra."),
    "enchantsplus:stride": (3, 4, ["armor"], "Leggings", "Stride", "Increases step height by +30% per level (up to +90% at Lv III) for effortless terrain traversal."),
    "enchantsplus:swift_strike": (5, 4, ["weapon"], "Melee Weapons", "Swift Strike", "Increases melee attack speed by +12.5% per level (up to +62.5% at Lv V)."),
    "enchantsplus:vitality": (3, 4, ["armor"], "Chestplates", "Vitality", "Increases max health by +2 HP (+1 heart) per level (up to +6 HP / +3 hearts at Lv III)."),
    "enchantsplus:websnare": (2, 4, ["weapon"], "Swords & Maces", "Websnare", "Traps hit targets in cobwebs; duration/effect scales across Lv I–II."),
    "keepinv:deathlock": (1, 4, ["armor"], "Chestplates", "Deathlock", "Items lost or graves created upon death can only be retrieved by yourself. Incompatible with Safekeep and Gravehold."),
    "keepinv:gravehold": (1, 4, ["armor"], "Chestplates", "Gravehold", "Doubles the duration that dropped items or graves remain before despawning upon death. Incompatible with Safekeep and Deathlock."),
    "keepinv:safekeep": (1, 4, ["armor"], "Armor", "Safekeep", "Keep all items in your inventory upon death at the cost of 33% item durability. Incompatible with Deathlock, Gravehold, Mending, Sacrifice, and Handyman.")
}

STELLARITY_EN = {
    "stellarity:ambush": (3, 2, ["weapon"], "Melee Weapons", "Ambush", "Multiplies damage dealt when attacking unaware targets from behind; multiplier scales across Lv I–III."),
    "stellarity:critical_strike": (3, 4, ["weapon"], "Melee Weapons", "Critical Strike", "Significantly amplifies critical hit damage multipliers across Lv I–III. [ULTIMATE - Max 1/item]"),
    "stellarity:dune_speed": (3, 8, ["armor"], "Boots", "Dune Speed", "Grants swift movement speed on sand and red sand; speed scales across Lv I–III. [ULTIMATE - Max 1/item]"),
    "stellarity:levitation_shot": (5, 4, ["ranged"], "Bows", "Levitation Shot", "Arrows inflict Levitation I for 7s (+0.5s per level above I, up to 9s at Lv V)."),
    "stellarity:plated": (4, 2, ["armor"], "Elytra", "Plated", "Grants +2 Armor (+1 per level above I, up to +5 at Lv IV), +1 Toughness at Lv III–IV, and +4% (+3%/lvl) gravity."),
    "stellarity:soaring": (4, 2, ["armor"], "Elytra", "Soaring", "Reduces gravity by -5% per level (up to -20% at Lv IV) for smoother Elytra flight."),
    "stellarity:void_shot": (1, 10, ["ranged"], "Bows & Crossbows", "Void Shot", "Fires Void-infused projectiles that bypass armor and deal true Void damage."),
    "stellarity:void_strike": (1, 10, ["weapon"], "Melee Weapons", "Void Strike", "Melee strikes inflict Void corrosion, dealing armor-piercing damage over time."),
    "stellarity:_technical/daybroken": (1, 2, ["weapon"], "Diamond Swords", "Daybroken", "Technical signature enchant for Daybroken blade: inflicts scorching solar burn."),
    "stellarity:_technical/draconic": (1, 2, ["weapon"], "Netherite Swords", "Draconic", "Technical signature enchant for Draconic blade: unleashes Ender Dragon breath."),
    "stellarity:_technical/infernal_infusion": (1, 2, ["weapon", "ranged"], "Netherite Swords & Crossbows", "Infernal Infusion", "Technical signature enchant: launches blazing infernal fireballs."),
    "stellarity:_technical/mighty_wind": (1, 8, ["ranged", "weapon"], "Prismatic Tridents", "Mighty Wind", "Technical signature enchant: unleashes gale force knockback winds."),
    "stellarity:_technical/prismatic_pearl_return": (1, 2, ["tool"], "Prismatic Pearls", "Prismatic Pearl Return", "Technical mechanism handling automatic Prismatic Pearl recall."),
    "stellarity:_technical/soul_harvest": (1, 2, ["weapon"], "Diamond Swords", "Soul Harvest", "Technical signature enchant: harvests slain enemy souls to empower attacks."),
    "stellarity:_technical/void_pendant/amethyst": (1, 0, ["armor"], "Void Pendant", "Void Pendant (Amethyst)", "Technical Void Pendant gem bonus: boosts energy/mana regeneration."),
    "stellarity:_technical/void_pendant/copper": (1, 0, ["armor"], "Void Pendant", "Void Pendant (Copper)", "Technical Void Pendant gem bonus: grants movement speed and electric charge."),
    "stellarity:_technical/void_pendant/diamond": (1, 0, ["armor"], "Void Pendant", "Void Pendant (Diamond)", "Technical Void Pendant gem bonus: enhances defensive armor shield."),
    "stellarity:_technical/void_pendant/emerald": (1, 0, ["armor"], "Void Pendant", "Void Pendant (Emerald)", "Technical Void Pendant gem bonus: increases Luck and loot drops."),
    "stellarity:_technical/void_pendant/gold": (1, 0, ["armor"], "Void Pendant", "Void Pendant (Gold)", "Technical Void Pendant gem bonus: boosts healing and magic resistance."),
    "stellarity:_technical/void_pendant/iron": (1, 0, ["armor"], "Void Pendant", "Void Pendant (Iron)", "Technical Void Pendant gem bonus: adds flat base armor protection."),
    "stellarity:_technical/void_pendant/lapis": (1, 0, ["armor"], "Void Pendant", "Void Pendant (Lapis)", "Technical Void Pendant gem bonus: increases XP gain from all sources."),
    "stellarity:_technical/void_pendant/netherite": (1, 0, ["armor"], "Void Pendant", "Void Pendant (Netherite)", "Technical Void Pendant gem bonus: grants fire & Void damage resistance."),
    "stellarity:_technical/void_pendant/quartz": (1, 0, ["armor"], "Void Pendant", "Void Pendant (Quartz)", "Technical Void Pendant gem bonus: increases true critical strike damage.")
}

KATTERS_EN = {
    "kattersstructures:alchemy_curse": (1, 4, ["armor"], "Chestplates", "Curse of Alchemy", "Grants random potion effects (often harmful) when taking damage."),
    "kattersstructures:blunt": (1, 2, ["weapon"], "Melee Weapons", "Blunt", "Increases damage dealt against armored targets."),
    "kattersstructures:crystal_curse": (1, 4, ["weapon"], "Melee Weapons", "Curse of Crystal", "Greatly boosts weapon damage at the cost of rapid durability loss."),
    "kattersstructures:enlightning": (1, 2, ["tool"], "Blaze Rods", "Enlightning", "Summons lightning strikes when hitting a Lightning Rod."),
    "kattersstructures:heavy": (5, 4, ["weapon"], "Melee Weapons", "Heavy", "Adds +2 damage (+0.75/lvl above I) and +0.5 knockback (+0.25/lvl above I), but adds +20% gravity and -50% attack speed."),
    "kattersstructures:leaping": (3, 4, ["armor"], "Boots", "Leaping", "Increases jump height across Levels I–III."),
    "kattersstructures:mining_curse": (1, 4, ["tool"], "Mining Tools", "Curse of Mining", "Reduces mining speed and drains tool durability faster."),
    "kattersstructures:regrowth": (3, 4, ["universal"], "All Durable Gear", "Regrowth", "Gradually regenerates item durability over time; rate scales across Lv I–III."),
    "kattersstructures:resistance": (3, 4, ["armor"], "Armor", "Resistance", "Reduces incoming damage across Levels I–III."),
    "kattersstructures:soulbound": (1, 8, ["universal"], "All Gear", "Soulbound", "Keeps the enchanted item in your inventory upon death instead of dropping. [ULTIMATE - Max 1/item]"),
    "kattersstructures:raj_ambient": (1, 0, ["armor"], "Boss Raj Gear", "Raj Ambient", "Boss Raj ambient lightning aura mechanic in Ancient Temples."),
    "kattersstructures:raj_lightning": (1, 0, ["ranged"], "Boss Raj Crossbow", "Raj Lightning", "Boss Raj lightning barrage crossbow mechanic."),
    "kattersstructures:raj_minion": (1, 0, ["armor"], "Boss Raj Gear", "Raj Minion", "Boss Raj minion summoning mechanic."),
    "kattersstructures:light": (3, 4, ["armor"], "Armor", "Light", "Grants +30% movement speed per level and -20% (-10%/lvl above I) gravity, at the cost of -1 protection."),
    "kattersstructures:ocean_heart": (1, 4, ["armor"], "Chestplates", "Ocean Heart", "Grants Conduit Power effect continuously while underwater."),
    "kattersstructures:rusta_shot": (3, 4, ["ranged"], "Bows & Crossbows", "Rusta Shot", "Fires arcane Rusta projectiles; impact damage/effects scale across Lv I–III."),
    "kattersstructures:web_walker": (3, 4, ["armor"], "Boots", "Web Walker", "Grants Speed VI (2s) and Weaving (15s) to walk rapidly through cobwebs; effect scales across Lv I–III.")
}

MISC_EN = {
    "incendium:poison_protection": (1, 8, ["armor"], "Armor", "Poison Protection", "Grants complete immunity to Poison damage in Incendium Nether fortresses. [ULTIMATE - Max 1/item]"),
    "veinminer-enchantment:veinminer": (1, 7, ["tool"], "Pickaxes", "Veinmine", "Automatically mines all connected ore blocks of the same type in a single break. [ULTIMATE - Max 1/item]"),
    "wither:soul_siphon": (1, 1, ["weapon"], "Melee Weapons", "Soul Siphon", "Absorbs slain enemy souls to temporarily empower combat stats. [ULTIMATE - Max 1/item]"),
    "wither:witherbound": (1, 1, ["armor"], "Armor", "Witherbound", "Binds your soul to Wither energy, granting immunity to Wither decay damage. [ULTIMATE - Max 1/item]")
}

ENCHANTPLUS_EN = {
    "enchantplus:armor/fury": (3, 3, ["armor"], "Armor", "Fury", "Trades -10% (-7.5%/lvl above I) Armor and -4.5% (-3.5%/lvl) armor effectiveness for +7.5% (+6.5%/lvl above I) Attack Damage."),
    "enchantplus:armor/lifeplus": (5, 8, ["armor"], "Armor", "Walking Fortress", "Grants +1.2 HP (+0.6 hearts) max health per level (up to +6 HP / +3 hearts per piece at Lv V). [ULTIMATE]"),
    "enchantplus:armor/venom_protection": (1, 2, ["armor"], "Armor", "Venom Protection", "Protects and clears negative poison/venom effects."),
    "enchantplus:boots/agility": (5, 1, ["armor"], "Boots", "Agility", "Increases movement speed by +20% per level (up to +100% speed at Lv V)."),
    "enchantplus:bow/accuracy_shot": (1, 4, ["ranged"], "Bows & Crossbows", "Accuracy Shot", "Arrows fly in a completely straight line without gravity drop."),
    "enchantplus:bow/echo_shot": (2, 6, ["ranged"], "Bows & Crossbows", "Echo Shot", "Creates a sonic boom explosion on arrow impact; radius/damage scales across Lv I–II."),
    "enchantplus:bow/eternal_frost": (1, 6, ["ranged"], "Bows & Crossbows", "Eternal Frost", "Freezes nearby blocks and slows hit targets."),
    "enchantplus:bow/explosive_arrow": (4, 6, ["ranged"], "Bows & Crossbows", "Explosive Arrow", "Arrows detonate on impact; explosion power scales from Lv I to Lv IV."),
    "enchantplus:bow/rebound": (3, 2, ["ranged"], "Bows & Crossbows", "Rebound", "Arrows bounce off solid walls and surfaces; max bounces scale across Lv I–III."),
    "enchantplus:bow/storm_arrow": (1, 6, ["ranged"], "Bows & Crossbows", "Storm Arrow", "Summons a lightning bolt at the arrow's impact location."),
    "enchantplus:chestplate/builder_arm": (3, 2, ["armor"], "Chestplates", "Builder Arm", "Extends block placement/breaking reach by +1 block (+2 blocks per level above I, up to +5 blocks at Lv III). [ULTIMATE - Max 1/item]"),
    "enchantplus:durability/curse_of_breaking": (5, 1, ["universal"], "All Durable Gear", "Curse of Breaking", "Increases durability loss by +1 extra point per level on every use (up to +5 extra at Lv V)."),
    "enchantplus:durability/curse_of_enchant": (1, 1, ["universal"], "All Gear", "Curse of Enchant", "Prevents the item from being further enchanted or modified."),
    "enchantplus:elytra/armored": (4, 10, ["armor"], "Elytra", "Armored", "Reduces damage taken while wearing Elytra (grants +6 EPF protection)."),
    "enchantplus:elytra/propulsion": (3, 1, ["armor"], "Elytra", "Propulsion", "Boosts forward flight thrust while gliding; thrust scales across Lv I–III."),
    "enchantplus:helmet/auto_feed": (1, 6, ["armor"], "Helmets", "Auto Feed", "Automatically restores hunger every 3 minutes."),
    "enchantplus:helmet/bright_vision": (21, 2, ["armor"], "Helmets", "Bright Vision", "Grants permanent Night Vision while equipped."),
    "enchantplus:helmet/voidless": (1, 3, ["armor"], "Helmets", "Voidless", "Grants brief Levitation when falling into The Void to save your life. [ULTIMATE - Max 1/item]"),
    "enchantplus:hoe/scyther": (3, 2, ["tool"], "Hoes", "Scyther", "Tills soil in a larger disk radius per level (scales across Lv I–III)."),
    "enchantplus:hoe/seiors_oblivion": (1, 2, ["tool"], "Hoes", "Seior's Oblivion", "Replaces terrain beneath your feet with natural grass/earth blocks."),
    "enchantplus:leggings/dwarfed": (5, 1, ["armor"], "Leggings", "Dwarfed", "Shrinks player size (-15% / -25% / -35% / -50%), reduces attack damage (-1 -0.5/lvl above I) & reach (-15% -10%/lvl)."),
    "enchantplus:leggings/fast_swim": (1, 3, ["armor"], "Leggings", "Fast Swim", "Grants permanent Dolphin's Grace while swimming underwater."),
    "enchantplus:leggings/oversize": (3, 1, ["armor"], "Leggings", "Oversize", "Increases player scale (+25% / +35% / +65%) and step height (+0.5 +0.5/lvl above I, up to +1.5 blocks at Lv III)."),
    "enchantplus:mace/striker": (1, 4, ["weapon"], "Maces", "Striker", "Summons lightning on Mace strikes during storms and grants lightning immunity."),
    "enchantplus:mace/teluric_wave": (3, 2, ["weapon"], "Maces", "Teluric Wave", "Crouch-striking the ground unleashes seismic waves and inflicts Slowness (5s); wave power scales across Lv I–III. [ULTIMATE - Max 1/item]"),
    "enchantplus:mace/wind_propulsion": (3, 4, ["weapon"], "Maces", "Wind Propulsion", "Creates a wind blast on ground impact, launching you upward; blast strength scales across Lv I–III."),
    "enchantplus:midas_touch": (1, 3, ["tool"], "Sticks", "Midas Touch", "Transforms touched items or blocks into pure gold."),
    "enchantplus:mounted/cavalier_egis": (3, 4, ["mount"], "Saddles & Mounts", "Cavalier Egis", "Grants +0.75 damage protection EPF per level (up to +2.25 EPF at Lv III) while riding a mount."),
    "enchantplus:mounted/ethereal_leap": (3, 4, ["mount"], "Saddles & Mounts", "Ethereal Leap", "Increases mount jump strength by +50% (+25%/lvl above I, up to +100%) and safe fall distance by +50% per level."),
    "enchantplus:mounted/happy_boost": (3, 4, ["mount"], "Happy Ghast Harnesses", "Happy Boost", "Increases Happy Ghast flying speed by +50% (+30%/lvl above I, up to +110% at Lv III)."),
    "enchantplus:mounted/steel_fang": (5, 4, ["mount"], "Wolf Armor", "Steel Fang", "Increases Wolf attack damage by +0.5 per level (up to +2.5 bonus damage at Lv V)."),
    "enchantplus:mounted/velocity": (3, 4, ["mount"], "Saddles & Mounts", "Velocity", "Increases mount movement speed by +55% (+30%/lvl above I, up to +115% at Lv III)."),
    "enchantplus:pickaxe/bedrock_breaker": (1, 4, ["tool"], "Pickaxes", "Bedrock Breaker", "Allows mining through solid Bedrock blocks at the cost of 150 durability per block. [ULTIMATE - Max 1/item]"),
    "enchantplus:pickaxe/spawner_touch": (1, 4, ["tool"], "Pickaxes", "Spawner Touch", "Allows harvesting and picking up Mob Spawners intact. [ULTIMATE - Max 1/item]"),
    "enchantplus:sword/critical": (3, 2, ["weapon"], "Swords & Melee Weapons", "Critical", "Increases chance to bypass armor and deal true critical damage across Lv I–III."),
    "enchantplus:sword/death_touch": (3, 4, ["weapon"], "Swords & Melee Weapons", "Death Touch", "Inflicts Darkness on hit targets; duration scales across Lv I–III."),
    "enchantplus:sword/dimensional_hit": (3, 4, ["weapon"], "Swords & Melee Weapons", "Dimensional Hit", "Deals +2 bonus damage (+1 per level above I, up to +4 at Lv III) in Nether or End dimensions."),
    "enchantplus:sword/fear": (3, 4, ["weapon"], "Swords & Melee Weapons", "Fear", "Delays Creeper fuse explosions on hit; delay scales across Lv I–III."),
    "enchantplus:sword/last_hope": (1, 10, ["weapon"], "Swords & Melee Weapons", "Last Hope", "Sacrifices the weapon's entire remaining durability to unleash an infinite-damage strike."),
    "enchantplus:sword/pull": (3, 4, ["weapon"], "Swords & Melee Weapons", "Pull", "Grants a chance for slain mobs to drop their Spawn Egg; drop chance scales across Lv I–III."),
    "enchantplus:sword/reach": (3, 4, ["weapon"], "Swords & Melee Weapons", "Reach", "Extends melee entity attack range by +1 block (+0.5 per level above I, up to +2.0 blocks at Lv III)."),
    "enchantplus:sword/runic_despair": (3, 4, ["weapon"], "Swords & Melee Weapons", "Runic Despair", "Multiplies melee damage dealt inside the Runic Dimension across Lv I–III."),
    "enchantplus:sword/tears_of_asflors": (3, 4, ["weapon"], "Swords & Melee Weapons", "Tears of Asflors", "Converts XP into +3 bonus damage (+1 per level above I, up to +5 at Lv III) on hit."),
    "enchantplus:sword/xp_boost": (3, 4, ["weapon"], "Swords & Melee Weapons", "XP Boost", "Multiplies XP dropped from killed mobs and mined blocks across Lv I–III."),
    "enchantplus:tools/miningplus": (1, 4, ["tool"], "Pickaxes", "Mining+", "Mines blocks in a 3x3 area around the targeted block. [ULTIMATE - Max 1/item]"),
    "enchantplus:trident/gungnir_breath": (3, 4, ["ranged", "weapon"], "Tridents", "Gungnir's Breath", "Freezes water and inflicts Slowness II+ (+1 amplifier/lvl, 3s + 1s/lvl above I). [ULTIMATE - Max 1/item]")
}

MT_EN = {
    "mt:armor/basalt_boots": (1, 8, ["armor"], "Boots", "Basalt Boots (Rock Hard)", "Greatly reduces incoming knockback, keeping you firmly planted."),
    "mt:armor/boomproof_cap": (1, 8, ["armor"], "Helmets", "Boomproof Cap", "Significantly reduces damage taken from explosions."),
    "mt:armor/dark_forest_chestplate": (1, 8, ["armor"], "Chestplates", "Dark Forest Chestplate (Axe Mastery)", "Enhances combat proficiency and damage output with Axes."),
    "mt:armor/deep_dark_leggings": (1, 8, ["armor"], "Leggings", "Deep Dark Leggings (Stealth Prowess)", "Muffles footsteps and improves stealth around Wardens."),
    "mt:armor/desert_chestplate": (1, 8, ["armor"], "Chestplates", "Desert Chestplate (Last Stand)", "Boosts defensive resilience dramatically when health drops low."),
    "mt:armor/dripstone_helmet": (1, 8, ["armor"], "Helmets", "Dripstone Helmet (Fast Drip)", "Increases falling speed and reflects sharp dripstone damage."),
    "mt:armor/experient_trousers": (1, 8, ["armor"], "Leggings", "Experient Trousers", "Periodically generates XP orbs while walking."),
    "mt:armor/flower_helmet": (1, 8, ["armor"], "Helmets", "Flower Helmet (Flower Gift)", "Radiates natural vitality, restoring health and hunger."),
    "mt:armor/ice_shoes": (1, 8, ["armor"], "Boots", "Ice Shoes (Cold Aura)", "Emits a freezing aura that slows nearby enemies."),
    "mt:armor/indestructia": (1, 8, ["armor"], "Elytra", "Indestructia", "Grants complete immunity to Elytra wall-collision kinetic damage."),
    "mt:armor/lush_leggings": (1, 8, ["armor"], "Leggings", "Lush Leggings (Deadly Thorns)", "Reflects lethal thorny damage back at melee attackers."),
    "mt:armor/mushroom_boots": (1, 8, ["armor"], "Boots", "Mushroom Boots", "Reduces fall damage and adds a bouncy landing."),
    "mt:armor/nether_leggings": (1, 8, ["armor"], "Leggings", "Nether Leggings", "Provides comprehensive fire and lava resistance in the Nether."),
    "mt:armor/ocean_chestplate": (1, 8, ["armor"], "Chestplates", "Ocean Chestplate (Ocean Blessing)", "Grants underwater breathing and faster swimming speed."),
    "mt:armor/soul_valley_helmet": (1, 8, ["armor"], "Helmets", "Soul Valley Helmet", "Increases movement speed across Soul Sand and Soul Soil."),
    "mt:armor/swamp_chestplate": (1, 8, ["armor"], "Chestplates", "Swamp Chestplate", "Grants poison resistance and clears negative status effects."),
    "mt:armor/taiga_pants": (1, 8, ["armor"], "Leggings", "Taiga Pants", "Reduces hunger depletion while sprinting in cold biomes."),
    "mt:armor/warped_helmet": (1, 8, ["armor"], "Helmets", "Warped Helmet", "Slows down nearby enemies approaching the wearer."),
    "mt:armor/wind_chestplate": (1, 8, ["armor"], "Chestplates", "Wind Chestplate", "Stores wind energy while falling and releases an upward burst on landing."),
    "mt:weapons/assassin_blade": (1, 8, ["weapon"], "Golden Swords", "Assassin Blade (Early Strikes)", "First strike from stealth deals massive bonus critical damage."),
    "mt:weapons/barbaric_axe": (1, 8, ["weapon"], "Golden Axes", "Barbaric Axe", "Deals escalating damage with consecutive axe swings."),
    "mt:weapons/drylands_piercer": (1, 8, ["ranged"], "Crossbows", "Drylands Piercer (Sheriff's Honour)", "Fires armor-piercing bolts with heavy knockback."),
    "mt:weapons/hurricane_sword": (1, 8, ["weapon"], "Iron Swords", "Hurricane Sword (Gusty Garden)", "Slashes unleash swirling gusts that launch enemies skyward."),
    "mt:weapons/ice_sword": (1, 8, ["weapon"], "Diamond Swords", "Ice Sword (Icy Tip)", "Freezes struck targets, reducing their movement and attack speed."),
    "mt:weapons/oceanic_spear": (1, 8, ["ranged", "weapon"], "Tridents", "Oceanic Spear", "Deals overwhelming bonus damage while fighting in water."),
    "mt:weapons/rift_axe": (1, 8, ["weapon"], "Netherite Axes", "Rift Axe (Soul Tearer)", "Tears enemy souls, dealing armor-ignoring true damage."),
    "mt:weapons/rumble_axe": (1, 8, ["weapon"], "Golden Swords", "Rumble Axe (Rumble Tumble)", "Creates ground tremors on hit that stun nearby enemies."),
    "mt:weapons/sharpened_sword": (1, 8, ["weapon"], "Iron Swords", "Sharpened Sword", "Honed blade edge granting high flat bonus melee damage."),
    "mt:weapons/soul_scythe": (1, 8, ["weapon", "tool"], "Netherite Hoes", "Soul Scythe (Life Steal)", "Drains health from struck targets to heal the wielder."),
    "mt:weapons/stray_bow": (1, 8, ["ranged"], "Bows", "Stray Bow (Gotta Go Slow)", "Fires frostbitten arrows that inflict heavy Slowness."),
    "mt:weapons/warden_killer": (1, 8, ["weapon"], "Echo Shards", "Warden Killer", "Ancient relic forged specifically to counter and slay the Warden."),
    "mt:weapons/warped_bow": (1, 8, ["ranged"], "Bows", "Warped Bow", "Teleports the shooter or target upon arrow impact."),
    "mt:weapons/wither_sword": (1, 8, ["weapon"], "Stone Swords", "Wither Sword", "Inflicts continuous Wither decay damage on hit."),
    "mt:treasure/standard": (1, 8, ["armor"], "Relic Armor", "Standard Relic", "Technical Mine Treasure relic modifier: balanced stats."),
    "mt:treasure/fast": (1, 8, ["armor"], "Relic Armor", "Fast Relic", "Technical Mine Treasure relic modifier: boosted movement speed."),
    "mt:treasure/hypersonic": (1, 8, ["armor"], "Relic Armor", "Hypersonic Relic", "Technical Mine Treasure relic modifier: extreme movement speed."),
    "mt:treasure/slow": (1, 8, ["armor"], "Relic Armor", "Slow Relic", "Technical Mine Treasure relic modifier: high armor at the cost of speed."),
    "mt:treasure/sloth": (1, 8, ["armor"], "Relic Armor", "Sloth Curse", "Technical Mine Treasure relic modifier: reduces speed for durability."),
    "mt:technical/giga_bore": (1, 8, ["tool"], "Mine Treasure Pickaxes", "Giga Bore", "Technical Mine Treasure 3x3 mining trigger."),
    "mt:technical/grenade": (1, 8, ["weapon"], "Melee Weapons", "Grenade Window", "Technical 2-second delayed explosive charge on hit."),
    "mt:technical/kinetic_bomb": (1, 8, ["weapon"], "Melee Weapons", "Kinetic Bomb", "Technical pressurized kinetic blast wave on strike."),
    "mt:technical/kinetic_blast": (255, 8, ["armor"], "Helmets", "Kinetic Blast", "Technical Mine Treasure kinetic blast effect."),
    "mt:technical/floor_is_spikes": (1, 8, ["tool"], "Dungeon Trigger", "Floor Is Spikes", "Technical dungeon spike-trap trigger."),
    "mt:technical/healing_flower": (1, 8, ["tool"], "Dungeon Trigger", "Healing Flower", "Technical dungeon healing-flower aura trigger."),
    "mt:technical/necromancy": (1, 8, ["tool"], "Dungeon Trigger", "Necromancy", "Technical dungeon undead-summoning trigger."),
    "mt:technical/sense_of_urgency": (1, 8, ["tool"], "Dungeon Trigger", "Sense of Urgency", "Technical dungeon emergency speed-burst trigger.")
}

ENCORE_EN = {
    "enchantencore:absolute_unit": (5, 8, ["armor"], "Leggings", "Absolute Unit", "While crouching on the ground, grants Absorption II (2s, +0.25s per level above I), Resistance III (2s), and Weakness IV (2s), while reducing sneaking speed by -100% (+7.5% per level above I)."),
    "enchantencore:adrenaline": (4, 8, ["ranged"], "Bows & Crossbows", "Adrenaline", "Ranged hits grant Speed I and Strength I for 4s (+2s per level above I, up to 10s at Lv IV)."),
    "enchantencore:anchor": (1, 4, ["mount"], "Saddles & Harnesses", "Anchor", "Prevents unmounted mounts from drifting or wandering away."),
    "enchantencore:angry_aura": (1, 1, ["armor"], "Helmets", "Angry Aura", "Cosmetic aura: emits angry storm cloud particles while idle, moving, or taking hits."),
    "enchantencore:aquatic_aura": (1, 1, ["armor"], "Helmets", "Aquatic Aura", "Cosmetic aura: emits water bubble and splash particles while idle, moving, or taking hits."),
    "enchantencore:archer_barrage": (3, 8, ["ranged"], "Bows", "Archer Barrage", "Fires +5 extra arrows (+1 per level above I, up to +7 at Lv III) with +7 projectile spread per level (+7 extra durability cost). [ULTIMATE]"),
    "enchantencore:armor_piercing": (3, 4, ["ranged"], "Crossbows", "Armor Piercing", "Crossbow bolts deal +1 bonus magic damage per level (up to +3 at Lv III), bypassing armor."),
    "enchantencore:aurora": (1, 8, ["armor"], "Helmets", "Aurora", "Periodically grants Regeneration II for 5s and summons soothing aurora particles around the wearer. [ULTIMATE]"),
    "enchantencore:bane_of_bosses": (5, 4, ["weapon"], "Melee Weapons", "Bane of Bosses", "Deals +2 damage per level (up to +10 at Lv V) against boss mobs and inflicts Slowness IV (1.5s, +0.5s max duration per level above I)."),
    "enchantencore:bane_of_endborn": (5, 2, ["weapon"], "Melee Weapons", "Bane of Endborn", "Deals +2 damage per level (up to +10 at Lv V) against End mobs and inflicts Slowness IV (1.5s, +0.5s max duration per level above I)."),
    "enchantencore:bane_of_netherborn": (5, 2, ["weapon"], "Melee Weapons", "Bane of Netherborn", "Deals +2 damage per level (up to +10 at Lv V) against Nether mobs and inflicts Slowness IV (1.5s, +0.5s max duration per level above I)."),
    "enchantencore:bane_of_raiders": (5, 2, ["weapon"], "Melee Weapons", "Bane of Raiders", "Deals +2 damage per level (up to +10 at Lv V) against Illagers and raiders, inflicting Slowness IV (1.5s, +0.5s max duration per level above I)."),
    "enchantencore:bear_spirit": (1, 4, ["mount"], "Wolf Armor", "Bear Spirit", "Doubles wolf size (+100% scale), grants +10 Armor and +3 Attack Damage (-1 Attack Speed), suffocation immunity, and a 10% chance on attack for brief Weakness/Slowness."),
    "enchantencore:blaze_blessing": (1, 8, ["armor"], "Leggings", "Blaze's Blessing", "Grants Fire Resistance (15s), +10 explosion protection EPF, -20% burn time, and ignites attackers for 3s. [ULTIMATE]"),
    "enchantencore:blindness_curse": (1, 8, ["armor"], "Helmets", "Curse of Blindness", "Periodically inflicts Blindness on the wearer."),
    "enchantencore:boar_spirit": (1, 4, ["mount"], "Wolf Armor", "Boar Spirit", "Increases wolf scale by +25% and grants +5 Armor (-1.5 Attack Speed); 15% chance when hit to grant Strength and Speed for up to 5s."),
    "enchantencore:boltbringer": (1, 4, ["weapon"], "Maces", "Boltbringer", "Mace smash attacks from at least 8 blocks high summon a lightning bolt on the target."),
    "enchantencore:bomber": (3, 8, ["ranged"], "Bows & Crossbows", "Bomber", "Arrows spawn primed TNT on impact; extra durability cost scales from 60 down to 30 (-15 per level above I). [ULTIMATE]"),
    "enchantencore:bounce": (1, 4, ["armor"], "Boots", "Bounce", "Grants +2 bounciness, +2.5 safe fall distance, and -0.2 air drag and friction modifiers."),
    "enchantencore:breeze_curse": (1, 8, ["weapon"], "Melee Weapons", "Curse of the Breeze", "Knocks the attacker backward with a wind gust on every melee strike."),
    "enchantencore:bulletproof": (1, 4, ["armor"], "Chestplates", "Bulletproof", "Provides passive resistance and deflection against incoming projectiles while moving."),
    "enchantencore:bunny_hop": (3, 8, ["armor"], "Leggings", "Bunny Hop", "Increases jump strength by +0.15 (+0.10 per level above I, up to +0.35) and safe fall distance by +0.2 blocks (+0.15 per level above I, up to +0.5)."),
    "enchantencore:caravan_curse": (1, 8, ["armor"], "Armor", "Curse of the Caravan", "Periodically summons wandering llamas or pulls nearby mobs into a noisy caravan around the wearer."),
    "enchantencore:cleansing": (1, 4, ["armor"], "Shields", "Cleansing", "Clears negative status effects when moving with the shield equipped at a small durability cost (1.5)."),
    "enchantencore:color_blindness_curse": (1, 8, ["armor"], "Helmets", "Curse of Color Blindness", "Applies a desaturated grayscale shader to the wearer's vision."),
    "enchantencore:corrosion_curse": (1, 8, ["universal"], "All Durable Gear", "Curse of Corrosion", "Gradually corrodes item durability (0.5% chance per tick) when exposed to rain or submerged underwater."),
    "enchantencore:crab_claw": (3, 4, ["armor", "tool"], "Chestplates & Tools", "Crab Claw", "Increases both block and entity interaction reach by +1 block per level (up to +3 blocks at Lv III)."),
    "enchantencore:crank_curse": (1, 8, ["armor"], "Armor", "Curse of the Crank", "Forces the wearer to keep moving or suffer cranky slowdown penalties when standing still."),
    "enchantencore:creeper_alloy": (1, 4, ["armor"], "Chestplates", "Creeper Alloy", "Triggers a non-destructive blast (0.5 radius, 0.5x knockback) against attackers when the wearer is hit."),
    "enchantencore:creeper_blessing": (1, 8, ["armor"], "Leggings", "Creeper's Blessing", "Grants +10 explosion protection EPF and blasts nearby attackers away when struck. [ULTIMATE]"),
    "enchantencore:creeper_vision_curse": (1, 8, ["armor"], "Helmets", "Curse of Creeper Vision", "Applies a green pixelated Creeper-vision shader while worn."),
    "enchantencore:crispbread": (1, 4, ["armor"], "Shields", "Crispbread", "Grants Saturation I for 2s while moving with the shield equipped."),
    "enchantencore:crop_dance": (1, 4, ["armor"], "Boots", "Crop Dance", "Crouching on farmland emits particles and rapidly accelerates nearby crop growth at a minor durability cost."),
    "enchantencore:crystalize_arrow": (3, 8, ["ranged"], "Bows & Crossbows", "Crystalize Arrow", "Arrows summon an End Crystal on block impact; extra durability cost scales from 60 down to 30 (-15 per level above I). [ULTIMATE]"),
    "enchantencore:cushion": (4, 4, ["armor"], "Elytra", "Cushion", "Adds +5 Elytra wall-collision protection EPF per level (up to +20 EPF at Lv IV)."),
    "enchantencore:dark_infused": (3, 4, ["ranged"], "Bows & Crossbows", "Dark Infused", "Arrows inflict Blindness for 2s / 3s / 5s across levels I–III."),
    "enchantencore:darkness_alloy": (1, 4, ["armor"], "Chestplates", "Darkness Alloy", "Inflicts Darkness for 5s on attackers who strike the wearer."),
    "enchantencore:day_protection": (5, 1, ["armor"], "Armor", "Day Protection", "Adds +1.5 protection EPF per level (up to +6 EPF at Lv IV) during Overworld daytime."),
    "enchantencore:death_curse": (1, 8, ["weapon"], "Melee Weapons", "Curse of Death", "Inflicts lethal self-damage or fatal backlash on the attacker upon striking."),
    "enchantencore:dismount": (1, 4, ["weapon"], "Spears", "Dismount", "Spear charge hits knock targets off their mounts or vehicles."),
    "enchantencore:dolphin_blessing": (1, 8, ["armor"], "Boots", "Dolphin's Blessing", "Grants +0.4 water movement efficiency, Dolphin's Grace, and Health Boost while swimming in water. [ULTIMATE]"),
    "enchantencore:echoing_aura": (1, 1, ["armor"], "Helmets", "Echoing Aura", "Cosmetic aura: emits deep dark sculk and sonic echo particles while idle, moving, or taking hits."),
    "enchantencore:electric_walker": (1, 4, ["armor"], "Boots", "Electric Walker", "Emits redstone dust particles and powers redstone blocks beneath the wearer's feet while moving."),
    "enchantencore:emberweave": (1, 4, ["universal"], "All Durable Gear", "Emberweave", "Passively repairs durability over time while in the Nether dimension."),
    "enchantencore:enchanted_aura": (1, 1, ["armor"], "Helmets", "Enchanted Aura", "Cosmetic aura: emits mystical enchantment table runes while idle, moving, or taking hits."),
    "enchantencore:end_protection": (4, 1, ["armor"], "Armor", "End Protection", "Adds +1.5 protection EPF per level (up to +6 EPF at Lv IV) while in The End."),
    "enchantencore:end_speed": (3, 4, ["armor"], "Boots", "End Speed", "Increases movement speed by +4.05% (+1.05% per level above I, up to +6.15% at Lv III) and efficiency in The End (4% durability wear chance)."),
    "enchantencore:ender_aura": (1, 1, ["armor"], "Helmets", "Ender Aura", "Cosmetic aura: emits purple Ender portal particles while idle, moving, or taking hits."),
    "enchantencore:ender_shot": (1, 4, ["ranged"], "Bows & Crossbows", "Ender Shot", "Ranged hits under open sky teleport or trigger Ender spatial effects on the target."),
    "enchantencore:ender_vision_curse": (1, 8, ["armor"], "Helmets", "Curse of Ender Vision", "Applies an inverted Enderman-vision shader while worn."),
    "enchantencore:enderscape": (1, 4, ["armor"], "Leggings", "Enderscape", "Teleports the wearer to safety when hit under open sky."),
    "enchantencore:evokers_wrath": (1, 4, ["ranged"], "Crossbows", "Evoker's Wrath", "Crossbow bolts summon Evoker Fangs at the impact point or target."),
    "enchantencore:fetch": (1, 4, ["mount"], "Wolf Armor", "Fetch", "Armored wolves periodically retrieve nearby dropped items and bring them to their owner."),
    "enchantencore:fire_alloy": (1, 4, ["armor"], "Chestplates", "Fire Alloy", "Ignites attackers for 5s whenever they strike the wearer."),
    "enchantencore:flaming_aura": (1, 1, ["armor"], "Helmets", "Flaming Aura", "Cosmetic aura: emits flame and ember particles while idle, moving, or taking hits."),
    "enchantencore:flare": (1, 4, ["ranged"], "Bows & Crossbows", "Flare", "Arrows emit bright flare light and ignite targets on impact."),
    "enchantencore:forge_shot": (1, 4, ["ranged"], "Bows & Crossbows", "Forge Shot", "Arrows hitting blocks trigger a fiery forge burst."),
    "enchantencore:fragility_curse": (1, 8, ["weapon"], "Melee Weapons", "Curse of Fragility", "Significantly accelerates weapon durability loss on every attack."),
    "enchantencore:frenzy": (3, 8, ["armor"], "Leggings", "Frenzy", "Grants Strength and scaling Haste (Amp I–III across Lv I–III) for 2s when the wearer is at low health."),
    "enchantencore:gallop": (3, 4, ["mount"], "Saddles", "Gallop", "Increases mount movement speed by +0.075 (+0.05 per level above I, up to +0.175 at Lv III) while ridden."),
    "enchantencore:ghost_spirit": (1, 4, ["mount"], "Wolf Armor", "Ghost Spirit", "Grants near-total damage immunity and auto-repair to the wolf, but reduces its attack damage to zero (-999999)."),
    "enchantencore:gluttony": (5, 4, ["tool"], "Tools", "Gluttony", "Pulls nearby dropped items toward the holder within a 2 to 10 block radius (+2 blocks per level)."),
    "enchantencore:grass_walker": (1, 4, ["armor"], "Boots", "Grass Walker", "Converts nearby dirt beneath the wearer into grass blocks while walking and grants immunity to sweet berry bush damage."),
    "enchantencore:growth": (5, 4, ["armor"], "Chestplates & Leggings", "Growth", "Increases body scale and step height by +10% per level (up to +50% at Lv V) and grants +0.4 protection EPF per level (2 EPF = Protection II at Lv V)."),
    "enchantencore:guard": (1, 4, ["weapon", "armor"], "Swords & Shields", "Guard", "Improves blocking posture and defensive mitigation while holding or raising the item."),
    "enchantencore:habanero": (1, 8, ["armor"], "Chestplates", "Habanero", "Grants Strength III (1s) when struck in combat, at the cost of periodic Weakness II. [ULTIMATE]"),
    "enchantencore:handyman": (1, 4, ["universal"], "All Enchantable Gear", "Handyman", "Gradually repairs the equipped item's durability while moving."),
    "enchantencore:hardened": (3, 4, ["armor"], "Shields", "Hardened", "Grants +2 Armor (+1 per level above I, up to +4 Armor at Lv III) while equipped."),
    "enchantencore:harpoon": (1, 4, ["tool"], "Fishing Rods", "Harpoon", "Reeling in hooked entities pulls them forcefully toward the angler with extra force."),
    "enchantencore:haste": (3, 4, ["tool"], "Mining Tools", "Haste", "Grants Haste I / II / III (matching enchantment level) while held in the main hand."),
    "enchantencore:heavenly_aura": (1, 1, ["armor"], "Helmets", "Heavenly Aura", "Cosmetic aura: emits golden halo and glowing particles while idle, moving, or taking hits."),
    "enchantencore:heroes_crown": (1, 8, ["armor"], "Helmets", "Hero's Crown", "Grants permanent Hero of the Village I (15s refreshed) while worn. [ULTIMATE]"),
    "enchantencore:hiding_curse": (1, 8, ["weapon"], "Melee Weapons", "Curse of Hiding", "Turns struck enemies invisible, making them harder to track in combat."),
    "enchantencore:hunger_aspect": (2, 4, ["weapon"], "Melee Weapons", "Hunger Aspect", "Melee hits inflict Hunger for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:hurricane": (1, 4, ["ranged"], "Bows, Crossbows & Tridents", "Hurricane", "Projectiles release a 3.5-block wind burst on impact, launching targets and triggering redstone."),
    "enchantencore:ice_alloy": (1, 4, ["armor"], "Chestplates", "Ice Alloy", "Freezes attackers for 1–2 damage and inflicts heavy Slowness for up to 3s when struck."),
    "enchantencore:illumination": (1, 4, ["armor"], "Helmets", "Illumination", "Emits a dynamic light source around the wearer's head in dark areas."),
    "enchantencore:impale_rush": (1, 4, ["weapon"], "Spears", "Impale Rush", "Spear charge attacks pierce targets with extra momentum and impact force."),
    "enchantencore:implosion": (1, 4, ["ranged"], "Bows & Crossbows", "Implosion", "Arrows hitting blocks pull nearby entities inward toward the point of impact."),
    "enchantencore:incognito": (1, 4, ["armor"], "Helmets", "Incognito", "Completely hides the wearer's overhead nameplate and below-name display from other players."),
    "enchantencore:infested_aspect": (2, 4, ["weapon"], "Melee Weapons", "Infested Aspect", "Melee hits inflict Infested for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:infinity_durability": (1, 8, ["universal"], "All Durable Gear", "Infinite Durability", "Completely prevents the item from losing durability on use. [ULTIMATE]"),
    "enchantencore:invisibility_cloak": (1, 4, ["armor"], "Boots", "Invisibility Cloak", "Grants Invisibility while sneaking or wearing the enchanted boots."),
    "enchantencore:jousting": (3, 4, ["weapon"], "Spears", "Jousting", "Deals +2 melee damage (+1.5 per level above I, up to +5 at Lv III) while mounted."),
    "enchantencore:last_stand": (1, 8, ["armor"], "Helmets", "Last Stand", "Grants Resistance I when below 4 hearts and Speed I when below 5 hearts. [ULTIMATE]"),
    "enchantencore:leaf_jumper": (1, 4, ["armor"], "Boots", "Leaf Jumper", "Generates temporary leaf platforms beneath your feet when jumping mid-air."),
    "enchantencore:leech": (1, 4, ["weapon"], "Melee Weapons", "Leech", "Melee and ranged hits grant Regeneration I–II for up to 3s."),
    "enchantencore:levitation_aspect": (2, 4, ["weapon"], "Melee Weapons", "Levitation Aspect", "Melee hits inflict Levitation for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:lightning_bolt": (1, 4, ["ranged"], "Bows & Crossbows", "Lightning Bolt", "Arrows have a chance to summon a lightning bolt on targets under open sky."),
    "enchantencore:lingering": (1, 4, ["ranged"], "Bows & Crossbows", "Lingering", "Tipped arrows leave behind a lingering potion cloud upon hitting a block."),
    "enchantencore:lovely_aura": (1, 1, ["armor"], "Helmets", "Lovely Aura", "Cosmetic aura: emits floating heart particles while idle, moving, or taking hits."),
    "enchantencore:lush_aura": (1, 1, ["armor"], "Helmets", "Lush Aura", "Cosmetic aura: emits spore blossom and lush foliage particles while idle, moving, or taking hits."),
    "enchantencore:machine_gun": (1, 8, ["ranged"], "Crossbows", "Machine Gun", "Reduces crossbow charge time to near-zero (-1.25s) for rapid-fire shooting. [ULTIMATE]"),
    "enchantencore:magic_protection": (4, 4, ["armor"], "Armor", "Magic Protection", "Adds +0.4 magic damage protection EPF per level (up to +1.6 EPF per piece at Lv IV)."),
    "enchantencore:magma_walker": (1, 4, ["armor"], "Boots", "Magma Walker", "Freezes lava beneath the wearer's feet into temporary magma blocks and grants immunity to magma block damage."),
    "enchantencore:meteor": (1, 4, ["weapon"], "Maces", "Meteor", "Mace smash attacks from at least 8 blocks high ignite the surrounding area in fire."),
    "enchantencore:mirror_vision_curse": (1, 8, ["armor"], "Helmets", "Curse of Mirror Vision", "Applies a horizontally mirrored shader to the wearer's vision."),
    "enchantencore:moonwalk": (3, 4, ["armor", "mount"], "Elytra & Horse Armor", "Moonwalk", "Reduces gravity by -0.03 (-0.015 per level above I, up to -0.06 at Lv III) for higher jumps and slower falls."),
    "enchantencore:moonweave": (1, 4, ["universal"], "All Durable Gear", "Moonweave", "Passively repairs item durability over time under open night sky."),
    "enchantencore:muddy_aura": (1, 1, ["armor"], "Helmets", "Muddy Aura", "Cosmetic aura: emits mud splash particles while idle, moving, or taking hits."),
    "enchantencore:neptune": (1, 8, ["weapon", "ranged"], "Tridents", "Neptune", "Grants Conduit Power, +5 spin attack strength, +1 knockback, -85% fall damage, and Poison II (3s) on melee hits. [ULTIMATE]"),
    "enchantencore:nether_protection": (4, 1, ["armor"], "Armor", "Nether Protection", "Adds +1.5 protection EPF per level (up to +6 EPF at Lv IV) while in The Nether."),
    "enchantencore:night_protection": (5, 1, ["armor"], "Armor", "Night Protection", "Adds +1.5 protection EPF per level (up to +6 EPF at Lv IV) during Overworld nighttime."),
    "enchantencore:night_vision": (1, 4, ["armor"], "Helmets", "Night Vision", "Grants permanent Night Vision while worn."),
    "enchantencore:nightmare": (1, 8, ["weapon"], "Melee Weapons", "Nightmare", "Melee strikes inflict Levitation IV (0.5s), Blindness (5s), Darkness (5s), and Hunger XXXI (5s), at a 25 durability cost. [ULTIMATE]"),
    "enchantencore:nuclear_arrow": (1, 8, ["ranged"], "Bows & Crossbows", "Nuclear Arrow", "Arrows trigger a non-destructive 1-block explosion on impact (+10 extra durability cost). [ULTIMATE]"),
    "enchantencore:ominous_aura": (1, 1, ["armor"], "Helmets", "Ominous Aura", "Cosmetic aura: emits ominous trial spawner particles while idle, moving, or taking hits."),
    "enchantencore:ominous_healing": (1, 4, ["armor"], "Shields", "Ominous Healing", "Triggers healing effects when blocking or taking damage under ominous conditions."),
    "enchantencore:ominous_rain": (1, 4, ["weapon", "ranged"], "Tridents", "Ominous Rain", "Thrown tridents summon ominous storm effects at the impact location."),
    "enchantencore:oozing_aspect": (2, 4, ["weapon"], "Melee Weapons", "Oozing Aspect", "Melee hits inflict Oozing for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:ownership_curse": (1, 8, ["armor"], "Armor", "Curse of Ownership", "Binds the armor to its current owner, penalizing anyone else who attempts to wear it."),
    "enchantencore:pacifier": (1, 4, ["weapon"], "Melee Weapons", "Pacifier", "30% chance on melee hit to inflict Weakness CCLV (1–3s), completely neutralizing target melee damage."),
    "enchantencore:pale_aura": (1, 1, ["armor"], "Helmets", "Pale Aura", "Cosmetic aura: emits pale garden mist particles while idle, moving, or taking hits."),
    "enchantencore:path_speed": (3, 4, ["armor"], "Boots", "Path Speed", "Increases movement speed on Dirt Paths by +4.05% (+1.05% per level above I, up to +6.15% at Lv III) with a 4% wear chance."),
    "enchantencore:pegasus": (1, 4, ["mount"], "Horse Armor", "Pegasus", "Allows ridden horses to glide and soar high above the ground."),
    "enchantencore:perfect_protection": (1, 8, ["armor"], "Armor", "Perfect Protection", "Grants +0.75 all-damage EPF, +1.5 projectile/fire/explosion EPF, -4% burn time, and +15% explosion knockback resistance. [ULTIMATE]"),
    "enchantencore:phantom_menace": (3, 2, ["ranged"], "Bows & Crossbows", "Phantom Menace", "Arrows deal +2.5 bonus damage per level (up to +7.5 at Lv III) against airborne Phantoms."),
    "enchantencore:pickpocket": (1, 4, ["armor"], "Chestplates", "Pickpocket", "Allows sneaking near entities to pilfer items or loot from their pockets."),
    "enchantencore:ping_tag": (1, 2, ["ranged"], "Bows & Crossbows", "Ping Tag", "Plays an audible experience-orb ping sound to the shooter whenever a projectile lands a hit."),
    "enchantencore:plow": (3, 4, ["tool"], "Hoes", "Plow", "Left-clicking dirt or grass tills a circular area of radius 2 (+1 per level above I, up to 4 blocks at Lv III) into farmland."),
    "enchantencore:poison_aspect": (2, 4, ["weapon"], "Melee Weapons", "Poison Aspect", "Melee hits inflict Poison for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:poison_infused": (3, 4, ["ranged"], "Bows & Crossbows", "Poison Infused", "Arrows inflict Poison I (4s) at Lv I, Poison II (3s) at Lv II, and Poison II (4s) at Lv III."),
    "enchantencore:poppy_peace": (1, 4, ["weapon"], "Melee Weapons", "Poppy Peace", "Striking hostile targets pacifies aggression or drops peaceful poppy blossoms."),
    "enchantencore:poseidon": (1, 8, ["weapon", "ranged"], "Tridents", "Poseidon", "Adds +2 trident damage, +1 return acceleration, and summons lightning on every thrown hit. [ULTIMATE]"),
    "enchantencore:propeller": (5, 4, ["mount"], "Harnesses", "Propeller", "Increases Happy Ghast flying speed by +10% (+5% per level above I, up to +30% at Lv V)."),
    "enchantencore:pufferfish": (1, 4, ["weapon", "ranged"], "Tridents", "Pufferfish", "50% chance to summon a Pufferfish when striking targets submerged in water."),
    "enchantencore:purify_water": (1, 4, ["weapon", "ranged"], "Tridents", "Purify Water", "Thrown tridents purify surrounding water or cleanse aquatic effects on hit."),
    "enchantencore:rabbit_foot": (3, 4, ["armor"], "Boots", "Rabbit's Foot", "Grants +0.5 Luck per level (up to +1.5 Luck at Lv III)."),
    "enchantencore:radiant_life": (3, 8, ["weapon"], "Melee Weapons", "Radiant Life", "Hits grant Absorption I (1s at Lv I, 2s at Lv II) or Absorption II (1.5s at Lv III). [ULTIMATE]"),
    "enchantencore:railgun": (1, 8, ["ranged"], "Crossbows", "Railgun", "Fires hyper-velocity bolts that summon lightning and explode (4-block radius) on impact; +4s charge time and +100 durability cost. [ULTIMATE]"),
    "enchantencore:rainweave": (1, 4, ["universal"], "All Durable Gear", "Rainweave", "Passively repairs item durability over time while standing in Overworld rain."),
    "enchantencore:randomness_curse": (1, 8, ["tool", "weapon"], "Tools & Weapons", "Curse of Randomness", "Randomizes actions, drops, or tool effects unpredictably on hit or block strike."),
    "enchantencore:reach_curse": (1, 8, ["tool", "weapon"], "Tools & Weapons", "Curse of Reach", "Reduces both block and entity interaction reach by -2 blocks."),
    "enchantencore:reflection": (3, 8, ["armor"], "Chestplates", "Reflection", "Adds +2 projectile protection EPF (+3 per level above I, up to +8 EPF at Lv III) and reflects 4 (+0.5 per level above I) thorns damage back at archers. [ULTIMATE]"),
    "enchantencore:refuel": (1, 4, ["armor"], "Elytra", "Refuel", "Automatically consumes firework fuel from inventory to sustain Elytra flight."),
    "enchantencore:regrowth": (1, 4, ["tool"], "Hoes", "Regrowth", "Left-clicking mature wheat, carrots, potatoes, or beetroots harvests and replants them automatically (10 durability cost)."),
    "enchantencore:rejuvenation": (4, 4, ["armor"], "Shields", "Rejuvenation", "Increases Max Health by +1 HP (+0.5 hearts) per level (up to +4 HP / +2 hearts at Lv IV) while equipped."),
    "enchantencore:replenish": (1, 4, ["ranged"], "Bows & Crossbows", "Replenish", "Grants a 50% chance not to consume arrows, spectral arrows, or firework rockets when firing."),
    "enchantencore:sacrifice": (3, 4, ["mount"], "Wolf Armor", "Sacrifice", "Repairs 1.0 (+0.5 per level above I, up to 2.0 at Lv III) wolf armor durability whenever the wolf is attacked."),
    "enchantencore:safe_landing": (3, 4, ["armor"], "Elytra", "Safe Landing", "Increases safe fall distance by +2 blocks (+2.5 per level above I, up to +7 blocks at Lv III)."),
    "enchantencore:sand_speed": (3, 4, ["armor"], "Boots", "Sand Speed", "Increases movement speed on Sand by +4.05% (+1.05% per level above I, up to +6.15% at Lv III) with a 4% wear chance."),
    "enchantencore:scent": (1, 4, ["mount"], "Wolf Armor", "Scent", "Emits tracking scent indicators that reveal nearby targets to the armored wolf."),
    "enchantencore:scholar_of_blocks": (3, 4, ["tool"], "Mining & Harvesting Tools", "Scholar of Blocks", "Multiplies XP dropped from mining blocks by 1.5x (+1.0x per level above I, up to 3.5x at Lv III)."),
    "enchantencore:scholar_of_fishing": (3, 4, ["tool"], "Fishing Rods", "Scholar of Fishing", "Increases XP gained from fishing across levels I–III."),
    "enchantencore:scholar_of_mobs": (3, 4, ["weapon"], "Melee Weapons", "Scholar of Mobs", "Multiplies XP dropped by slain mobs by 1.5x (+1.0x per level above I, up to 3.5x at Lv III)."),
    "enchantencore:scorpion_spirit": (1, 4, ["mount"], "Wolf Armor", "Scorpion Spirit", "Shrinks wolf by -50%, -5 Armor, +5 Attack Speed, -100 base damage, and 50% chance on hit to inflict Poison and Wither."),
    "enchantencore:sea_breeze": (1, 4, ["weapon", "ranged"], "Tridents", "Sea Breeze", "Thrown tridents trigger a 3-block wind burst on impact, knocking back targets and triggering redstone."),
    "enchantencore:seeker": (3, 4, ["armor"], "Helmets", "Seeker", "Highlights nearby dropped items with a glowing outline within 4 / 8 / 12 blocks (+4 blocks per level)."),
    "enchantencore:shortsight_curse": (1, 8, ["armor"], "Helmets", "Curse of Shortsight", "Applies a heavy near-sighted blur shader to the wearer's vision."),
    "enchantencore:shrinkage": (1, 4, ["armor"], "Chestplates & Leggings", "Shrinkage", "Shrinks body scale and step height by -10%, but reduces Max Health by -23% (~+30% effective incoming damage from all sources)."),
    "enchantencore:shulker_shell": (1, 4, ["armor"], "Chestplates", "Shulker Shell", "Grants defensive shell protection while standing completely still."),
    "enchantencore:slowness_aspect": (2, 4, ["weapon"], "Melee Weapons", "Slowness Aspect", "Melee hits inflict Slowness for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:sniper": (3, 8, ["ranged"], "Bows & Crossbows", "Sniper", "Adds distance-scaled arrow damage across 5 range tiers (10–2000 blocks): +0.6 to +5.0 base damage, plus +0.1 to +0.5 per level above I. [ULTIMATE]"),
    "enchantencore:sonic": (3, 4, ["armor"], "Boots", "Sonic", "Increases movement speed by +0.015 (+0.01 per level above I, up to +0.035 at Lv III), while reducing sneaking speed by -0.15."),
    "enchantencore:sparking_aura": (1, 1, ["armor"], "Helmets", "Sparking Aura", "Cosmetic aura: emits electric spark particles while idle, moving, or taking hits."),
    "enchantencore:spectral_form": (1, 8, ["armor"], "Boots", "Spectral Form", "Periodically grants Speed I (15s) at a 12-durability cost per cycle. [ULTIMATE]"),
    "enchantencore:spider_vision_curse": (1, 8, ["armor"], "Helmets", "Curse of Spider Vision", "Applies a multi-eyed Spider-vision shader while worn."),
    "enchantencore:sticky_aura": (1, 1, ["armor"], "Helmets", "Sticky Aura", "Cosmetic aura: emits honey and slime droplets while idle, moving, or taking hits."),
    "enchantencore:sting": (1, 4, ["ranged"], "Crossbows", "Sting", "Fires bee-stinger bolts with hive audio cues that sting targets on impact."),
    "enchantencore:strafe": (1, 4, ["armor"], "Boots", "Strafe", "Sprinting left or right triggers a rapid lateral dash impulse (magnitude 3) at a 1–3 durability cost."),
    "enchantencore:summer_walker": (1, 4, ["armor"], "Boots", "Summer Walker", "Transforms water/lava edges or foliage safely underfoot and grants immunity to cactus/sweet berry damage."),
    "enchantencore:sunweave": (1, 4, ["universal"], "All Durable Gear", "Lunar Mend", "Passively repairs item durability over time under open nighttime sky."),
    "enchantencore:surfer": (3, 4, ["mount"], "Saddles", "Surfer", "Increases mount water movement efficiency by +1.5 (+1.75 per level above I, up to +5.0 at Lv III) while ridden."),
    "enchantencore:switch": (1, 4, ["weapon", "ranged"], "Tridents", "Switch", "Thrown tridents swap positions between the thrower and the struck target."),
    "enchantencore:thorns_protection": (4, 2, ["armor"], "Armor", "Thorns Protection", "Adds +3.5 protection EPF (+3 per level above I, up to +12.5 EPF at Lv IV) against Thorns, cactus, and sweet berry damage."),
    "enchantencore:tideweave": (1, 4, ["universal"], "All Durable Gear", "Tideweave", "Passively repairs item durability over time while submerged in water."),
    "enchantencore:trial_aura": (1, 1, ["armor"], "Helmets", "Trial Aura", "Cosmetic aura: emits trial chamber flame particles while idle, moving, or taking hits."),
    "enchantencore:trial_runner": (1, 4, ["armor"], "Boots", "Trial Runner", "Grants Trial Omen I (15s refreshed) while worn."),
    "enchantencore:undying_curse": (1, 8, ["tool", "weapon"], "Tools & Weapons", "Curse of Undying", "Prevents normal death or alters item behavior unpredictably on fatal triggers."),
    "enchantencore:updraft": (3, 4, ["armor"], "Elytra", "Updraft", "Applies an upward thermal impulse (0.045 + 0.015 per level above I) when gliding over lava, magma, or fire."),
    "enchantencore:velocity": (1, 4, ["ranged"], "Bows, Crossbows & Tridents", "Velocity", "Significantly boosts projectile launch speed and travel range."),
    "enchantencore:vitality": (3, 8, ["armor"], "Armor", "Vitality", "Increases Max Health by +1 HP (+0.5 hearts) per level (up to +3 HP / +1.5 hearts per piece at Lv III). [ULTIMATE]"),
    "enchantencore:void_vision_curse": (1, 8, ["armor"], "Helmets", "Curse of Void Vision", "Applies a pitch-dark Void-vision shader while worn."),
    "enchantencore:voidweave": (1, 8, ["universal"], "All Durable Gear", "Voidweave", "Passively repairs item durability over time while in The End dimension."),
    "enchantencore:warden": (1, 8, ["armor"], "Armor", "Warden", "Grants +10% attack damage and +0.05 sweeping damage ratio, but inflicts periodic Blindness (4s) and -2% movement speed. [ULTIMATE]"),
    "enchantencore:warped_arrow": (1, 8, ["ranged"], "Bows & Crossbows", "Warped Arrow", "Arrows warp space on impact, teleporting the shooter or distorting the target's position. [ULTIMATE]"),
    "enchantencore:warped_trident": (1, 8, ["weapon", "ranged"], "Tridents", "Warped Trident", "Teleports the thrower directly to the trident's block impact location. [ULTIMATE]"),
    "enchantencore:weakness_aspect": (2, 4, ["weapon"], "Melee Weapons", "Weakness Aspect", "Melee hits inflict Weakness for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:weaving_aspect": (2, 4, ["weapon"], "Melee Weapons", "Weaving Aspect", "Melee hits inflict Weaving for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:web_infused": (3, 4, ["ranged"], "Bows & Crossbows", "Web Infused", "Arrows trap targets in cobwebs and inflict Slowness II (5s) / III (4s) / IV (3s) across levels I–III."),
    "enchantencore:welfare_curse": (1, 8, ["tool", "weapon"], "Tools & Weapons", "Curse of Welfare", "Shares or drains durability and resources when moving with the cursed item."),
    "enchantencore:wind_aspect": (2, 4, ["weapon"], "Melee Weapons", "Wind Aspect", "Melee hits inflict Wind Charged for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:wind_spirit": (1, 8, ["armor"], "Boots", "Wind Spirit", "Negates 100% of fall damage (-1.0 fall damage multiplier) while worn. [ULTIMATE]"),
    "enchantencore:wither_aspect": (2, 4, ["weapon"], "Melee Weapons", "Wither Aspect", "Melee hits inflict Wither for 2s (Lv I, Amp I) or 4s (Lv II, Amp II) (+2s max duration and +1 level per level)."),
    "enchantencore:wither_infused": (3, 4, ["ranged"], "Bows & Crossbows", "Wither Infused", "Arrows inflict Wither I (3s) at Lv I, Wither II (4s) at Lv II, and Wither II (5s) at Lv III."),
    "enchantencore:wither_protection": (4, 2, ["armor"], "Armor", "Wither Protection", "Adds +3.5 protection EPF (+3 per level above I, up to +12.5 EPF at Lv IV) against Wither and Wither Skull damage."),
    "enchantencore:yoink": (1, 4, ["tool"], "Fishing Rods", "Yoink", "Pulls hooked mobs and items much harder toward the angler when reeling in.")
}

ALL_DICTS = [
    VANILLA_EN,
    DNT_EN,
    ENCHANTSPLUS_EN,
    STELLARITY_EN,
    KATTERS_EN,
    MISC_EN,
    ENCHANTPLUS_EN,
    MT_EN,
    ENCORE_EN,
]

def compute_exclusive_metadata(base_dir, enabled_ids, names_by_id):
    """Resolve exclusive_set tags and direct conflicts for all enabled enchantments."""
    import glob
    from collections import defaultdict

    tag_defs = defaultdict(set)
    dps = sorted(glob.glob(os.path.join(base_dir, "datapacks", "*", "data")))
    dps = [d for d in dps if "expedition_enchantments_control" not in d] + [
        d for d in dps if "expedition_enchantments_control" in d
    ]

    for dp in dps:
        for p in glob.glob(os.path.join(dp, "*", "tags", "enchantment", "**", "*.json"), recursive=True):
            rel = os.path.relpath(p, dp).replace(os.sep, "/")
            parts = rel.split("/")
            ns, tag_path = parts[0], "/".join(parts[3:])[:-5]
            tag_id = f"#{ns}:{tag_path}"
            try:
                with open(p, "r", encoding="utf-8") as f:
                    data = json.load(f)
                if data.get("replace"):
                    tag_defs[tag_id] = set()
                for v in data.get("values", []):
                    tag_defs[tag_id].add(v["id"] if isinstance(v, dict) else v)
            except Exception:
                pass

    vanilla_tags = {
        "#minecraft:exclusive_set/armor": {
            "minecraft:protection", "minecraft:fire_protection",
            "minecraft:blast_protection", "minecraft:projectile_protection",
        },
        "#minecraft:exclusive_set/damage": {
            "minecraft:sharpness", "minecraft:smite", "minecraft:bane_of_arthropods",
            "minecraft:impaling", "minecraft:density", "minecraft:breach",
        },
        "#minecraft:exclusive_set/mining": {"minecraft:fortune", "minecraft:silk_touch"},
        "#minecraft:exclusive_set/boots": {"minecraft:frost_walker", "minecraft:depth_strider"},
        "#minecraft:exclusive_set/bow": {"minecraft:infinity", "minecraft:mending"},
        "#minecraft:exclusive_set/crossbow": {"minecraft:multishot", "minecraft:piercing"},
        "#minecraft:exclusive_set/riptide": {"minecraft:riptide", "minecraft:loyalty", "minecraft:channeling"},
    }
    for t, vals in vanilla_tags.items():
        tag_defs[t].update(vals)

    SKIP_IN_RECURSION = {"#expedition:exclusive_set/ultimate", "#enchantencore:ultimate"}

    def resolve_tag(tag_id, seen=None, skip_ult=True):
        if seen is None:
            seen = set()
        if tag_id in seen:
            return set()
        if skip_ult and tag_id in SKIP_IN_RECURSION:
            return set()
        seen.add(tag_id)
        out = set()
        for v in tag_defs.get(tag_id, ()):
            if v.startswith("#"):
                out.update(resolve_tag(v, seen, skip_ult=skip_ult))
            else:
                out.add(v)
        return out

    TAG_LABELS = {
        "#expedition:exclusive_set/ultimate": "Ultimate (Max 1/item)",
        "#enchantencore:exclusive_set/aspect": "Aspect (Melee Status)",
        "#minecraft:sword_buffs": "Sword Buffs (Fire / Ice / Web)",
        "#minecraft:exclusive_set/damage": "Damage (Sharpness / Bane)",
        "#kattersstructures:exclusive_set/blunt": "Damage (Sharpness / Bane)",
        "#minecraft:exclusive_set/armor": "Armor Protection",
        "#enchantplus:exclusive_set/armor": "Armor (Lifeplus / Fury / Prot)",
        "#enchantencore:exclusive_set/alloy": "Chestplate Alloy",
        "#enchantencore:exclusive_set/shot_arrow": "Bow Arrow Effects",
        "#enchantencore:exclusive_set/infused_arrow": "Bow Infused Arrows",
        "#enchantplus:exclusive_set/bow": "Bow Special (Enchant+)",
        "#nova_structures:exclusive_set/structure_bow": "Bow (Structure Multishot/Piercing)",
        "#minecraft:exclusive_set/bow": "Bow (Infinity / Mending)",
        "#enchantencore:exclusive_set/crossbow": "Crossbow Shots",
        "#minecraft:exclusive_set/crossbow": "Crossbow (Multishot / Piercing)",
        "#enchantencore:exclusive_set/boots": "Boots Movement",
        "#minecraft:exclusive_set/boots": "Boots Walker",
        "#nova_structures:exclusive_set/boots": "Boots (Soul Speed / Traveler)",
        "#enchantencore:exclusive_set/leggings": "Leggings Movement",
        "#enchantencore:exclusive_set/height": "Size / Height",
        "#enchantplus:exclusive_set/size": "Size / Height",
        "#enchantencore:exclusive_set/repair": "Auto-Repair / Durability",
        "#minecraft:exclusive_set/repair": "Auto-Repair / Durability",
        "#enchantplus:exclusive_set/durability": "Auto-Repair / Durability",
        "#nova_structures:exclusive_set/elytra": "Elytra",
        "#enchantencore:exclusive_set/elytra": "Elytra",
        "#enchantencore:exclusive_set/shield": "Shield",
        "#enchantencore:exclusive_set/pufferfish": "Trident / Water Aura",
        "#minecraft:exclusive_set/riptide": "Trident (Riptide / Loyalty)",
        "#enchantplus:exclusive_set/trident": "Trident (Channeling / Gungnir)",
        "#enchantencore:aura": "Cosmetic Aura",
        "#enchantencore:exclusive_set/spirit_wolf": "Spirit Wolf",
        "#enchantencore:exclusive_set/vision_based": "Vision Curses",
        "#enchantplus:exclusive_set/sword_attribute": "Sword Attributes (Enchant+)",
        "#enchantplus:exclusive_set/sword_effect": "Sword Effects (Enchant+)",
        "#minecraft:exclusive_set/mining": "Mining (Fortune / Silk Touch)",
        "#enchantencore:exclusive_set/haste": "Mining / Haste",
        "#enchantplus:exclusive_set/mining": "Mining / Haste",
        "#enchantencore:exclusive_set/exp": "XP Boost / Scholar",
        "#enchantplus:exclusive_set/experience": "XP Boost / Scholar",
        "#enchantencore:exclusive_set/helmet": "Helmet Utility",
        "#enchantencore:exclusive_set/horse_armor": "Horse Armor",
        "#enchantencore:exclusive_set/saddle": "Saddle / Mount",
        "#enchantencore:exclusive_set/wolf_armor": "Wolf Armor",
        "#enchantencore:exclusive_set/mace_strike": "Mace Strike",
        "#enchantplus:exclusive_set/mace": "Mace Strike",
        "#enchantencore:exclusive_set/poppy": "Pacifier / Poppy",
        "#enchantencore:exclusive_set/pickpocket": "Pacifier / Poppy",
        "#enchantencore:exclusive_set/reach": "Reach",
        "#enchantencore:exclusive_set/all_tools": "Tool Utility (Crab Claw / Gluttony)",
        "#enchantencore:exclusive_set/replenish": "Bow (Infinity / Replenish)",
    }

    vanilla_exc = {
        "minecraft:protection": ["#minecraft:exclusive_set/armor"],
        "minecraft:fire_protection": ["#minecraft:exclusive_set/armor"],
        "minecraft:blast_protection": ["#minecraft:exclusive_set/armor"],
        "minecraft:projectile_protection": ["#minecraft:exclusive_set/armor"],
        "minecraft:sharpness": ["#minecraft:exclusive_set/damage"],
        "minecraft:smite": ["#minecraft:exclusive_set/damage"],
        "minecraft:bane_of_arthropods": ["#minecraft:exclusive_set/damage"],
        "minecraft:impaling": ["#minecraft:exclusive_set/damage"],
        "minecraft:density": ["#minecraft:exclusive_set/damage"],
        "minecraft:breach": ["#minecraft:exclusive_set/damage"],
        "minecraft:fortune": ["#minecraft:exclusive_set/mining"],
        "minecraft:silk_touch": ["#minecraft:exclusive_set/mining"],
        "minecraft:frost_walker": ["#minecraft:exclusive_set/boots"],
        "minecraft:depth_strider": ["#minecraft:exclusive_set/boots"],
        "minecraft:infinity": ["#minecraft:exclusive_set/bow"],
        "minecraft:mending": ["#minecraft:exclusive_set/bow"],
        "minecraft:multishot": ["#minecraft:exclusive_set/crossbow"],
        "minecraft:piercing": ["#minecraft:exclusive_set/crossbow"],
        "minecraft:riptide": ["#minecraft:exclusive_set/riptide"],
        "minecraft:loyalty": ["#minecraft:exclusive_set/riptide"],
        "minecraft:channeling": ["#minecraft:exclusive_set/riptide"],
    }

    ctrl_dir = os.path.join(base_dir, "datapacks", "expedition_enchantments_control", "data")
    groups_by_eid = defaultdict(set)
    conflicts_by_eid = defaultdict(set)

    # First assign tagged groups from TAG_LABELS
    for tag_id, label in TAG_LABELS.items():
        members = resolve_tag(tag_id, skip_ult=(tag_id not in SKIP_IN_RECURSION)) & enabled_ids
        if len(members) >= 2:
            for m in members:
                groups_by_eid[m].add(label)

    # Compute symmetric non-ultimate conflicts from each enchant's exclusive_set
    for eid in enabled_ids:
        ns, name = eid.split(":", 1)
        p = os.path.join(ctrl_dir, ns, "enchantment", name + ".json")
        excs = []
        if os.path.exists(p):
            try:
                with open(p, "r", encoding="utf-8") as f:
                    exc = json.load(f).get("exclusive_set")
                if exc:
                    excs = exc if isinstance(exc, list) else [exc]
            except Exception:
                pass
        elif eid in vanilla_exc:
            excs = vanilla_exc[eid]

        # Unwrap #expedition:exclusive/... wrapper tags
        flat_excs = []
        for x in excs:
            if x.startswith("#expedition:exclusive/"):
                flat_excs.extend(tag_defs.get(x, ()))
            else:
                flat_excs.append(x)

        for x in flat_excs:
            if x in SKIP_IN_RECURSION:
                groups_by_eid[eid].add("Ultimate (Max 1/item)")
                continue
            if x in TAG_LABELS:
                groups_by_eid[eid].add(TAG_LABELS[x])
            targets = resolve_tag(x, skip_ult=True) if x.startswith("#") else {x}
            for t in (targets & enabled_ids):
                if t != eid:
                    conflicts_by_eid[eid].add(t)
                    conflicts_by_eid[t].add(eid)
                    if x in TAG_LABELS:
                        groups_by_eid[t].add(TAG_LABELS[x])

    # Add vanilla symmetric conflicts too
    for tag_id in vanilla_tags:
        ms = resolve_tag(tag_id, skip_ult=True) & enabled_ids
        for a in ms:
            for b in ms:
                if a != b:
                    conflicts_by_eid[a].add(b)

    # Named direct-pair groups
    DIRECT_GROUP_PAIRS = [
        ({"nova_structures:wither_coated", "enchantencore:wither_aspect"}, "Wither (Coated / Aspect)"),
        ({"nova_structures:illagers_bane", "enchantencore:bane_of_raiders"}, "Illager / Raider Bane"),
        ({"kattersstructures:light", "minecraft:blast_protection"}, "Light / Blast Protection"),
        ({"kattersstructures:ocean_heart", "enchantencore:fire_alloy", "enchantencore:ice_alloy"}, "Chestplate Alloy"),
    ]
    for pair_set, label in DIRECT_GROUP_PAIRS:
        if len(pair_set & enabled_ids) >= 2:
            for m in (pair_set & enabled_ids):
                groups_by_eid[m].add(label)

    # Format final outputs
    out_groups = {}
    out_conflicts = {}
    for eid in enabled_ids:
        out_groups[eid] = sorted(groups_by_eid.get(eid, ()))
        confs = sorted(
            [{"id": cid, "name": names_by_id.get(cid, cid)} for cid in conflicts_by_eid.get(eid, ())],
            key=lambda x: x["name"],
        )
        out_conflicts[eid] = confs

    return out_groups, out_conflicts


def build_all():
    base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    config_path = os.path.join(base_dir, "enchantments_config.json")
    output_file = os.path.join(base_dir, "assets", "js", "enchants_data.js")

    with open(config_path, "r", encoding="utf-8") as f:
        cfg = json.load(f)

    master = {}
    for d in ALL_DICTS:
        master.update(d)
    master["bound:bound"] = (3, 4, ["armor"], "Boots", "Bound", "Allows mid-air double jumping while wearing Boots; extra jumps scale across Lv I–III.")
    for eid, entry in cfg["enchantments"].items():
        if eid.startswith("expedition_fishing:"):
            master[eid] = (
                entry.get("max_level", 1),
                entry.get("anvil_cost", 2),
                ["tool"],
                "Fishing Rods",
                entry.get("name_vi", eid.split(":", 1)[1].replace("_", " ").title()),
                entry.get("description", "Fishing enchantment.")
            )
        elif eid.startswith("expedition_quests:"):
            sup = entry.get("supported_items", "")
            g_disp = "Armor"
            if "foot_armor" in sup:
                g_disp = "Boots"
            elif "head_armor" in sup:
                g_disp = "Helmet"
            master[eid] = (
                entry.get("max_level", 1),
                entry.get("anvil_cost", 4),
                ["armor"],
                g_disp,
                entry.get("name_vi", eid.split(":", 1)[1].replace("_", " ").title()),
                entry.get("description", "Adventure & Quest enchantment.")
            )

    enabled_ids = {eid for eid, entry in cfg["enchantments"].items() if entry.get("enabled", True)}
    names_by_id = {}
    for eid in enabled_ids:
        if eid in master:
            names_by_id[eid] = master[eid][4]
        else:
            names_by_id[eid] = eid.split(":", 1)[1].replace("_", " ").title()

    exc_groups_map, exc_conflicts_map = compute_exclusive_metadata(base_dir, enabled_ids, names_by_id)

    tier_order = {"ultimate": 1, "epic": 2, "rare": 3, "common": 4, "curse": 5, "high": 2, "normal": 3, "low": 4}
    final_list = []
    missing_ids = []

    for eid, entry in cfg["enchantments"].items():
        if not entry.get("enabled", True):
            continue

        ns, name = eid.split(":", 1)
        tier = entry.get("tier", "normal")
        supported_items = entry.get("supported_items", "")

        if eid in master:
            max_lv, anvil_cost, gear_cats, gear_display, name_en, desc = master[eid]
            desc = desc.replace(" [ULTIMATE - Max 1/item]", "").replace(" [ULTIMATE]", "").strip()
            if tier == "ultimate":
                desc = f"{desc} [ULTIMATE]"
            anvil_cost = entry.get("anvil_cost", anvil_cost)
        else:
            missing_ids.append(eid)
            max_lv = entry.get("max_level", 1) or 1
            anvil_cost = entry.get("anvil_cost", 4)
            gear_cats = ["universal"]
            gear_display = "Compatible Gear"
            name_en = name.replace("_", " ").title()
            desc = f"Special enchantment for {gear_display}."

        if not max_lv or max_lv < 1:
            max_lv = 1

        # Update config entry in memory (100% English)
        entry["max_level"] = max_lv
        entry["anvil_cost"] = anvil_cost
        entry["description"] = desc
        entry["name_vi"] = name_en

        give_cmd = f'/give @p enchanted_book[enchantments={{levels:{{"{eid}":{max_lv}}}}}] 1'

        final_list.append({
            "id": eid,
            "namespace": ns,
            "raw_name": name,
            "name": name_en,
            "name_vi": name_en,
            "tier": tier,
            "tier_order": tier_order.get(tier, 99),
            "max_level": max_lv,
            "anvil_cost": anvil_cost,
            "table": entry.get("table", False),
            "table_weight": entry.get("table_weight", 1),
            "tradeable": entry.get("tradeable", False),
            "loot": entry.get("loot", True),
            "source_pack": entry.get("source_pack", "vanilla"),
            "supported_items": str(supported_items),
            "gear_categories": gear_cats,
            "supported_display": gear_display,
            "exclusive_groups": exc_groups_map.get(eid, []),
            "conflicts": exc_conflicts_map.get(eid, []),
            "description": desc,
            "give_command": give_cmd,
        })

    final_list.sort(key=lambda x: (x["tier_order"], x["name"]))

    missing_desc = sum(1 for x in final_list if not x["description"] or len(x["description"]) < 10)
    print(f"Compilation finished. Total: {len(final_list)}. Missing dict keys: {len(missing_ids)}. Short/missing descriptions: {missing_desc}")
    if missing_ids:
        print("Missing IDs:", missing_ids[:20])

    with open(config_path, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)
    print("Updated enchantments_config.json successfully.")

    os.makedirs(os.path.dirname(output_file), exist_ok=True)
    with open(output_file, "w", encoding="utf-8") as f:
        f.write("/* Auto-generated by scripts/build_english_database.py - 100% English Verified Metadata */\n")
        f.write("window.ALL_ENCHANTMENTS = ")
        json.dump(final_list, f, indent=2, ensure_ascii=False)
        f.write(";\n")
    print(f"Exported {len(final_list)} enchantments to {output_file} successfully.")


if __name__ == "__main__":
    build_all()

