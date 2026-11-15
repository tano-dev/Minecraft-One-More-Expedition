import os
import json
import sys

# Add repo root to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
from scripts.enchant_manager import ULTIMATE_IDS

all_ultimates = set(ULTIMATE_IDS)
all_ultimates.add('minecraft:wind_burst')

count = 0
for root, dirs, files in os.walk('datapacks'):
    if 'expedition_enchantments_control' in root:
        continue
    for f in files:
        if f.endswith('.json') and '/enchantment' in root.replace('\\', '/'):
            p = os.path.join(root, f)
            norm = p.replace('\\', '/')
            # extract namespace and relative path after data/<ns>/enchantment/
            if '/data/' not in norm or '/enchantment/' not in norm:
                continue
            after_data = norm.split('/data/', 1)[1]
            ns = after_data.split('/enchantment/', 1)[0]
            after_ench = after_data.split('/enchantment/', 1)[1]
            enchant_name = after_ench[:-5]
            eid = f"{ns}:{enchant_name}"

            if eid in all_ultimates:
                with open(p, 'r', encoding='utf-8') as fp:
                    try:
                        data = json.load(fp)
                    except Exception as e:
                        print(f"Error reading {p}: {e}")
                        continue

                desc = data.get('description')
                if isinstance(desc, dict):
                    desc['color'] = 'gold'
                    desc['bold'] = True
                elif isinstance(desc, str):
                    data['description'] = {'text': desc, 'color': 'gold', 'bold': True}
                else:
                    data['description'] = {'text': enchant_name.replace('_', ' ').title(), 'color': 'gold', 'bold': True}

                with open(p, 'w', encoding='utf-8') as fp:
                    json.dump(data, fp, indent=2)
                print(f"Updated {eid} -> {p}")
                count += 1

print(f"Total source ultimate enchant files updated: {count}")
