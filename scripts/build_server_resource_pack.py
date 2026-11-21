"""
Build & Merge Server Resource Pack for One More Expedition.
Merges assets/ from all resource packs in resource_pack/ (including overlay layers for Incendium/Sparkles),
injects Katters Structures definitions and CIT configs, merges conflicting item definitions (e.g. netherite helmet),
and injects tiered enchanted book textures, custom model data, and CIT configs.
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
        "warping_wonders_4.2.zip",
        "Katters Structures Resource Pack v2.9.zip",
        "Dungeons and Taverns v6.2.0.zip",
        "Stellarity-6.0.1-RP.zip",
        "Sparkles_v1.1.12.zip"
    ]

    # Overlay directories in Sparkles to apply in order
    sparkles_overlays = [
        "1-20-5-overlay",
        "1-21-4-overlay",
        "1-21-9-overlay",
        "26-1-overlay"
    ]

    extracted_count = 0
    for zname in source_zips:
        zpath = os.path.join(rp_dir, zname)
        if not os.path.exists(zpath):
            print(f"[Warning] {zpath} does not exist, skipping...")
            continue
        print(f"[Merge] Merging assets from {zname}...")
        with zipfile.ZipFile(zpath, "r") as zf:
            namelist = zf.namelist()

            # 1a. Base assets
            for member in namelist:
                if member.startswith("assets/") and not member.endswith("/"):
                    target_path = os.path.join(out_dir, member)
                    os.makedirs(os.path.dirname(target_path), exist_ok=True)
                    with open(target_path, "wb") as f_out:
                        f_out.write(zf.read(member))
                    extracted_count += 1
                elif member == "pack.png" and not os.path.exists(os.path.join(out_dir, "pack.png")):
                    with open(os.path.join(out_dir, "pack.png"), "wb") as f_out:
                        f_out.write(zf.read(member))

            # 1b. If this is Sparkles, also apply overlays in chronological order
            if "Sparkles" in zname:
                for ov in sparkles_overlays:
                    ov_prefix = f"{ov}/"
                    ov_assets_prefix = f"{ov}/assets/"
                    count_ov = 0
                    for member in namelist:
                        if member.startswith(ov_assets_prefix) and not member.endswith("/"):
                            # Strip the overlay folder prefix (e.g. 1-21-4-overlay/assets/... -> assets/...)
                            rel_asset = member[len(ov_prefix):]
                            target_path = os.path.join(out_dir, rel_asset)
                            os.makedirs(os.path.dirname(target_path), exist_ok=True)
                            with open(target_path, "wb") as f_out:
                                f_out.write(zf.read(member))
                            count_ov += 1
                            extracted_count += 1
                    print(f"  [Sparkles Overlay] Layered {count_ov} files from {ov}")

    print(f"[Merge] Total assets extracted: {extracted_count}")

    # 2. Pack metadata
    mcmeta = {
        "pack": {
            "pack_format": 46,
            "supported_formats": [15, 99],
            "description": "§6One More Expedition §7- Server Resource Pack\n§eMerged Textures & Tiered Enchanted Books"
        }
    }
    with open(os.path.join(out_dir, "pack.mcmeta"), "w", encoding="utf-8") as f:
        json.dump(mcmeta, f, indent=2, ensure_ascii=False)

    # 3. Fix & Merge Conflicting Item Models: netherite_helmet.json
    # Sparkles: CustomModelData threshold 1450200 -> incendium:item/scouts
    # Stellarity: 13 armor trim cases -> fallback minecraft:item/netherite_helmet
    netherite_helmet_def = {
        "model": {
            "type": "minecraft:range_dispatch",
            "property": "minecraft:custom_model_data",
            "entries": [
                {
                    "threshold": 1450200,
                    "model": {
                        "type": "minecraft:model",
                        "model": "incendium:item/scouts"
                    }
                }
            ],
            "fallback": {
                "type": "minecraft:select",
                "property": "minecraft:trim_material",
                "cases": [
                    {"when": "stellarity:chorus", "model": {"type": "minecraft:model", "model": "stellarity:item/trim/netherite/netherite_helmet_chorus_trim"}},
                    {"when": "stellarity:enderite", "model": {"type": "minecraft:model", "model": "stellarity:item/trim/netherite/netherite_helmet_enderite_trim"}},
                    {"when": "minecraft:quartz", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_quartz_trim"}},
                    {"when": "minecraft:iron", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_iron_trim"}},
                    {"when": "minecraft:netherite", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_netherite_trim"}},
                    {"when": "minecraft:redstone", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_redstone_trim"}},
                    {"when": "minecraft:copper", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_copper_trim"}},
                    {"when": "minecraft:gold", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_gold_trim"}},
                    {"when": "minecraft:emerald", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_emerald_trim"}},
                    {"when": "minecraft:diamond", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_diamond_trim"}},
                    {"when": "minecraft:lapis", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_lapis_trim"}},
                    {"when": "minecraft:amethyst", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_amethyst_trim"}},
                    {"when": "minecraft:resin", "model": {"type": "minecraft:model", "model": "minecraft:item/netherite_helmet_resin_trim"}}
                ],
                "fallback": {
                    "type": "minecraft:model",
                    "model": "minecraft:item/netherite_helmet"
                }
            }
        }
    }
    with open(os.path.join(out_dir, "assets", "minecraft", "items", "netherite_helmet.json"), "w", encoding="utf-8") as f:
        json.dump(netherite_helmet_def, f, indent=2)
    print("[Fix] Merged netherite_helmet.json (Incendium Scouts + Stellarity Trims)")

    # 4. Katters Structures: Item definitions & CIT configurations
    # Items: the_beast_within, the_soul_siphon, the_staff_of_ages, weather_shaper (all based on blaze_rod)
    katter_items = [
        {
            "id": "the_beast_within",
            "name": "The Beast within",
            "translate": "item.kattersstructures.the_beast_within",
            "model": "kattersstructures:item/the_beast_within"
        },
        {
            "id": "the_soul_siphon",
            "name": "The Soul Siphon",
            "translate": "item.kattersstructures.the_soul_siphon",
            "model": "kattersstructures:item/the_soul_siphon"
        },
        {
            "id": "the_staff_of_ages",
            "name": "The Staff of Ages",
            "translate": "item.kattersstructures.the_staff_of_ages",
            "model": "kattersstructures:item/the_staff_of_ages"
        },
        {
            "id": "weather_shaper",
            "name": "The Weather Shaper",
            "translate": "item.kattersstructures.weather_shaper",
            "model": "kattersstructures:item/weather_shaper"
        }
    ]

    # 4a. assets/minecraft/items/blaze_rod.json (Vanilla 1.21.4 item model fallback)
    blaze_rod_def = {
        "model": {
            "type": "minecraft:select",
            "property": "minecraft:component",
            "component": "minecraft:item_name",
            "cases": [
                {
                    "when": {
                        "translate": item["translate"],
                        "fallback": item["name"]
                    },
                    "model": {
                        "type": "minecraft:model",
                        "model": item["model"]
                    }
                } for item in katter_items
            ],
            "fallback": {
                "type": "minecraft:model",
                "model": "minecraft:item/blaze_rod"
            }
        }
    }
    with open(os.path.join(out_dir, "assets", "minecraft", "items", "blaze_rod.json"), "w", encoding="utf-8") as f:
        json.dump(blaze_rod_def, f, indent=2)

    # 4b. OptiFine / CIT Resewn properties for Katters Structures
    katter_cit_dir = os.path.join(out_dir, "assets", "minecraft", "optifine", "cit", "kattersstructures")
    os.makedirs(katter_cit_dir, exist_ok=True)
    for item in katter_items:
        cit_prop = os.path.join(katter_cit_dir, f"{item['id']}.properties")
        with open(cit_prop, "w", encoding="utf-8") as f:
            f.write(
                f"type=item\n"
                f"items=blaze_rod\n"
                f"model={item['model']}\n"
                f"texture={item['model']}\n"
                f"nbt.kattersstructures=regex:({item['id']}|{item['id']}_nm)\n"
            )
    print(f"[Katters] Injected blaze_rod.json item definition and {len(katter_items)} CIT properties")

    # 5. Setup custom textures for tiered enchanted books
    tiers = ["common", "rare", "epic", "ultimate"]
    tier_cmd = {
        "common": 1001,
        "rare": 1002,
        "epic": 1003,
        "ultimate": 1004
    }

    exp_tex_dir = os.path.join(out_dir, "assets", "expedition", "textures", "item")
    os.makedirs(exp_tex_dir, exist_ok=True)

    cit_book_dir = os.path.join(out_dir, "assets", "minecraft", "optifine", "cit", "enchanted_book")
    os.makedirs(cit_book_dir, exist_ok=True)

    for t in tiers:
        src_png = os.path.join(rp_dir, f"enchanted_book_{t}.png")
        if not os.path.exists(src_png):
            print(f"[Warning] Texture {src_png} missing!")
            continue

        shutil.copyfile(src_png, os.path.join(exp_tex_dir, f"enchanted_book_{t}.png"))
        shutil.copyfile(src_png, os.path.join(cit_book_dir, f"enchanted_book_{t}.png"))

        # CIT Properties (OptiFine / CIT Resewn)
        cit_prop = os.path.join(cit_book_dir, f"enchanted_book_{t}.properties")
        with open(cit_prop, "w", encoding="utf-8") as f:
            f.write(f"type=item\nitems=enchanted_book\nnbt.expedition_tier={t}\ntexture=enchanted_book_{t}.png\n")

    # 5a. Create models in assets/expedition/models/item/
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

    # 5b. Create 1.21.4 item definition in assets/minecraft/items/enchanted_book.json
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

    # 5c. Create legacy item model in assets/minecraft/models/item/enchanted_book.json
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

    # 6. Zip the resource pack
    print("[Zip] Packaging Expedition_Server_Resource_Pack.zip...")
    with zipfile.ZipFile(zip_out, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as zipf:
        for root, dirs, files in os.walk(out_dir):
            for file in files:
                full_path = os.path.join(root, file)
                arcname = os.path.relpath(full_path, out_dir)
                zipf.write(full_path, arcname)

    # 7. Hash SHA-1
    with open(zip_out, "rb") as f:
        sha1 = hashlib.sha1(f.read()).hexdigest()

    zip_size_mb = os.path.getsize(zip_out) / (1024 * 1024)
    print(f"[Done] Resource pack built: {zip_out} ({zip_size_mb:.2f} MB)")
    print(f"[Done] SHA-1 Hash: {sha1}")

    # 8. Sync to server directory if present and update server.properties
    allay_zip = "D:/AllayOps/Expedition_Server_Resource_Pack.zip"
    allay_props = "D:/AllayOps/server.properties"
    if os.path.exists("D:/AllayOps"):
        shutil.copyfile(zip_out, allay_zip)
        print(f"[Sync] Copied to {allay_zip}")

        if os.path.exists(allay_props):
            with open(allay_props, "r", encoding="utf-8") as pf:
                lines = pf.readlines()
            new_lines = []
            updated = False
            for line in lines:
                if line.startswith("resource-pack-sha1="):
                    new_lines.append(f"resource-pack-sha1={sha1}\n")
                    updated = True
                else:
                    new_lines.append(line)
            if not updated:
                new_lines.append(f"resource-pack-sha1={sha1}\n")
            with open(allay_props, "w", encoding="utf-8") as pf:
                pf.writelines(new_lines)
            print(f"[Sync] Updated resource-pack-sha1 in {allay_props}")

if __name__ == "__main__":
    build()
