"""Deterministic CLI runner for the expedition-dragon opaque-box E2E test suite."""

from __future__ import annotations

import argparse
import importlib
import json
import linecache
import re
import sys
import unittest
from pathlib import Path
from typing import Any

SUITE_ROOT = Path(__file__).resolve().parent
if str(SUITE_ROOT) not in sys.path:
    sys.path.insert(0, str(SUITE_ROOT))

from harness_py import java_runner, self_check  # noqa: E402

REGISTRY_PATH = SUITE_ROOT / "registry.json"
CANONICAL_ID_RE = re.compile(
    r"(?<![A-Za-z0-9])(T[12][-_]F\d{2}[-_]\d{2}|T[34][-_]\d{2})(?![A-Za-z0-9])",
    re.IGNORECASE,
)


def load_registry() -> dict[str, Any]:
    return json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))


def normalize_feature(feature: str) -> str:
    m = re.fullmatch(r"[Ff]0*(\d+)", feature.strip())
    if not m:
        raise ValueError(f"Invalid --feature {feature!r}: expected F1..F22")
    num = int(m.group(1))
    if not (1 <= num <= 22):
        raise ValueError(f"Invalid --feature {feature!r}: expected F1..F22")
    return f"F{num}"


def extract_canonical_ids(text: str) -> list[str]:
    return [m.upper().replace("_", "-") for m in CANONICAL_ID_RE.findall(text)]


def _camel_to_snake(name: str) -> str:
    return re.sub(r"([a-z])([A-Z])", r"\1_\2", name).lower()


def _resolve_base_test_id(test_obj: Any, all_test_ids: set[str]) -> str:
    base_tc = getattr(test_obj, "test_case", None)
    if base_tc is not None:
        return str(base_tc.id())
    tid = str(test_obj.id())
    if tid in all_test_ids:
        return tid
    for candidate in all_test_ids:
        if tid.startswith(candidate + " "):
            return candidate
    return tid


def _purge_tier_modules(tier_dir: Path, extra_names: set[str] | None = None) -> None:
    resolved_tier = str(tier_dir.resolve()).lower()
    stems = {p.stem for p in tier_dir.rglob("*.py")}
    if extra_names:
        stems.update(extra_names)
    for mod_name in list(sys.modules):
        mod = sys.modules.get(mod_name)
        mod_file = getattr(mod, "__file__", None)
        if mod_file:
            try:
                if str(Path(mod_file).resolve()).lower().startswith(resolved_tier):
                    sys.modules.pop(mod_name, None)
                    continue
            except OSError:
                pass
        if mod_name in stems or any(mod_name.startswith(s + ".") for s in stems):
            sys.modules.pop(mod_name, None)
    importlib.invalidate_caches()
    linecache.clearcache()


def discover_python_tier(tier_num: int, suite_root: Path | None = None) -> list[dict[str, Any]]:
    root = suite_root or SUITE_ROOT
    tier_dir = root / f"tier{tier_num}"
    if not tier_dir.is_dir() or not any(tier_dir.rglob("test_*.py")):
        return []

    tier_dir_str = str(tier_dir)
    saved_sys_path = list(sys.path)
    before_modules = set(sys.modules.keys())
    _purge_tier_modules(tier_dir)
    if tier_dir_str not in sys.path:
        sys.path.insert(0, tier_dir_str)

    try:
        loader = unittest.TestLoader()
        try:
            suite = loader.discover(start_dir=tier_dir_str, pattern="test_*.py", top_level_dir=tier_dir_str)
        except Exception as exc:
            return [{
                "id": f"python_tier{tier_num}_discovery",
                "tier": tier_num,
                "engine": "python",
                "status": "failed",
                "message": f"Python test discovery failed: {exc}",
            }]

        all_tests: list[unittest.TestCase] = []

        def _collect(s: unittest.TestSuite | unittest.TestCase | None) -> None:
            if s is None:
                return
            if isinstance(s, unittest.TestSuite):
                for item in s:
                    _collect(item)
            else:
                all_tests.append(s)

        # Collect BEFORE runner.run(suite) because TestSuite.run() clears executed slots to None (_cleanup=True).
        _collect(suite)
        all_test_ids = {tc.id() for tc in all_tests}

        runner = unittest.TextTestRunner(verbosity=1)
        result = runner.run(suite)

        _ERROR_HOLDER_RE = re.compile(
            r"^(?:setUpClass|tearDownClass|setUpModule|tearDownModule)\s*\(([^)]+)\)$"
        )

        failed_msgs: dict[str, list[str]] = {}
        extra_failures: list[tuple[str, str]] = []
        for t, tb in result.failures + result.errors:
            base_id = _resolve_base_test_id(t, all_test_ids)
            lines = [ln.strip() for ln in tb.splitlines() if ln.strip()]
            msg = lines[-1] if lines else "Test failed"
            if base_id in all_test_ids:
                failed_msgs.setdefault(base_id, []).append(msg)
            else:
                raw_id = str(t.id())
                holder_match = _ERROR_HOLDER_RE.match(raw_id)
                if holder_match:
                    scope = holder_match.group(1).strip()
                    matched = [
                        tid for tid in all_test_ids
                        if tid == scope or tid.startswith(scope + ".")
                    ]
                    if matched:
                        for tid in matched:
                            failed_msgs.setdefault(tid, []).append(f"{raw_id}: {msg}")
                        continue
                extra_failures.append((raw_id, msg))

        skipped_msgs: dict[str, str] = {}
        for t, reason in result.skipped:
            base_id = _resolve_base_test_id(t, all_test_ids)
            if base_id in all_test_ids:
                skipped_msgs[base_id] = f"Skipped: {reason}"
            else:
                raw_id = str(t.id())
                holder_match = _ERROR_HOLDER_RE.match(raw_id)
                if holder_match:
                    scope = holder_match.group(1).strip()
                    matched = [
                        tid for tid in all_test_ids
                        if tid == scope or tid.startswith(scope + ".")
                    ]
                    if matched:
                        for tid in matched:
                            skipped_msgs.setdefault(tid, f"Skipped ({raw_id}): {reason}")
                        continue
                extra_failures.append((raw_id, f"Skipped: {reason}"))

        out: list[dict[str, Any]] = []
        seen_ids: set[str] = set()
        for tc in all_tests:
            tid = tc.id()
            if tid in seen_ids:
                continue
            seen_ids.add(tid)
            if tid in failed_msgs:
                out.append({
                    "id": tid,
                    "tier": tier_num,
                    "engine": "python",
                    "status": "failed",
                    "message": "; ".join(failed_msgs[tid]),
                })
            elif tid in skipped_msgs:
                out.append({
                    "id": tid,
                    "tier": tier_num,
                    "engine": "python",
                    "status": "failed",
                    "message": skipped_msgs[tid],
                })
            else:
                out.append({
                    "id": tid,
                    "tier": tier_num,
                    "engine": "python",
                    "status": "passed",
                    "message": "",
                })

        for extra_id, msg in extra_failures:
            out.append({
                "id": extra_id,
                "tier": tier_num,
                "engine": "python",
                "status": "failed",
                "message": msg,
            })
        return out
    finally:
        newly_loaded = set(sys.modules.keys()) - before_modules
        _purge_tier_modules(tier_dir, extra_names=newly_loaded)
        sys.path[:] = saved_sys_path


def _select_gametest_filter_and_prefixes(gametest_tier_dir: Path, tier_num: int) -> tuple[str, list[str]]:
    class_names: list[str] = []
    gametest_methods: list[str] = []
    for java_file in sorted(gametest_tier_dir.rglob("*.java")):
        src = java_file.read_text(encoding="utf-8", errors="replace")
        class_names.extend(
            re.findall(r"\b(?:public\s+)?(?:final\s+|abstract\s+)?class\s+([A-Za-z_][A-Za-z0-9_]*)", src)
        )
        gametest_methods.extend(
            re.findall(
                r"@GameTest\b[^;{}]*?\bpublic\s+void\s+([A-Za-z_][A-Za-z0-9_]*)\s*\(",
                src,
                flags=re.DOTALL,
            )
        )

    prefixes = [f"expedition-dragon-e2e-gametest:{_camel_to_snake(c)}_" for c in class_names]
    if len(class_names) == 1:
        gt_filter = f"expedition-dragon-e2e-gametest:{_camel_to_snake(class_names[0])}_*"
    elif class_names and all(c.startswith(f"Tier{tier_num}") for c in class_names):
        gt_filter = f"expedition-dragon-e2e-gametest:tier{tier_num}*"
    elif gametest_methods and all(m.lower().startswith(f"t{tier_num}_") for m in gametest_methods):
        gt_filter = f"expedition-dragon-e2e-gametest:*_t{tier_num}_*"
    else:
        gt_filter = "expedition-dragon-e2e-gametest:*"
    return gt_filter, prefixes


def discover_java_tier(tier_num: int, suite_root: Path | None = None) -> list[dict[str, Any]]:
    root = suite_root or SUITE_ROOT
    out: list[dict[str, Any]] = []

    java_tier_dir = root / "harness-java" / "src" / "test" / "java" / "net" / "onemoreexpedition" / "e2e" / f"tier{tier_num}"
    if java_tier_dir.is_dir() and any(java_tier_dir.rglob("*.java")):
        res = java_runner.run_junit_tag(f"tier{tier_num}")
        junit_out: list[dict[str, Any]] = []
        for case in res["cases"]:
            junit_out.append({
                "id": f"{case.classname}#{case.name}",
                "tier": tier_num,
                "engine": "java",
                "status": "passed" if case.status == "passed" else "failed",
                "message": case.message,
            })
        if res["returncode"] != 0 and not junit_out:
            junit_out.append({
                "id": f"java_tier{tier_num}_runner",
                "tier": tier_num,
                "engine": "java",
                "status": "failed",
                "message": f"Gradle JUnit tier{tier_num} exited {res['returncode']} with no XML results",
            })
        out.extend(junit_out)

    gametest_tier_dir = (
        root / "harness-java" / "src" / "gametest" / "java" / "net" / "onemoreexpedition" / "e2e" / f"tier{tier_num}"
    )
    if gametest_tier_dir.is_dir() and any(gametest_tier_dir.rglob("*.java")):
        gt_filter, class_prefixes = _select_gametest_filter_and_prefixes(gametest_tier_dir, tier_num)
        gt_res = java_runner.run_gametest(test_filter=gt_filter)
        gt_out: list[dict[str, Any]] = []
        for case in gt_res["cases"]:
            if case.name == "minecraft:always_pass":
                continue
            keep = case.name == "GameTestServerBoot"
            if not keep and class_prefixes:
                keep = any(case.name.startswith(p) for p in class_prefixes)
            elif not keep:
                low = case.name.lower()
                keep = bool(
                    re.search(rf"(?:^|[:_])t{tier_num}_", low)
                    or re.search(rf"(?:^|:)tier{tier_num}(?![0-9])", low)
                )
            if keep:
                gt_out.append({
                    "id": case.name,
                    "tier": tier_num,
                    "engine": "gametest",
                    "status": "passed" if case.status == "passed" else "failed",
                    "message": case.message,
                })
        if gt_res["returncode"] != 0 and not gt_out:
            gt_out.append({
                "id": f"gametest_tier{tier_num}_runner",
                "tier": tier_num,
                "engine": "gametest",
                "status": "failed",
                "message": f"Gradle runGameTest tier{tier_num} exited {gt_res['returncode']} with no matching XML results",
            })
        out.extend(gt_out)

    return out


def _entry_features(entry: dict[str, Any]) -> set[str]:
    feats: set[str] = set()
    if entry.get("feature"):
        feats.add(str(entry["feature"]).strip().upper())
    raw_list = entry.get("features")
    if isinstance(raw_list, list):
        for f in raw_list:
            if f:
                feats.add(str(f).strip().upper())
    return feats


def _matches_feature_filter(result_item: dict[str, Any], canonical_feature: str, reg_by_id: dict[str, dict[str, Any]]) -> bool:
    cids = extract_canonical_ids(result_item["id"])
    matched_reg = [reg_by_id[cid] for cid in cids if cid in reg_by_id]
    if matched_reg:
        return any(canonical_feature in _entry_features(entry) for entry in matched_reg)
    feat_num = int(canonical_feature[1:])
    feat_pattern = rf"F0?{feat_num}" if feat_num < 10 else rf"F{feat_num}"
    feat_token_re = re.compile(rf"(?<![A-Za-z0-9]){feat_pattern}(?![A-Za-z0-9])", re.IGNORECASE)
    return bool(feat_token_re.search(result_item["id"]))


def run_tiers(
    tiers: list[int],
    feature_filter: str | None = None,
    require_full_registry: bool = False,
    suite_root: Path | None = None,
) -> dict[str, Any]:
    reg = load_registry()
    all_reg_tests = reg.get("tests", [])
    reg_by_id = {t["id"]: t for t in all_reg_tests}
    reg_tests = all_reg_tests

    canonical_feature: str | None = None
    if feature_filter:
        canonical_feature = normalize_feature(feature_filter)
        reg_tests = [t for t in all_reg_tests if canonical_feature in _entry_features(t)]

    summary: dict[str, Any] = {
        "tier1": {"total": 0, "passed": 0, "failed": 0, "errors": 0},
        "tier2": {"total": 0, "passed": 0, "failed": 0, "errors": 0},
        "tier3": {"total": 0, "passed": 0, "failed": 0, "errors": 0},
        "tier4": {"total": 0, "passed": 0, "failed": 0, "errors": 0},
        "total": 0,
        "passed": 0,
        "failed": 0,
        "errors": 0,
        "results": [],
    }

    executed_results: list[dict[str, Any]] = []
    for t in tiers:
        executed_results.extend(discover_python_tier(t, suite_root=suite_root))
        executed_results.extend(discover_java_tier(t, suite_root=suite_root))

    if canonical_feature:
        executed_results = [
            r for r in executed_results
            if _matches_feature_filter(r, canonical_feature, reg_by_id)
        ]

    for r in executed_results:
        t_key = f"tier{r['tier']}"
        summary[t_key]["total"] += 1
        summary["total"] += 1
        if r["status"] == "passed":
            summary[t_key]["passed"] += 1
            summary["passed"] += 1
        else:
            summary[t_key]["failed"] += 1
            summary["failed"] += 1

    if require_full_registry:
        executed_id_set: set[str] = set()
        for r in executed_results:
            executed_id_set.update(extract_canonical_ids(r["id"]))
        missing = [
            t["id"] for t in reg_tests
            if t["tier"] in tiers and t["id"] not in executed_id_set
        ]
        for mid in missing:
            tier_val = reg_by_id[mid]["tier"]
            t_key = f"tier{tier_val}"
            summary[t_key]["total"] += 1
            summary[t_key]["failed"] += 1
            summary["total"] += 1
            summary["failed"] += 1
            executed_results.append({
                "id": mid,
                "tier": tier_val,
                "engine": "registry",
                "status": "failed",
                "message": "Planned registry test ID not yet implemented/executed",
            })

    summary["results"] = executed_results
    summary["registry_planned_total"] = len(reg_tests)
    return summary


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="expedition-dragon Opaque-Box E2E Test Runner")
    parser.add_argument("--tier", choices=["1", "2", "3", "4", "all"], default="all")
    parser.add_argument("--feature", help="Optional feature filter (F1..F22)")
    parser.add_argument("--json-out", help="Optional path to write structured JSON report")
    parser.add_argument("--self-check", action="store_true", help="Run built-in harness self-verification suite")
    parser.add_argument("--require-full-registry", action="store_true", help="Fail if any planned registry.json test ID did not execute")
    args = parser.parse_args(argv)

    if args.feature is not None:
        try:
            args.feature = normalize_feature(args.feature)
        except ValueError as exc:
            parser.error(str(exc))

    if args.self_check:
        report = self_check.run_self_check()
        if args.json_out:
            out_path = Path(args.json_out)
            out_path.parent.mkdir(parents=True, exist_ok=True)
            out_path.write_text(json.dumps(report, indent=2), encoding="utf-8")
        print(json.dumps(report, indent=2))
        return 0 if report["ok"] else 1

    tiers = [1, 2, 3, 4] if args.tier == "all" else [int(args.tier)]
    report = run_tiers(tiers, feature_filter=args.feature, require_full_registry=args.require_full_registry)
    if args.json_out:
        out_path = Path(args.json_out)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_text(json.dumps(report, indent=2), encoding="utf-8")

    print(
        f"E2E Summary: total={report['total']} passed={report['passed']} "
        f"failed={report['failed']} errors={report['errors']} "
        f"(planned_registry={report['registry_planned_total']})"
    )
    return 0 if (report["failed"] == 0 and report["errors"] == 0) else 1


if __name__ == "__main__":
    raise SystemExit(main())
