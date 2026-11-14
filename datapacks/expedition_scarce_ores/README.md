# Expedition Scarce Ores (Datapack)

Reduces natural ore generation in newly generated chunks for **Minecraft 26.3** (`min_format: 121`, `max_format: 121`).

> [!NOTE]
> Standard Minecraft `.json` datapack files **do not support `//` or `/* */` comments** (Gson/Codec will reject them on load). All **Vanilla Default** reference values are documented here instead so the JSON files remain 100% valid.

## Ore Generation Adjustments (vs. Vanilla 26.3 Defaults)

| Ore Feature | Vanilla Default | New Setting | Effective Reduction |
| :--- | :--- | :--- | :--- |
| `ore_diamond` (small veins) | `count: 7` (every chunk) | `rarity_filter: 2` + `count: 5` (avg `2.5`/chunk) | **-64.3%** (~-65%) |
| `ore_diamond_buried` | `count: 4` (every chunk) | `rarity_filter: 3` + `count: 4` (avg `1.33`/chunk) | **-66.7%** (~-65%) |
| `ore_diamond_medium` | `count: 2` (every chunk) | `rarity_filter: 3` + `count: 2` (avg `0.67`/chunk) | **-66.7%** (~-65%) |
| `ore_diamond_large` | `rarity_filter: 9` (1 in 9 chunks) | `rarity_filter: 26` (1 in 26 chunks) | **-65.4%** (~-65%) |
| `ore_iron_middle` | `count: 10` | `count: 8` | **-20.0%** |
| `ore_iron_small` | `count: 10` | `count: 8` | **-20.0%** |
| `ore_iron_upper` | `count: 90` | `count: 72` | **-20.0%** |
| `ore_ancient_debris_large` | `1` attempt per chunk (100%) | `rarity_filter: 3` (33.3% chunks) | **-66.7%** (~-70% combined) |
| `ore_debris_small` | `1` attempt per chunk (100%) | `rarity_filter: 4` (25.0% chunks) | **-75.0%** (~-70% combined) |
| `ore_gold` | `count: 4` | `count: 2` | **-50.0%** |
| `ore_emerald` | `count: 100` | `count: 200` | **x2 (+100% more)** |

> **Note:** Only affects newly generated chunks. Existing explored chunks retain their original ores.
