"""Opaque-box validator for config/expedition-dragon.json."""

from __future__ import annotations

import json
import math
from pathlib import Path
from typing import Any

WORKSPACE_CONFIG = Path(r"d:\Minecraft-One-More-Expedition\config\expedition-dragon.json")
ALLAYOPS_CONFIG = Path(r"D:\AllayOps\config\expedition-dragon.json")

CANONICAL_VARIANTS = {
    "infernal": "RED",
    "voidgloom": "PURPLE",
    "revenant": "GREEN",
    "enlightened": "YELLOW",
    "chronos": "BLUE",
}


def _extract_variants_map(data: dict[str, Any]) -> dict[str, dict[str, Any]]:
    raw = data.get("variants")
    if isinstance(raw, dict):
        return {str(k).strip().lower(): v for k, v in raw.items() if isinstance(v, dict)}
    if isinstance(raw, list):
        out: dict[str, dict[str, Any]] = {}
        for item in raw:
            if isinstance(item, dict) and ("id" in item or "name" in item or "variant" in item):
                key = str(item.get("id") or item.get("name") or item.get("variant")).strip().lower()
                out[key] = item
        return out
    return {}


def _is_finite_number(val: Any) -> bool:
    return isinstance(val, (int, float)) and not isinstance(val, bool) and math.isfinite(val)


def validate_config_dict(data: dict[str, Any], require_default_tuning: bool = False) -> list[str]:
    errors: list[str] = []
    if not isinstance(data, dict):
        return ["Config root must be a JSON object"]

    variants = _extract_variants_map(data)
    if not variants:
        errors.append("Missing 'variants' section in config/expedition-dragon.json")
        return errors

    total_weight = 0.0
    for name, expected_color in CANONICAL_VARIANTS.items():
        if name not in variants:
            errors.append(f"Missing variant config for '{name}'")
            continue
        v = variants[name]
        weight = v.get("weight")
        if not _is_finite_number(weight) or weight < 0:
            errors.append(f"Variant '{name}' invalid weight: {weight!r}")
        else:
            total_weight += float(weight)
            if require_default_tuning and abs(float(weight) - 0.20) > 1e-6:
                errors.append(f"Variant '{name}' default weight expected 0.20, got {weight}")

        hp = v.get("baseHp", v.get("base_hp"))
        if not _is_finite_number(hp) or hp < 1000 or hp > 2000:
            errors.append(f"Variant '{name}' baseHp expected in [1000, 2000], got {hp!r}")

        for mult_key in ("damageMultiplier", "damage_multiplier"):
            if mult_key in v and (not _is_finite_number(v[mult_key]) or v[mult_key] <= 0):
                errors.append(f"Variant '{name}' {mult_key} must be finite > 0, got {v[mult_key]!r}")
        for mult_key in ("cooldownMultiplier", "cooldown_multiplier"):
            if mult_key in v and (not _is_finite_number(v[mult_key]) or v[mult_key] <= 0):
                errors.append(f"Variant '{name}' {mult_key} must be finite > 0, got {v[mult_key]!r}")

        color = v.get("bossBarColor", v.get("boss_bar_color", v.get("color")))
        if color is not None and str(color).strip().upper() != expected_color:
            errors.append(f"Variant '{name}' color expected {expected_color}, got {color!r}")

    if not math.isfinite(total_weight) or total_weight <= 0.0:
        errors.append(f"Total variant weights must be > 0, got {total_weight}")
    elif require_default_tuning and abs(total_weight - 1.0) > 1e-5:
        errors.append(f"Default variant weights must sum to 1.0, got {total_weight}")

    rewards_key = next((k for k in ("rewards", "rewardConfig", "reward_config", "rewardTables") if k in data), None)
    if rewards_key is None:
        errors.append("Missing reward table configuration ('rewards' / 'rewardConfig')")
    else:
        rewards_obj = data[rewards_key]
        if not isinstance(rewards_obj, dict) or len(rewards_obj) == 0:
            errors.append(f"Reward configuration '{rewards_key}' must be a non-empty object")
        else:
            pct_key = next((k for k in ("minDamagePercent", "min_damage_percent") if k in rewards_obj), None)
            if pct_key is None:
                errors.append(f"Missing minDamagePercent in '{rewards_key}'")
            else:
                pct = rewards_obj[pct_key]
                if not _is_finite_number(pct) or pct <= 0 or pct > 1.0:
                    errors.append(f"'{rewards_key}.{pct_key}' must be finite in (0, 1], got {pct!r}")

            flat_key = next((k for k in ("minDamageFlat", "min_damage_flat") if k in rewards_obj), None)
            if flat_key is None:
                errors.append(f"Missing minDamageFlat in '{rewards_key}'")
            else:
                flat = rewards_obj[flat_key]
                if not _is_finite_number(flat) or flat < 0:
                    errors.append(f"'{rewards_key}.{flat_key}' must be finite >= 0, got {flat!r}")

    cooldowns_key = next(
        (k for k in ("cooldowns", "abilities", "moveset", "combat", "diveCooldownTicks") if k in data),
        None,
    )
    if cooldowns_key is None:
        errors.append("Missing ability/moveset cooldown configuration")
    else:
        cd_obj = data[cooldowns_key]
        if isinstance(cd_obj, dict):
            if len(cd_obj) == 0:
                errors.append(f"Cooldown/ability configuration '{cooldowns_key}' must not be empty")
            else:
                for ck, cv in cd_obj.items():
                    if isinstance(cv, bool) and (
                        cooldowns_key == "cooldowns"
                        or any(tok in str(ck).lower() for tok in ("tick", "cooldown", "delay", "interval", "duration"))
                    ):
                        errors.append(f"'{cooldowns_key}.{ck}' must be a number, got boolean {cv!r}")
                    elif isinstance(cv, float) and not math.isfinite(cv):
                        errors.append(f"'{cooldowns_key}.{ck}' must be finite, got {cv!r}")
        elif not _is_finite_number(cd_obj) or cd_obj <= 0:
            errors.append(f"'{cooldowns_key}' must be a non-empty object or finite positive number, got {cd_obj!r}")

    return errors


def inspect_config_file(path: Path, require_default_tuning: bool = False) -> dict[str, Any]:
    if not path.is_file():
        return {"ok": False, "path": str(path), "errors": [f"Config file missing: {path}"]}
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return {"ok": False, "path": str(path), "errors": [f"Invalid JSON in {path}: {exc}"]}

    errors = validate_config_dict(data, require_default_tuning=require_default_tuning)
    return {"ok": len(errors) == 0, "path": str(path), "data": data, "errors": errors}
