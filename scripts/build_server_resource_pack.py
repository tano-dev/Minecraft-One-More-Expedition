"""
Build & Merge Server Resource Pack for One More Expedition.
Merges assets/ from all resource packs in resource_pack/ and injects
tiered enchanted book textures, custom model data, and CIT configs.
"""

import os
import glob
import shutil
import zipfile
import hashlib
import json

def build():
    repo_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    rp_dir = os.path.join(repo_root, "resource_pack")
    out_dir = os.path.join(rp_dir, "Expedition_Server_Resource_Pack")
    zip_out = os.path.join(rp_dir, "Expedition_Server_Resource_Pack.zip")

    if os.path.exists(out_dir):
        shutil.rmtree(out_dir)
    os.makedirs(out_dir, exist_ok=True)

    # 1. Merge assets from all resource pack zips
    source_zips = [
        "Sparkles_v1.1.12.zip",
        "Katters Structures Resource Pack v2.9.zip",
        "warping_wonders_4.2.zip",
        "Dungeons and Taverns v6.2.0.zip",
        "Stellarity-6.0.1-RP.zip"
    ]

    extracted_count = 0
    for zname in source_zips:
        zpath = os.path.join(rp_dir, zname)
        if not os.path.exists(zpath):
            print(f"[Warning] {zpath} does not exist, skipping...")
            continue
        print(f"[Merge] Merging assets from {zname}...")
        with zipfile.ZipFile(zpath, "r") as zf:
            for member in zf.namelist():
                if member.startswith("assets/") and not member.endswith("/"):
                    target_path = os.path.join(out_dir, member)
                    os.makedirs(os.path.dirname(target_path), exist_ok=True)
                    with open(target_path, "wb") as f_out:
                        f_out.write(zf.read(member))
                    extracted_count += 1
                elif member == "pack.png" and not os.path.exists(os.path.join(out_dir, "pack.png")):
                    with open(os.path.join(out_dir, "pack.png"), "wb") as f_out:
                        f_out.write(zf.read(member))

    print(f"[Merge] Total assets extracted: {extracted_count}")

    # 2. Pack metadata
    mcmeta = {
        "pack": {
            "pack_format": 97,
            "supported_formats": [15, 99],
            "description": "§6One More Expedition §7- Server Resource Pack\n§eMerged Textures & Tiered Enchanted Books"
        }
    }
    with open(os.path.join(out_dir, "pack.mcmeta"), "w", encoding="utf-8") as f:
        json.dump(mcmeta, f, indent=2, ensure_ascii=False)

    # 3. Setup custom textures for tiered enchanted books
    tiers = ["common", "rare", "epic", "ultimate"]
    tier_cmd = {
        "common": 1001,
        "rare": 1002,
        "epic": 1003,
        "ultimate": 1004
    }

    exp_tex_dir = os.path.join(out_dir, "assets", "expedition", "textures", "item")
    os.makedirs(exp_tex_dir, exist_ok=True)

    cit_dir = os.path.join(out_dir, "assets", "minecraft", "optifine", "cit", "enchanted_book")
    os.makedirs(cit_dir, exist_ok=True)

    for t in tiers:
        src_png = os.path.join(rp_dir, f"enchanted_book_{t}.png")
        if not os.path.exists(src_png):
            print(f"[Warning] Texture {src_png} missing!")
            continue

        shutil.copyfile(src_png, os.path.join(exp_tex_dir, f"enchanted_book_{t}.png"))
        shutil.copyfile(src_png, os.path.join(cit_dir, f"enchanted_book_{t}.png"))

        # CIT Properties (OptiFine / CIT Resewn)
        cit_prop = os.path.join(cit_dir, f"enchanted_book_{t}.properties")
        with open(cit_prop, "w", encoding="utf-8") as f:
            f.write(f"type=item\nitems=enchanted_book\nnbt.expedition_tier={t}\ntexture=enchanted_book_{t}.png\n")

    # 4. Create models in assets/expedition/models/item/
    exp_model_dir = os.path.join(out_dir, "assets", "expedition", "models", "item")
    os.makedirs(exp_model_dir, exist_ok=True)

    for t in tiers:
        model_json = {
            "parent": "minecraft:item/generated",
            "textures": {
                "layer0": f"expedition:item/enchanted_book_{t}"
            }
        }
        with open(os.path.join(exp_model_dir, f"enchanted_book_{t}.json"), "w", encoding="utf-8") as f:
            json.dump(model_json, f, indent=2)

    # 5. Create 1.21.4 item definition in assets/minecraft/items/enchanted_book.json
    mc_items_dir = os.path.join(out_dir, "assets", "minecraft", "items")
    os.makedirs(mc_items_dir, exist_ok=True)

    item_def = {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:custom_model_data",
            "index": 0,
            "cases": [
                {
                    "when": t,
                    "model": {
                        "type": "minecraft:model",
                        "model": f"expedition:item/enchanted_book_{t}"
                    }
                } for t in tiers
            ],
            "fallback": {
                "type": "minecraft:model",
                "model": "minecraft:item/enchanted_book"
            }
        }
    }
    with open(os.path.join(mc_items_dir, "enchanted_book.json"), "w", encoding="utf-8") as f:
        json.dump(item_def, f, indent=2)

    # 6. Create legacy item model in assets/minecraft/models/item/enchanted_book.json
    mc_models_dir = os.path.join(out_dir, "assets", "minecraft", "models", "item")
    os.makedirs(mc_models_dir, exist_ok=True)

    legacy_model = {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": "minecraft:item/enchanted_book"
        },
        "overrides": [
            {
                "predicate": {"custom_model_data": tier_cmd[t]},
                "model": f"expedition:item/enchanted_book_{t}"
            } for t in tiers
        ]
    }
    with open(os.path.join(mc_models_dir, "enchanted_book.json"), "w", encoding="utf-8") as f:
        json.dump(legacy_model, f, indent=2)

    # 7. Zip the resource pack
    print("[Zip] Packaging Expedition_Server_Resource_Pack.zip...")
    with zipfile.ZipFile(zip_out, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zipf:
        for root, dirs, files in os.walk(out_dir):
            for file in files:
                full_path = os.path.join(root, file)
                arcname = os.path.relpath(full_path, out_dir)
                zipf.write(full_path, arcname)

    # 8. Hash SHA-1
    with open(zip_out, "rb") as f:
        sha1 = hashlib.sha1(f.read()).hexdigest()

    zip_size_mb = os.path.getsize(zip_out) / (1024 * 1024)
    print(f"[Done] Resource pack built: {zip_out} ({zip_size_mb:.2f} MB)")
    print(f"[Done] SHA-1 Hash: {sha1}")

    # 9. Sync to server directory if present
    allay_zip = "D:/AllayOps/Expedition_Server_Resource_Pack.zip"
    if os.path.exists("D:/AllayOps"):
        shutil.copyfile(zip_out, allay_zip)
        print(f"[Sync] Copied to {allay_zip}")

if __name__ == "__main__":
    build()
