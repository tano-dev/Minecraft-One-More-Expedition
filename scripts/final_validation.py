import json
import re

with open("assets/js/enchants_data.js", "r", encoding="utf-8") as f:
    text = f.read()

start = text.find('[')
end = text.rfind(']') + 1
data = json.loads(text[start:end])

print(f"Total enchantments in database: {len(data)}")

# 1. Check fallback / raw strings
suspicious = []
for item in data:
    d = item.get("description", "")
    n = item.get("name_vi", "")
    if any(k in d for k in ["Phù phép đặc sắc", "Hiệu ứng phù phép đặc biệt", "enchantment.", "item.", "boss."]):
        suspicious.append((item["id"], "Suspicious pattern in desc", d))
    if re.search(r'[\u4e00-\u9fff]', d):
        suspicious.append((item["id"], "Chinese characters in desc", d))
    if len(d) < 15:
        suspicious.append((item["id"], "Too short desc", d))
    if item["max_level"] is None or item["max_level"] < 1:
        suspicious.append((item["id"], "Invalid max_level", item["max_level"]))

print(f"Suspicious items count: {len(suspicious)}")
for s in suspicious:
    print("  ", s)

# 2. Check tier counts
tiers = {}
for item in data:
    t = item["tier"]
    tiers[t] = tiers.get(t, 0) + 1
print("Tier distribution:", tiers)

# 3. Check vanilla key levels
vanilla_checks = ["minecraft:sharpness", "minecraft:unbreaking", "minecraft:fortune", "minecraft:efficiency", "minecraft:protection", "minecraft:feather_falling"]
for item in data:
    if item["id"] in vanilla_checks:
        print(f"  {item['id']} -> max_level: {item['max_level']}, anvil_cost: {item['anvil_cost']}, cats: {item['gear_categories']}")

# 4. Check category distribution
cat_counts = {}
for item in data:
    for c in item["gear_categories"]:
        cat_counts[c] = cat_counts.get(c, 0) + 1
print("Gear category distribution:", cat_counts)
