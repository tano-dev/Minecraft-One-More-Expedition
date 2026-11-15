import os
import json

def fix_enchants():
    files_to_fix = [
        'grass_walker.json',
        'magma_walker.json',
        'plow.json',
        'summer_walker.json',
        'hurricane.json'
    ]

    packs = ['enchantments_encore', 'expedition_enchantments_control']

    for pack in packs:
        for fname in files_to_fix:
            fpath = os.path.join('datapacks', pack, 'data', 'enchantencore', 'enchantment', fname)
            if not os.path.exists(fpath):
                continue
            with open(fpath, 'r', encoding='utf-8') as f:
                content = f.read()

            content = content.replace('"minecraft:entity_type": "minecraft:all_of"', '"type": "minecraft:all_of"')

            if fname == 'hurricane.json':
                # Parse as JSON to be 100% robust
                try:
                    d = json.loads(content)
                    hit_blocks = d.get('effects', {}).get('minecraft:hit_block', [])
                    for hb in hit_blocks:
                        req = hb.get('requirements', {})
                        pred = req.get('predicate', {})
                        if 'minecraft:entity_type' in pred and isinstance(pred['minecraft:entity_type'], list):
                            pred['minecraft:entity_type'] = '#enchantencore:hard_projectiles'
                    content = json.dumps(d, indent=2)
                except Exception as e:
                    print(f"Error parsing hurricane: {e}")

            with open(fpath, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f'Fixed {fpath}')

def fix_seiors_oblivion():
    for sub in ['feature', 'configured_feature']:
        p = os.path.join('datapacks', 'NeoEnchant-5.16.0', 'data', 'enchantplus', 'worldgen', sub, 'seiors_oblivion.json')
        if os.path.exists(p):
            with open(p, 'r', encoding='utf-8') as f:
                data = json.load(f)
            for feat in data.get('features', []):
                new_placement = []
                for pl in feat.get('placement', []):
                    if pl.get('type') == 'minecraft:random_offset':
                        xz = pl.get('xz_spread', {'type': 'minecraft:trapezoid', 'min': -7, 'max': 7, 'plateau': 0})
                        y = pl.get('y_spread', {'type': 'minecraft:trapezoid', 'min': -3, 'max': 3, 'plateau': 0})
                        new_placement.append({
                            'type': 'minecraft:offset',
                            'x': xz,
                            'y': y,
                            'z': xz
                        })
                    else:
                        new_placement.append(pl)
                feat['placement'] = new_placement
            with open(p, 'w', encoding='utf-8') as f:
                json.dump(data, f, indent=2)
            print(f'Fixed {p}')

if __name__ == '__main__':
    fix_enchants()
    fix_seiors_oblivion()
