"""
Export Enchantments Data for Web Wiki (assets/js/enchants_data.js)
Reads enchantments_config.json, datapack JSONs, and multi-language lang files,
then generates a clean, rich, structured database for index.html and enchants.html.
"""

import json
import os
import re

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CONFIG_PATH = os.path.join(REPO_ROOT, "enchantments_config.json")
OUTPUT_DIR = os.path.join(REPO_ROOT, "assets", "js")
OUTPUT_FILE = os.path.join(OUTPUT_DIR, "enchants_data.js")

# Friendly names mapping for vanilla & custom enchants
KNOWN_DESCRIPTIONS = {
    "minecraft:sharpness": "Tăng sát thương gây ra bằng vũ khí cận chiến cho mọi loại kẻ địch.",
    "minecraft:smite": "Tăng thêm lượng lớn sát thương lên các sinh vật xác sống (Undead: Zombie, Skeleton, Wither...).",
    "minecraft:bane_of_arthropods": "Tăng sát thương lên các loài chân đốt (Nhện, Ong, Silverfish, Endermite) và gây hiệu ứng chậm.",
    "minecraft:knockback": "Đẩy lùi mục tiêu về phía sau khi bị đánh trúng.",
    "minecraft:fire_aspect": "Thiêu đốt mục tiêu khi tấn công cận chiến.",
    "minecraft:looting": "Tăng số lượng và tỉ lệ rơi các vật phẩm quý hiếm từ quái vật bị tiêu diệt.",
    "minecraft:sweeping_edge": "Tăng sát thương của đòn chém quét lan (Sweeping Attack).",
    "minecraft:unbreaking": "Giúp trang bị có cơ hội không bị giảm độ bền khi sử dụng.",
    "minecraft:mending": "Tự động sửa chữa độ bền của trang bị bằng cách hấp thụ điểm kinh nghiệm (XP).",
    "minecraft:efficiency": "Tăng tốc độ khai thác và đào bới của công cụ.",
    "minecraft:silk_touch": "Thu hoạch các khối nguyên vẹn chính xác như chúng vốn có thay vì làm rơi vật phẩm vỡ.",
    "minecraft:fortune": "Tăng sản lượng tài nguyên khoáng sản rơi ra từ quặng.",
    "minecraft:power": "Tăng đáng kể sát thương gây ra từ mũi tên bắn ra từ cung.",
    "minecraft:punch": "Đẩy lùi mục tiêu khi bị trúng tên từ cung.",
    "minecraft:flame": "Mũi tên bắn ra sẽ biến thành tên lửa, thiêu đốt mục tiêu khi bắn trúng.",
    "minecraft:infinity": "Bắn cung không tốn tên thường (chỉ cần mang theo ít nhất 1 mũi tên trong kho đồ).",
    "minecraft:luck_of_the_sea": "Tăng cơ hội câu được kho báu quý giá từ dưới nước.",
    "minecraft:lure": "Rút ngắn thời gian chờ cá cắn câu.",
    "minecraft:loyalty": "Đinh ba tự động bay quay trở lại tay chủ nhân sau khi phóng đi.",
    "minecraft:impaling": "Tăng sát thương lên các sinh vật dưới nước khi ném hoặc đâm đinh ba.",
    "minecraft:riptide": "Phóng người chơi đi như một mũi tên xé gió khi đang ở dưới nước hoặc khi trời mưa.",
    "minecraft:channeling": "Triệu hồi sấm sét giáng xuống mục tiêu khi ném đinh ba trong cơn bão sấm.",
    "minecraft:multishot": "Bắn ra 3 mũi tên cùng lúc từ nỏ với chi phí chỉ tiêu tốn 1 mũi tên.",
    "minecraft:quick_charge": "Rút ngắn thời gian nạp đạn của nỏ.",
    "minecraft:piercing": "Mũi tên bắn từ nỏ xuyên qua nhiều kẻ thù và có thể nhặt lại sau khi bắn.",
    "minecraft:density": "Tăng sát thương của Chùy (Mace) theo khoảng cách rơi tự do của người chơi.",
    "minecraft:breach": "Giảm hiệu quả phòng ngự từ áo giáp của mục tiêu khi đánh bằng Chùy (Mace).",
    "minecraft:wind_burst": "Kích hoạt luồng gió bùng nổ hất tung người chơi lên không trung khi tấn công bằng Chùy từ trên cao.",
    "minecraft:protection": "Giảm đa số các loại sát thương gánh chịu từ môi trường và quái vật.",
    "minecraft:fire_protection": "Giảm sát thương từ lửa, dung nham và giảm thời gian bị cháy.",
    "minecraft:feather_falling": "Giảm đáng kể sát thương nhận vào khi rơi từ trên cao.",
    "minecraft:blast_protection": "Giảm sát thương từ các vụ nổ và giảm độ đẩy lùi của vụ nổ.",
    "minecraft:projectile_protection": "Giảm sát thương từ các đòn đánh tầm xa (mũi tên, đạn ném).",
    "minecraft:respiration": "Kéo dài thời gian nhịn thở dưới nước và tăng tầm nhìn dưới nước.",
    "minecraft:aqua_affinity": "Tăng tốc độ đào bới dưới nước lên bằng tốc độ bình thường trên cạn.",
    "minecraft:thorns": "Phản lại sát thương lên kẻ địch khi chúng tấn công người mặc giáp.",
    "minecraft:depth_strider": "Tăng tốc độ di chuyển và bơi lội dưới nước.",
    "minecraft:frost_walker": "Đóng băng mặt nước thành băng tạm thời khi bước đi trên nước.",
    "minecraft:soul_speed": "Tăng tốc độ di chuyển khi đi trên khối cát linh hồn hoặc đất linh hồn.",
    "minecraft:swift_sneak": "Tăng tốc độ di chuyển khi đang ngồi xổm (sneak).",
    "minecraft:curse_of_binding": "Vật phẩm khi mặc vào sẽ không thể tự tháo ra cho đến khi nhân vật chết.",
    "minecraft:curse_of_vanishing": "Vật phẩm sẽ biến mất vĩnh viễn thay vì rơi ra đất khi nhân vật chết.",
    
    # Custom Ported Ultimates
    "enchantencore:absolute_unit": "Chế độ Tanker tối thượng: Khi ngồi xổm trên mặt đất, nhận hiệu ứng Hấp thụ (Absorption II) và Kháng cự (Resistance III), tạo sóng âm chấn động.",
    "enchantencore:adrenaline": "Bắn cung trúng đích tăng mạnh tốc độ di chuyển và hồi phục sinh lực trong giao tranh khốc liệt.",
    "enchantencore:archer_barrage": "Bắn ra một cơn mưa tên liên hoàn từ cung, quét sạch mọi kẻ thù trước mắt.",
    "enchantencore:aurora": "Mũ giáp phát quang hào quang cực quang tuyệt đẹp, hồi máu cho bản thân và đồng minh xung quanh.",
    "enchantencore:blaze_blessing": "Ban phước từ Quỷ lửa: Kháng hoàn toàn lửa, thiêu đốt kẻ địch xung quanh khi bị tấn công.",
    "enchantencore:bomber": "Mũi tên bắn ra phát nổ cực mạnh khi chạm vào bất kỳ kẻ địch hoặc khối nào.",
    "enchantencore:creeper_blessing": "Khi lượng máu xuống thấp nguy kịch, kích hoạt làn sóng nổ đẩy lùi và bảo vệ chủ nhân.",
    "enchantencore:crystalize_arrow": "Mũi tên pha lê làm đóng băng và làm bất động mục tiêu trong tinh thể pha lê.",
    "enchantencore:dolphin_blessing": "Tốc độ bơi lội siêu việt dưới nước như cá heo thần thoại, tăng lượng oxy vô tận.",
    "enchantencore:frenzy": "Mỗi đòn đánh trúng tăng thêm tốc độ đánh và tốc độ di chuyển dồn dập trong trạng thái cuồng nộ.",
    "enchantencore:habanero": "Thiêu đốt mặt đất và gây sát thương bỏng cực kỳ khủng khiếp lên mọi kẻ địch lân cận.",
    "enchantencore:heroes_crown": "Trao hiệu ứng Anh Hùng Làng (Hero of the Village V) liên tục, giảm giá giao dịch tối đa với dân làng.",
    "enchantencore:infinity_durability": "Độ bền vĩnh cửu: Vật phẩm không bao giờ bị giảm độ bền hoặc vỡ khi sử dụng.",
    "enchantencore:last_stand": "Khi sắp cạn máu, ngay lập tức hồi sinh một lượng máu lớn và tạo khiên phòng thủ bất hoại.",
    "enchantencore:machine_gun": "Tốc độ bắn nỏ siêu thanh liên thanh liên tục như súng máy tự động.",
    "enchantencore:neptune": "Đinh ba mang sức mạnh của Thần biển Neptune, triệu hồi bão tố và sát thương biển cả áp đảo.",
    "enchantencore:nightmare": "Đòn đánh gây bóng tối hắc ám và làm suy yếu ý chí chiến đấu của kẻ địch.",
    "enchantencore:nuclear_arrow": "Mũi tên nguyên tử tạo ra vụ nổ hủy diệt trên diện rộng với uy lực tối thượng.",
    "enchantencore:perfect_protection": "Bộ giáp bảo hộ hoàn mỹ: Giảm 40% mọi nguồn sát thương gánh chịu từ mọi phía.",
    "enchantencore:poseidon": "Đinh ba triệu hồi dòng nước xoáy và sóng thần nghiền nát mục tiêu.",
    "enchantencore:radiant_life": "Mỗi đòn chém hồi phục trực tiếp một lượng máu dồi dào cho người sử dụng.",
    "enchantencore:railgun": "Mũi tên điện từ bay với vận tốc ánh sáng theo đường thẳng tuyệt đối, xuyên phá mọi mục tiêu.",
    "enchantencore:reflection": "Phản lại toàn bộ mũi tên và đạn bắn tầm xa trở ngược lại kẻ tấn công.",
    "enchantencore:sniper": "Sát thương mũi tên tăng theo cấp số nhân dựa trên khoảng cách bắn cực xa.",
    "enchantencore:spectral_form": "Biến thân thành thể linh hồn: Miễn nhiễm sát thương vật lý trong chốc lát.",
    "enchantencore:vitality": "Tăng vĩnh viễn lượng máu tối đa của nhân vật lên thêm nhiều trái tim vàng.",
    "enchantencore:warden": "Khai mở sức mạnh bóng tối của Warden: Đòn đánh phát ra sóng xung kích Sonic Boom xuyên giáp.",
    "enchantencore:warped_arrow": "Mũi tên dị giới dịch chuyển tức thời người bắn đến vị trí tên chạm đất.",
    "enchantencore:warped_trident": "Dịch chuyển tức thời đến bất kỳ vị trí nào mà cây Đinh ba cắm trúng.",
    "enchantencore:wind_spirit": "Tăng vọt tốc độ di chuyển và khả năng nhảy cao bay lượn trên không như ngọn gió.",
    
    # Rare & Special Enchants
    "enchantencore:armor_piercing": "Đòn tấn công tầm xa bỏ qua chỉ số giáp phòng ngự của mục tiêu.",
    "enchantencore:dark_infused": "Tẩm độc tố bóng tối gây mù lòa và giảm thị lực kẻ địch.",
    "enchantencore:hurricane": "Tạo ra cơn cuồng phong lốc xoáy thổi bay mọi thực thể xung quanh điểm trúng tên.",
    "enchantencore:magic_protection": "Giảm mạnh sát thương từ các nguồn phép thuật (Potion, Poison, Wither, Evoker Fangs).",
    "enchantencore:poison_infused": "Mũi tên tẩm độc gây trúng độc nặng lên kẻ địch trúng phải.",
    "enchantencore:purify_water": "Thanh lọc nước xung quanh và làm tan biến các hiệu ứng xấu dưới nước.",
    "enchantencore:web_infused": "Tạo ra mạng nhện giữ chân kẻ thù ngay tại điểm mũi tên tiếp đất.",
    "enchantencore:wither_infused": "Tẩm nguyền rủa Wither ăn mòn máu của mục tiêu liên tục theo thời gian.",
    "enchantencore:sting": "Khiến ong xung quanh phát điên và đồng loạt lao vào tấn công kẻ địch bị trúng đòn.",
    "enchantencore:poppy_peace": "Lan tỏa thông điệp hòa bình, khiến mob trung lập không tấn công người chơi.",
    "enchantencore:cleansing": "Đỡ đòn bằng khiên giúp thanh tẩy sạch toàn bộ hiệu ứng bất lợi đang có trên người.",
    "enchantencore:forge_shot": "Mũi tên bắn qua lửa tự động nung chảy và gây sát thương cháy cực lớn.",
    "enchantplus:pickaxe/bedrock_breaker": "Phá hủy được cả khối Đá nền (Bedrock), tiêu hao 150 độ bền mỗi khối.",
    "enchantplus:pickaxe/spawner_touch": "Cho phép đào và thu hoạch trực tiếp lồng quái (Mob Spawner) nguyên vẹn.",
    "enchantplus:pickaxe/vein_miner": "Tự động đào toàn bộ các khối quặng cùng loại nối liền nhau chỉ trong 1 lần đập.",
    "enchantplus:tools/miningplus": "Đào bới khối theo diện tích rộng 3x3 quanh điểm đập.",
    "enchantplus:axe/timber": "Đốn hạ toàn bộ cả thân cây lớn từ gốc đến ngọn chỉ với 1 phát chặt rìu.",
    "enchantplus:helmet/voidless": "Tự động kích hoạt hiệu ứng Bay lơ lửng (Levitation) khi bị rơi xuống The Void để thoát chết.",
    "enchantplus:mace/teluric_wave": "Tạo ra các đợt sóng xung chấn địa chấn hất tung và gây sát thương diện rộng khi gõ Chùy xuống đất.",
    "enchantplus:sword/life_steal": "Hút một phần lượng máu của mục tiêu chuyển hóa thành máu cho bản thân khi chém trúng.",
    "enchantplus:sword/critical": "Gia tăng đáng kể tỉ lệ xuất hiện đòn đánh chí mạng gây sát thương chuẩn xuyên giáp.",
    "enchantplus:boots/lava_walker": "Biến dung nham dưới chân thành đá tạm thời để người chơi đi bộ trên dung nham an toàn.",
    "enchantplus:bow/explosive_arrow": "Mũi tên tạo ra vụ nổ công phá khi chạm đích.",
    "enchantplus:trident/gungnir_breath": "Đinh ba phóng ra hơi thở băng giá làm đóng băng nước và làm chậm kẻ thù.",
    "veinminer-enchantment:veinminer": "Đào liên hoàn toàn bộ mạch quặng khoáng sản nối liền chỉ với 1 thao tác.",
    "nova_structures:photosynthesis": "Tự động quang hợp hồi phục độ bền của trang bị khi người chơi đứng dưới ánh sáng mặt trời.",
    "nova_structures:wax_wings": "Tăng cường khả năng cản gió và lượn êm ái hơn cho Cánh Elytra.",
    "wither:soul_siphon": "Hút linh hồn của sinh vật bị hạ gục, tăng cường sức mạnh chiến đấu trong thoáng chốc.",
    "wither:witherbound": "Liên kết linh hồn với bóng tối Wither, ban khả năng kháng sát thương phân rã Wither."
}

def load_all_languages():
    langs = {}
    # Scan all datapacks for lang files
    for root, dirs, files in os.walk(os.path.join(REPO_ROOT, "datapacks")):
        for f in files:
            if f.endswith(".json") and "lang" in root.lower():
                p = os.path.join(root, f)
                try:
                    with open(p, "r", encoding="utf-8") as fp:
                        d = json.load(fp)
                        for k, v in d.items():
                            if isinstance(v, str):
                                langs[k] = v
                except:
                    pass
    return langs

def map_gear_type(supported_items):
    s = str(supported_items).lower()
    cats = []
    
    if any(k in s for k in ["sword", "sharp_weapon", "weapon", "mace", "spear", "axe"]):
        cats.append("weapon")
    if any(k in s for k in ["bow", "crossbow", "trident"]):
        cats.append("ranged")
    if any(k in s for k in ["armor", "head", "chest", "leg", "foot", "elytra", "shield"]):
        cats.append("armor")
    if any(k in s for k in ["mining", "pickaxe", "shovel", "hoe", "fishing", "tool"]):
        cats.append("tool")
    if any(k in s for k in ["horse", "wolf", "saddle", "harness"]):
        cats.append("mount")
        
    if not cats:
        if "durability" in s or "vanishing" in s or "damageable" in s or "all_enchantable" in s:
            cats = ["universal"]
        else:
            cats = ["other"]
            
    return cats

def extract_clean_desc(eid, cfg_desc, raw_effects, lang_map):
    # 1. Check known high-quality Vietnamese descriptions
    if eid in KNOWN_DESCRIPTIONS:
        return KNOWN_DESCRIPTIONS[eid]
        
    # 2. Check lang files lore
    k_lore = f"enchantment_lore.{eid}.description"
    if k_lore in lang_map:
        text = lang_map[k_lore]
        # Clean "Compatible with: ..." prefix
        parts = text.split("\n\n")
        if len(parts) > 1:
            return parts[-1].strip().lstrip("§")
        return text.replace("§Compatible with:", "Tương thích:").strip()
        
    # 3. Check enchantment.<ns>.<name>.desc
    ns, name = eid.split(":", 1)
    k_desc = f"enchantment.{ns}.{name}.desc"
    if k_desc in lang_map:
        return lang_map[k_desc]
        
    # 4. Fallback based on name or description text
    if isinstance(cfg_desc, str) and not cfg_desc.startswith("enchantment."):
        return f"Hiệu ứng phù phép đặc biệt {cfg_desc} giúp nâng cấp sức mạnh trang bị."
        
    title = name.replace("_", " ").title()
    return f"Phù phép đặc sắc thuộc bộ trang bị {title}, mang lại các thuộc tính bổ trợ hữu ích."

def main():
    print("[export] Loading enchantments_config.json...")
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg = json.load(f)
        
    lang_map = load_all_languages()
    print(f"[export] Loaded {len(lang_map)} localization keys from datapacks.")
    
    enchantments = cfg.get("enchantments", {})
    exported_list = []
    
    tier_order = {"ultimate": 1, "epic": 2, "rare": 3, "common": 4, "curse": 5}
    
    for eid, data in enchantments.items():
        if not data.get("enabled", True):
            continue
            
        ns, name = eid.split(":", 1)
        tier = data.get("tier", "common")
        is_curse = data.get("curse", False)
        if is_curse:
            tier = "curse"
            
        max_lv = data.get("max_level", 1)
        supported = data.get("supported_items", "")
        gear_cats = map_gear_type(supported)
        
        # Friendly title
        title_en = name.replace("_", " ").replace("/", " - ").title()
        
        # Check if vi_vn has a translated name
        k_vi = f"enchantment.{ns}.{name}"
        title_vi = lang_map.get(k_vi, title_en)
        
        # Description
        desc = extract_clean_desc(eid, data.get("description"), None, lang_map)
        
        # Format give command
        give_cmd = f'/give @p enchanted_book[enchantments={{levels:{{"{eid}":{max_lv}}}}}] 1'
        
        entry = {
            "id": eid,
            "namespace": ns,
            "raw_name": name,
            "name": title_en,
            "name_vi": title_vi,
            "tier": tier,
            "tier_order": tier_order.get(tier, 99),
            "max_level": max_lv,
            "anvil_cost": data.get("anvil_cost", 2),
            "table": data.get("table", False),
            "tradeable": data.get("tradeable", False),
            "loot": data.get("loot", True),
            "source_pack": data.get("source_pack", "vanilla"),
            "supported_items": supported,
            "gear_categories": gear_cats,
            "description": desc,
            "give_command": give_cmd
        }
        exported_list.append(entry)
        
    # Sort by tier priority, then by name
    exported_list.sort(key=lambda x: (x["tier_order"], x["name"]))
    
    os.makedirs(OUTPUT_DIR, exist_ok=True)
    
    # Write to enchants_data.js
    with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
        f.write("/* Auto-generated by scripts/export_wiki_enchants.py - DO NOT EDIT MANUALLY */\n")
        f.write("window.ALL_ENCHANTMENTS = ")
        json.dump(exported_list, f, indent=2, ensure_ascii=False)
        f.write(";\n")
        
    print(f"[export] Successfully exported {len(exported_list)} enchantments to {OUTPUT_FILE}")
    
    # Print statistics
    stats = {}
    for item in exported_list:
        t = item["tier"]
        stats[t] = stats.get(t, 0) + 1
    print("[export] Stats by tier:")
    for t, c in stats.items():
        print(f"  - {t}: {c}")

if __name__ == "__main__":
    main()
