"""
Export Enchantments Data for Web Wiki (assets/js/enchants_data.js)
Delegates to the master verified English builder scripts/build_english_database.py
to ensure 100% accurate metadata, complete per-level scaling descriptions,
proper equipment categories, and exact max levels.
"""

import os
import subprocess
import sys

def main():
    script_dir = os.path.dirname(os.path.abspath(__file__))
    master_builder = os.path.join(script_dir, "build_english_database.py")
    
    print("[export] Running master English database builder...")
    res = subprocess.run([sys.executable, master_builder], cwd=os.path.dirname(script_dir))
    if res.returncode == 0:
        print("[export] Database successfully built and verified!")
    else:
        print(f"[export] Master builder failed with code {res.returncode}")
        sys.exit(res.returncode)

if __name__ == "__main__":
    main()
