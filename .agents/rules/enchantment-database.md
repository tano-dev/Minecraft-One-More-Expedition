# Enchantment Database Standards & Maintenance Rule

Whenever any new enchantment is added (via datapack JSON, `.mcfunction`, or custom Fabric mod Java code) or an existing enchantment is modified:

1. **Read & Verify from Source Code:**
   - Never guess or write placeholder descriptions. Always read the actual datapack JSON (`effects`, `max_level`, `supported_items`, `exclusive_set`), linked `.mcfunction` scripts, or Java mod implementation (`enchants_balance.json`, Mixin/Event handlers) to extract exact mechanics and numbers.

2. **Player-Friendly English Descriptions with Per-Level Scaling:**
   - Keep all enchantment names and `description` fields in **English**.
   - Use **simple, player-friendly Minecraft terminology**. Avoid developer/technical jargon such as `EPF`, `ticks`, `entity`, `attribute`, `amplifier`, `SCC`, `RNG`, or `internal`.
     - Convert EPF to approximate percentage damage reduction (e.g., `1 EPF = 4% damage reduction`, `Protection: 2% / 6% / 10% / 14% per piece`).
     - Convert ticks to seconds (20 ticks = `1s`).
     - Use `mobs`, `targets`, `players`, or `creatures` instead of `entities`.
   - For any multi-level enchantment (`max_level > 1`), explicitly state **what each level does** (e.g., `+5% per level (up to +15% at Lv III)` or `1.5x / 2.0x / 2.5x across Lv I–III`).

3. **Equipment Classification (`gear_categories` + `sub_categories`):**
   - Every enchantment must be registered in `enchantments_config.json` and `scripts/build_english_database.py` (either via `VERIFIED_OVERRIDES` / its pack dict or dynamic mod loader).
   - Ensure `supported_items` maps accurately via `resolve_sub_categories()` to specific item sub-types (`sword`, `axe`, `mace`, `spear`, `trident`, `bow`, `crossbow`, `helmet`, `chestplate`, `leggings`, `boots`, `elytra`, `shield`, `pickaxe`, `shovel`, `hoe`, `shears`, `fishing_rod`, `horse_armor`, `wolf_armor`, `harness`, `saddle`, `all_gear`, `special`).

4. **Rebuild & Verify:**
   - Always run `python scripts/build_english_database.py` (or `python scripts/export_wiki_enchants.py`) after changes to regenerate `assets/js/enchants_data.js` and sync `enchantments_config.json`, ensuring `Missing dict keys: 0` and `Short/missing descriptions: 0`.
