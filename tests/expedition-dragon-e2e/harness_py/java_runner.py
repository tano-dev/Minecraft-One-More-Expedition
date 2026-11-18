"""Gradle JUnit 5 & Fabric GameTest runner + JUnit XML parser for harness-java/."""

from __future__ import annotations

import math
import os
import subprocess
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path
from typing import Any

HARNESS_JAVA_DIR = Path(__file__).resolve().parent.parent / "harness-java"


@dataclass
class JavaCaseResult:
    name: str
    classname: str
    status: str  # "passed" | "failed" | "error" | "skipped"
    time_seconds: float
    message: str = ""
    details: str = ""


def parse_junit_xml_dir(xml_dir: Path) -> list[JavaCaseResult]:
    results: list[JavaCaseResult] = []
    if not xml_dir.is_dir():
        return results
    for xml_file in sorted(xml_dir.glob("TEST-*.xml")):
        results.extend(parse_junit_xml_file(xml_file))
    return results


def parse_junit_xml_file(xml_file: Path) -> list[JavaCaseResult]:
    results: list[JavaCaseResult] = []
    if not xml_file.is_file():
        return results
    try:
        tree = ET.parse(xml_file)
    except (ET.ParseError, OSError, ValueError) as exc:
        return [
            JavaCaseResult(
                name=xml_file.stem,
                classname="net.onemoreexpedition.e2e.junitxml",
                status="failed",
                time_seconds=0.0,
                message=f"Failed to parse JUnit XML {xml_file.name}: {exc}",
            )
        ]

    root = tree.getroot()
    suites = list(root.iter("testsuite"))
    for suite in suites:
        for tc in suite.findall("testcase"):
            name = tc.attrib.get("name", "<unknown>")
            classname = tc.attrib.get("classname", "")
            raw_time = tc.attrib.get("time", "0") or "0"
            try:
                t = float(raw_time)
                if not math.isfinite(t):
                    raise ValueError(f"Non-finite time {raw_time!r}")
            except ValueError as exc:
                results.append(JavaCaseResult(
                    name=name,
                    classname=classname,
                    status="failed",
                    time_seconds=0.0,
                    message=f"Invalid testcase time attribute {raw_time!r}: {exc}",
                ))
                continue

            failure = tc.find("failure")
            error = tc.find("error")
            skipped = tc.find("skipped")
            if failure is not None:
                results.append(JavaCaseResult(
                    name=name,
                    classname=classname,
                    status="failed",
                    time_seconds=t,
                    message=failure.attrib.get("message", ""),
                    details=(failure.text or "").strip(),
                ))
            elif error is not None:
                # Treat target mod runtime errors as clean test failures unless harness failed
                results.append(JavaCaseResult(
                    name=name,
                    classname=classname,
                    status="failed",
                    time_seconds=t,
                    message=error.attrib.get("message", ""),
                    details=(error.text or "").strip(),
                ))
            elif skipped is not None:
                results.append(JavaCaseResult(
                    name=name,
                    classname=classname,
                    status="skipped",
                    time_seconds=t,
                    message=skipped.attrib.get("message", ""),
                ))
            else:
                results.append(JavaCaseResult(
                    name=name,
                    classname=classname,
                    status="passed",
                    time_seconds=t,
                ))
    return results


def _gradlew_cmd(harness_dir: Path) -> list[str]:
    if os.name == "nt":
        return [str(harness_dir / "gradlew.bat")]
    return [str(harness_dir / "gradlew")]


def run_junit_tag(
    tag: str | None = None,
    harness_dir: Path = HARNESS_JAVA_DIR,
    extra_args: list[str] | None = None,
    timeout: int = 300,
) -> dict[str, Any]:
    xml_dir = harness_dir / "build" / "test-results" / "test"
    if xml_dir.is_dir():
        for old in xml_dir.glob("TEST-*.xml"):
            old.unlink(missing_ok=True)

    cmd = _gradlew_cmd(harness_dir) + ["test", "--no-daemon", "--rerun-tasks"]
    if tag:
        cmd.append(f"-Pe2eTag={tag}")
    if extra_args:
        cmd.extend(extra_args)

    proc = subprocess.run(
        cmd,
        cwd=str(harness_dir),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=timeout,
        check=False,
    )
    cases = parse_junit_xml_dir(xml_dir)
    return {
        "returncode": proc.returncode,
        "cases": cases,
        "stdout": proc.stdout,
        "stderr": proc.stderr,
    }


def run_gametest(
    harness_dir: Path = HARNESS_JAVA_DIR,
    add_mods: str | None = None,
    test_filter: str | None = None,
    report_file: Path | None = None,
    timeout: int = 300,
) -> dict[str, Any]:
    if report_file is None:
        report_file = harness_dir / "build" / "test-results" / "gametest" / "TEST-gametest.xml"
    report_file.parent.mkdir(parents=True, exist_ok=True)
    report_file.unlink(missing_ok=True)

    cmd = _gradlew_cmd(harness_dir) + [
        "runGameTest",
        "--no-daemon",
        f"-Pe2eGameTestReport={report_file}",
    ]
    if add_mods is not None:
        cmd.append(f"-Pe2eAddMods={add_mods}")
    if test_filter:
        cmd.append(f"-Pe2eGameTestFilter={test_filter}")

    proc = subprocess.run(
        cmd,
        cwd=str(harness_dir),
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=timeout,
        check=False,
    )
    cases = parse_junit_xml_file(report_file)
    if proc.returncode != 0 and not cases:
        cases = [
            JavaCaseResult(
                name="GameTestServerBoot",
                classname="net.onemoreexpedition.e2e.gametest",
                status="failed",
                time_seconds=0.0,
                message=f"runGameTest exited {proc.returncode} without writing report XML (possible Mixin/mod startup failure)",
                details=(proc.stdout + "\n" + proc.stderr)[-2000:],
            )
        ]
    return {
        "returncode": proc.returncode,
        "report_file": str(report_file),
        "cases": cases,
        "stdout": proc.stdout,
        "stderr": proc.stderr,
    }
