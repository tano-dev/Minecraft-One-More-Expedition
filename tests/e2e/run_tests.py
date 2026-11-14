#!/usr/bin/env python3
"""
Expedition Skyblock 4-Tier E2E Test Suite Runner.
Executes all E2E test suites (Tiers 1-4) or filtered tiers, collects assertion metrics,
displays structured summary reports, and exits with code 0 on complete pass.
"""
import argparse
import sys
import time
from typing import List, Dict, Any

# Ensure tests root is importable
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))))

from tests.e2e.contract.test_framework import registry, TestContext, TestCase
import tests.e2e.test_tier1_features
import tests.e2e.test_tier2_boundaries
import tests.e2e.test_tier3_cross_features
import tests.e2e.test_tier4_scenarios


def parse_args():
    parser = argparse.ArgumentParser(description="Run expedition-skyblock E2E test suites.")
    parser.add_argument(
        "--tier",
        type=str,
        default=None,
        help="Filter by tier: '1', '2', '3', '4' or 'Tier 1', 'Tier 2', etc."
    )
    parser.add_argument(
        "--verbose", "-v",
        action="store_true",
        help="Display verbose assertion output."
    )
    parser.add_argument(
        "--json-output",
        type=str,
        default=None,
        help="Write test results to a JSON file."
    )
    return parser.parse_args()


def main():
    args = parse_args()

    tier_filter = args.tier
    if tier_filter:
        if not tier_filter.lower().startswith("tier"):
            tier_filter = f"Tier {tier_filter}"

    all_cases: List[TestCase] = registry.test_cases
    if tier_filter:
        selected_cases = [tc for tc in all_cases if tc.tier.lower() == tier_filter.lower()]
    else:
        selected_cases = all_cases

    if not selected_cases:
        print(f"[ERROR] No tests found matching tier filter: {tier_filter}")
        sys.exit(1)

    print("=" * 80)
    print(" EXPEDITION SKYBLOCK 4-TIER E2E TEST RUNNER")
    print(" Target: Minecraft 26.3 Fabric Loader 0.20.1 | Java 25")
    print("=" * 80)
    print(f"Total Test Cases Loaded: {len(selected_cases)}")
    if tier_filter:
        print(f"Filter Applied: {tier_filter}")
    print("-" * 80)

    results: List[TestContext] = []
    total_start = time.perf_counter()

    passed_count = 0
    failed_count = 0
    total_assertions = 0

    for idx, tc in enumerate(selected_cases, 1):
        ctx = tc.run()
        results.append(ctx)
        total_assertions += ctx.assertions_count

        status_tag = "[PASS]" if ctx.passed else "[FAIL]"
        print(f"{status_tag} ({idx:2d}/{len(selected_cases):2d}) [{ctx.tier}] {tc.name}: {tc.description} ({ctx.assertions_count} assertions, {ctx.duration_seconds*1000:.1f}ms)")

        if ctx.passed:
            passed_count += 1
        else:
            failed_count += 1
            print(f"  >>> FAILED: {tc.name}")
            for err in ctx.errors:
                for line in err.strip().split("\n"):
                    print(f"      {line}")

    total_duration = time.perf_counter() - total_start

    print("=" * 80)
    print(" E2E TEST EXECUTION SUMMARY")
    print("=" * 80)
    print(f"Total Suites Run:      {len(results)}")
    print(f"Passed:                {passed_count}")
    print(f"Failed:                {failed_count}")
    print(f"Total Assertions:      {total_assertions}")
    print(f"Total Execution Time:  {total_duration*1000:.2f} ms")
    print("-" * 80)

    # Breakdown by Tier
    tiers = ["Tier 1", "Tier 2", "Tier 3", "Tier 4"]
    for t in tiers:
        t_results = [r for r in results if r.tier.lower() == t.lower()]
        if t_results:
            t_passed = sum(1 for r in t_results if r.passed)
            t_assertions = sum(r.assertions_count for r in t_results)
            print(f"  {t:7s}: {t_passed}/{len(t_results)} passed ({t_assertions:3d} assertions)")

    print("=" * 80)

    if args.json_output:
        import json
        summary_data = {
            "total": len(results),
            "passed": passed_count,
            "failed": failed_count,
            "total_assertions": total_assertions,
            "duration_ms": total_duration * 1000,
            "tests": [
                {
                    "name": r.name,
                    "tier": r.tier,
                    "description": r.description,
                    "passed": r.passed,
                    "assertions": r.assertions_count,
                    "duration_ms": r.duration_seconds * 1000,
                    "errors": r.errors
                }
                for r in results
            ]
        }
        with open(args.json_output, "w", encoding="utf-8") as f:
            json.dump(summary_data, f, indent=2)
        print(f"JSON results saved to: {args.json_output}")

    if failed_count > 0:
        print("[FATAL] E2E TEST SUITE RUN COMPLETED WITH FAILURES.")
        sys.exit(1)
    else:
        print("[SUCCESS] ALL 4 TIERS PASSED WITH 100% SUCCESS RATE.")
        sys.exit(0)


if __name__ == "__main__":
    main()
