import sys, os, shutil, gzip
sys.path.insert(0, r'd:\Minecraft-One-More-Expedition\scripts')

test_path = r'D:\Minecraft-One-More-Expedition\datapacks\Epic Villages 2.0.0\data\minecraft\structure\village\plains\houses\plains_accessory_1.nbt'
bak_path = test_path + '.bak'

# Backup
shutil.copy2(test_path, bak_path)
orig_size = os.path.getsize(bak_path)

# Load script và chạy
with open(r'd:\Minecraft-One-More-Expedition\scripts\fix_epic_villages_void.py') as f:
    code = f.read()

ns = {"__file__": r'd:\Minecraft-One-More-Expedition\scripts\fix_epic_villages_void.py'}
exec(compile(code, 'fix_epic_villages_void.py', 'exec'), ns)
flood_fill_nbt_exterior_air = ns['flood_fill_nbt_exterior_air']

changed, cnt = flood_fill_nbt_exterior_air(test_path)
print(f"Changed: {changed}, blocks converted: {cnt}")

# Verify có thể đọc lại
with gzip.open(test_path, 'rb') as f:
    data = f.read()

text = data.decode('latin-1')
print(f"structure_void present: {'minecraft:structure_void' in text}")
print(f"minecraft:air still present: {'minecraft:air' in text}")
print(f"File OK, decompressed size: {len(data):,} bytes")

# Restore backup (không giữ thay đổi test)
shutil.copy2(bak_path, test_path)
os.remove(bak_path)
print("Backup restored.")
