#!/usr/bin/env python3
"""
Local Wiki Editor & Datapack Sync Server for Minecraft: One More Expedition.

Usage:
  python scripts/wiki_editor_server.py           # Starts http://localhost:8765/enchants.html
  python scripts/wiki_editor_server.py --apply patch.json  # Applies an offline exported patch
"""

import argparse
import http.server
import json
import os
import socketserver
import sys
import webbrowser

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if REPO_ROOT not in sys.path:
    sys.path.insert(0, REPO_ROOT)
SCRIPTS_DIR = os.path.dirname(os.path.abspath(__file__))
if SCRIPTS_DIR not in sys.path:
    sys.path.insert(0, SCRIPTS_DIR)

import build_english_database
import enchant_manager

CONFIG_PATH = os.path.join(REPO_ROOT, "enchantments_config.json")
RULES_PATH = os.path.join(REPO_ROOT, "custom_enchant_rules.json")
DATA_JS_PATH = os.path.join(REPO_ROOT, "assets", "js", "enchants_data.js")


def _load_current_wiki_groups():
    """Return {eid: [group_names]} from current assets/js/enchants_data.js."""
    if not os.path.exists(DATA_JS_PATH):
        return {}
    with open(DATA_JS_PATH, "r", encoding="utf-8") as f:
        raw = f.read()
    prefix = "window.ALL_ENCHANTMENTS = "
    if prefix not in raw:
        return {}
    payload = raw.split(prefix, 1)[1].rstrip(";\n\r ")
    items = json.loads(payload)
    return {x["id"]: x.get("exclusive_groups", []) for x in items}


def apply_changes(changes: dict) -> dict:
    """Apply Web Editor changes to enchantments_config.json & custom_enchant_rules.json, then rebuild."""
    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        cfg = json.load(f)
    with open(RULES_PATH, "r", encoding="utf-8") as f:
        rules = json.load(f)

    em = cfg["enchantments"]
    group_to_tag = rules.get("group_to_tag", {})
    overrides = rules.setdefault("enchant_overrides", {})
    prev_groups_map = _load_current_wiki_groups()

    updated = 0
    for eid, patch in changes.items():
        if eid not in em or not isinstance(patch, dict):
            continue
        entry = em[eid]

        if "enabled" in patch:
            entry["enabled"] = bool(patch["enabled"])
            if not entry["enabled"]:
                entry["tier"] = "disabled_duplicate"
                entry["table"] = False
                entry["tradeable"] = False

        if "tier" in patch and entry.get("enabled", True):
            tier = patch["tier"]
            if tier == "disabled":
                entry["enabled"] = False
                entry["tier"] = "disabled_duplicate"
                entry["table"] = False
                entry["tradeable"] = False
            elif tier in ("common", "rare", "epic", "ultimate", "curse"):
                entry["enabled"] = True
                entry["tier"] = tier
                entry["curse"] = (tier == "curse")
                if tier not in ("common", "rare"):
                    entry["table"] = False

        if "table" in patch and entry.get("enabled", True):
            entry["table"] = bool(patch["table"]) and entry.get("tier") in ("common", "rare")
        if "tradeable" in patch and entry.get("enabled", True):
            entry["tradeable"] = bool(patch["tradeable"])
        if "loot" in patch and entry.get("enabled", True):
            entry["loot"] = bool(patch["loot"])
        if "table_weight" in patch:
            try:
                w = int(patch["table_weight"])
                entry["table_weight"] = max(1, min(100, w))
            except (TypeError, ValueError):
                pass

        if "exclusive_groups" in patch:
            new_groups = [g for g in patch["exclusive_groups"] if g in group_to_tag]
            old_groups = prev_groups_map.get(eid, [])
            ov = overrides.setdefault(eid, {})

            removed_tags = set(ov.get("remove_from_tags", []))
            for g in old_groups:
                if g not in new_groups and g in group_to_tag:
                    removed_tags.add(group_to_tag[g])
            for g in new_groups:
                if g in group_to_tag and group_to_tag[g] in removed_tags:
                    removed_tags.remove(group_to_tag[g])

            if removed_tags:
                ov["remove_from_tags"] = sorted(removed_tags)
            else:
                ov.pop("remove_from_tags", None)

            new_tags = [group_to_tag[g] for g in new_groups]
            if not new_tags:
                ov["exclusive_set"] = None
            elif len(new_tags) == 1:
                ov["exclusive_set"] = new_tags[0]
            else:
                ov["exclusive_set"] = new_tags

        updated += 1

    with open(CONFIG_PATH, "w", encoding="utf-8") as f:
        json.dump(cfg, f, indent=2, ensure_ascii=False)
    with open(RULES_PATH, "w", encoding="utf-8") as f:
        json.dump(rules, f, indent=2, ensure_ascii=False)

    # Rebuild datapack + catalog + web database
    enchant_manager.cmd_build()
    build_english_database.build_all()

    with open(CONFIG_PATH, "r", encoding="utf-8") as f:
        fresh_cfg = json.load(f)
    stats = enchant_manager._compute_table_probabilities(fresh_cfg["enchantments"])
    return {"ok": True, "updated": updated, "stats": stats}


class WikiEditorHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=REPO_ROOT, **kwargs)

    def end_headers(self):
        self.send_header("Cache-Control", "no-store, no-cache, must-revalidate")
        super().end_headers()

    def _send_json(self, code: int, payload: dict):
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        if self.path.split("?")[0] == "/api/health":
            self._send_json(200, {"ok": True, "mode": "local-sync"})
            return
        if self.path.split("?")[0] == "/api/stats":
            with open(CONFIG_PATH, "r", encoding="utf-8") as f:
                cfg = json.load(f)
            stats = enchant_manager._compute_table_probabilities(cfg["enchantments"])
            self._send_json(200, {"ok": True, "stats": stats})
            return
        super().do_GET()

    def do_POST(self):
        if self.path.split("?")[0] == "/api/sync":
            try:
                length = int(self.headers.get("Content-Length", "0"))
                raw = self.rfile.read(length)
                data = json.loads(raw.decode("utf-8"))
                res = apply_changes(data.get("changes", {}))
                self._send_json(200, res)
            except Exception as exc:
                self._send_json(500, {"ok": False, "error": str(exc)})
            return
        self._send_json(404, {"ok": False, "error": "Not found"})


def main():
    parser = argparse.ArgumentParser(description="Wiki Editor & Datapack Sync Server")
    parser.add_argument("--port", type=int, default=8765)
    parser.add_argument("--no-open", action="store_true")
    parser.add_argument("--apply", metavar="PATCH_JSON", help="Apply an exported patch JSON and exit")
    args = parser.parse_args()

    if args.apply:
        with open(args.apply, "r", encoding="utf-8") as f:
            patch = json.load(f)
        res = apply_changes(patch.get("changes", patch))
        print(json.dumps(res, indent=2, ensure_ascii=False))
        return

    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("127.0.0.1", args.port), WikiEditorHandler) as httpd:
        url = f"http://127.0.0.1:{args.port}/enchants.html"
        print(f"[wiki-editor] Running at {url}")
        print("[wiki-editor] Press Ctrl+C to stop.")
        if not args.no_open:
            webbrowser.open(url)
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\n[wiki-editor] Stopped.")


if __name__ == "__main__":
    main()
