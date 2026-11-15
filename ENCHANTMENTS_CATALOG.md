# Enchantments Master Catalog (A → Z)

> Tự động tổng hợp bởi `scripts/enchant_manager.py` từ `enchantments_config.json`.

## 1. Tổng Quan Phân Bố 4 Độ Hiếm

| Tier | Màu sắc | Số lượng | Nguồn kiếm chính | Quy tắc đặc biệt |
| :--- | :--- | :---: | :--- | :--- |
| **Ultimate** | Gold (`gold`) | **40** | Rương kho báu / Hầm ngục hiếm | **Tối đa 1 Ultimate / vật phẩm** |
| **Epic** | Light Purple (`light_purple`) | **67** | Rương kho báu / Hầm ngục | Không ra ở bàn phù phép / dân làng |
| **Rare** | Aqua (`aqua`) | **104** | Hầm ngục + 2–5% ở Bàn phù phép | Không bán ở dân làng |
| **Common** | Gray (`gray`) | **155** | Bàn phù phép, Dân làng, Rương | Phù phép cơ bản |
| **Curses** | Red (`red`) | **34** | Rương kho báu / Curse | Lời nguyền |
| **Filtered Duplicates** | — | **16** | Đã tắt | Lọc trùng giữa các pack |

## 2. Xác Suất Rare Khi Enchant Theo Loại Trang Bị (Mục tiêu 2% – 5%)

| Loại trang bị | Tổng Enchant | Tổng Weight | Số Rare Enchantable | Tỉ lệ Rare (%) |
| :--- | :---: | :---: | :---: | :---: |
| **Sword** | 13 | 116 | 3 | **2.59%** |
| **Pickaxe** | 4 | 41 | 2 | **4.88%** |
| **Axe** | 10 | 109 | 2 | **1.83%** |
| **Bow** | 5 | 39 | 2 | **5.13%** |
| **Crossbow** | 4 | 43 | 1 | **2.33%** |
| **Trident** | 6 | 64 | 1 | **1.56%** |
| **Helmet** | 11 | 99 | 2 | **2.02%** |
| **Chestplate** | 10 | 91 | 2 | **2.2%** |
| **Leggings** | 8 | 67 | 2 | **2.99%** |
| **Boots** | 11 | 90 | 3 | **3.33%** |

## 3. Ultimate Enchants (Tối đa 1 / vật phẩm) (40)

| Enchantment ID | Max Lv | Nguồn Pack | Table? | Loot Weight | Items áp dụng |
| :--- | :---: | :--- | :---: | :---: | :--- |
| `enchantencore:absolute_unit` | 5 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/leg_armor` |
| `enchantencore:archer_barrage` | 3 | `enchantments_encore` | No | 1 | `minecraft:bow` |
| `enchantencore:aurora` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/head_armor` |
| `enchantencore:blaze_blessing` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/leg_armor` |
| `enchantencore:bomber` | 3 | `enchantments_encore` | No | 1 | `#enchantencore:all_bows` |
| `enchantencore:creeper_blessing` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/leg_armor` |
| `enchantencore:crystalize_arrow` | 3 | `enchantments_encore` | No | 1 | `#enchantencore:all_bows` |
| `enchantencore:infinity_durability` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/durability` |
| `enchantencore:last_stand` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/head_armor` |
| `enchantencore:machine_gun` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/crossbow` |
| `enchantencore:neptune` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/trident` |
| `enchantencore:nightmare` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:nuclear_arrow` | 1 | `enchantments_encore` | No | 1 | `#enchantencore:all_bows` |
| `enchantencore:perfect_protection` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/armor` |
| `enchantencore:poseidon` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/trident` |
| `enchantencore:radiant_life` | 3 | `enchantments_encore` | No | 1 | `#enchantencore:weapons` |
| `enchantencore:railgun` | 1 | `enchantments_encore` | No | 1 | `minecraft:crossbow` |
| `enchantencore:reflection` | 3 | `enchantments_encore` | No | 1 | `#minecraft:chest_armor` |
| `enchantencore:sniper` | 3 | `enchantments_encore` | No | 1 | `#enchantencore:all_bows` |
| `enchantencore:warden` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/armor` |
| `enchantencore:warped_arrow` | 1 | `enchantments_encore` | No | 1 | `#enchantencore:all_bows` |
| `enchantencore:warped_trident` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/trident` |
| `enchantencore:wind_spirit` | 1 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/foot_armor` |
| `enchantplus:armor/lifeplus` | 5 | `NeoEnchant-5.16.0` | No | 1 | `#minecraft:enchantable/armor` |
| `enchantplus:chestplate/builder_arm` | 3 | `NeoEnchant-5.16.0` | No | 1 | `#minecraft:enchantable/chest_armor` |
| `enchantplus:helmet/voidless` | 1 | `NeoEnchant-5.16.0` | No | 1 | `#minecraft:enchantable/head_armor` |
| `enchantplus:mace/teluric_wave` | 3 | `NeoEnchant-5.16.0` | No | 1 | `#minecraft:enchantable/mace` |
| `enchantplus:pickaxe/bedrock_breaker` | 1 | `NeoEnchant-5.16.0` | No | 1 | `#voxel:enchantable/pickaxes` |
| `enchantplus:pickaxe/spawner_touch` | 1 | `NeoEnchant-5.16.0` | No | 1 | `#voxel:enchantable/pickaxes` |
| `enchantplus:sword/last_hope` | 1 | `NeoEnchant-5.16.0` | No | 1 | `#minecraft:enchantable/melee_weapon` |
| `enchantplus:tools/miningplus` | 1 | `NeoEnchant-5.16.0` | No | 1 | `#minecraft:enchantable/mining_loot` |
| `enchantplus:trident/gungnir_breath` | 3 | `NeoEnchant-5.16.0` | No | 1 | `#minecraft:enchantable/trident` |
| `enchantsplus:graviole` | 3 | `Enchants Plus v2.1 26.3` | No | 1 | `minecraft:elytra` |
| `minecraft:wind_burst` | 3 | `enchantments_encore` | No | 1 | `#minecraft:enchantable/mace` |
| `stellarity:critical_strike` | 3 | `Stellarity-6.0.1` | No | 1 | `#minecraft:enchantable/sharp_weapon` |
| `stellarity:void_shot` | 1 | `Stellarity-6.0.1` | No | 1 | `#stellarity:enchantable/ranged` |
| `stellarity:void_strike` | 1 | `Stellarity-6.0.1` | No | 1 | `#minecraft:enchantable/sharp_weapon` |
| `veinminer-enchantment:veinminer` | 1 | `veinminer-enchantment-1.3.6` | No | 1 | `#minecraft:pickaxes` |
| `wither:soul_siphon` | 1 | `[26.3-26.4]witherswrath-1.17` | No | 1 | `#minecraft:enchantable/weapon` |
| `wither:witherbound` | 1 | `[26.3-26.4]witherswrath-1.17` | No | 1 | `#minecraft:enchantable/armor` |

## 4. Epic Enchants (Chỉ kiếm từ Kho báu / Hầm ngục) (67)

| Enchantment ID | Max Lv | Nguồn Pack | Table? | Loot Weight | Items áp dụng |
| :--- | :---: | :--- | :---: | :---: | :--- |
| `enchantencore:adrenaline` | 4 | `enchantments_encore` | No | 2 | `#enchantencore:all_bows` |
| `enchantencore:bear_spirit` | 1 | `enchantments_encore` | No | 2 | `minecraft:wolf_armor` |
| `enchantencore:dolphin_blessing` | 1 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:emberweave` | 1 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/durability` |
| `enchantencore:frenzy` | 3 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/leg_armor` |
| `enchantencore:ghost_spirit` | 1 | `enchantments_encore` | No | 2 | `minecraft:wolf_armor` |
| `enchantencore:haste` | 3 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/mining` |
| `enchantencore:invisibility_cloak` | 1 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:leech` | 1 | `enchantments_encore` | No | 2 | `#enchantencore:weapons` |
| `enchantencore:lightning_bolt` | 1 | `enchantments_encore` | No | 2 | `#enchantencore:all_bows` |
| `enchantencore:moonweave` | 1 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/durability` |
| `enchantencore:pegasus` | 1 | `enchantments_encore` | No | 2 | `#enchantencore:all_horse_armor` |
| `enchantencore:refuel` | 1 | `enchantments_encore` | No | 2 | `minecraft:elytra` |
| `enchantencore:sunweave` | 1 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/durability` |
| `enchantencore:vitality` | 3 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/armor` |
| `enchantencore:voidweave` | 1 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/durability` |
| `enchantplus:boots/agility` | 5 | `NeoEnchant-5.16.0` | No | 2 | `#minecraft:enchantable/foot_armor` |
| `enchantplus:mace/striker` | 1 | `NeoEnchant-5.16.0` | No | 2 | `#minecraft:enchantable/mace` |
| `enchantplus:sword/critical` | 3 | `NeoEnchant-5.16.0` | No | 2 | `#minecraft:enchantable/melee_weapon` |
| `enchantplus:sword/xp_boost` | 3 | `NeoEnchant-5.16.0` | No | 2 | `#voxel:enchantable/mining_and_damage` |
| `enchantsplus:pyrolysis` | 1 | `Enchants Plus v2.1 26.3` | No | 2 | `#minecraft:enchantable/mining_loot` |
| `enchantsplus:swift_strike` | 5 | `Enchants Plus v2.1 26.3` | No | 2 | `#minecraft:enchantable/sharp_weapon` |
| `enchantsplus:vitality` | 3 | `Enchants Plus v2.1 26.3` | No | 2 | `#minecraft:enchantable/chest_armor` |
| `minecraft:breach` | 4 | `minecraft (vanilla)` | No | 2 | `#minecraft:enchantable/mace` |
| `minecraft:density` | 5 | `minecraft (vanilla)` | No | 2 | `#minecraft:enchantable/mace` |
| `minecraft:frost_walker` | 2 | `minecraft (vanilla)` | No | 2 | `#minecraft:enchantable/foot_armor` |
| `minecraft:mending` | 1 | `minecraft (vanilla)` | No | 2 | `#minecraft:enchantable/durability` |
| `minecraft:soul_speed` | 3 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/foot_armor` |
| `minecraft:swift_sneak` | 3 | `enchantments_encore` | No | 2 | `#minecraft:enchantable/leg_armor` |
| `mt:armor/basalt_boots` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:foot_armor` |
| `mt:armor/boomproof_cap` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:foot_armor` |
| `mt:armor/dark_forest_chestplate` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:chest_armor` |
| `mt:armor/deep_dark_leggings` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:leg_armor` |
| `mt:armor/desert_chestplate` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:chest_armor` |
| `mt:armor/dripstone_helmet` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:head_armor` |
| `mt:armor/experient_trousers` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:leg_armor` |
| `mt:armor/flower_helmet` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:head_armor` |
| `mt:armor/ice_shoes` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:foot_armor` |
| `mt:armor/indestructia` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:elytra` |
| `mt:armor/lush_leggings` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:leg_armor` |
| `mt:armor/mushroom_boots` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:foot_armor` |
| `mt:armor/nether_leggings` | 1 | `mine-treasure-1.6.5` | No | 2 | `#leg_armor` |
| `mt:armor/ocean_chestplate` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:chest_armor` |
| `mt:armor/soul_valley_helmet` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:head_armor` |
| `mt:armor/swamp_chestplate` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:chest_armor` |
| `mt:armor/taiga_pants` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:leg_armor` |
| `mt:armor/warped_helmet` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:foot_armor` |
| `mt:armor/wind_chestplate` | 1 | `mine-treasure-1.6.5` | No | 2 | `#minecraft:chest_armor` |
| `mt:weapons/assassin_blade` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:golden_sword` |
| `mt:weapons/barbaric_axe` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:golden_axe` |
| `mt:weapons/drylands_piercer` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:crossbow` |
| `mt:weapons/hurricane_sword` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:iron_sword` |
| `mt:weapons/ice_sword` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:diamond_sword` |
| `mt:weapons/oceanic_spear` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:trident` |
| `mt:weapons/rift_axe` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:netherite_axe` |
| `mt:weapons/rumble_axe` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:golden_sword` |
| `mt:weapons/sharpened_sword` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:iron_sword` |
| `mt:weapons/soul_scythe` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:netherite_hoe` |
| `mt:weapons/stray_bow` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:bow` |
| `mt:weapons/warden_killer` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:echo_shard` |
| `mt:weapons/warped_bow` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:bow` |
| `mt:weapons/wither_sword` | 1 | `mine-treasure-1.6.5` | No | 2 | `minecraft:stone_sword` |
| `nova_structures:ghasted` | 3 | `Dungeons and Taverns v6.1.0` | No | 2 | `#minecraft:enchantable/crossbow` |
| `nova_structures:multishot` | 1 | `Dungeons and Taverns v6.1.0` | No | 2 | `#minecraft:enchantable/bow` |
| `nova_structures:photosynthesis` | 1 | `Dungeons and Taverns v6.1.0` | No | 2 | `#minecraft:enchantable/durability` |
| `stellarity:ambush` | 3 | `Stellarity-6.0.1` | No | 2 | `#minecraft:enchantable/sharp_weapon` |
| `stellarity:plated` | 4 | `Stellarity-6.0.1` | No | 2 | `#stellarity:enchantable/elytra` |

## 5. Rare Enchants (Kho báu + 2–5% Bàn phù phép) (104)

| Enchantment ID | Max Lv | Nguồn Pack | Table? | Loot Weight | Items áp dụng |
| :--- | :---: | :--- | :---: | :---: | :--- |
| `enchantencore:armor_piercing` | 3 | `enchantments_encore` | No | 5 | `minecraft:crossbow` |
| `enchantencore:bane_of_bosses` | 5 | `enchantments_encore` | No | 5 | `#enchantencore:weapons` |
| `enchantencore:boar_spirit` | 1 | `enchantments_encore` | No | 5 | `minecraft:wolf_armor` |
| `enchantencore:boltbringer` | 1 | `enchantments_encore` | No | 5 | `minecraft:mace` |
| `enchantencore:bulletproof` | 1 | `enchantments_encore` | No | 5 | `#minecraft:chest_armor` |
| `enchantencore:crab_claw` | 3 | `enchantments_encore` | No | 5 | `#enchantencore:valid_for_crab_claw` |
| `enchantencore:day_protection` | 4 | `enchantments_encore` | Yes | 5 | `#minecraft:enchantable/armor` |
| `enchantencore:end_protection` | 4 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/armor` |
| `enchantencore:end_speed` | 3 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:ender_shot` | 1 | `enchantments_encore` | No | 5 | `#enchantencore:all_bows` |
| `enchantencore:enderscape` | 1 | `enchantments_encore` | No | 5 | `#minecraft:leg_armor` |
| `enchantencore:evokers_wrath` | 1 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/crossbow` |
| `enchantencore:fetch` | 1 | `enchantments_encore` | No | 5 | `minecraft:wolf_armor` |
| `enchantencore:flare` | 1 | `enchantments_encore` | No | 5 | `#enchantencore:all_bows` |
| `enchantencore:habanero` | 1 | `enchantments_encore` | No | 5 | `#minecraft:chest_armor` |
| `enchantencore:heroes_crown` | 1 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/head_armor` |
| `enchantencore:hurricane` | 1 | `enchantments_encore` | No | 5 | `minecraft:bow, minecraft:trident...` |
| `enchantencore:implosion` | 1 | `enchantments_encore` | No | 5 | `#enchantencore:all_bows` |
| `enchantencore:incognito` | 1 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/head_armor` |
| `enchantencore:jousting` | 3 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/lunge` |
| `enchantencore:levitation_aspect` | 2 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:magic_protection` | 4 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/armor` |
| `enchantencore:meteor` | 1 | `enchantments_encore` | No | 5 | `minecraft:mace` |
| `enchantencore:moonwalk` | 3 | `enchantments_encore` | No | 5 | `minecraft:elytra, minecraft:diamond_horse_armor...` |
| `enchantencore:nether_protection` | 4 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/armor` |
| `enchantencore:night_protection` | 4 | `enchantments_encore` | Yes | 5 | `#minecraft:enchantable/armor` |
| `enchantencore:night_vision` | 1 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/head_armor` |
| `enchantencore:pacifier` | 1 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:path_speed` | 3 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:poison_aspect` | 2 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:poison_infused` | 3 | `enchantments_encore` | No | 5 | `#enchantencore:all_bows` |
| `enchantencore:regrowth` | 1 | `enchantments_encore` | No | 5 | `#minecraft:hoes` |
| `enchantencore:rejuvenation` | 4 | `enchantments_encore` | No | 5 | `minecraft:shield` |
| `enchantencore:replenish` | 1 | `enchantments_encore` | No | 5 | `#enchantencore:all_bows` |
| `enchantencore:sand_speed` | 3 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:scholar_of_blocks` | 3 | `enchantments_encore` | No | 5 | `#enchantencore:no_weapons` |
| `enchantencore:scholar_of_fishing` | 3 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/fishing` |
| `enchantencore:scholar_of_mobs` | 3 | `enchantments_encore` | No | 5 | `#enchantencore:weapons` |
| `enchantencore:scorpion_spirit` | 1 | `enchantments_encore` | No | 5 | `minecraft:wolf_armor` |
| `enchantencore:shulker_shell` | 1 | `enchantments_encore` | No | 5 | `#minecraft:chest_armor` |
| `enchantencore:sonic` | 3 | `enchantments_encore` | No | 5 | `#minecraft:foot_armor` |
| `enchantencore:trial_runner` | 1 | `enchantments_encore` | No | 5 | `#minecraft:foot_armor` |
| `enchantencore:velocity` | 1 | `enchantments_encore` | No | 5 | `minecraft:bow, minecraft:crossbow...` |
| `enchantencore:web_infused` | 3 | `enchantments_encore` | No | 5 | `#enchantencore:all_bows` |
| `enchantencore:wither_aspect` | 2 | `enchantments_encore` | No | 5 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:wither_infused` | 3 | `enchantments_encore` | No | 5 | `#enchantencore:all_bows` |
| `enchantplus:armor/fury` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#minecraft:enchantable/armor` |
| `enchantplus:bow/accuracy_shot` | 1 | `NeoEnchant-5.16.0` | No | 5 | `#voxel:enchantable/range` |
| `enchantplus:bow/echo_shot` | 2 | `NeoEnchant-5.16.0` | No | 5 | `#voxel:enchantable/range` |
| `enchantplus:bow/eternal_frost` | 1 | `NeoEnchant-5.16.0` | No | 5 | `#voxel:enchantable/range` |
| `enchantplus:bow/explosive_arrow` | 4 | `NeoEnchant-5.16.0` | No | 5 | `#voxel:enchantable/range` |
| `enchantplus:bow/rebound` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#voxel:enchantable/range` |
| `enchantplus:bow/storm_arrow` | 1 | `NeoEnchant-5.16.0` | No | 5 | `#voxel:enchantable/range` |
| `enchantplus:elytra/armored` | 4 | `NeoEnchant-5.16.0` | No | 5 | `#voxel:enchantable/elytra` |
| `enchantplus:elytra/propulsion` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#voxel:enchantable/elytra` |
| `enchantplus:leggings/fast_swim` | 1 | `NeoEnchant-5.16.0` | No | 5 | `#minecraft:enchantable/leg_armor` |
| `enchantplus:mace/wind_propulsion` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#minecraft:enchantable/mace` |
| `enchantplus:midas_touch` | 1 | `NeoEnchant-5.16.0` | No | 5 | `minecraft:stick` |
| `enchantplus:mounted/ethereal_leap` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#enchantplus:enchantable/mounted` |
| `enchantplus:mounted/happy_boost` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#minecraft:harnesses` |
| `enchantplus:mounted/velocity` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#enchantplus:enchantable/mounted` |
| `enchantplus:sword/death_touch` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#minecraft:enchantable/weapon` |
| `enchantplus:sword/dimensional_hit` | 3 | `NeoEnchant-5.16.0` | Yes | 5 | `#minecraft:enchantable/melee_weapon` |
| `enchantplus:sword/pull` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#minecraft:enchantable/weapon` |
| `enchantplus:sword/reach` | 3 | `NeoEnchant-5.16.0` | Yes | 5 | `#minecraft:enchantable/melee_weapon` |
| `enchantplus:sword/runic_despair` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#minecraft:enchantable/melee_weapon` |
| `enchantplus:sword/tears_of_asflors` | 3 | `NeoEnchant-5.16.0` | No | 5 | `#minecraft:enchantable/melee_weapon` |
| `enchantsplus:breeze_burst` | 1 | `Enchants Plus v2.1 26.3` | No | 5 | `#minecraft:enchantable/bow` |
| `enchantsplus:crabs_touch` | 3 | `Enchants Plus v2.1 26.3` | No | 5 | `#enchantsplus:tools` |
| `enchantsplus:ice_aspect` | 2 | `Enchants Plus v2.1 26.3` | Yes | 5 | `#minecraft:enchantable/fire_aspect` |
| `enchantsplus:kickback` | 3 | `Enchants Plus v2.1 26.3` | No | 5 | `minecraft:shield` |
| `enchantsplus:retrieval` | 4 | `Enchants Plus v2.1 26.3` | No | 5 | `#minecraft:enchantable/chest_armor` |
| `enchantsplus:scorch_walker` | 2 | `Enchants Plus v2.1 26.3` | No | 5 | `#minecraft:enchantable/foot_armor` |
| `incendium:poison_protection` | 1 | `Incendium_Legacy_v5.5.4+26.3` | No | 5 | `#enchantable/armor` |
| `kattersstructures:blunt` | 1 | `Katters Structures v2.9` | No | 5 | `#minecraft:enchantable/sharp_weapon` |
| `kattersstructures:enlightning` | 1 | `Katters Structures v2.9` | No | 5 | `minecraft:blaze_rod` |
| `kattersstructures:light` | 3 | `Katters Structures v2.9` | No | 5 | `#minecraft:enchantable/armor` |
| `kattersstructures:ocean_heart` | 1 | `Katters Structures v2.9` | No | 5 | `#minecraft:enchantable/chest_armor` |
| `kattersstructures:raj_ambient` | 1 | `Katters Structures v2.9` | No | 5 | `#minecraft:enchantable/chest_armor` |
| `kattersstructures:raj_lightning` | 1 | `Katters Structures v2.9` | No | 5 | `minecraft:crossbow` |
| `kattersstructures:raj_minion` | 1 | `Katters Structures v2.9` | No | 5 | `#minecraft:enchantable/chest_armor` |
| `kattersstructures:rusta_shot` | 3 | `Katters Structures v2.9` | No | 5 | `minecraft:crossbow` |
| `kattersstructures:web_walker` | 3 | `Katters Structures v2.9` | No | 5 | `#minecraft:enchantable/foot_armor` |
| `minecraft:channeling` | 1 | `minecraft (vanilla)` | No | 5 | `#minecraft:enchantable/trident` |
| `minecraft:feather_falling` | 4 | `minecraft (vanilla)` | Yes | 5 | `#minecraft:enchantable/foot_armor` |
| `minecraft:flame` | 1 | `minecraft (vanilla)` | Yes | 5 | `#minecraft:enchantable/bow` |
| `minecraft:fortune` | 3 | `minecraft (vanilla)` | Yes | 5 | `#minecraft:enchantable/mining_loot` |
| `minecraft:infinity` | 1 | `minecraft (vanilla)` | Yes | 5 | `#minecraft:enchantable/bow` |
| `minecraft:looting` | 3 | `minecraft (vanilla)` | Yes | 5 | `#minecraft:enchantable/sword` |
| `minecraft:multishot` | 1 | `minecraft (vanilla)` | Yes | 5 | `#minecraft:enchantable/crossbow` |
| `minecraft:riptide` | 3 | `minecraft (vanilla)` | Yes | 5 | `#minecraft:enchantable/trident` |
| `minecraft:silk_touch` | 1 | `minecraft (vanilla)` | Yes | 5 | `#minecraft:enchantable/mining_loot` |
| `nova_structures:aerials_bane` | 5 | `Dungeons and Taverns v6.1.0` | No | 5 | `#nova_structures:enchantable/combat_tools` |
| `nova_structures:gravity` | 3 | `Dungeons and Taverns v6.1.0` | No | 5 | `#minecraft:enchantable/crossbow` |
| `nova_structures:hydro_veil` | 4 | `Dungeons and Taverns v6.1.0` | No | 5 | `#minecraft:harnesses` |
| `nova_structures:piercing` | 4 | `Dungeons and Taverns v6.1.0` | No | 5 | `#minecraft:enchantable/bow` |
| `nova_structures:power` | 5 | `Dungeons and Taverns v6.1.0` | No | 5 | `#minecraft:enchantable/crossbow` |
| `nova_structures:spiteful` | 3 | `Dungeons and Taverns v6.1.0` | Yes | 5 | `#nova_structures:enchantable/combat_weapons` |
| `nova_structures:swift_soar` | 3 | `Dungeons and Taverns v6.1.0` | No | 5 | `#minecraft:harnesses` |
| `nova_structures:wax_wings` | 1 | `Dungeons and Taverns v6.1.0` | No | 5 | `minecraft:elytra` |
| `nova_structures:wither_coated` | 3 | `Dungeons and Taverns v6.1.0` | No | 5 | `#nova_structures:enchantable/wither_coated` |
| `stellarity:dune_speed` | 3 | `Stellarity-6.0.1` | No | 5 | `#minecraft:enchantable/foot_armor` |
| `stellarity:levitation_shot` | 5 | `Stellarity-6.0.1` | No | 5 | `#minecraft:enchantable/bow` |
| `stellarity:soaring` | 4 | `Stellarity-6.0.1` | No | 5 | `#stellarity:enchantable/elytra` |

## 6. Common Enchants (155)

| Enchantment ID | Max Lv | Nguồn Pack | Table? | Loot Weight | Items áp dụng |
| :--- | :---: | :--- | :---: | :---: | :--- |
| `enchantencore:anchor` | 1 | `enchantments_encore` | No | 10 | `#enchantencore:valid_for_anchor` |
| `enchantencore:angry_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:aquatic_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:bane_of_endborn` | 5 | `enchantments_encore` | Yes | 10 | `#enchantencore:weapons` |
| `enchantencore:bane_of_netherborn` | 5 | `enchantments_encore` | Yes | 10 | `#enchantencore:weapons` |
| `enchantencore:bane_of_raiders` | 5 | `enchantments_encore` | Yes | 10 | `#enchantencore:weapons` |
| `enchantencore:bounce` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:bunny_hop` | 3 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/leg_armor` |
| `enchantencore:cleansing` | 1 | `enchantments_encore` | No | 10 | `minecraft:shield` |
| `enchantencore:creeper_alloy` | 1 | `enchantments_encore` | Yes | 10 | `#minecraft:chest_armor` |
| `enchantencore:crispbread` | 1 | `enchantments_encore` | No | 10 | `minecraft:shield` |
| `enchantencore:crop_dance` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:cushion` | 4 | `enchantments_encore` | Yes | 10 | `minecraft:elytra` |
| `enchantencore:dark_infused` | 3 | `enchantments_encore` | No | 10 | `#enchantencore:all_bows` |
| `enchantencore:darkness_alloy` | 1 | `enchantments_encore` | No | 10 | `#minecraft:chest_armor` |
| `enchantencore:dismount` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/lunge` |
| `enchantencore:echoing_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:electric_walker` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:enchanted_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:ender_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:fire_alloy` | 1 | `enchantments_encore` | Yes | 10 | `#minecraft:chest_armor` |
| `enchantencore:flaming_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:forge_shot` | 1 | `enchantments_encore` | No | 10 | `#enchantencore:all_bows` |
| `enchantencore:gallop` | 3 | `enchantments_encore` | No | 10 | `minecraft:saddle` |
| `enchantencore:gluttony` | 5 | `enchantments_encore` | No | 10 | `#enchantencore:all_tools` |
| `enchantencore:grass_walker` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:growth` | 1 | `enchantments_encore` | No | 10 | `#enchantencore:valid_for_growth` |
| `enchantencore:guard` | 1 | `enchantments_encore` | No | 10 | `#enchantencore:can_block` |
| `enchantencore:handyman` | 1 | `enchantments_encore` | No | 10 | `#enchantencore:all_enchantable` |
| `enchantencore:hardened` | 3 | `enchantments_encore` | No | 10 | `minecraft:shield` |
| `enchantencore:harpoon` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/fishing` |
| `enchantencore:heavenly_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:hunger_aspect` | 2 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:ice_alloy` | 1 | `enchantments_encore` | No | 10 | `#minecraft:chest_armor` |
| `enchantencore:illumination` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/head_armor` |
| `enchantencore:impale_rush` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/lunge` |
| `enchantencore:infested_aspect` | 2 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:leaf_jumper` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:lingering` | 1 | `enchantments_encore` | No | 10 | `#enchantencore:all_bows` |
| `enchantencore:lovely_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:lush_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:magma_walker` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:muddy_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:ominous_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:ominous_healing` | 1 | `enchantments_encore` | No | 10 | `minecraft:shield` |
| `enchantencore:ominous_rain` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/trident` |
| `enchantencore:oozing_aspect` | 2 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:pale_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:phantom_menace` | 3 | `enchantments_encore` | Yes | 10 | `#enchantencore:all_bows` |
| `enchantencore:pickpocket` | 1 | `enchantments_encore` | No | 10 | `#minecraft:chest_armor` |
| `enchantencore:ping_tag` | 1 | `enchantments_encore` | No | 10 | `#enchantencore:valid_for_ping_tag` |
| `enchantencore:plow` | 3 | `enchantments_encore` | Yes | 10 | `#minecraft:hoes` |
| `enchantencore:poppy_peace` | 1 | `enchantments_encore` | No | 10 | `#enchantencore:weapons` |
| `enchantencore:propeller` | 5 | `enchantments_encore` | No | 10 | `#minecraft:harnesses` |
| `enchantencore:pufferfish` | 1 | `enchantments_encore` | Yes | 10 | `#minecraft:enchantable/trident` |
| `enchantencore:purify_water` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/trident` |
| `enchantencore:rabbit_foot` | 3 | `enchantments_encore` | Yes | 10 | `#minecraft:foot_armor` |
| `enchantencore:rainweave` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/durability` |
| `enchantencore:sacrifice` | 3 | `enchantments_encore` | No | 10 | `#enchantencore:all_enchantable_wolf_armor` |
| `enchantencore:safe_landing` | 3 | `enchantments_encore` | Yes | 10 | `minecraft:elytra` |
| `enchantencore:scent` | 1 | `enchantments_encore` | No | 10 | `minecraft:wolf_armor` |
| `enchantencore:sea_breeze` | 1 | `enchantments_encore` | Yes | 10 | `#minecraft:enchantable/trident` |
| `enchantencore:seeker` | 3 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/head_armor` |
| `enchantencore:shrinkage` | 1 | `enchantments_encore` | No | 10 | `#enchantencore:valid_for_shrinkage` |
| `enchantencore:slowness_aspect` | 2 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:sparking_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:spectral_form` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:sticky_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:sting` | 1 | `enchantments_encore` | No | 10 | `minecraft:crossbow` |
| `enchantencore:strafe` | 1 | `enchantments_encore` | No | 10 | `#minecraft:foot_armor` |
| `enchantencore:summer_walker` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/foot_armor` |
| `enchantencore:surfer` | 3 | `enchantments_encore` | No | 10 | `minecraft:saddle` |
| `enchantencore:switch` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/trident` |
| `enchantencore:thorns_protection` | 4 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/armor` |
| `enchantencore:tideweave` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/durability` |
| `enchantencore:trial_aura` | 1 | `enchantments_encore` | No | 10 | `#minecraft:head_armor` |
| `enchantencore:updraft` | 3 | `enchantments_encore` | No | 10 | `minecraft:elytra` |
| `enchantencore:weakness_aspect` | 2 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:weaving_aspect` | 2 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:wind_aspect` | 2 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/fire_aspect` |
| `enchantencore:wither_protection` | 4 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/armor` |
| `enchantencore:yoink` | 1 | `enchantments_encore` | No | 10 | `#minecraft:enchantable/fishing` |
| `enchantplus:armor/venom_protection` | 1 | `NeoEnchant-5.16.0` | No | 10 | `#minecraft:enchantable/armor` |
| `enchantplus:helmet/auto_feed` | 1 | `NeoEnchant-5.16.0` | No | 10 | `#minecraft:enchantable/head_armor` |
| `enchantplus:helmet/bright_vision` | 21 | `NeoEnchant-5.16.0` | No | 10 | `#minecraft:enchantable/head_armor` |
| `enchantplus:hoe/scyther` | 3 | `NeoEnchant-5.16.0` | Yes | 10 | `#voxel:enchantable/hoes` |
| `enchantplus:hoe/seiors_oblivion` | 1 | `NeoEnchant-5.16.0` | No | 10 | `#voxel:enchantable/hoes` |
| `enchantplus:mounted/cavalier_egis` | 3 | `NeoEnchant-5.16.0` | No | 10 | `#enchantplus:enchantable/mounted` |
| `enchantplus:mounted/steel_fang` | 5 | `NeoEnchant-5.16.0` | No | 10 | `#enchantplus:enchantable/dog` |
| `enchantplus:sword/fear` | 3 | `NeoEnchant-5.16.0` | No | 10 | `#minecraft:enchantable/melee_weapon` |
| `enchantsplus:harvest` | 1 | `Enchants Plus v2.1 26.3` | No | 10 | `#minecraft:hoes` |
| `enchantsplus:kinetic_protection` | 4 | `Enchants Plus v2.1 26.3` | Yes | 10 | `#minecraft:enchantable/head_armor` |
| `enchantsplus:precision` | 2 | `Enchants Plus v2.1 26.3` | No | 10 | `minecraft:crossbow, minecraft:bow` |
| `enchantsplus:skyguard` | 4 | `Enchants Plus v2.1 26.3` | Yes | 10 | `minecraft:elytra` |
| `enchantsplus:stride` | 3 | `Enchants Plus v2.1 26.3` | No | 10 | `#minecraft:enchantable/leg_armor` |
| `enchantsplus:toxic` | 1 | `Enchants Plus v2.1 26.3` | No | 10 | `#enchantsplus:bows` |
| `enchantsplus:websnare` | 2 | `Enchants Plus v2.1 26.3` | No | 10 | `#minecraft:enchantable/fire_aspect` |
| `kattersstructures:heavy` | 5 | `Katters Structures v2.9` | No | 10 | `#minecraft:enchantable/weapon` |
| `minecraft:aqua_affinity` | 1 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/head_armor` |
| `minecraft:bane_of_arthropods` | 5 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/weapon` |
| `minecraft:blast_protection` | 4 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/armor` |
| `minecraft:depth_strider` | 3 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/foot_armor` |
| `minecraft:efficiency` | 5 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/mining` |
| `minecraft:fire_aspect` | 2 | `enchantments_encore` | Yes | 10 | `#minecraft:enchantable/fire_aspect` |
| `minecraft:fire_protection` | 4 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/armor` |
| `minecraft:impaling` | 5 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/trident` |
| `minecraft:knockback` | 2 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/sword` |
| `minecraft:loyalty` | 3 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/trident` |
| `minecraft:luck_of_the_sea` | 3 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/fishing` |
| `minecraft:lure` | 3 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/fishing` |
| `minecraft:piercing` | 4 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/crossbow` |
| `minecraft:power` | 5 | `Enchants Plus v2.1 26.3` | Yes | 10 | `minecraft:bow, minecraft:crossbow` |
| `minecraft:projectile_protection` | 4 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/armor` |
| `minecraft:protection` | 4 | `enchantments_encore` | Yes | 10 | `#minecraft:enchantable/armor` |
| `minecraft:punch` | 2 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/bow` |
| `minecraft:quick_charge` | 3 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/crossbow` |
| `minecraft:respiration` | 3 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/head_armor` |
| `minecraft:sharpness` | 5 | `enchantments_encore` | Yes | 10 | `#minecraft:enchantable/sharp_weapon` |
| `minecraft:smite` | 5 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/weapon` |
| `minecraft:sweeping_edge` | 3 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/sword` |
| `minecraft:thorns` | 3 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/armor` |
| `minecraft:unbreaking` | 3 | `minecraft (vanilla)` | Yes | 10 | `#minecraft:enchantable/durability` |
| `mt:technical/floor_is_spikes` | 1 | `mine-treasure-1.6.5` | No | 10 | `minecraft:acacia_button` |
| `mt:technical/giga_bore` | 1 | `mine-treasure-1.6.5` | No | 10 | `#minecraft:enchantable/mining` |
| `mt:technical/grenade` | 1 | `mine-treasure-1.6.5` | No | 10 | `#minecraft:enchantable/sharp_weapon` |
| `mt:technical/healing_flower` | 1 | `mine-treasure-1.6.5` | No | 10 | `minecraft:acacia_button` |
| `mt:technical/kinetic_blast` | 255 | `mine-treasure-1.6.5` | No | 10 | `#minecraft:enchantable/head_armor` |
| `mt:technical/kinetic_bomb` | 1 | `mine-treasure-1.6.5` | No | 10 | `#minecraft:enchantable/sharp_weapon` |
| `mt:technical/necromancy` | 1 | `mine-treasure-1.6.5` | No | 10 | `minecraft:acacia_button` |
| `mt:technical/sense_of_urgency` | 1 | `mine-treasure-1.6.5` | No | 10 | `minecraft:acacia_button` |
| `mt:treasure/fast` | 1 | `mine-treasure-1.6.5` | No | 10 | `#minecraft:enchantable/armor` |
| `mt:treasure/hypersonic` | 1 | `mine-treasure-1.6.5` | No | 10 | `#minecraft:enchantable/armor` |
| `mt:treasure/sloth` | 1 | `mine-treasure-1.6.5` | No | 10 | `#minecraft:enchantable/armor` |
| `mt:treasure/slow` | 1 | `mine-treasure-1.6.5` | No | 10 | `#minecraft:enchantable/armor` |
| `mt:treasure/standard` | 1 | `mine-treasure-1.6.5` | No | 10 | `#minecraft:enchantable/armor` |
| `nova_structures:antidote` | 1 | `Dungeons and Taverns v6.1.0` | No | 10 | `#minecraft:enchantable/chest_armor` |
| `nova_structures:function_only` | 5 | `Dungeons and Taverns v6.1.0` | No | 10 | `minecraft:stick, minecraft:flint` |
| `nova_structures:illagers_bane` | 5 | `Dungeons and Taverns v6.1.0` | No | 10 | `#nova_structures:enchantable/combat_weapons` |
| `nova_structures:outreach` | 4 | `Dungeons and Taverns v6.1.0` | No | 10 | `#minecraft:enchantable/chest_armor` |
| `nova_structures:traveler` | 3 | `Dungeons and Taverns v6.1.0` | No | 10 | `#minecraft:enchantable/foot_armor` |
| `stellarity:_technical/daybroken` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:diamond_sword` |
| `stellarity:_technical/draconic` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:netherite_sword` |
| `stellarity:_technical/infernal_infusion` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:netherite_sword, minecraft:crossbow` |
| `stellarity:_technical/mighty_wind` | 1 | `Stellarity-6.0.1` | No | 10 | `#minecraft:enchantable/trident` |
| `stellarity:_technical/prismatic_pearl_return` | 1 | `Stellarity-6.0.1` | No | 10 | `` |
| `stellarity:_technical/soul_harvest` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:diamond_sword` |
| `stellarity:_technical/void_pendant/amethyst` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:iron_nugget` |
| `stellarity:_technical/void_pendant/copper` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:iron_nugget` |
| `stellarity:_technical/void_pendant/diamond` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:iron_nugget` |
| `stellarity:_technical/void_pendant/emerald` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:iron_nugget` |
| `stellarity:_technical/void_pendant/gold` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:iron_nugget` |
| `stellarity:_technical/void_pendant/iron` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:iron_nugget` |
| `stellarity:_technical/void_pendant/lapis` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:iron_nugget` |
| `stellarity:_technical/void_pendant/netherite` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:iron_nugget` |
| `stellarity:_technical/void_pendant/quartz` | 1 | `Stellarity-6.0.1` | No | 10 | `minecraft:iron_nugget` |

## 7. Enchants Bị Lọc Do Trùng Lặp (16)

| Enchantment ID (Đã tắt) | Nguồn Pack | Thay thế bởi (`duplicate_of`) |
| :--- | :--- | :--- |
| `enchantencore:swift_slash` | `enchantments_encore` | `enchantsplus:swift_strike` |
| `enchantplus:axe/timber` | `NeoEnchant-5.16.0` | `enchantencore:lumberjack` |
| `enchantplus:boots/lava_walker` | `NeoEnchant-5.16.0` | `enchantsplus:scorch_walker` |
| `enchantplus:boots/step_assist` | `NeoEnchant-5.16.0` | `enchantencore:step_up` |
| `enchantplus:bow/breezing_arrow` | `NeoEnchant-5.16.0` | `enchantsplus:breeze_burst` |
| `enchantplus:elytra/kinetic_protection` | `NeoEnchant-5.16.0` | `enchantsplus:kinetic_protection` |
| `enchantplus:hoe/harvest` | `NeoEnchant-5.16.0` | `enchantsplus:harvest` |
| `enchantplus:leggings/leaping` | `NeoEnchant-5.16.0` | `enchantencore:leaping` |
| `enchantplus:pickaxe/vein_miner` | `NeoEnchant-5.16.0` | `veinminer-enchantment:veinminer` |
| `enchantplus:sword/attack_speed` | `NeoEnchant-5.16.0` | `enchantsplus:swift_strike` |
| `enchantplus:sword/curse_of_clumsiness` | `NeoEnchant-5.16.0` | `enchantsplus:clumsiness_curse` |
| `enchantplus:sword/life_steal` | `NeoEnchant-5.16.0` | `enchantencore:leech` |
| `enchantplus:sword/poison_aspect` | `NeoEnchant-5.16.0` | `enchantencore:poison_aspect` |
| `enchantplus:tools/auto_smelt` | `NeoEnchant-5.16.0` | `enchantsplus:pyrolysis` |
| `enchantsplus:gluttony` | `Enchants Plus v2.1 26.3` | `enchantencore:gluttony` |
| `enchantsplus:outreach` | `Enchants Plus v2.1 26.3` | `nova_structures:outreach` |
