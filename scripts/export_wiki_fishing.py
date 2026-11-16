#!/usr/bin/env python3
"""
Export Expedition Fishing Data for Web Wiki (assets/js/fishing_data.js)
Reads:
- mods_src/expedition-fishing/src/main/resources/data/expedition_fishing/mobs.json
- mods_src/expedition-fishing/src/main/resources/data/expedition_fishing/items.json
- mods_src/expedition-fishing/src/main/resources/data/expedition_fishing/artifacts.json
- mods_src/expedition-fishing/src/main/resources/data/expedition_fishing/recipes.json
- mods_src/expedition-fishing/src/main/resources/data/expedition_fishing/biomes.json
- mods_src/expedition-fishing/src/main/resources/data/expedition_fishing/enchants_balance.json
- assets/js/enchants_data.js (for Fishing Enchantments details)
"""

import json
import os
import re

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(ROOT_DIR, "mods_src", "expedition-fishing", "src", "main", "resources", "data", "expedition_fishing")
OUT_JS = os.path.join(ROOT_DIR, "assets", "js", "fishing_data.js")

ABILITY_DESCRIPTIONS = {
    "INK_CLOUD": "Phun đám mây mực đen làm mù (Blindness) và làm chậm mục tiêu xung quanh.",
    "MINI_BLAST": "Tích tụ năng lượng thực vật rồi phát nổ tầm gần gây sát thương nổ và hất văng.",
    "FROST_BEAM": "Bắn tia băng giá xuyên thấu theo đường thẳng, gây sát thương lạnh và đóng băng.",
    "GROUND_SLAM": "Đập mạnh xuống mặt đất, tạo sóng chấn động hất tung người chơi lên không trung.",
    "DUNE_BEAM": "Phóng tia năng lượng cát cháy từ sa mạc gây sát thương đốt và làm suy yếu.",
    "VULTURE_MISSILE": "Lao vút hoặc phóng luồng đạn khí xoáy truy đuổi mục tiêu từ trên không.",
    "TEMPLE_RITUAL": "Triệu hồi vòng tròn nanh phép (Evoker Fangs) và nghi thức cổ đại dưới chân người chơi.",
    "KING_SLIME_SPLASH": "Nhảy cao và dậm mạnh tạo cơn mưa dịch nhờn gây sát thương diện rộng.",
    "EMBER_VOLLEY": "Bắn loạt cầu lửa liên hoàn về phía người chơi.",
    "HEAT_MARK": "Khắc ấn nhiệt lên mục tiêu, tăng nhiệt độ (Heat) và kích nổ lửa sau vài giây.",
    "MAGMA_BURST": "Phun trào dung nham nóng chảy xung quanh khi đáp đất.",
    "LAVA_SURGE": "Tạo sóng dung nham đẩy lùi và thiêu đốt mọi mục tiêu.",
    "HOGLIN_FLAMETHROWER": "Phun luồng lửa địa ngục hình nón liên tục về phía trước.",
    "BRUTE_TUSK_SWEEP": "Húc mạnh bằng cặp nanh khổng lồ, gây sát thương lớn và hất văng xa.",
    "TIDAL_PULL": "Tạo xoáy nước đại dương cuốn hút toàn bộ người chơi về phía Boss.",
    "SOUL_RUPTURE": "Xé toạc mặt đất bằng năng lượng linh hồn xanh, gây sát thương chuẩn.",
    "SOUL_GEYSERS": "Triệu hồi các cột mạch nước linh hồn phun trào dưới chân người chơi.",
    "WARPED_ENDERMAN_GAZE": "Ánh nhìn dị giới gây mù lòa, buồn nôn và rút máu khi nhìn trực diện.",
    "WARPED_ENDERMAN_SNARE": "Trói chân mục tiêu bằng dây leo kỳ dị và dịch chuyển tập kích.",
    "DARK_RITUAL": "Bao phủ chiến trường trong bóng tối sâu thẳm (Darkness) và triệu hồi xung nhịp Sonic.",
    "WARDEN_PHASE_TWO": "Cuồng nộ khi máu dưới 50%: tăng tốc độ, kháng hiệu ứng và cường hóa sóng âm.",
    "MAGMA_EYE_LASERS": "Quét hai luồng tia laser nhiệtạch từ mắt gây sát thương xuyên giáp.",
    "MAGMA_ARM_ROCKETS": "Phóng nắm đấm dung nham phát nổ tầm xa.",
    "BLOOD_TONGUE": "Phóng chiếc lưỡi máu dài kéo giật người chơi lại gần và hút máu (kèm bắn cầu lửa liên thanh mỗi 1 giây).",
    "BLOOD_EXPLOSION": "Tụ huyết độc và phát nổ diện rộng, gây sát thương lớn và hiệu ứng Wither.",
    "POSEIDON_WAVE": "Triệu hồi sóng thần đại dương cuốn phăng và gây sát thương thủy nguyên tố.",
    "POSEIDON_STORM_WAVE": "Gọi giông bão sấm sét đánh liên hoàn kết hợp sóng xoáy diện rộng.",
    "WAILING_TWIN_LASER": "Hai cá thể Ghast kết nối tia laser tử thần quét ngang chiến trường.",
    "WAILING_TWIN_TOXIC_SPIT": "Khạc mưa đạn độc axit gây Poison và ăn mòn giáp.",
    "EVENT_HORIZON_PULL": "Tạo chân trời sự kiện kéo mục tiêu vào tâm trọng trường Hư Không.",
    "RIFTBOUND_GAZE": "Ánh nhìn khe nứt không gian làm tê liệt và gây sát thương tâm linh.",
    "RIFTBOUND_SNARE": "Giam cầm mục tiêu trong lồng pha lê End và dịch chuyển tấn công.",
    "CRYSTAL_SHARD_VOLLEY": "Bắn mưa mảnh vỡ tinh thể thạch anh tím xuyên giáp.",
    "PRISMATIC_REFRACTION": "Tạo khiên khúc xạ ánh sáng phản lại một phần sát thương và bắn tia cầu vồng.",
    "BLOOD_YOKE": "Xiềng xích huyết thệ rút máu người chơi để hồi phục cho Boss.",
    "WAR_DRUM": "Gầm vang tiếng trống trận cường hóa tốc độ và sức tấn công.",
    "FIRE_GEYSERS": "Triệu hồi hàng loạt cột lửa địa ngục phun trào liên tiếp.",
    "VOID_METEOR_SHOWER": "Gọi mưa thiên thạch Hư Không rơi xuống khu vực xung quanh người chơi.",
    "STARLIGHT_PILLAR": "Giáng các cột sáng tinh tú từ bầu trời The End gây sát thương thiêng.",
    "VOID_CORRUPTION": "Ám năng lượng Hư Không gây hiệu ứng Wither, Levitation ngắn và giảm hồi máu.",
    "DIMENSIONAL_SLASH": "Chém rách không gian, lướt qua mục tiêu và để lại vết cắt phát nổ.",
    "STRIDER_LAVA_SPEW": "Thú cưỡi Strider phun dung nham đặc quánh làm chậm và thiêu đốt.",
    "NETHER_REND": "Bổ nhát chém địa ngục tạo vệt lửa dài trên mặt đất.",
    "GRAVITY_SINGULARITY": "Triệu hồi điểm kỳ dị Hư Không hút chặt người chơi rồi phát nổ chấn động.",
    "TORSO_TENTACLES": "Quất hàng loạt xúc tu bóng tối xung quanh thân thể.",
    "ABYSSAL_NOVA": "Giải phóng vụ nổ tân tinh vực thẳm ra 360 độ.",
    "VALLEY_DIRGE": "Khúc cầu hồn thung lũng linh hồn triệu hồi đầu lâu Wither cường hóa.",
    "TIME_DILATION_FIELD": "Tạo trường ngưng đọng thời gian làm chậm cực đại mọi chuyển động của người chơi.",
    "TEMPORAL_REWIND": "Quay ngược thời gian để dịch chuyển về vị trí an toàn và hồi phục một phần máu."
}

def load_json(name):
    path = os.path.join(DATA_DIR, name)
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)

def extract_lore_lines(lore_raw):
    if not lore_raw:
        return []
    lines = []
    if isinstance(lore_raw, list):
        for el in lore_raw:
            if isinstance(el, str):
                lines.append(el)
            elif isinstance(el, dict) and "text" in el:
                lines.append(str(el["text"]))
    return lines

def main():
    mobs_raw = load_json("mobs.json")
    items_raw = load_json("items.json")
    artifacts_raw = load_json("artifacts.json")
    recipes_raw = load_json("recipes.json")
    biomes_raw = load_json("biomes.json")
    balance_raw = load_json("enchants_balance.json")

    # Map mob -> list of biomes where it spawns
    mob_biomes = {}
    biomes_list = []
    for b_key, b_val in biomes_raw.items():
        if not isinstance(b_val, dict):
            continue
        p_weights = b_val.get("passive_weights", {})
        h_weights = b_val.get("hostile_weights", {})
        land_mobs = b_val.get("land_mobs", {}).get("mobs", []) if isinstance(b_val.get("land_mobs"), dict) else []

        for m in p_weights:
            mob_biomes.setdefault(m, []).append({"biome": b_key, "pool": "PASSIVE", "weight": p_weights[m]})
        for m in h_weights:
            mob_biomes.setdefault(m, []).append({"biome": b_key, "pool": "HOSTILE", "weight": h_weights[m]})
        for m in land_mobs:
            mob_biomes.setdefault(m, []).append({"biome": b_key, "pool": "LAND", "weight": 1.0})

        dim = "OVERWORLD"
        if "NETHER" in b_key or "CRIMSON" in b_key or "WARPED" in b_key or "SOUL" in b_key or "BASALT" in b_key:
            dim = "NETHER"
        elif "END" in b_key:
            dim = "END"
        elif "DEEP_DARK" in b_key or "DRIPSTONE" in b_key or "LUSH" in b_key:
            dim = "CAVE"

        biomes_list.append({
            "id": b_key,
            "dimension": dim,
            "passive_weights": p_weights,
            "hostile_weights": h_weights,
            "land_mobs": land_mobs,
            "passive_count": len(p_weights),
            "hostile_count": len(h_weights)
        })

    # Map item -> recipes producing it
    item_recipes = {}
    recipes_list = []
    for r_id, r_val in recipes_raw.items():
        if not isinstance(r_val, dict):
            continue
        res_id = r_val.get("result", "")
        rec_obj = {
            "id": r_id,
            "result": res_id,
            "count": r_val.get("count", 1),
            "level": r_val.get("level", 0),
            "advancement": r_val.get("advancement", ""),
            "recipe_type": r_val.get("recipe_type", "shaped"),
            "pattern": r_val.get("pattern", []),
            "ingredients": r_val.get("ingredients", {})
        }
        recipes_list.append(rec_obj)
        if res_id:
            item_recipes.setdefault(res_id, []).append(rec_obj)

    # Map item -> mobs dropping it
    item_dropped_by = {}

    rarity_order = {"COMMON": 1, "UNCOMMON": 2, "RARE": 3, "EPIC": 4, "LEGENDARY": 5, "MYTHIC": 6, "SPECIAL": 7}

    # Process Mobs
    mobs_list = []
    for m_id, m in mobs_raw.items():
        if not isinstance(m, dict):
            continue
        sp = m.get("spawn", {}) if isinstance(m.get("spawn"), dict) else {}
        sp_type = sp.get("type", "SINGLE")
        ent_type = sp.get("rider_entity_type") or m.get("entity_type", "COD")
        scale = sp.get("scale", 1.0)
        slime_size = sp.get("slime_size", None)

        # Compute HP & Damage summary
        hp_val = 20.0
        hp_display = "20 HP"
        is_mounted = False
        mount_info = None

        if sp_type == "GROUP" and "members" in sp:
            parts = []
            total_hp = 0.0
            for mem in sp["members"]:
                c = mem.get("count", 1)
                mt = mem.get("entity_type", "MOB")
                mh = mem.get("health", 20.0)
                total_hp += c * mh
                parts.append(f"{c}x {mt} ({mh:g} HP)")
            hp_val = total_hp
            hp_display = " + ".join(parts)
        elif "health" in sp:
            hp_val = float(sp["health"])
            if "mount" in sp and isinstance(sp["mount"], dict):
                is_mounted = True
                rider_hp = round(hp_val * 0.8)
                mount_hp = round(hp_val * 0.2)
                m_ent = sp["mount"].get("entity_type", "MOUNT")
                m_scale = sp["mount"].get("scale", 1.0)
                hp_display = f"{hp_val:g} HP (Boss {rider_hp} / Thú cưỡi {mount_hp})"
                mount_info = {"entity_type": m_ent, "scale": m_scale, "rider_hp": rider_hp, "mount_hp": mount_hp}
            else:
                hp_display = f"{hp_val:g} HP"
        elif "health_multiplier" in sp:
            hm = float(sp["health_multiplier"])
            base_map = {
                "WARDEN": 500, "ELDER_GUARDIAN": 80, "RAVAGER": 100, "IRON_GOLEM": 100,
                "ENDERMAN": 40, "HOGLIN": 40, "POLAR_BEAR": 30, "GUARDIAN": 30,
                "WITCH": 26, "CAVE_SPIDER": 12, "SILVERFISH": 8, "ENDERMITE": 8,
                "COD": 3, "SALMON": 3, "TROPICAL_FISH": 3, "PUFFERFISH": 3, "GHAST": 10, "MAGMA_CUBE": 16
            }
            b = base_map.get(ent_type, 20)
            hp_val = b * hm
            hp_display = f"{hp_val:g} HP ({hm:g}x)"
        else:
            base_map = {"COD": 3, "SALMON": 3, "PUFFERFISH": 3, "TROPICAL_FISH": 3, "SQUID": 10, "GLOW_SQUID": 10, "DOLPHIN": 10, "FROG": 10, "NAUTILUS": 14, "AXOLOTL": 14, "POLAR_BEAR": 30, "TURTLE": 30, "BOGGED": 16, "SPIDER": 16, "SLIME": 16}
            hp_val = float(base_map.get(ent_type, 20))
            hp_display = f"{hp_val:g} HP"

        if "damage" in sp:
            dmg_display = f"{sp['damage']:g}"
            dmg_val = float(sp["damage"])
        elif "aggro" in sp and isinstance(sp["aggro"], dict) and "damage" in sp["aggro"]:
            dmg_val = float(sp["aggro"]["damage"])
            dmg_display = f"{dmg_val:g} (Aggro)"
        elif "damage_multiplier" in sp:
            dmg_val = float(sp["damage_multiplier"]) * 6.0
            dmg_display = f"{sp['damage_multiplier']:g}x Vanilla"
        else:
            dmg_val = 0.0 if m.get("category") in ("PASSIVE", "TREASURE") else 6.0
            dmg_display = "0" if m.get("category") in ("PASSIVE", "TREASURE") else "Vanilla"

        abilities = []
        if "abilities" in sp and isinstance(sp["abilities"], list):
            for ab in sp["abilities"]:
                if isinstance(ab, dict) and "id" in ab:
                    ab_id = ab["id"]
                    abilities.append({
                        "id": ab_id,
                        "cooldown_ticks": ab.get("cooldown_ticks", 100),
                        "cooldown_sec": round(ab.get("cooldown_ticks", 100) / 20.0, 1),
                        "description": ABILITY_DESCRIPTIONS.get(ab_id, "Kỹ năng đặc biệt của thủy quái.")
                    })

        drops = []
        if "drops" in m and isinstance(m["drops"], list):
            for d in m["drops"]:
                if isinstance(d, dict):
                    item_id = d.get("item") or d.get("type", "loot")
                    ch = round(d.get("chance", 1.0) * 100, 2)
                    mn = d.get("min", 1)
                    mx = d.get("max", 1)
                    drops.append({
                        "item": item_id,
                        "min": mn,
                        "max": mx,
                        "chance": ch
                    })
                    item_dropped_by.setdefault(item_id, []).append({
                        "mob_id": m_id,
                        "mob_name": m.get("display_name", m_id),
                        "rarity": m.get("rarity", "COMMON"),
                        "chance": ch
                    })

        req_lv = m.get("required_level", 0)
        cat = m.get("category", "PASSIVE")
        rar = m.get("rarity", "COMMON")

        # Determine region/environment tag
        b_entries = mob_biomes.get(m_id, [])
        env_tags = set()
        for be in b_entries:
            bn = be["biome"]
            if "END" in bn:
                env_tags.add("END_VOID")
            elif "NETHER" in bn or "CRIMSON" in bn or "WARPED" in bn or "SOUL" in bn or "BASALT" in bn:
                env_tags.add("NETHER_LAVA")
            elif "DEEP_DARK" in bn or "DRIPSTONE" in bn or "LUSH" in bn:
                env_tags.add("CAVE")
            else:
                env_tags.add("OVERWORLD")
        if not env_tags:
            if req_lv >= 35 and m_id not in ("ender_angler", "crimson_abomination", "nether_lord", "the_wither"):
                env_tags.add("END_VOID")
            elif req_lv >= 27:
                env_tags.add("NETHER_LAVA")
            else:
                env_tags.add("OVERWORLD")

        is_boss = rar in ("EPIC", "LEGENDARY") and cat == "HOSTILE"

        mobs_list.append({
            "id": m_id,
            "name": m.get("display_name", m_id),
            "rarity": rar,
            "rarity_rank": rarity_order.get(rar, 0),
            "category": cat,
            "is_boss": is_boss,
            "required_level": req_lv,
            "xp": m.get("xp", 0.0),
            "weight": m.get("weight", 1.0),
            "entity_type": ent_type,
            "spawn_type": sp_type,
            "scale": scale,
            "slime_size": slime_size,
            "is_mounted": is_mounted,
            "mount_info": mount_info,
            "hp_val": hp_val,
            "hp_display": hp_display,
            "dmg_val": dmg_val,
            "dmg_display": dmg_display,
            "abilities": abilities,
            "drops": drops,
            "biomes": b_entries,
            "environments": sorted(list(env_tags))
        })

    mobs_list.sort(key=lambda x: (x["required_level"], x["rarity_rank"], x["id"]))

    # Process Items
    items_list = []
    for i_id, it in items_raw.items():
        if not isinstance(it, dict):
            continue
        pdc = it.get("pdc", {}) if isinstance(it.get("pdc"), dict) else {}
        epdc = it.get("extra_pdc", {}) if isinstance(it.get("extra_pdc"), dict) else {}

        sca = it.get("sea_creature_attack") or pdc.get("sea_creature_attack") or epdc.get("sea_creature_attack") or 0.0
        scc = it.get("scc") or it.get("rare_creature_chance") or it.get("scc_bonus") or pdc.get("scc") or 0.0
        fspeed = it.get("fishing_speed") or it.get("fishing_speed_bonus") or pdc.get("fishing_speed") or 0.0
        dcatch = it.get("double_catch") or pdc.get("double_catch") or 0.0
        tbonus = it.get("treasure_bonus") or pdc.get("treasure_bonus") or 0.0

        lore = extract_lore_lines(it.get("lore", []))
        desc = it.get("description", "")
        if not desc and lore:
            desc = " ".join(lore)

        rar = it.get("rarity", "COMMON")
        cat = it.get("category", "MATERIAL")

        items_list.append({
            "id": i_id,
            "name": it.get("display_name", i_id),
            "material": it.get("material", "COD"),
            "rarity": rar,
            "rarity_rank": rarity_order.get(rar, 0),
            "category": cat,
            "required_level": it.get("required_level", 0),
            "upgrade_tier": it.get("upgrade_tier", 0),
            "lava_capable": bool(it.get("lava_capable", False)),
            "void_capable": bool(it.get("void_capable", False)),
            "fishing_speed": fspeed,
            "scc": scc,
            "sea_creature_attack": sca,
            "double_catch": dcatch,
            "treasure_bonus": tbonus,
            "armor": it.get("armor", 0),
            "toughness": it.get("toughness", 0),
            "attack_damage": it.get("attack_damage", 0),
            "description": desc,
            "lore": lore,
            "recipes": item_recipes.get(i_id, []),
            "dropped_by": item_dropped_by.get(i_id, [])
        })

    items_list.sort(key=lambda x: (x["category"], x["required_level"], x["rarity_rank"], x["id"]))

    # Process Artifacts
    artifacts_list = []
    for a_id, art in artifacts_raw.items():
        if not isinstance(art, dict):
            continue
        rar = art.get("rarity", "COMMON")
        effs = art.get("passive_effects", [])
        artifacts_list.append({
            "id": a_id,
            "name": art.get("display_name", a_id),
            "description": art.get("description", ""),
            "rarity": rar,
            "rarity_rank": rarity_order.get(rar, 0),
            "effects": effs
        })
    artifacts_list.sort(key=lambda x: (x["rarity_rank"], x["id"]))

    payload = {
        "mobs": mobs_list,
        "items": items_list,
        "artifacts": artifacts_list,
        "recipes": recipes_list,
        "biomes": biomes_list,
        "balance": balance_raw
    }

    os.makedirs(os.path.dirname(OUT_JS), exist_ok=True)
    with open(OUT_JS, "w", encoding="utf-8") as f:
        f.write("// Auto-generated by scripts/export_wiki_fishing.py\n")
        f.write("window.FISHING_WIKI_DATA = ")
        json.dump(payload, f, ensure_ascii=False, indent=2)
        f.write(";\n")

    print(f"[export_wiki_fishing] Exported {len(mobs_list)} mobs, {len(items_list)} items, {len(artifacts_list)} artifacts, {len(recipes_list)} recipes, {len(biomes_list)} biomes -> {OUT_JS}")

if __name__ == "__main__":
    main()
