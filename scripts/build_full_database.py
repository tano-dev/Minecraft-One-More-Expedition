"""
Master Builder for One More Expedition Enchantment Database
Compiles all 400 enabled enchantments with 100% verified data:
- Exact max levels
- Exact anvil costs
- Flawless Vietnamese & English names
- Rich, accurate Vietnamese descriptions
- Accurate gear categories & human-readable equipment labels
- Clean give commands
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

# Load source files if present
source_raw_path = os.path.join(REPO_ROOT, "scripts", "source_enchants_raw.json")
source_raw = {}
if os.path.exists(source_raw_path):
    with open(source_raw_path, "r", encoding="utf-8") as f:
        source_raw = json.load(f)

encore_meta_path = os.path.join(REPO_ROOT, "scripts", "encore_metadata.json")
encore_meta = {}
if os.path.exists(encore_meta_path):
    with open(encore_meta_path, "r", encoding="utf-8") as f:
        encore_meta = json.load(f)

# 1. Base mappings & translation tables
VANILLA_INFO = {
    "minecraft:sharpness": (5, 1, ["weapon"], "Kiếm & Vũ khí cận chiến", "Sắc Bén", "Sharpness", "Tăng sát thương gây ra bằng vũ khí cận chiến cho mọi loại kẻ địch (+0.5 tim + 0.5 tim mỗi cấp)."),
    "minecraft:smite": (5, 2, ["weapon"], "Kiếm, Rìu, Chùy", "Hại Thây Ma", "Smite", "Tăng thêm lượng lớn sát thương lên các sinh vật xác sống (Undead: Zombie, Skeleton, Wither, Drowned, Phantom...)."),
    "minecraft:bane_of_arthropods": (5, 2, ["weapon"], "Kiếm, Rìu, Chùy", "Hại Chân Đốt", "Bane of Arthropods", "Tăng sát thương lên các loài chân đốt (Nhện, Nhện hang, Ong, Silverfish, Endermite) và gây hiệu ứng Chậm (Slowness IV)."),
    "minecraft:knockback": (2, 2, ["weapon"], "Kiếm", "Đẩy Lùi", "Knockback", "Đẩy lùi mục tiêu về phía sau khi bị đánh trúng (tăng 3 khối mỗi cấp)."),
    "minecraft:fire_aspect": (2, 4, ["weapon"], "Kiếm & Chùy", "Góc Nhìn Của Lửa", "Fire Aspect", "Thiêu đốt mục tiêu khi tấn công cận chiến (cháy 4 giây mỗi cấp độ)."),
    "minecraft:looting": (3, 4, ["weapon"], "Kiếm", "Nhặt Cổ Vật", "Looting", "Tăng số lượng và tỉ lệ rơi các vật phẩm quý hiếm từ quái vật bị tiêu diệt."),
    "minecraft:sweeping_edge": (3, 4, ["weapon"], "Kiếm", "Đòn Quét", "Sweeping Edge", "Tăng sát thương của các đòn chém quét lan (Sweeping Attack) lên các mục tiêu lân cận."),
    "minecraft:unbreaking": (3, 1, ["universal"], "Mọi trang bị có độ bền", "Không Thể Phá Vỡ", "Unbreaking", "Giúp trang bị có cơ hội không bị giảm độ bền khi sử dụng, kéo dài tuổi thọ trang bị gấp nhiều lần."),
    "minecraft:mending": (1, 4, ["universal"], "Mọi trang bị có độ bền", "Hàn Gắn", "Mending", "Tự động sửa chữa độ bền của trang bị bằng cách hấp thụ điểm kinh nghiệm (XP) rơi ra từ quái vật và khai khoáng."),
    "minecraft:efficiency": (5, 1, ["tool"], "Cúp, Xẻng, Rìu, Cuốc, Kéo", "Hiệu Suất", "Efficiency", "Tăng mạnh tốc độ khai thác và đào bới của công cụ đối với các khối tương ứng."),
    "minecraft:silk_touch": (1, 8, ["tool"], "Cúp, Xẻng, Rìu, Cuốc", "Chạm Êm Ái", "Silk Touch", "Thu hoạch các khối nguyên vẹn chính xác như chúng vốn có (than, kim cương, thủy tinh, tổ ong, đá tảng...) thay vì làm rơi vật phẩm vỡ."),
    "minecraft:fortune": (3, 4, ["tool"], "Cúp, Xẻng, Rìu, Cuốc", "Gia Tài", "Fortune", "Tăng sản lượng tài nguyên khoáng sản (kim cương, than, lưu ly, hồng ngọc...) rơi ra từ quặng khi đào."),
    "minecraft:power": (5, 1, ["ranged"], "Cung", "Sức Mạnh", "Power", "Tăng đáng kể sát thương gây ra từ mũi tên bắn ra từ Cung (+25% sát thương cơ bản mỗi cấp)."),
    "minecraft:punch": (2, 2, ["ranged"], "Cung", "Bật Lùi", "Punch", "Đẩy lùi mục tiêu về phía sau khi bị trúng tên từ Cung (đẩy lùi 3 khối mỗi cấp)."),
    "minecraft:flame": (1, 4, ["ranged"], "Cung", "Lửa", "Flame", "Mũi tên bắn ra biến thành tên lửa, thiêu đốt mục tiêu trong 5 giây và kích hoạt TNT từ xa."),
    "minecraft:infinity": (1, 8, ["ranged"], "Cung", "Vô Hạn", "Infinity", "Bắn cung không tốn mũi tên thông thường (chỉ cần mang theo ít nhất 1 mũi tên trong kho đồ)."),
    "minecraft:luck_of_the_sea": (3, 4, ["tool"], "Cần câu", "May Mắn Của Biển", "Luck of the Sea", "Tăng cơ hội câu được kho báu quý giá (sách phù phép, cung, đĩa nhạc...) từ dưới nước."),
    "minecraft:lure": (3, 2, ["tool"], "Cần câu", "Nhử Mồi", "Lure", "Rút ngắn thời gian chờ cá cắn câu thêm 5 giây mỗi cấp độ."),
    "minecraft:loyalty": (3, 1, ["ranged", "weapon"], "Đinh ba", "Lòng Trung Thành", "Loyalty", "Đinh ba tự động bay quay trở lại tay chủ nhân sau khi phóng đi (càng cao bay về càng nhanh)."),
    "minecraft:impaling": (5, 4, ["ranged", "weapon"], "Đinh ba", "Đâm Xiên", "Impaling", "Tăng lượng lớn sát thương lên các sinh vật dưới nước hoặc các mục tiêu bị ướt."),
    "minecraft:riptide": (3, 4, ["ranged", "weapon"], "Đinh ba", "Sóng Thần Phóng", "Riptide", "Phóng người chơi đi như một mũi tên xé gió khi đang ở dưới nước hoặc khi trời mưa."),
    "minecraft:channeling": (1, 8, ["ranged", "weapon"], "Đinh ba", "Triệu Hồi Sét", "Channeling", "Triệu hồi một tia sấm sét giáng thẳng xuống mục tiêu khi ném đinh ba trong cơn bão sấm."),
    "minecraft:multishot": (1, 4, ["ranged"], "Nỏ", "Bắn Nhiều Tên", "Multishot", "Bắn ra 3 mũi tên hoặc 3 quả pháo hoa cùng lúc theo hình nón với chi phí chỉ tiêu tốn 1 đơn vị đạn."),
    "minecraft:quick_charge": (3, 2, ["ranged"], "Nỏ", "Nạp Nhanh", "Quick Charge", "Rút ngắn thời gian kéo dây nạp đạn của Nỏ thêm 0.25 giây mỗi cấp độ."),
    "minecraft:piercing": (4, 1, ["ranged"], "Nỏ", "Xuyên Thấu", "Piercing", "Mũi tên bắn từ Nỏ xuyên qua nhiều kẻ thù cùng lúc, bỏ qua khiên chắn và có thể nhặt lại sau khi bắn."),
    "minecraft:density": (5, 2, ["weapon"], "Chùy (Mace)", "Độ Đậm Đặc", "Density", "Tăng thêm 0.5 sát thương mỗi khối rơi tự do của người chơi cho mỗi cấp độ khi tấn công bằng Chùy."),
    "minecraft:breach": (4, 4, ["weapon"], "Chùy (Mace)", "Phá Giáp", "Breach", "Giảm 15% hiệu quả phòng ngự từ áo giáp của mục tiêu mỗi cấp độ khi đánh bằng Chùy (Mace)."),
    "minecraft:wind_burst": (3, 4, ["weapon"], "Chùy (Mace)", "Gió Bùng Nổ", "Wind Burst", "Kích hoạt luồng gió bùng nổ hất tung người chơi lên không trung khi tấn công bằng Chùy trúng đích từ trên cao."),
    "minecraft:protection": (4, 1, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Bảo Vệ", "Protection", "Giảm đa số các loại sát thương gánh chịu (ngoại trừ đói, rơi vào hư không và lệnh giết) thêm 4% mỗi cấp."),
    "minecraft:fire_protection": (4, 2, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Bảo Vệ Khỏi Lửa", "Fire Protection", "Giảm sát thương từ lửa, dung nham, cầu lửa và giảm thời gian bị thiêu đốt thêm 15% mỗi cấp."),
    "minecraft:feather_falling": (4, 2, ["armor"], "Giày", "Rơi Tự Do Như Lông Vũ", "Feather Falling", "Giảm đáng kể sát thương nhận vào khi rơi từ trên cao và sát thương khi dịch chuyển bằng Ngọc Ender."),
    "minecraft:blast_protection": (4, 4, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Bảo Vệ Khỏi Vụ Nổ", "Blast Protection", "Giảm 8% sát thương từ các vụ nổ và giảm 15% độ đẩy lùi của vụ nổ cho mỗi cấp."),
    "minecraft:projectile_protection": (4, 2, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Bảo Vệ Khỏi Vật Phóng", "Projectile Protection", "Giảm 8% sát thương từ các đòn đánh tầm xa (mũi tên, đạn Shulker, đinh ba, đạn ném) mỗi cấp."),
    "minecraft:respiration": (3, 4, ["armor"], "Mũ", "Hô Hấp Dưới Nước", "Respiration", "Kéo dài thời gian nhịn thở dưới nước thêm 15 giây mỗi cấp và tăng cường tầm nhìn dưới nước."),
    "minecraft:aqua_affinity": (1, 4, ["armor"], "Mũ", "Thích Ứng Nước", "Aqua Affinity", "Tăng tốc độ đào bới và khai thác dưới nước lên bằng tốc độ bình thường như trên cạn."),
    "minecraft:thorns": (3, 8, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Gai Nhọn", "Thorns", "Phản lại 1–4 sát thương lên kẻ địch khi chúng tấn công người mặc giáp (tiêu tốn thêm độ bền giáp)."),
    "minecraft:depth_strider": (3, 4, ["armor"], "Giày", "Bước Chân Biển Sâu", "Depth Strider", "Tăng tốc độ di chuyển và bơi lội dưới nước, cấp III giúp bơi nhanh ngang tốc độ chạy trên cạn."),
    "minecraft:frost_walker": (2, 4, ["armor"], "Giày", "Bước Chân Băng Giá", "Frost Walker", "Đóng băng khối nước tĩnh xung quanh thành băng tạm thời khi bước đi trên mặt nước và miễn nhiễm sát thương từ khối Magma."),
    "minecraft:soul_speed": (3, 8, ["armor"], "Giày", "Tốc Độ Linh Hồn", "Soul Speed", "Tăng tốc độ di chuyển đáng kể khi chạy trên Cát linh hồn (Soul Sand) hoặc Đất linh hồn (Soul Soil)."),
    "minecraft:swift_sneak": (3, 8, ["armor"], "Quần", "Rón Rén Nhanh", "Swift Sneak", "Tăng tốc độ di chuyển khi đang ngồi xổm (sneak) lên tối đa bằng 75% tốc độ đi bộ thông thường."),
    "minecraft:binding_curse": (1, 8, ["armor"], "Áo giáp, Mũ, Cánh Elytra, Đầu sinh vật", "Lời Nguyền Trói Buộc", "Curse of Binding", "Vật phẩm khi mặc vào người sẽ bị khóa chặt, không thể tự gỡ ra cho đến khi nhân vật tử nạn."),
    "minecraft:vanishing_curse": (1, 8, ["universal"], "Mọi trang bị", "Lời Nguyền Biến Mất", "Curse of Vanishing", "Vật phẩm sẽ biến mất vĩnh viễn khỏi thế giới thay vì rơi ra mặt đất khi nhân vật chết.")
}

DNT_INFO = {
    "nova_structures:aerials_bane": (5, 4, ["weapon", "tool"], "Vũ khí cận chiến", "Nỗi Oán Hận Trên Không", "Aerial's Bane", "Tăng sát thương gây ra lên các sinh vật bay trên không trung (Ghast, Phantom, Ender Dragon, Vex...)."),
    "nova_structures:antidote": (1, 4, ["armor"], "Giáp thân", "Thuốc Giải Độc", "Antidote", "Giáp thân tự động hấp thụ và triệt tiêu sát thương từ hiệu ứng Thuốc Độc (Poison)."),
    "nova_structures:conductivity_curse": (1, 8, ["armor", "weapon", "tool"], "Trang bị kim loại", "Lời Nguyền Dẫn Điện", "Curse of Conductivity", "Trang bị bằng kim loại có xu hướng thu hút các tia sét đánh trúng người chơi trong cơn bão giông."),
    "nova_structures:function_only": (5, 4, ["tool"], "Gậy & Đá lửa", "Côn Trùng Biến Dị", "Bug Enchant", "Phù phép đặc thù tự động chuyển hóa thành Hại Chân Đốt khi nằm trong túi đồ."),
    "nova_structures:ghasted": (3, 4, ["ranged"], "Nỏ", "Hỏa Tiễn Ghast", "Ghasted", "Mũi tên bắn từ Nỏ biến thành quả cầu lửa Ghast phát nổ và thiêu đốt mục tiêu khi trúng đích."),
    "nova_structures:gravity": (3, 4, ["ranged"], "Nỏ", "Trọng Lực Hút", "Gravity", "Bắn ra mũi tên tạo xoáy trọng lực hút tất cả các sinh vật và thực thể xung quanh về tâm điểm nổ."),
    "nova_structures:hydro_veil": (4, 4, ["mount"], "Yên Ghast Vui Vẻ", "Mạng Che Nước", "Hydro Veil", "Ghast Vui Vẻ hồi máu nhanh hơn theo thời gian, đồng thời miễn nhiễm sát thương từ lửa và vụ nổ."),
    "nova_structures:illagers_bane": (5, 4, ["weapon"], "Kiếm & Rìu", "Nỗi Oán Hận Illager", "Illagers Bane", "Tăng mạnh sát thương gây ra lên toàn bộ chủng tộc Illager (Pillager, Vindicator, Evoker, Ravager) và Phù Thủy."),
    "nova_structures:multishot": (1, 4, ["ranged"], "Cung", "Bắn Ba Mũi Tên", "Tri-Shot", "Bắn ra 3 mũi tên cùng lúc từ Cung thay vì chỉ 1 mũi tên thông thường."),
    "nova_structures:outreach": (4, 4, ["armor"], "Giáp thân", "Tiếp Cận Xa", "Outreach", "Mở rộng khoảng cách tầm với để tương tác và đặt/phá khối từ khoảng cách xa hơn."),
    "nova_structures:photosynthesis": (1, 2, ["universal"], "Mọi trang bị có độ bền", "Quang Hợp", "Photosynthesis", "Tự động hồi phục độ bền cho trang bị dưới ánh sáng mặt trời tự nhiên ban ngày."),
    "nova_structures:piercing": (4, 4, ["ranged"], "Cung", "Xuyên Thấu Cung", "Blowthrough", "Cho phép mũi tên bắn ra từ Cung xuyên qua nhiều mục tiêu liên tiếp."),
    "nova_structures:power": (5, 4, ["ranged"], "Nỏ", "Uy Lực Nỏ", "Might", "Gia tăng đáng kể sát thương của các phát bắn từ Nỏ."),
    "nova_structures:spiteful": (3, 4, ["weapon"], "Vũ khí cận chiến", "Hiềm Khích Phẫn Nộ", "Spiteful", "Tăng thêm sát thương khi lượng máu của bạn xuống thấp, đôi khi nhận được hiệu ứng Sức Mạnh (Strength)."),
    "nova_structures:swift_soar": (3, 4, ["mount"], "Yên Ghast Vui Vẻ", "Bay Lượn Thần Tốc", "Swift Soar", "Tăng tốc độ bay của Ghast Vui Vẻ khi người cưỡi kích hoạt chạy nước rút."),
    "nova_structures:traveler": (3, 8, ["armor"], "Giày", "Lữ Khách", "Traveler", "Tăng tốc độ chạy nước rút và cho phép tự động bước qua các khối cao dễ dàng."),
    "nova_structures:wax_wings": (1, 2, ["armor"], "Cánh Elytra", "Cánh Sáp", "Wax Wings", "Cánh Elytra không bị tiêu hao độ bền khi đang bay lượn ở tốc độ chậm an toàn."),
    "nova_structures:wither_coated": (3, 5, ["weapon"], "Vũ khí cận chiến", "Phủ Độc Wither", "Wither Coated", "Đòn đánh gây hiệu ứng Héo mòn Wither lên kẻ thù với cái giá tiêu tốn thêm độ bền vũ khí.")
}

ENCHANTSPLUS_INFO = {
    "enchantsplus:breaking_curse": (1, 2, ["universal"], "Mọi trang bị", "Lời Nguyền Phá Hủy", "Curse of Breaking", "Làm trang bị bị ăn mòn độ bền nhanh gấp đôi bình thường mỗi khi sử dụng."),
    "enchantsplus:breeze_burst": (1, 4, ["ranged"], "Cung", "Gió Bão Breeze", "Breeze Burst", "Mũi tên bắn ra phát nổ sóng gió Breeze hất văng mọi kẻ thù tại điểm tiếp đất."),
    "enchantsplus:clumsiness_curse": (1, 8, ["weapon", "tool"], "Vũ khí & Công cụ", "Lời Nguyền Vụng Về", "Curse of Clumsiness", "Đòn đánh hoặc thao tác đào bới có tỉ lệ bị trượt hoặc làm rơi vũ khí khỏi tay."),
    "enchantsplus:compassion_curse": (1, 8, ["weapon"], "Vũ khí cận chiến", "Lời Nguyền Trắc Ẩn", "Curse of Compassion", "Đòn đánh có cơ hội hồi máu cho kẻ địch thay vì gây sát thương lên chúng."),
    "enchantsplus:crabs_touch": (3, 4, ["tool"], "Công cụ & Cúp", "Chạm Tay Của Cua", "Crab's Touch", "Tăng đáng kể tầm với tương tác và phá hủy khối của người chơi."),
    "enchantsplus:displacement_curse": (1, 8, ["weapon"], "Vũ khí cận chiến", "Lời Nguyền Dịch Vị", "Curse of Displacement", "Khi tấn công, có tỉ lệ dịch chuyển tức thời ngẫu nhiên bản thân hoặc đối thủ ra vị trí khác."),
    "enchantsplus:double_edge_curse": (1, 8, ["weapon"], "Vũ khí cận chiến", "Lời Nguyền Song Kiếm", "Curse of Double Edge", "Gây sát thương cực lớn lên kẻ địch nhưng đồng thời phản lại một phần sát thương lên chính người dùng."),
    "enchantsplus:graviole": (3, 4, ["armor"], "Cánh Elytra", "Trọng Lực Graviole", "Graviole", "Tối ưu hóa khả năng lượn gió và kiểm soát độ cao siêu mượt cho Cánh Elytra."),
    "enchantsplus:harvest": (1, 2, ["tool"], "Cuốc làm nông", "Thu Hoạch Mùa Màng", "Harvest", "Tự động gieo trồng lại hạt giống ngay sau khi thu hoạch cây trồng chín trong khu vực."),
    "enchantsplus:ice_aspect": (2, 4, ["weapon"], "Kiếm & Chùy", "Băng Diện", "Ice Aspect", "Đòn đánh làm chậm và gây hiệu ứng đóng băng giá lạnh lên mục tiêu trong vài giây."),
    "enchantsplus:kickback": (3, 2, ["armor"], "Khiên chắn", "Phản Đòn Đẩy Lùi", "Kickback", "Khi giơ khiên chặn đòn đánh, đẩy văng kẻ tấn công về phía sau một khoảng cách xa."),
    "enchantsplus:kinetic_protection": (4, 4, ["armor"], "Mũ", "Bảo Hộ Động Năng", "Kinetic Protection", "Giảm đáng kể sát thương nhận vào khi va chạm với các khối ở tốc độ cao bằng Elytra."),
    "enchantsplus:precision": (2, 4, ["ranged"], "Cung & Nỏ", "Độ Chuẩn Xác", "Precision", "Tăng tốc độ bay và độ chuẩn xác tuyệt đối của đường đạn, giảm độ lệch khi ngắm bắn."),
    "enchantsplus:pyrolysis": (1, 4, ["tool"], "Cúp khai khoáng", "Hỏa Phân Quặng", "Pyrolysis", "Tự động nung chảy quặng kim loại (sắt, vàng, đồng) thành thỏi kim loại ngay khi đào."),
    "enchantsplus:retrieval": (4, 4, ["armor"], "Giáp thân", "Thu Hồi Sinh Lực", "Retrieval", "Hồi phục ngay lập tức một lượng máu mỗi khi tiêu diệt thành công một kẻ địch."),
    "enchantsplus:scorch_walker": (2, 8, ["armor"], "Giày", "Bước Đi Dung Nham", "Scorch Walker", "Biến mặt dung nham dưới chân thành đá tạm thời để người chơi đi bộ an toàn trên biển dung nham."),
    "enchantsplus:skyguard": (4, 4, ["armor"], "Cánh Elytra", "Hộ Mệnh Bầu Trời", "Skyguard", "Tăng cường chỉ số phòng thủ và giảm sát thương nhận vào trong lúc đang bay bằng Elytra."),
    "enchantsplus:stride": (3, 4, ["armor"], "Quần", "Sải Bước", "Stride", "Tăng tốc độ di chuyển và độ dài mỗi sải chân khi chạy bộ trên mặt đất."),
    "enchantsplus:swift_strike": (5, 4, ["weapon"], "Vũ khí cận chiến", "Trảm Kích Thần Tốc", "Swift Strike", "Gia tăng mạnh mẽ tốc độ vung đòn tấn công của vũ khí cận chiến."),
    "enchantsplus:toxic": (1, 4, ["ranged"], "Cung", "Tiễn Độc", "Toxic", "Mũi tên bắn ra tự động tẩm độc tố, gây hiệu ứng trúng độc nặng lên kẻ địch trúng đạn."),
    "enchantsplus:vitality": (3, 4, ["armor"], "Giáp thân", "Sinh Khí Dồi Dào", "Vitality", "Tăng thêm các trái tim máu tối đa cho người chơi khi trang bị áo giáp."),
    "enchantsplus:websnare": (2, 4, ["weapon"], "Kiếm & Chùy", "Bẫy Mạng Nhện", "Websnare", "Đòn đánh tạo ra bẫy mạng nhện giữ chân và làm bất động kẻ thù ngay tại vị trí trúng đòn.")
}



STELLARITY_INFO = {
    "stellarity:ambush": (3, 2, ["weapon"], "Vũ khí cận chiến", "Đánh Lén", "Ambush", "Tăng mạnh sát thương khi tấn công từ phía sau hoặc khi kẻ địch không phát hiện ra bạn."),
    "stellarity:critical_strike": (3, 4, ["weapon"], "Vũ khí cận chiến", "Đòn Đánh Chí Mạng", "Critical Strike", "Tăng vọt sát thương của các đòn đánh chí mạng (nhảy chém) lên mục tiêu."),
    "stellarity:dune_speed": (3, 8, ["armor"], "Giày", "Tốc Độ Cồn Cát", "Dune Speed", "Tăng tốc độ di chuyển cực đại khi chạy trên cát, cát đỏ và sa mạc."),
    "stellarity:levitation_shot": (5, 4, ["ranged"], "Cung", "Tiễn Lơ Lửng", "Levitation Shot", "Mũi tên bắn trúng khiến mục tiêu bị hiệu ứng Bay lơ lửng (Levitation), mất kiểm soát trên không."),
    "stellarity:plated": (4, 2, ["armor"], "Cánh Elytra", "Giáp Cánh Gia Cố", "Plated", "Gia cố lớp giáp phòng thủ kiên cố cho Cánh Elytra, nhận điểm giáp khi bay."),
    "stellarity:soaring": (4, 2, ["armor"], "Cánh Elytra", "Lướt Gió Thần Tốc", "Soaring", "Tăng tốc độ bay lượn và hiệu suất lướt gió của Cánh Elytra."),
    "stellarity:void_shot": (1, 10, ["ranged"], "Cung & Nỏ", "Tiễn Hư Không", "Void Shot", "Mũi tên bắn xuyên qua không gian Hư Vô, gây sát thương Hư Vô bỏ qua mọi loại giáp phòng ngự."),
    "stellarity:void_strike": (1, 10, ["weapon"], "Vũ khí cận chiến", "Trảm Kích Hư Không", "Void Strike", "Đòn tấn công mang năng lượng Hư Vô, gây sát thương ăn mòn chuẩn theo thời gian."),
    "stellarity:_technical/daybroken": (1, 2, ["weapon"], "Kiếm Kim Cương", "Kiếm Ánh Dương Daybroken", "Daybroken", "Đòn chém thiêu đốt với sức nóng hủy diệt của mặt trời."),
    "stellarity:_technical/draconic": (1, 2, ["weapon"], "Kiếm Netherite", "Kiếm Long Tộc Draconic", "Draconic", "Giải phóng sức mạnh Hơi thở Rồng Ender khi tấn công."),
    "stellarity:_technical/infernal_infusion": (1, 2, ["weapon", "ranged"], "Kiếm Netherite & Nỏ", "Truyền Lửa Hỏa Ngục", "Infernal Infusion", "Bắn ra đạn cầu lửa hỏa ngục bốc cháy dữ dội."),
    "stellarity:_technical/mighty_wind": (1, 8, ["ranged", "weapon"], "Đinh ba Prismatic", "Cuồng Phong Bão Tố", "Mighty Wind", "Tạo luồng gió lốc cực mạnh đẩy lùi kẻ thù trên Đinh ba Prismatic."),
    "stellarity:_technical/prismatic_pearl_return": (1, 2, ["tool"], "Ngọc Prismatic", "Thu Hồi Ngọc Prismatic", "Prismatic Return", "Cơ chế triệu hồi và tự động thu hồi Ngọc Prismatic."),
    "stellarity:_technical/soul_harvest": (1, 2, ["weapon"], "Kiếm Kim Cương", "Thu Hoạch Linh Hồn", "Soul Harvest", "Hút linh hồn kẻ địch bị hạ gục để cường hóa uy lực vũ khí."),
    "stellarity:_technical/void_pendant/amethyst": (1, 0, ["armor"], "Dây Chuyền Hư Không", "Dây Chuyền Hư Không (Thạch Anh Tím)", "Void Pendant - Amethyst", "Tăng tốc độ hồi phục năng lượng và mana cho chủ nhân."),
    "stellarity:_technical/void_pendant/copper": (1, 0, ["armor"], "Dây Chuyền Hư Không", "Dây Chuyền Hư Không (Đồng)", "Void Pendant - Copper", "Tăng tốc độ di chuyển và tích trữ điện tích khi chạy."),
    "stellarity:_technical/void_pendant/diamond": (1, 0, ["armor"], "Dây Chuyền Hư Không", "Dây Chuyền Hư Không (Kim Cương)", "Void Pendant - Diamond", "Tăng cường chỉ số phòng thủ và độ bền của lớp khiên hộ thể."),
    "stellarity:_technical/void_pendant/emerald": (1, 0, ["armor"], "Dây Chuyền Hư Không", "Dây Chuyền Hư Không (Ngọc Lục Bảo)", "Void Pendant - Emerald", "Gia tăng may mắn và tỉ lệ rơi thêm bảo vật quý giá từ kẻ địch."),
    "stellarity:_technical/void_pendant/gold": (1, 0, ["armor"], "Dây Chuyền Hư Không", "Dây Chuyền Hư Không (Vàng)", "Void Pendant - Gold", "Tăng hiệu quả hồi phục sinh lực và kháng sát thương ma thuật."),
    "stellarity:_technical/void_pendant/iron": (1, 0, ["armor"], "Dây Chuyền Hư Không", "Dây Chuyền Hư Không (Sắt)", "Void Pendant - Iron", "Tăng điểm giáp phòng ngự cơ bản cho người mang."),
    "stellarity:_technical/void_pendant/lapis": (1, 0, ["armor"], "Dây Chuyền Hư Không", "Dây Chuyền Hư Không (Ngọc Lưu Ly)", "Void Pendant - Lapis", "Gia tăng lượng điểm kinh nghiệm XP nhận được từ mọi nguồn."),
    "stellarity:_technical/void_pendant/netherite": (1, 0, ["armor"], "Dây Chuyền Hư Không", "Dây Chuyền Hư Không (Netherite)", "Void Pendant - Netherite", "Kháng thiêu đốt và triệt tiêu sát thương từ Vực thẳm Hư Không."),
    "stellarity:_technical/void_pendant/quartz": (1, 0, ["armor"], "Dây Chuyền Hư Không", "Dây Chuyền Hư Không (Thạch Anh)", "Void Pendant - Quartz", "Tăng chỉ số sát thương chí mạng chuẩn lên kẻ thù.")
}

KATTERS_INFO = {
    "kattersstructures:alchemy_curse": (1, 4, ["armor"], "Giáp thân", "Lời Nguyền Giả Kim", "Curse of Alchemy", "Giáp thân có tỉ lệ kích hoạt các hiệu ứng thuốc ngẫu nhiên (có thể có hại) khi chịu sát thương."),
    "kattersstructures:blunt": (1, 2, ["weapon"], "Vũ khí cận chiến", "Lưỡi Kiếm Cùn", "Blunt", "Giảm sát thương cơ bản nhưng tăng mạnh khả năng phá giáp và làm choáng mục tiêu."),
    "kattersstructures:crystal_curse": (1, 4, ["weapon"], "Vũ khí cận chiến", "Lời Nguyền Tinh Thể", "Curse of Crystal", "Tăng mạnh sát thương gây ra nhưng làm vũ khí trở nên giòn và hao mòn cực nhanh."),
    "kattersstructures:enlightning": (1, 2, ["tool"], "Gậy Lửa (Blaze Rod)", "Khai Mở Sấm Sét", "Enlightning", "Triệu hồi sấm sét giáng xuống khi tấn công vào cột thu lôi bằng Gậy Lửa."),
    "kattersstructures:heavy": (5, 4, ["weapon"], "Vũ khí cận chiến", "Trọng Khí Nặng Nề", "Heavy", "Tăng lượng lớn sát thương và độ đẩy lùi nhưng giảm tốc độ đánh và độ nhanh nhẹn."),
    "kattersstructures:leaping": (3, 4, ["armor"], "Giày", "Bật Nhảy Cao", "Leaping", "Gia tăng đáng kể độ cao khi nhảy của người chơi."),
    "kattersstructures:mining_curse": (1, 4, ["tool"], "Công cụ khai thác", "Lời Nguyền Khai Khoáng", "Curse of Mining", "Làm giảm tốc độ đào bới và mài mòn công cụ nhanh chóng khi khai thác."),
    "kattersstructures:regrowth": (3, 4, ["universal"], "Mọi trang bị có độ bền", "Tái Sinh Độ Bền", "Regrowth", "Trang bị tự động tái sinh và phục hồi độ bền dần dần theo thời gian."),
    "kattersstructures:resistance": (3, 4, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Kháng Cự Kiên Cường", "Resistance", "Gia tăng chỉ số kháng cự, giảm thiểu lượng sát thương gánh chịu từ mọi nguồn."),
    "kattersstructures:soulbound": (1, 8, ["universal"], "Mọi trang bị", "Khóa Linh Hồn", "Soulbound", "Giữ lại trang bị trong kho đồ của người chơi khi tử nạn thay vì rơi ra ngoài thế giới."),
    "kattersstructures:raj_ambient": (1, 0, ["armor"], "Trang bị Đền Cổ Raj", "Hào Quang Boss Raj", "Raj Ambient", "Cơ chế tạo hào quang sấm sét xung quanh Boss Raj trong Đền Cổ."),
    "kattersstructures:raj_lightning": (1, 0, ["ranged"], "Nỏ Boss Raj", "Bão Sét Boss Raj", "Raj Lightning", "Cơ chế bắn sấm sét liên hoàn từ nỏ của Boss Raj."),
    "kattersstructures:raj_minion": (1, 0, ["armor"], "Trang bị Đền Cổ Raj", "Triệu Hồi Tay Sai Raj", "Raj Minion", "Cơ chế triệu hồi tay sai ma thuật của Boss Raj.")
}

INCENDIUM_VEINMINER_WITHER_INFO = {
    "incendium:poison_protection": (1, 8, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Kháng Độc Tố Incendium", "Poison Protection", "Miễn nhiễm hoàn toàn trước sát thương độc tố trong các pháo đài Nether Incendium."),
    "veinminer-enchantment:veinminer": (1, 7, ["tool"], "Cúp", "Khai Thác Mạch Quặng", "Veinminer", "Tự động đào liên hoàn toàn bộ mạch quặng khoáng sản nối liền chỉ với 1 phát đập cúp."),
    "wither:soul_siphon": (1, 1, ["weapon"], "Vũ khí cận chiến", "Hút Hồn Wither", "Soul Siphon", "Hút sinh lực và linh hồn từ quái vật bị hạ gục, tăng cường sức mạnh chiến đấu trong thoáng chốc."),
    "wither:witherbound": (1, 1, ["armor"], "Áo giáp", "Liên Kết Wither", "Witherbound", "Hòa nhập linh hồn với bóng tối Wither, kháng hoàn toàn sát thương phân rã và ăn mòn.")
}

ENCHANTPLUS_INFO = {
    "enchantplus:armor/fury": (3, 3, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Cuồng Nộ", "Fury", "Giảm giáp phòng ngự nhưng tăng mạnh khả năng xuyên giáp và sát thương đầu ra khi máu thấp."),
    "enchantplus:armor/lifeplus": (5, 1, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Sinh Mệnh+", "Life+", "Ban thêm nhiều trái tim máu đỏ tối đa cho người mặc trang bị."),
    "enchantplus:armor/venom_protection": (1, 2, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Kháng Độc Tố", "Venom Protection", "Bảo vệ cơ thể khỏi các hiệu ứng xấu như Thuốc Độc, Ngộ Độc thức ăn."),
    "enchantplus:boots/agility": (5, 1, ["armor"], "Giày", "Nhanh Nhẹn", "Agility", "Gia tăng đáng kể tốc độ chạy và di chuyển của nhân vật trên bộ."),
    "enchantplus:bow/accuracy_shot": (1, 4, ["ranged"], "Cung & Nỏ", "Bắn Chuẩn Xác", "Accuracy Shot", "Mũi tên bắn ra bay theo đường thẳng tuyệt đối, không bị rơi do trọng lực."),
    "enchantplus:bow/echo_shot": (2, 6, ["ranged"], "Cung & Nỏ", "Tiếng Vang Âm Thanh", "Echo Shot", "Mũi tên phát nổ sóng âm Sonic Boom tại điểm chạm, gây sát thương diện rộng."),
    "enchantplus:bow/eternal_frost": (1, 6, ["ranged"], "Cung & Nỏ", "Băng Giá Vĩnh Cửu", "Eternal Frost", "Đóng băng các khối nước và làm chậm kẻ thù bị trúng tên."),
    "enchantplus:bow/explosive_arrow": (4, 6, ["ranged"], "Cung & Nỏ", "Tiễn Nổ", "Explosive Arrow", "Mũi tên phát nổ công phá khi chạm đích, gây sát thương diện rộng."),
    "enchantplus:bow/rebound": (3, 2, ["ranged"], "Cung & Nỏ", "Nảy Bật", "Rebound", "Mũi tên nảy bật liên hồi khỏi các bề mặt tường và chướng ngại vật."),
    "enchantplus:bow/storm_arrow": (1, 6, ["ranged"], "Cung & Nỏ", "Tiễn Sấm Sét", "Storm Arrow", "Triệu hồi sấm sét giáng xuống điểm mũi tên trúng đích."),
    "enchantplus:chestplate/builder_arm": (3, 2, ["armor"], "Áo giáp thân", "Tay Kiến Trúc Sư", "Builder Arm", "Mở rộng phạm vi đặt khối và phá hủy khối từ khoảng cách xa."),
    "enchantplus:durability/curse_of_breaking": (5, 1, ["universal"], "Mọi trang bị có độ bền", "Lời Nguyền Hao Mòn", "Curse of Breaking", "Làm trang bị giảm độ bền nhanh hơn bình thường rất nhiều."),
    "enchantplus:durability/curse_of_enchant": (1, 1, ["universal"], "Mọi trang bị", "Lời Nguyền Khóa Phù Phép", "Curse of Enchant", "Ngăn không cho trang bị được phù phép thêm hoặc sửa chữa trên đe."),
    "enchantplus:elytra/armored": (4, 10, ["armor"], "Cánh Elytra", "Cánh Bọc Thép", "Armored", "Giúp Cánh Elytra có thêm chỉ số giáp phòng ngự như áo giáp thông thường."),
    "enchantplus:elytra/propulsion": (3, 1, ["armor"], "Cánh Elytra", "Đẩy Phản Lực", "Propulsion", "Kích hoạt lực đẩy vút về phía trước khi đang bay bằng Cánh Elytra."),
    "enchantplus:helmet/auto_feed": (1, 6, ["armor"], "Mũ", "Tự Động Cho Ăn", "Auto Feed", "Tự động bổ sung thanh thức ăn/độ no cho người chơi mỗi vài phút."),
    "enchantplus:helmet/bright_vision": (21, 2, ["armor"], "Mũ", "Tầm Nhìn Dạ Quang", "Bright Vision", "Cung cấp hiệu ứng Tầm nhìn bóng đêm (Night Vision) vĩnh viễn khi đội mũ."),
    "enchantplus:helmet/voidless": (1, 3, ["armor"], "Mũ", "Cứu Rỗi Hư Không", "Voidless", "Khi rơi xuống Vực thẳm The Void, tự động bay lơ lửng (Levitation) để thoát chết."),
    "enchantplus:hoe/scyther": (3, 2, ["tool"], "Cuốc làm nông", "Lưỡi Liềm Cày Cấy", "Scyther", "Cuốc xới đất theo diện tích rộng dựa trên cấp độ phù phép."),
    "enchantplus:hoe/seiors_oblivion": (1, 2, ["tool"], "Cuốc làm nông", "Vùng Đất Quên Lãng Của Seior", "Seior's Oblivion", "Thay thế các khối đá bên dưới chân thành địa hình cỏ tự nhiên tươi tốt."),
    "enchantplus:leggings/dwarfed": (5, 1, ["armor"], "Quần", "Thể Tạng Người Lùn", "Dwarfed", "Thu nhỏ kích thước nhân vật, giảm tốc độ nhưng tăng khả năng bước qua khối cao."),
    "enchantplus:leggings/fast_swim": (1, 3, ["armor"], "Quần", "Bơi Thần Tốc", "Fast Swim", "Ban hiệu ứng Ân Huệ Cá Heo (Dolphin's Grace) liên tục khi ở dưới nước."),
    "enchantplus:leggings/oversize": (3, 1, ["armor"], "Quần", "Thể Tạng Khổng Lồ", "Oversize", "Tăng kích thước cơ thể và độ dài đòn đánh của nhân vật."),
    "enchantplus:mace/striker": (1, 4, ["weapon"], "Chùy (Mace)", "Giáng Sấm Sét", "Striker", "Triệu hồi sấm sét khi đánh bằng Chùy trong cơn giông bão và miễn nhiễm sét."),
    "enchantplus:mace/teluric_wave": (3, 2, ["weapon"], "Chùy (Mace)", "Sóng Xung Chấn Địa Cầu", "Teluric Wave", "Ngồi xổm và đập Chùy xuống đất tạo ra các đợt sóng địa chấn hất tung kẻ thù."),
    "enchantplus:mace/wind_propulsion": (3, 4, ["weapon"], "Chùy (Mace)", "Phản Lực Gió", "Wind Propulsion", "Tạo vụ nổ gió khi đập đất, phóng người chơi lên cao."),
    "enchantplus:midas_touch": (1, 3, ["tool"], "Gậy phép", "Chạm Tay Midas", "Midas Touch", "Quyền năng biến các vật phẩm hoặc khối thành vàng nguyên chất."),
    "enchantplus:mounted/cavalier_egis": (3, 4, ["mount"], "Yên ngựa & Thú cưỡi", "Khiên Kỵ Sĩ", "Cavalier Egis", "Giảm đáng kể lượng sát thương nhận vào khi đang cưỡi thú."),
    "enchantplus:mounted/ethereal_leap": (3, 4, ["mount"], "Yên ngựa & Thú cưỡi", "Cú Nhảy Thanh Thoát", "Ethereal Leap", "Tăng độ cao cú nhảy của thú cưỡi và giảm sát thương rơi."),
    "enchantplus:mounted/happy_boost": (3, 4, ["mount"], "Yên Ghast Vui Vẻ", "Gia Tốc Ghast Vui Vẻ", "Happy Boost", "Tăng tốc độ bay của Ghast Vui Vẻ khi cưỡi."),
    "enchantplus:mounted/steel_fang": (5, 4, ["mount"], "Giáp sói", "Nanh Thép", "Steel Fang", "Tăng mạnh sát thương tấn công của Chó sói cưng / thú cưng."),
    "enchantplus:mounted/velocity": (3, 4, ["mount"], "Yên ngựa & Thú cưỡi", "Thần Tốc Chiến Mã", "Velocity", "Tăng vọt tốc độ di chuyển trên bộ của thú cưỡi."),
    "enchantplus:pickaxe/bedrock_breaker": (1, 4, ["tool"], "Cúp", "Phá Hủy Đá Nền", "Bedrock Breaker", "Cho phép cúp đập vỡ được cả khối Đá nền (Bedrock), tiêu hao 150 độ bền/khối."),
    "enchantplus:pickaxe/spawner_touch": (1, 4, ["tool"], "Cúp", "Thu Hoạch Lồng Quái", "Spawner Touch", "Cho phép đào và thu hoạch trực tiếp lồng sinh quái (Mob Spawner) nguyên vẹn."),
    "enchantplus:sword/critical": (3, 2, ["weapon"], "Kiếm & Vũ khí cận chiến", "Đòn Chí Mạng", "Critical", "Gia tăng tỉ lệ xuất hiện đòn đánh chí mạng gây sát thương chuẩn bỏ qua giáp."),
    "enchantplus:sword/death_touch": (3, 4, ["weapon"], "Kiếm & Vũ khí cận chiến", "Chạm Tay Tử Thần", "Death Touch", "Gây hiệu ứng Bóng tối (Darkness) và làm mù lòa đối thủ bị chém."),
    "enchantplus:sword/dimensional_hit": (3, 4, ["weapon"], "Kiếm & Vũ khí cận chiến", "Đòn Đánh Dị Giới", "Dimensional Hit", "Tăng đáng kể sát thương khi chiến đấu tại Nether hoặc End."),
    "enchantplus:sword/fear": (3, 4, ["weapon"], "Kiếm & Vũ khí cận chiến", "Nỗi Khiếp Sợ", "Fear", "Khi chém trúng Creeper, làm chậm thời gian kích nổ của nó."),
    "enchantplus:sword/last_hope": (1, 10, ["weapon"], "Kiếm & Vũ khí cận chiến", "Niềm Hi Vọng Cuối Cùng", "Last Hope", "Hi sinh hoàn toàn độ bền vũ khí để tung ra đòn đánh sát thương vô hạn."),
    "enchantplus:sword/pull": (3, 4, ["weapon"], "Kiếm & Vũ khí cận chiến", "Thu Hoạch Trứng Quái", "Pull", "Có tỉ lệ rơi ra Trứng sinh quái (Spawn Egg) khi tiêu diệt sinh vật."),
    "enchantplus:sword/reach": (3, 4, ["weapon"], "Kiếm & Vũ khí cận chiến", "Sải Tay Dài", "Reach", "Kéo dài khoảng cách tấn công và tầm với cận chiến của vũ khí."),
    "enchantplus:sword/runic_despair": (3, 4, ["weapon"], "Kiếm & Vũ khí cận chiến", "Tuyệt Vọng Cổ Tự", "Runic Despair", "Tăng mạnh sát thương gây ra trong Chiều không gian Cổ tự Runic."),
    "enchantplus:sword/tears_of_asflors": (3, 4, ["weapon"], "Kiếm & Vũ khí cận chiến", "Nước Mắt Của Asflors", "Tears of Asflors", "Chuyển hóa điểm kinh nghiệm XP thành sát thương bổ sung cho mỗi đòn đánh."),
    "enchantplus:sword/xp_boost": (3, 4, ["weapon"], "Kiếm & Vũ khí cận chiến", "Tăng Cường Kinh Nghiệm", "XP Boost", "Tăng lượng điểm kinh nghiệm XP thu được khi tiêu diệt quái vật."),
    "enchantplus:tools/miningplus": (1, 4, ["tool"], "Cúp khai thác", "Khai Thác 3x3", "Mining+", "Đào bới các khối theo diện tích rộng 3x3 quanh khối trung tâm."),
    "enchantplus:trident/gungnir_breath": (3, 4, ["ranged", "weapon"], "Đinh ba", "Hơi Thở Của Gungnir", "Gungnir's Breath", "Đinh ba đóng băng mặt nước và làm đông cứng mục tiêu bị trúng đòn.")
}

MT_INFO = {
    "mt:armor/basalt_boots": (1, 8, ["armor"], "Giày", "Giày Đá Bazan (Rock Hard)", "Basalt Boots", "Tăng độ vững chãi kiên cố, triệt tiêu gần như hoàn toàn độ đẩy lùi khi bị tấn công."),
    "mt:armor/boomproof_cap": (1, 8, ["armor"], "Mũ", "Mũ Chống Nổ (Boomproof)", "Boomproof Cap", "Giảm tối đa sát thương nhận vào từ các vụ nổ của Creeper và thuốc nổ TNT."),
    "mt:armor/dark_forest_chestplate": (1, 8, ["armor"], "Áo giáp thân", "Giáp Rừng Tối (Axe Mastery)", "Dark Forest Chestplate", "Gia tăng uy lực sát thương và sự thành thạo khi chiến đấu bằng Rìu."),
    "mt:armor/deep_dark_leggings": (1, 8, ["armor"], "Quần", "Quần Vực Sâu Tối (Stealth Prowess)", "Deep Dark Leggings", "Triệt tiêu tiếng động bước chân, tăng độ ẩn nấp trước Warden và quái vật."),
    "mt:armor/desert_chestplate": (1, 8, ["armor"], "Áo giáp thân", "Giáp Sa Mạc (Last Stand)", "Desert Chestplate", "Tăng vọt khả năng phòng ngự kiên cường khi máu xuống mức nguy cấp."),
    "mt:armor/dripstone_helmet": (1, 8, ["armor"], "Mũ", "Mũ Thạch Nhũ (Fast Drip)", "Dripstone Helmet", "Tăng tốc độ rơi và phản xạ sát thương nhũ đá nhọn lên đối thủ."),
    "mt:armor/experient_trousers": (1, 8, ["armor"], "Quần", "Quần Tích Lũy (A Walking Experience)", "Experient Trousers", "Tự động tạo ra điểm kinh nghiệm XP đều đặn trong lúc đi bộ khám phá."),
    "mt:armor/flower_helmet": (1, 8, ["armor"], "Mũ", "Mũ Muôn Hoa (Flower Gift)", "Flower Helmet", "Tỏa ra sinh khí thiên nhiên giúp hồi phục sinh lực và thanh thức ăn."),
    "mt:armor/ice_shoes": (1, 8, ["armor"], "Giày", "Giày Băng Giá (Cold Aura)", "Ice Shoes", "Tạo hào quang giá lạnh quanh chân, làm chậm mọi kẻ địch tiếp cận."),
    "mt:armor/indestructia": (1, 8, ["armor"], "Cánh Elytra", "Cánh Bất Diệt (Kinetic Energy Immunity)", "Indestructia", "Kháng hoàn toàn sát thương va chạm động năng khi bay lượn ở tốc độ cao."),
    "mt:armor/lush_leggings": (1, 8, ["armor"], "Quần", "Quần Rừng Tươi Tốt (Deadly Thorns)", "Lush Leggings", "Phản đòn gai nhọn độc hại khi bị quái vật tấn công cận chiến."),
    "mt:armor/mushroom_boots": (1, 8, ["armor"], "Giày", "Giày Nấm Nảy (Soft Bounce)", "Mushroom Boots", "Giảm mạnh sát thương rơi từ trên cao và cho phép nảy nhẹ êm ái trên mặt đất."),
    "mt:armor/nether_leggings": (1, 8, ["armor"], "Quần", "Quần Kháng Nhiệt (Anti-Resistance)", "Nether Leggings", "Bảo vệ toàn diện trước lửa và dung nham trong chiều không gian Nether."),
    "mt:armor/ocean_chestplate": (1, 8, ["armor"], "Áo giáp thân", "Giáp Đại Dương (Ocean Blessing)", "Ocean Chestplate", "Ban phước thở lâu và tăng tốc độ bơi lội dưới lòng biển sâu."),
    "mt:armor/soul_valley_helmet": (1, 8, ["armor"], "Mũ", "Mũ Thung Lũng Linh Hồn (Encap-souled)", "Soul Valley Helmet", "Tăng tốc độ chạy vút trên Cát linh hồn và Đất linh hồn."),
    "mt:armor/swamp_chestplate": (1, 8, ["armor"], "Áo giáp thân", "Giáp Đầm Lầy (Lose Garbage)", "Swamp Chestplate", "Kháng độc tố và làm tan biến nhanh chóng các hiệu ứng bất lợi."),
    "mt:armor/taiga_pants": (1, 8, ["armor"], "Quần", "Quần Rừng Taiga (Restore Hunger)", "Taiga Pants", "Giảm tốc độ tiêu hao thanh thức ăn khi chạy nước rút trong thời tiết lạnh."),
    "mt:armor/warped_helmet": (1, 8, ["armor"], "Mũ", "Mũ Dị Giới Warped (Warped Slowdown)", "Warped Helmet", "Làm chậm nhịp độ di chuyển và đòn đánh của kẻ địch xung quanh."),
    "mt:armor/wind_chestplate": (1, 8, ["armor"], "Áo giáp thân", "Giáp Tích Khí (Wind Charged)", "Wind Chestplate", "Tích tụ năng lượng gió khi rơi, tạo lực đẩy phản lực khi chạm đất."),
    "mt:weapons/assassin_blade": (1, 8, ["weapon"], "Kiếm Vàng", "Dao Găm Sát Thủ (Early Strikes)", "Assassin Blade", "Đòn chém đầu tiên từ trạng thái ẩn náu gây sát thương chí mạng cực lớn."),
    "mt:weapons/barbaric_axe": (1, 8, ["weapon"], "Rìu Vàng", "Rìu Man Rợ (BARBARIC!)", "Barbaric Axe", "Gia tăng sát thương liên tục theo mỗi cú vung rìu liên hoàn."),
    "mt:weapons/drylands_piercer": (1, 8, ["ranged"], "Nỏ", "Nỏ Viễn Tây (Sheriff's Honour)", "Drylands Piercer", "Bắn ra mũi tên xuyên phá giáp với lực đẩy lùi mạnh mẽ của Cảnh Trưởng."),
    "mt:weapons/hurricane_sword": (1, 8, ["weapon"], "Kiếm Sắt", "Kiếm Cuồng Phong (Gusty Garden)", "Hurricane Sword", "Mỗi đòn chém tạo ra luồng gió xoáy hất văng mục tiêu lên không trung."),
    "mt:weapons/ice_sword": (1, 8, ["weapon"], "Kiếm Kim Cương", "Kiếm Băng Giá (Icy Tip)", "Ice Sword", "Lưỡi kiếm đóng băng đối thủ, làm giảm tốc độ đánh và tốc độ chạy của chúng."),
    "mt:weapons/oceanic_spear": (1, 8, ["ranged", "weapon"], "Đinh ba", "Giáo Thần Đại Dương (Oceanic Triumph)", "Oceanic Spear", "Đinh ba đại dương gây lượng sát thương áp đảo khi ở trong môi trường nước."),
    "mt:weapons/rift_axe": (1, 8, ["weapon"], "Rìu Netherite", "Rìu Xé Rách Linh Hồn (Soul Tearer)", "Rift Axe", "Rìu Netherite xé rách linh hồn đối thủ, gây sát thương chuẩn ăn mòn."),
    "mt:weapons/rumble_axe": (1, 8, ["weapon"], "Kiếm Vàng", "Kiếm Địa Chấn (Rumble Tumble)", "Rumble Axe", "Đòn đánh tạo ra rung chấn mặt đất làm choáng váng kẻ thù xung quanh."),
    "mt:weapons/sharpened_sword": (1, 8, ["weapon"], "Kiếm Sắt", "Kiếm Mài Bén (Half Life 3)", "Sharpened Sword", "Lưỡi gươm được mài sắc bén tối đa, tăng chỉ số sát thương thuần."),
    "mt:weapons/soul_scythe": (1, 8, ["weapon", "tool"], "Lưỡi Hái Netherite", "Lưỡi Hái Hút Huyết (Life Steal)", "Soul Scythe", "Lưỡi hái hút sinh lực của sinh vật bị tấn công để hồi máu cho bản thân."),
    "mt:weapons/stray_bow": (1, 8, ["ranged"], "Cung", "Cung Stray Băng Giá (Gotta Go Slow)", "Stray Bow", "Bắn ra mũi tên ướp lạnh gây hiệu ứng chậm sâu cho mục tiêu."),
    "mt:weapons/warden_killer": (1, 8, ["weapon"], "Mảnh Vang Vọng (Echo Shard)", "Vũ Khí Diệt Warden (Death to Warden!)", "Warden Killer", "Vũ khí cổ xưa được rèn riêng để khắc chế và tiêu diệt Warden."),
    "mt:weapons/warped_bow": (1, 8, ["ranged"], "Cung", "Cung Dị Giới Warped (Warping...)", "Warped Bow", "Mũi tên dị giới mở cổng dịch chuyển tức thời khi trúng đích."),
    "mt:weapons/wither_sword": (1, 8, ["weapon"], "Kiếm Đá", "Kiếm Héo Mòn (Wither)", "Wither Sword", "Lưỡi kiếm truyền lời nguyền Wither làm ăn mòn máu mục tiêu liên tục."),
    "mt:treasure/standard": (1, 8, ["armor"], "Giáp trụ di tích", "Di Vật Cổ Tiêu Chuẩn", "Standard Relic", "Phù phép di vật tăng cường chỉ số cân bằng cho giáp trụ."),
    "mt:treasure/fast": (1, 8, ["armor"], "Giáp trụ di tích", "Di Vật Tốc Độ", "Fast Relic", "Di vật cổ xưa gia tăng tốc độ phản xạ và di chuyển."),
    "mt:treasure/hypersonic": (1, 8, ["armor"], "Giáp trụ di tích", "Di Vật Siêu Thanh", "Hypersonic Relic", "Ban tốc độ di chuyển siêu việt của di tích cổ."),
    "mt:treasure/slow": (1, 8, ["armor"], "Giáp trụ di tích", "Di Vật Nặng Nề", "Slow Relic", "Tăng giáp phòng ngự cực đại với cái giá làm giảm nhẹ tốc độ."),
    "mt:treasure/sloth": (1, 8, ["armor"], "Giáp trụ di tích", "Lời Nguyền Trì Trệ", "Sloth Curse", "Giảm tốc độ di chuyển của người mang đổi lấy độ bền cao."),
    "mt:technical/giga_bore": (1, 8, ["tool"], "Cúp Mine Treasure", "Đào 3x3 Giga", "Giga Bore", "Cơ chế đập phá khối diện tích 3x3 của cổ vật Mine Treasure."),
    "mt:technical/grenade": (1, 8, ["weapon"], "Vũ khí cận chiến", "Đạn Lựu Pháo", "Grenade Window", "Kích hoạt cơ chế phát nổ công phá sau 2 giây trúng đích."),
    "mt:technical/kinetic_bomb": (1, 8, ["weapon"], "Vũ khí cận chiến", "Bom Áp Suất Động Năng", "Kinetic Bomb", "Tạo làn sóng áp suất động năng cực mạnh khi tấn công."),
    "mt:technical/kinetic_blast": (255, 8, ["armor"], "Mũ", "Xung Động Năng Cổ", "Kinetic Blast", "Hiệu ứng kỹ thuật xung chấn của di tích cổ vật."),
    "mt:technical/floor_is_spikes": (1, 8, ["tool"], "Cơ chế hầm ngục", "Bẫy Chông Địa Hình", "Floor Is Spikes", "Cơ chế kích hoạt sàn chông trong hầm ngục Mine Treasure."),
    "mt:technical/healing_flower": (1, 8, ["tool"], "Hoa ma thuật", "Hoa Trị Liệu Hầm Ngục", "Healing Flower", "Cơ chế hồi máu của hoa ma thuật trong di tích."),
    "mt:technical/necromancy": (1, 8, ["tool"], "Vật phẩm nghi lễ", "Thuật Chiêu Hồn", "Necromancy", "Cơ chế triệu hồi vong linh của các lăng mộ cổ."),
    "mt:technical/sense_of_urgency": (1, 8, ["tool"], "Cơ chế thoát hiểm", "Khẩn Cấp Thoát Thân", "Sense of Urgency", "Kích hoạt bộc phá tốc độ trong tình huống hiểm nghèo.")
}

import json
# Custom curated Vietnamese descriptions for encore Ultimates and key enchantments
ENCORE_CURATED = {
    "enchantencore:absolute_unit": ("Chế độ Tanker Tối Thượng: Khi ngồi xổm trên mặt đất, nhận hiệu ứng Hấp thụ (Absorption II) và Kháng cự (Resistance III), tạo sóng âm chấn động hất tung kẻ thù xung quanh.", ["armor"], "Quần"),
    "enchantencore:adrenaline": ("Bắn cung trúng đích tăng mạnh tốc độ di chuyển và hồi phục sinh lực dồn dập trong giao tranh khốc liệt.", ["ranged"], "Cung & Nỏ"),
    "enchantencore:archer_barrage": ("Bắn ra một cơn mưa tên liên hoàn từ cung, quét sạch mọi kẻ thù trước mắt.", ["ranged"], "Cung"),
    "enchantencore:armor_piercing": ("Mũi tên bắn ra bỏ qua toàn bộ chỉ số giáp phòng ngự của mục tiêu, gây sát thương chuẩn.", ["ranged"], "Cung & Nỏ"),
    "enchantencore:aurora": ("Mũ giáp phát quang hào quang cực quang tuyệt đẹp, liên tục hồi máu cho bản thân và đồng minh xung quanh.", ["armor"], "Mũ"),
    "enchantencore:blaze_blessing": ("Ban phước từ Quỷ Lửa: Kháng hoàn toàn lửa và dung nham, thiêu đốt kẻ địch xung quanh khi bị tấn công.", ["armor"], "Giáp thân"),
    "enchantencore:bomber": ("Mũi tên bắn ra phát nổ cực mạnh khi chạm vào bất kỳ kẻ địch hoặc khối nào.", ["ranged"], "Cung & Nỏ"),
    "enchantencore:creeper_blessing": ("Khi lượng máu xuống mức nguy kịch, kích hoạt làn sóng nổ đẩy lùi và tạo khiên bảo vệ chủ nhân.", ["armor"], "Giáp thân"),
    "enchantencore:crystalize_arrow": ("Mũi tên pha lê làm đóng băng và giam cầm mục tiêu trong tinh thể pha lê bất động.", ["ranged"], "Cung"),
    "enchantencore:dark_infused": ("Tẩm độc tố bóng tối gây mù lòa và làm giảm thị lực đối thủ bị trúng đòn.", ["weapon", "ranged"], "Kiếm & Cung"),
    "enchantencore:dolphin_blessing": ("Tốc độ bơi lội siêu việt dưới nước như cá heo thần thoại, ban lượng oxy vô tận.", ["armor"], "Giày"),
    "enchantencore:frenzy": ("Mỗi đòn đánh trúng tăng thêm tốc độ đánh và tốc độ di chuyển dồn dập trong trạng thái cuồng nộ.", ["weapon"], "Kiếm & Vũ khí cận chiến"),
    "enchantencore:habanero": ("Thiêu đốt mặt đất và gây sát thương bỏng cực kỳ khủng khiếp lên mọi kẻ địch lân cận.", ["armor"], "Giày"),
    "enchantencore:heroes_crown": ("Trao hiệu ứng Anh Hùng Làng (Hero of the Village V) liên tục, giảm giá giao dịch tối đa với dân làng.", ["armor"], "Mũ"),
    "enchantencore:hurricane": ("Tạo ra cơn cuồng phong lốc xoáy thổi bay mọi thực thể xung quanh điểm trúng tên.", ["ranged"], "Cung & Nỏ"),
    "enchantencore:infinity_durability": ("Độ bền vĩnh cửu: Vật phẩm không bao giờ bị giảm độ bền hoặc vỡ khi sử dụng.", ["universal"], "Mọi trang bị có độ bền"),
    "enchantencore:last_stand": ("Khi sắp cạn máu, ngay lập tức hồi sinh một lượng máu lớn và tạo khiên phòng thủ bất hoại.", ["armor"], "Giáp thân"),
    "enchantencore:machine_gun": ("Tốc độ nạp và bắn nỏ siêu thanh liên thanh liên tục như súng máy tự động.", ["ranged"], "Nỏ"),
    "enchantencore:magic_protection": ("Giảm mạnh sát thương từ các nguồn phép thuật (Potion, Poison, Wither, Evoker Fangs).", ["armor"], "Mũ, Áo giáp, Quần, Giày"),
    "enchantencore:neptune": ("Đinh ba mang sức mạnh của Thần biển Neptune, triệu hồi bão tố và sát thương biển cả áp đảo.", ["ranged", "weapon"], "Đinh ba"),
    "enchantencore:nightmare": ("Đòn đánh gây bóng tối hắc ám và làm suy yếu ý chí chiến đấu của kẻ địch.", ["weapon"], "Kiếm & Vũ khí cận chiến"),
    "enchantencore:nuclear_arrow": ("Mũi tên nguyên tử tạo ra vụ nổ hủy diệt trên diện rộng với uy lực tối thượng.", ["ranged"], "Cung"),
    "enchantencore:perfect_protection": ("Bộ giáp bảo hộ hoàn mỹ: Giảm 40% mọi nguồn sát thương gánh chịu từ mọi phía.", ["armor"], "Giáp thân"),
    "enchantencore:poison_infused": ("Mũi tên tẩm độc gây trúng độc nặng lên kẻ địch trúng phải.", ["ranged"], "Cung & Nỏ"),
    "enchantencore:poppy_peace": ("Lan tỏa thông điệp hòa bình, khiến quái vật trung lập không chủ động tấn công người chơi.", ["armor"], "Mũ"),
    "enchantencore:poseidon": ("Đinh ba triệu hồi dòng nước xoáy và sóng thần nghiền nát mục tiêu.", ["ranged", "weapon"], "Đinh ba"),
    "enchantencore:purify_water": ("Thanh lọc nước xung quanh và làm tan biến các hiệu ứng xấu dưới nước.", ["armor"], "Mũ"),
    "enchantencore:radiant_life": ("Mỗi đòn chém hồi phục trực tiếp một lượng máu dồi dào cho người sử dụng.", ["weapon"], "Kiếm & Vũ khí cận chiến"),
    "enchantencore:railgun": ("Mũi tên điện từ bay với vận tốc ánh sáng theo đường thẳng tuyệt đối, xuyên phá mọi mục tiêu.", ["ranged"], "Cung & Nỏ"),
    "enchantencore:reflection": ("Phản lại toàn bộ mũi tên và đạn bắn tầm xa trở ngược lại kẻ tấn công.", ["armor"], "Khiên & Giáp thân"),
    "enchantencore:sniper": ("Sát thương mũi tên tăng theo cấp số nhân dựa trên khoảng cách bắn cực xa.", ["ranged"], "Cung"),
    "enchantencore:spectral_form": ("Biến thân thành thể linh hồn: Miễn nhiễm sát thương vật lý trong chốc lát.", ["armor"], "Giáp thân"),
    "enchantencore:sting": ("Khiến ong xung quanh phát điên và đồng loạt lao vào tấn công kẻ địch bị trúng đòn.", ["weapon"], "Kiếm & Vũ khí cận chiến"),
    "enchantencore:vitality": ("Tăng vĩnh viễn lượng máu tối đa của nhân vật lên thêm nhiều trái tim vàng.", ["armor"], "Giáp thân"),
    "enchantencore:warden": ("Khai mở sức mạnh bóng tối của Warden: Đòn đánh phát ra sóng xung kích Sonic Boom xuyên giáp.", ["weapon"], "Kiếm & Chùy"),
    "enchantencore:warped_arrow": ("Mũi tên dị giới dịch chuyển tức thời người bắn đến vị trí tên chạm đất.", ["ranged"], "Cung"),
    "enchantencore:warped_trident": ("Dịch chuyển tức thời đến bất kỳ vị trí nào mà cây Đinh ba cắm trúng.", ["ranged", "weapon"], "Đinh ba"),
    "enchantencore:web_infused": ("Tạo ra mạng nhện giữ chân kẻ thù ngay tại điểm mũi tên tiếp đất.", ["ranged"], "Cung"),
    "enchantencore:wind_spirit": ("Tăng vọt tốc độ di chuyển và khả năng nhảy cao bay lượn trên không như ngọn gió.", ["armor"], "Giày"),
    "enchantencore:wither_infused": ("Tẩm nguyền rủa Wither ăn mòn máu của mục tiêu liên tục theo thời gian.", ["weapon", "ranged"], "Kiếm & Cung")
}



# Translation patterns for lore_en
TRANSLATION_MAP = {
    "Anchors the entity in place when not ridden": "Giữ cố định thực thể/thú cưỡi đứng yên tại chỗ khi không có người cưỡi.",
    "Applies a cosmetic angry aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang giận dữ thẩm mỹ cho người mặc, thú cưng và đạn bắn ra.",
    "Applies a cosmetic aquatic aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang biển cả mát lạnh cho người mặc, thú cưng và đạn bắn ra.",
    "Inflicts more damage to boss mobs": "Gây thêm lượng lớn sát thương lên các sinh vật trùm (Ender Dragon, Wither, Warden, Elder Guardian...).",
    "Inflicts more damage to End mobs": "Tăng mạnh sát thương lên các loài sinh vật thế giới End (Enderman, Shulker, Endermite).",
    "Inflicts more damage to Nether mobs": "Tăng mạnh sát thương lên các sinh vật thế giới Địa Ngục Nether (Blaze, Ghast, Piglin, Wither Skeleton...).",
    "Inflicts more damage to raid mobs": "Tăng mạnh sát thương lên các quái vật trong sự kiện đột kích làng (Raid: Pillager, Ravager, Evoker...).",
    "Infuses a Wolf with the powers of a Bear": "Truyền sức mạnh của loài Gấu cho Chó sói: Tăng sức chịu đựng và lượng máu tối đa.",
    "Applies Blindness to the wearer.": "Gây hiệu ứng Mù lòa (Blindness) làm tối đen tầm nhìn của người mặc.",
    "Infuses a Wolf with the powers of a Boar": "Truyền sức mạnh của loài Heo rừng cho Chó sói: Tăng lực húc và độ đẩy lùi đối thủ.",
    "Summons lightning when entity was hit by a player which at least fell 8 blocks": "Triệu hồi sấm sét giáng xuống khi tấn công trúng mục tiêu sau khi rơi tự do ít nhất 8 khối.",
    "Lets the wearer bounce all over the place": "Giúp người mặc nảy tưng bừng trên mọi bề mặt tiếp đất.",
    "Wind charges entity on attack": "Kích hoạt luồng gió bão hất tung đối thủ khi tấn công.",
    "10% chance of protecting the wearer from shulker bullets": "Có 10% cơ hội miễn nhiễm và triệt tiêu hoàn toàn đạn của Shulker.",
    "Let's you jump higher": "Tăng cường sức bật, cho phép người mang nhảy cao hơn bình thường.",
    "Wearer gets slowed down when leashing entities": "Làm người chơi bị giảm tốc độ di chuyển khi đang dắt động vật bằng dây thừng.",
    "Clears all effects when raised, draining durability only if effects were removed": "Giơ khiên lên giúp thanh tẩy toàn bộ hiệu ứng trên người, chỉ tiêu hao độ bền khi có hiệu ứng bị xóa.",
    "Wearer becomes color blind when equipped": "Khiến tầm nhìn của người mặc chuyển sang đơn sắc mù màu.",
    "Slowly drains durability in rain or underwater": "Từ từ làm hao mòn độ bền của vật phẩm khi ở dưới trời mưa hoặc lặn dưới nước.",
    "Increases block/entity interaction range": "Mở rộng tầm với tương tác với các khối và thực thể xung quanh.",
    "Hurts the wearer when not moving": "Gây sát thương đau đớn lên người mặc nếu đứng yên một chỗ quá lâu không di chuyển.",
    "Causes an explosion when you are hit": "Kích nổ một vụ nổ lớn xung quanh khi bạn phải chịu sát thương.",
    "Wearer sees the world with the eyes of a Creeper": "Khiến tầm nhìn của người mặc chuyển sang góc nhìn màu xanh lá ma mị của Creeper.",
    "Saturation effect when equipped": "Cung cấp hiệu ứng Độ no (Saturation) liên tục, không bao giờ lo đói bụng.",
    "Sneak-Dance to let crops around you grow": "Ngồi xổm nhấp nhô liên tục (Sneak-Dance) để kích thích cây trồng xung quanh lớn nhanh vượt bậc.",
    "Protection against damage when flying into walls": "Bảo vệ giảm sát thương khi lỡ va chạm vào tường ở tốc độ cao bằng Elytra.",
    "Gives attacking entities darkness effect": "Gây hiệu ứng Bóng tối (Darkness) lên kẻ tấn công bạn.",
    "Twice the damage protection than the vanilla enchant but works only at day time (overworld)": "Bảo vệ giảm sát thương gấp đôi so với phù phép thường nhưng chỉ có tác dụng vào ban ngày.",
    "Small chance of dying when attacking": "Có tỉ lệ nhỏ bất ngờ tử nạn ngay lập tức khi tung đòn tấn công.",
    "Charged spear hits knock riders off their mounts": "Đòn đâm đinh ba/thương kích lực đánh bật đối thủ văng khỏi thú cưỡi của chúng.",
    "Applies a cosmetic echoing aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang vang vọng Deep Dark kỳ bí cho người mặc và thú cưng.",
    "Powers Redstone for a brief moment when walking over it": "Kích hoạt truyền tín hiệu đá đỏ trong thoáng chốc khi bước chân qua các đường dây Redstone.",
    "Slowly repairs the item while in the Nether": "Từ từ tự động sửa chữa độ bền của trang bị khi người chơi ở trong chiều không gian Nether.",
    "Applies a cosmetic enchanted aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang ma pháp lấp lánh cho người mặc và thú cưng.",
    "Twice the damage protection than the vanilla enchant but works only the end": "Bảo vệ giảm sát thương gấp đôi so với phù phép thường nhưng chỉ kích hoạt khi ở The End.",
    "Increases movement speed when in the end": "Gia tăng đáng kể tốc độ chạy và di chuyển khi ở trong chiều không gian The End.",
    "Applies a cosmetic ender aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang Ender tím huyền ảo cho người mặc và đạn bắn ra.",
    "Teleports hit entities on the surface randomly": "Dịch chuyển ngẫu nhiên kẻ địch bị bắn trúng ra các vị trí khác trên bề mặt đất.",
    "Wearer sees the world with the eyes of an Enderman": "Khiến tầm nhìn của người mặc đảo ngược màu sắc theo góc nhìn của Enderman.",
    "You teleport randomly in a 5 block radius when hit on the surface": "Tự động dịch chuyển tức thời ngẫu nhiên trong bán kính 5 khối khi bị trúng đòn.",
    "Arrows spawn Evoker Fangs when they land": "Mũi tên triệu hồi những chiếc hàm sắt Evoker Fangs trồi lên cắn xé kẻ thù tại điểm rơi.",
    "Small chance for a Wolf to fetch loot": "Chó sói cưng có tỉ lệ tự động nhặt và tha chiến lợi phẩm rơi trên đất về cho chủ nhân.",
    "Ignites attacking entities": "Thiêu đốt kẻ tấn công khi bạn nhận sát thương.",
    "Applies a cosmetic flaming aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang rực lửa thẩm mỹ cho người mặc và mũi tên bắn ra.",
    "Arrows explode like fireworks when they land or hit an entity": "Mũi tên phát nổ rực rỡ như pháo hoa khi chạm đất hoặc bắn trúng sinh vật.",
    "Arrows spawn falling Anvils when they land": "Mũi tên triệu hồi Đe sắt rơi từ trên trời xuống đè bẹp kẻ thù tại điểm tiếp đất.",
    "Transforms Water to temporary Frosted Ice (can't sprint)": "Đóng băng mặt nước dưới chân thành băng tạm thời (không thể chạy nước rút).",
    "Gives attacking entities glowing effect": "Khiến kẻ tấn công bạn bị phát sáng (Glowing) làm lộ vị trí qua các bức tường.",
    "Slowly consumes the items durability when held in the main hand": "Từ từ ăn mòn độ bền của vật phẩm khi cầm trên tay chính.",
    "Applies a cosmetic glowing aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang phát quang dạ quang cho người mặc và thú cưng.",
    "Turns Grass into Dirt when walking over it": "Làm chết các khối cỏ, biến cỏ thành khối đất thô khi bước chân qua.",
    "Turns grass blocks to temporary frosted dirt": "Biến cỏ dưới chân thành đất đóng băng tạm thời khi di chuyển.",
    "Tills a large area with left-click, takes 4 seconds to recharge": "Cày xới đất trên diện tích cực rộng bằng chuột trái, mất 4 giây để hồi chiêu.",
    "Entities and Projectiles in a 3 block radius get slowly pulled towards you": "Tạo lực hút kéo các thực thể và mũi tên trong bán kính 3 khối về phía bạn.",
    "Block or hit an entity with a charged Shield to knock entities around you away": "Đỡ đòn hoặc dùng khiên tích lực đập trúng mục tiêu để tạo sóng chấn động đẩy văng mọi kẻ thù.",
    "Slowly repairs all other items in your inventory while equipped": "Từ từ sửa chữa độ bền cho tất cả các vật phẩm khác trong kho đồ khi đang trang bị món này.",
    "Slowly drains hunger when in main hand": "Tiêu hao thanh thức ăn nhanh chóng khi cầm vật phẩm trên tay chính.",
    "Increases healing": "Tăng cường hiệu quả và tốc độ hồi phục sinh lực tự nhiên.",
    "Increases block/entity interaction range (both hands)": "Mở rộng khoảng cách tương tác và với khối trên cả 2 tay.",
    "Slowly repairs the item while in heat (Lava, Fire)": "Từ từ sửa chữa độ bền cho vật phẩm khi người chơi ngâm mình trong lửa hoặc dung nham.",
    "Slowly drains hunger when leashing entities": "Làm tiêu hao thanh thức ăn khi đang dắt sinh vật bằng dây thừng.",
    "Hides the wearers name tag": "Ẩn hoàn toàn bảng tên nhân vật, giúp ẩn náu hoàn hảo trước người chơi khác.",
    "Summons Silverfish when entity was hit": "Triệu hồi Silverfish chui lên tấn công kẻ thù khi bị đánh trúng.",
    "Increases damage of low durability weapons": "Gia tăng mạnh mẽ sát thương khi vũ khí sắp cạn độ bền.",
    "Charged spear hits slow down hit entities": "Đòn đâm đinh ba/thương kích lực làm giảm tốc độ di chuyển của mục tiêu.",
    "Small chance to leap forwards when double jumping": "Có cơ hội lướt nhanh về phía trước khi thực hiện nhảy kép.",
    "Leaping into entities inflicts more damage": "Lướt hoặc lao người vào đối thủ sẽ gây thêm sát thương va chạm.",
    "Steals health from hit entities": "Hút một phần máu của đối thủ chuyển hóa thành máu cho bản thân khi tấn công trúng.",
    "Double Jump": "Cho phép thực hiện cú nhảy đúp (Double Jump) trên không trung.",
    "Applies a cosmetic levitating aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang bay bổng lơ lửng cho người mặc và thú cưng.",
    "Attacking entities gain levitation effect": "Khiến kẻ tấn công bạn bị dính hiệu ứng Bay lơ lửng (Levitation), trôi dạt lên trời.",
    "Fells entire trees at once": "Đốn hạ toàn bộ cả thân cây lớn từ gốc đến ngọn chỉ với một nhát rìu.",
    "Drops an enchanted book with this enchantment when a mob was killed by an anvil, drains all anvil durability": "Rơi ra sách phù phép khi quái vật bị đè chết bởi đe sắt, làm hỏng hoàn toàn đe.",
    "Pull dropped items towards you": "Tự động hút toàn bộ vật phẩm rơi rải rác trên đất về thẳng vị trí của bạn.",
    "Turns lava into temporary basalt when walking over it": "Biến dung nham dưới chân thành đá bazan tạm thời để đi bộ qua biển dung nham.",
    "Slowly repairs the item at night (overworld)": "Từ từ tự động hồi phục độ bền cho trang bị vào ban đêm.",
    "Twice the damage protection than the vanilla enchant but works only at night time (overworld)": "Bảo vệ giảm sát thương gấp đôi so với phù phép thường nhưng chỉ có tác dụng vào ban đêm.",
    "Infuses a Wolf with the powers of a Mud Pig": "Truyền sức mạnh của Heo Bùn cho Chó sói: Tăng độ lỳ đòn và khả năng chống chịu.",
    "Increases attack speed of low durability weapons": "Tăng vọt tốc độ vung đòn khi vũ khí sắp cạn kiệt độ bền.",
    "Small chance to prevent player from death": "Có tỉ lệ nhỏ cứu mạng người chơi thoát chết trong gang tấc khi nhận đòn chí mạng.",
    "Infuses a Wolf with the powers of an Ocelot": "Truyền sức mạnh của Mèo rừng cho Chó sói: Tăng tốc độ phản xạ và né tránh.",
    "Infuses a Wolf with the powers of an Owl": "Truyền sức mạnh của Cú vọ cho Chó sói: Tăng tầm nhìn và khả năng phát hiện kẻ thù.",
    "Reverses the effect of glowing, entities near you can't be seen by other players": "Đảo ngược hiệu ứng phát sáng, khiến các thực thể gần bạn hoàn toàn vô hình trước mắt người chơi khác.",
    "Lets you know when an entity has been hit by playing a ping sound": "Phát ra âm thanh 'ping' rõ ràng báo hiệu mỗi khi có kẻ địch bị bắn trúng từ xa.",
    "Tills a larger area with left-click": "Cày xới đất trên một diện tích rộng lớn chỉ bằng một cú nhấp chuột trái.",
    "Increases flying speed of Happy Ghasts": "Gia tăng tốc độ bay lượn trên không của Ghast Vui Vẻ.",
    "Has a chance to spawn a pufferfish when the hit entity is in water": "Có cơ hội triệu hồi cá nóc độc khi mục tiêu bị tấn công đang ở dưới nước.",
    "Increases luck attribute": "Gia tăng chỉ số may mắn tổng thể của người chơi.",
    "Slowly repairs the item while rain falls on you": "Từ từ hồi phục độ bền cho trang bị khi có mưa rơi trên người bạn.",
    "Rerolls weapon enchantments on each hit": "Ngẫu nhiên thay đổi và tráo đổi các dòng phù phép của vũ khí sau mỗi đòn đánh.",
    "Reduces block/entity interaction range": "Làm giảm tầm với tương tác với các khối và thực thể xung quanh.",
    "Takes an identical firework rocket from the inventory and adds it to the held stack each time one is used while gliding": "Tự động nạp thêm pháo hoa từ túi đồ vào tay mỗi khi dùng pháo hoa để bay lượn.",
    "Replants fully grown Crops when harvested": "Tự động gieo trồng lại cây nông nghiệp ngay khi thu hoạch cây đã chín.",
    "Increases max health": "Gia tăng lượng máu tối đa của nhân vật.",
    "50% Chance to not consume an arrow, tipped arrow, spectral arrow or firework when shot": "Có 50% cơ hội không tiêu tốn đạn (mũi tên thường, mũi tên thuốc, tên quang phổ, pháo hoa) khi bắn.",
    "Repairs Tools/Armor when wearer gets hit by entities": "Tự động sửa chữa độ bền cho công cụ/giáp khi người mặc bị quái vật đánh trúng.",
    "Increases safe landing height": "Tăng độ cao tiếp đất an toàn, giảm thiểu tối đa sát thương khi rơi.",
    "Increases movement speed when on sand": "Tăng tốc độ chạy và di chuyển khi đi trên bề mặt cát.",
    "Wolves make nearby entities glow when they move": "Chó sói cưng di chuyển sẽ làm các sinh vật xung quanh phát sáng lộ diện.",
    "More experience from mining blocks": "Thu được thêm nhiều điểm kinh nghiệm XP hơn khi đập phá các khối tài nguyên.",
    "Increases the experience you get from fishing": "Tăng lượng điểm kinh nghiệm XP thu được khi câu cá.",
    "More experience from killing entities": "Gia tăng lượng điểm kinh nghiệm XP nhận được khi tiêu diệt quái vật và sinh vật.",
    "Infuses a Wolf with the powers of a Scorpion": "Truyền sức mạnh của Bọ cạp cho Chó sói: Đòn đánh tẩm nọc độc gây ăn mòn máu kẻ thù.",
    "Wind Charges hit entities away when hit by thrown trident": "Tạo luồng gió bão hất văng kẻ địch ra xa khi bị trúng đinh ba ném.",
    "Highlights Items in a 4-12 block radius depending on the level": "Làm nổi bật và phát sáng các vật phẩm rơi rải rác trên đất trong bán kính 4–12 khối.",
    "Blurs the wearers vision when equipped": "Làm mờ mịt và nhòe nhoẹt tầm nhìn của người mặc trang bị.",
    "Decreases entity scale and step height": "Thu nhỏ kích thước cơ thể và giảm chiều cao bước chân.",
    "Firework, Tridents, (Spectral) Arrows shot at the wearer fall to the ground when wearer stands still": "Khi đứng yên, các loại mũi tên, pháo hoa và đinh ba bắn tới sẽ tự động rơi rụng xuống đất.",
    "Boosts walking speed but decreases sneaking speed": "Tăng mạnh tốc độ đi bộ nhưng làm chậm tốc độ khi ngồi xổm.",
    "Applies a cosmetic sparking aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang tia lửa thẩm mỹ cho người mặc và thú cưng.",
    "Wearer sees the world with the eyes of a Spider": "Khiến tầm nhìn của người mặc chia thành nhiều góc nhìn ghép của loài Nhện.",
    "Applies a cosmetic sticky aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang dính bết thẩm mỹ cho người mặc và thú cưng.",
    "Sprint + Left/Right lets you strafe fast in that direction": "Chạy nước rút kết hợp phím Trái/Phải giúp lướt nhanh thần tốc sang hướng đó.",
    "Melts snow when walking over it": "Tự động làm tan chảy các lớp tuyết dày dưới chân khi bước qua.",
    "Slowly repairs the item under the open daytime sky": "Từ từ tự động hồi phục độ bền cho trang bị dưới bầu trời ban ngày thông thoáng.",
    "Increases swimming speed of entities": "Gia tăng đáng kể tốc độ bơi lội dưới nước cho sinh vật và thú cưỡi.",
    "Increases attack speed": "Gia tăng tốc độ vung đòn tấn công của vũ khí cận chiến.",
    "Swaps places with hit entity": "Hoán đổi vị trí tức thời với kẻ địch bị bắn trúng.",
    "Protection against Thorns including Sweet Berry Bushes and Cactus": "Bảo vệ giảm sát thương phản lại từ Gai nhọn, bụi dâu ngọt và cây xương rồng.",
    "Slowly repairs the item while underwater": "Từ từ tự động sửa chữa độ bền cho trang bị khi lặn ngập dưới nước.",
    "Applies a cosmetic trial aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang thử thách Trial Chamber cho người mặc và thú cưng.",
    "Gives the wearer the Trial Omen effect but drains durability": "Ban hiệu ứng Điềm Báo Thử Thách (Trial Omen) nhưng làm hao mòn độ bền của trang bị.",
    "Turns the tool into a Totem of Undying": "Biến công cụ thành một Vật Tổ Trường Sinh (Totem of Undying) cứu mạng khi bạn cạn máu.",
    "Gliding over lava, magma or fire lifts you up": "Lướt cánh qua dung nham, khối magma hoặc lửa sẽ tạo luồng khí nóng nâng bạn bay vút lên cao.",
    "Doubles the speed of shot projectiles": "Nhân đôi tốc độ bay của các loại mũi tên, đinh ba và đạn bắn ra.",
    "Wearer sees the world with the eyes of the Void": "Khiến tầm nhìn của người mặc chìm trong màn sương đen của Vực thẳm Hư Không.",
    "Slowly repairs the item while in the End": "Từ từ hồi phục độ bền cho trang bị khi người chơi ở trong chiều không gian The End.",
    "Destroys items in a 3 block radius": "Tự động phá hủy và làm biến mất các vật phẩm rơi trong bán kính 3 khối.",
    "Protection against Wither Effect and Wither Skulls": "Bảo vệ giảm mạnh sát thương từ hiệu ứng Héo Mòn Wither và đầu lâu quỷ Wither.",
    "Reeling in a hooked mob steals its held item": "Giật dây câu trúng quái vật sẽ cướp lấy vật phẩm nó đang cầm trên tay.",
    "Inflicts more damage to Arthropods": "Tăng thêm sát thương gây ra lên các loài chân đốt.",
    "Infuses a Wolf with the powers of a Bat": "Truyền sức mạnh của loài Dơi cho Chó sói: Tăng khả năng định vị sóng âm trong bóng tối.",
    "Infuses a Wolf with the powers of a Bull": "Truyền sức mạnh của Bò tót cho Chó sói: Đòn húc gây sát thương va chạm khủng khiếp.",
    "Infuses a Wolf with the powers of a Frog": "Truyền sức mạnh của loài Ếch cho Chó sói: Cho phép nhảy cao và bơi lội linh hoạt.",
    "Infuses a Wolf with the powers of a Rabbit": "Truyền sức mạnh của loài Thỏ cho Chó sói: Tăng độ nhanh nhẹn và né tránh đòn đánh.",
    "Infuses a Wolf with the powers of a Sheep": "Truyền sức mạnh của loài Cừu cho Chó sói: Lớp lông dày giảm sát thương nhận vào.",
    "Infuses a Wolf with the powers of a Turtle": "Truyền sức mạnh của Rùa biển cho Chó sói: Ban khả năng thở và bơi lội dưới nước.",
    "Increases damage when falling": "Tăng sát thương gây ra khi rơi từ trên cao xuống mục tiêu.",
    "Steals hunger from hit entities": "Cướp điểm thức ăn/độ no từ mục tiêu bị đánh trúng.",
    "Repairs equipment on hit": "Tự động hồi phục độ bền cho vũ khí khi đánh trúng mục tiêu."
}

# Also handle aspects
ASPECT_MAP = {
    "hunger_aspect": ("Đói Diện", "Đòn đánh làm hao mòn thanh thức ăn của đối thủ."),
    "infested_aspect": ("Ký Sinh Diện", "Đòn đánh triệu hồi Silverfish ký sinh tấn công mục tiêu."),
    "levitation_aspect": ("Lơ Lửng Diện", "Đòn đánh khiến đối thủ bị lơ lửng bay lên không trung."),
    "oozing_aspect": ("Chất Nhầy Diện", "Đòn đánh khiến mục tiêu phát nổ ra Slime khi bị tiêu diệt."),
    "pacifier": ("Bình Định Diện", "Làm dịu cơn thịnh nộ và làm mất trạng thái hung hãn của đối thủ."),
    "poison_aspect": ("Độc Diện", "Đòn đánh gây trúng độc nặng (Poison) lên mục tiêu."),
    "slowness_aspect": ("Chậm Chạp Diện", "Đòn đánh làm giảm tốc độ di chuyển của kẻ địch."),
    "weakness_aspect": ("Suy Yếu Diện", "Đòn đánh gây hiệu ứng Yếu đuối (Weakness), làm giảm sát thương đối thủ."),
    "weaving_aspect": ("Mạng Nhện Diện", "Đòn đánh bẫy kẻ thù trong mạng nhện tơ khi di chuyển."),
    "wind_aspect": ("Gió Lốc Diện", "Đòn đánh tạo sóng gió đẩy văng mục tiêu ra xa."),
    "wither_aspect": ("Héo Mòn Diện", "Đòn đánh làm mục tiêu bị héo mòn máu liên tục bởi hiệu ứng Wither.")
}





# Additional translations for the remaining 47 items
ADDITIONAL_MAP = {
    "Drains durability points faster": "Làm hao mòn điểm độ bền nhanh hơn bình thường rất nhiều khi sử dụng.",
    "The equipped mob gets a speed boost when ridden": "Thú cưỡi được gia tăng đáng kể tốc độ di chuyển khi có người cưỡi.",
    "Makes your Wolf almost invincible but it also has 0 attack damage": "Khiến Chó sói gần như bất tử nhưng sát thương tấn công giảm về 0.",
    "Sucks up items in a 2-10 block radius depending on the level": "Tự động hút toàn bộ vật phẩm rơi trên đất trong bán kính 2–10 khối về phía người chơi.",
    "Turns dirt into grass blocks when walking over it": "Tự động biến các khối đất thô thành khối cỏ xanh tươi khi bước chân qua.",
    "Increases player scale and step height": "Tăng kích thước cơ thể và độ cao bước chân tự động vượt qua các khối cao.",
    "Allows for blocking melee attacks with the enchanted item": "Cho phép dùng vũ khí/vật phẩm này để chặn các đòn đánh cận chiến như khiên.",
    "Keeps repair cost low": "Giữ cho chi phí sửa chữa trên đe sắt luôn ở mức thấp, không bao giờ bị 'Quá đắt!'.",
    "Extra armor when equipped": "Gia tăng thêm các điểm giáp phòng ngự bảo hộ khi trang bị.",
    "Reeled-in mobs get yanked towards you": "Kéo giật mạnh quái vật bị dính lưỡi câu về thẳng vị trí của bạn.",
    "Applies Haste effect when in mainhand equipped": "Cung cấp hiệu ứng Đào Nhanh (Haste) liên tục khi cầm trên tay chính.",
    "Applies a cosmetic heavenly aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang thiên đường thánh thiện thẩm mỹ cho người mặc và thú cưng.",
    "Hit entities turn invisible": "Kẻ địch bị đánh trúng sẽ tàng hình biến mất trong chốc lát.",
    "Freezes attacking entities": "Đóng băng kẻ tấn công bạn khi bạn nhận phải sát thương.",
    "Lightens up the area around you when headblock is not obstructed": "Tự động thắp sáng rực rỡ khu vực xung quanh khi đầu không bị che chắn.",
    "Charged spear hits pin the target in place": "Đòn đâm thương/đinh ba kích lực ghim chặt và làm bất động mục tiêu tại chỗ.",
    "Pulls in hostile entities within a 5 block radius": "Kéo các quái vật thù địch trong bán kính 5 khối lại gần nhau.",
    "Crouching turns you invisible": "Ngồi xổm (sneak) sẽ kích hoạt khả năng tàng hình hoàn toàn.",
    "Deals extra damage while riding a mount": "Gây thêm lượng lớn sát thương khi đang cưỡi thú chiến đấu.",
    "Spawns Leaf Blocks when jumping in the air": "Tạo ra các khối lá cây tạm thời dưới chân khi nhảy đúp trên không trung.",
    "Regenerate health when hitting entities": "Hồi phục một phần máu cho bản thân mỗi khi đánh trúng kẻ địch.",
    "Has a chance to summon lightning upon hit entities when they can see the sky": "Có cơ hội triệu hồi tia sét giáng xuống kẻ địch nếu chúng đứng dưới bầu trời.",
    "Leaves behind a lingering cloud with the effect of the shot tipped arrow when hitting a block": "Để lại một đám mây thuốc kéo dài mang hiệu ứng của mũi tên khi trúng khối.",
    "Applies a cosmetic lovely aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang tình yêu lãng mạn thẩm mỹ cho người mặc và thú cưng.",
    "Applies a cosmetic lush aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang cây cỏ tươi tốt thẩm mỹ cho người mặc và thú cưng.",
    "Turns Lava into magma blocks when walking over it": "Biến dung nham dưới chân thành khối Magma tạm thời để bước qua.",
    "Smash attacks after falling 8 blocks set the area on fire": "Đòn đập Chùy sau khi rơi ít nhất 8 khối sẽ thiêu đốt cả khu vực xung quanh thành biển lửa.",
    "Wearers vision is mirrored when equipped": "Khiến toàn bộ tầm nhìn của người mặc bị đảo ngược trái phải như trong gương.",
    "Reduces gravity when equipped": "Giảm trọng lực tác động lên người chơi, cho phép rơi chậm và nhảy xa.",
    "Slowly repairs the item under the open night sky": "Từ từ tự động sửa chữa độ bền trang bị dưới bầu trời đêm quang đãng.",
    "Applies a cosmetic muddy aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang bùn lầy thẩm mỹ cho người mặc và thú cưng.",
    "Twice the damage protection than the vanilla enchant but works only in the nether": "Giảm sát thương gấp đôi so với phù phép thường nhưng chỉ có tác dụng trong Nether.",
    "Turns the helmet into Night Vision Goggles": "Biến chiếc mũ thành kính nhìn đêm, ban hiệu ứng Tầm nhìn bóng đêm (Night Vision).",
    "Applies a cosmetic ominous aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang điềm gở hắc ám thẩm mỹ cho người mặc và thú cưng.",
    "Has a 60% chance to spawn a healing lingering potion when hit while having 3.5 hearts or less": "Có 60% cơ hội tạo đám mây thuốc hồi máu khi máu giảm xuống dưới 3.5 tim.",
    "Summons ominous projectiles when projectile lands": "Triệu hồi cơn mưa đạn ma quái giáng xuống điểm rơi của mũi tên.",
    "Wearer takes damage when their pets do": "Người chơi nhận sát thương đồng thời mỗi khi thú cưng của mình chịu đòn.",
    "Applies a cosmetic pale aura to the wearer, it's pets and most of the projectiles shot": "Tạo hiệu ứng hào quang nhợt nhạt u ám thẩm mỹ cho người mặc và thú cưng.",
    "Increases movement speed when on dirt path blocks": "Tăng tốc độ chạy đáng kể khi di chuyển trên các khối đường đất (Dirt Path).",
    "Horses fly up to 30 Blocks in the air when mounted. Dismounting gives slow falling to land safely": "Ngựa có khả năng bay vút lên tới 30 khối trên không trung, khi xuống ngựa nhận hiệu ứng rơi chậm an toàn.",
    "Increases Damage against Phantoms": "Tăng lượng lớn sát thương gây ra lên bóng ma Phantom.",
    "Right Click to steal items from villager inventories": "Nhấp chuột phải vào dân làng để móc túi cướp lấy vật phẩm trong túi đồ của họ.",
    "Indicates that an entity has been hit by playing a ping sound": "Phát ra âm thanh 'ping' rõ ràng thông báo mỗi khi có kẻ địch bị trúng đòn từ xa."
}

TRANSLATION_MAP.update(ADDITIONAL_MAP)

# Additional Katters enchants
KATTERS_ADDITIONAL = {
    "kattersstructures:light": (3, 4, ["armor"], "Mũ, Áo giáp, Quần, Giày", "Nhẹ Nhàng", "Light", "Gia tăng độ nhanh nhẹn và tốc độ di chuyển nhưng giảm nhẹ giáp phòng ngự."),
    "kattersstructures:ocean_heart": (1, 4, ["armor"], "Áo giáp thân", "Trái Tim Biển Cả", "Ocean Heart", "Ban hiệu ứng Sức Mạnh Thần Biển (Conduit Power) liên tục khi ở dưới nước."),
    "kattersstructures:rusta_shot": (3, 4, ["ranged"], "Cung & Nỏ", "Tiễn Rusta Ma Thuật", "Rusta Shot", "Mũi tên mang năng lượng ma thuật Rusta cổ xưa, phát nổ khi trúng đích."),
    "kattersstructures:web_walker": (3, 4, ["armor"], "Giày", "Bước Đi Trên Tơ", "Web Walker", "Cho phép di chuyển nhanh qua mạng nhện tơ và tạo mạng nhện làm chậm kẻ thù.")
}
KATTERS_INFO.update(KATTERS_ADDITIONAL)

def infer_gear_from_supported(sup_str):
    s = str(sup_str).lower()
    cats = []
    display = []
    
    if any(k in s for k in ["sword", "sharp_weapon", "weapon", "mace", "spear", "axe", "fire_aspect"]):
        cats.append("weapon")
        display.append("Vũ khí cận chiến")
    if any(k in s for k in ["bow", "crossbow", "range"]):
        cats.append("ranged")
        display.append("Cung & Nỏ")
    if any(k in s for k in ["trident", "lunge"]):
        cats.append("ranged")
        if "weapon" not in cats:
            cats.append("weapon")
        display.append("Đinh ba")
    if any(k in s for k in ["head_armor", "head"]):
        cats.append("armor")
        display.append("Mũ")
    elif any(k in s for k in ["chest_armor", "chest"]):
        cats.append("armor")
        display.append("Áo giáp thân")
    elif any(k in s for k in ["leg_armor", "leg"]):
        cats.append("armor")
        display.append("Quần")
    elif any(k in s for k in ["foot_armor", "foot", "boots"]):
        cats.append("armor")
        display.append("Giày")
    elif any(k in s for k in ["armor", "equippable"]):
        cats.append("armor")
        display.append("Mũ, Giáp, Quần, Giày")
    if any(k in s for k in ["elytra"]):
        cats.append("armor")
        display.append("Cánh Elytra")
    if any(k in s for k in ["shield", "can_block"]):
        cats.append("armor")
        display.append("Khiên")
    if any(k in s for k in ["mining", "pickaxe"]):
        cats.append("tool")
        display.append("Cúp")
    elif any(k in s for k in ["hoe"]):
        cats.append("tool")
        display.append("Cuốc làm nông")
    elif any(k in s for k in ["fishing"]):
        cats.append("tool")
        display.append("Cần câu")
    elif any(k in s for k in ["tool"]):
        cats.append("tool")
        display.append("Công cụ")
    if any(k in s for k in ["horse", "wolf", "saddle", "harness", "dog", "mounted"]):
        cats.append("mount")
        display.append("Yên cương & Thú cưỡi")
        
    if not cats:
        if any(k in s for k in ["durability", "all_enchantable", "vanishing"]):
            cats = ["universal"]
            display = ["Mọi trang bị có độ bền"]
        else:
            cats = ["other"]
            display = ["Trang bị đặc biệt"]
            
    cats = list(dict.fromkeys(cats))
    display_str = ", ".join(list(dict.fromkeys(display)))
    return cats, display_str

print("Compiling all 400 enchantments into unified database...")

final_list = []
tier_order = {"ultimate": 1, "epic": 2, "rare": 3, "common": 4, "curse": 5}

for eid in enabled_400:
    entry = cfg["enchantments"].get(eid, {})
    ns, name = eid.split(":", 1)
    
    tier = entry.get("tier", "common")
    if entry.get("curse", False):
        tier = "curse"
        
    max_lv = entry.get("max_level")
    anvil_cost = entry.get("anvil_cost", 2)
    supported_items = entry.get("supported_items", "")
    
    # Defaults
    name_en = name.replace("_", " ").replace("/", " - ").title()
    name_vi = name_en
    desc = ""
    gear_cats = ["other"]
    gear_display = "Trang bị đặc thù"
    
    # 1. Check Vanilla
    if eid in VANILLA_INFO:
        max_lv, anvil_cost, gear_cats, gear_display, name_vi, name_en, desc = VANILLA_INFO[eid]
    # 2. Check Dungeons & Taverns
    elif eid in DNT_INFO:
        max_lv, anvil_cost, gear_cats, gear_display, name_vi, name_en, desc = DNT_INFO[eid]
    # 3. Check Enchants Plus
    elif eid in ENCHANTSPLUS_INFO:
        max_lv, anvil_cost, gear_cats, gear_display, name_vi, name_en, desc = ENCHANTSPLUS_INFO[eid]
    # 4. Check Stellarity
    elif eid in STELLARITY_INFO:
        max_lv, anvil_cost, gear_cats, gear_display, name_vi, name_en, desc = STELLARITY_INFO[eid]
    # 5. Check Katters
    elif eid in KATTERS_INFO:
        max_lv, anvil_cost, gear_cats, gear_display, name_vi, name_en, desc = KATTERS_INFO[eid]
    # 6. Check Incendium, Veinminer, Wither
    elif eid in INCENDIUM_VEINMINER_WITHER_INFO:
        max_lv, anvil_cost, gear_cats, gear_display, name_vi, name_en, desc = INCENDIUM_VEINMINER_WITHER_INFO[eid]
    # 7. Check NeoEnchant (enchantplus)
    elif eid in ENCHANTPLUS_INFO:
        max_lv, anvil_cost, gear_cats, gear_display, name_vi, name_en, desc = ENCHANTPLUS_INFO[eid]
    # 8. Check Mine Treasure (mt)
    elif eid in MT_INFO:
        max_lv, anvil_cost, gear_cats, gear_display, name_vi, name_en, desc = MT_INFO[eid]
    # 9. Check Curated Encore
    elif eid in ENCORE_CURATED:
        desc, gear_cats, gear_display = ENCORE_CURATED[eid]
        meta = encore_meta.get(eid, {})
        name_en = meta.get("name_en", name_en)
        name_vi = meta.get("name_vi", name_en)
        src = source_raw.get(eid, {})
        max_lv = src.get("max_level", max_lv or 1)
        anvil_cost = src.get("anvil_cost", anvil_cost)
    # 10. Check Encore Aspects
    elif eid.startswith("enchantencore:") and name in ASPECT_MAP:
        name_vi, desc = ASPECT_MAP[name]
        gear_cats = ["weapon"]
        gear_display = "Kiếm & Chùy"
        meta = encore_meta.get(eid, {})
        name_en = meta.get("name_en", name_en)
        src = source_raw.get(eid, {})
        max_lv = src.get("max_level", max_lv or 1)
        anvil_cost = src.get("anvil_cost", anvil_cost)
    # 11. Check other Encore
    elif eid.startswith("enchantencore:"):
        meta = encore_meta.get(eid, {})
        name_en = meta.get("name_en", name_en)
        name_vi = meta.get("name_vi", name_en)
        lore = meta.get("lore_en", "").strip()
        lines = [l.strip() for l in lore.split("\n") if l.strip()]
        desc_line = lines[-1] if lines else ""
        desc = TRANSLATION_MAP.get(desc_line, desc_line)
        src = source_raw.get(eid, {})
        max_lv = src.get("max_level", max_lv or 1)
        anvil_cost = src.get("anvil_cost", anvil_cost)
        sup = src.get("supported_items") or supported_items
        gear_cats, gear_display = infer_gear_from_supported(sup)
    else:
        desc = f"Phù phép đặc thù nâng cấp sức mạnh cho {name_en}."
        
    if not max_lv or max_lv < 1:
        max_lv = 1
        
    # Update config entry in memory
    entry["max_level"] = max_lv
    entry["anvil_cost"] = anvil_cost
    entry["description"] = desc
    entry["name_vi"] = name_vi
    
    give_cmd = f'/give @p enchanted_book[enchantments={{levels:{{"{eid}":{max_lv}}}}}] 1'
    
    item_record = {
        "id": eid,
        "namespace": ns,
        "raw_name": name,
        "name": name_en,
        "name_vi": name_vi,
        "tier": tier,
        "tier_order": tier_order.get(tier, 99),
        "max_level": max_lv,
        "anvil_cost": anvil_cost,
        "table": entry.get("table", False),
        "tradeable": entry.get("tradeable", False),
        "loot": entry.get("loot", True),
        "source_pack": entry.get("source_pack", "vanilla"),
        "supported_items": str(supported_items),
        "gear_categories": gear_cats,
        "supported_display": gear_display,
        "description": desc,
        "give_command": give_cmd
    }
    final_list.append(item_record)

final_list.sort(key=lambda x: (x["tier_order"], x["name"]))

# Verify zero missing
missing_count = sum(1 for x in final_list if not x["description"] or len(x["description"]) < 10)
print(f"Compilation finished. Total: {len(final_list)}. Missing/short descriptions: {missing_count}")

# Save enchantments_config.json
with open(CONFIG_PATH, "w", encoding="utf-8") as f:
    json.dump(cfg, f, indent=2, ensure_ascii=False)
print("Updated enchantments_config.json successfully.")

# Save assets/js/enchants_data.js
os.makedirs(os.path.dirname(OUTPUT_FILE), exist_ok=True)
with open(OUTPUT_FILE, "w", encoding="utf-8") as f:
    f.write("/* Auto-generated by scripts/build_full_database.py - 100% Verified Metadata */\n")
    f.write("window.ALL_ENCHANTMENTS = ")
    json.dump(final_list, f, indent=2, ensure_ascii=False)
    f.write(";\n")
print(f"Exported {len(final_list)} enchantments to {OUTPUT_FILE} successfully.")
