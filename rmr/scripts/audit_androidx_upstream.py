#!/usr/bin/env python3
"""Audit AndroidX RmR divergence against a named AndroidX upstream reference.

The result is deliberately evidence-oriented: source registration, AAR output,
test execution, behavioural compatibility, optimisation, and legal review are
reported as distinct states.  Missing proof is written as TOKEN_VAZIO.
"""

from __future__ import annotations

import argparse
import datetime as datetime
import fnmatch
import hashlib
import json
import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Any


REPORT_VERSION = 1
REMOTE_NAME = "audit-upstream-androidx"
REF_RE = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._/-]*$")
EXPECTED_MODULES = (
    (":rmr:rmr-core", "rmr/rmr-core"),
    (":rmr:rafaelia", "rmr/rafaelia"),
    (":rmr:rafaelia-core", "rmr/rafaelia-core"),
    (":rmr:rmr-room", "rmr/rmr-room"),
    (":rmr:rmr-navigation", "rmr/rmr-navigation"),
    (":rmr:rmr-lifecycle", "rmr/rmr-lifecycle"),
    (":rmr:rmr-preference", "rmr/rmr-preference"),
)


class AuditError(RuntimeError):
    """An audit input was unavailable, so the result must be TOKEN_VAZIO."""


def run_git(repo: Path, *args: str, check: bool = True) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        ["git", "-C", str(repo), *args],
        check=False,
        capture_output=True,
        text=True,
    )
    if check and result.returncode:
        command = "git " + " ".join(args)
        detail = (result.stderr or result.stdout).strip()
        raise AuditError(f"{command} failed ({result.returncode}): {detail}")
    return result


def git_text(repo: Path, *args: str) -> str:
    return run_git(repo, *args).stdout.strip()


def canonical_json(value: Any) -> str:
    return json.dumps(value, indent=2, sort_keys=True, ensure_ascii=False) + "\n"


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(canonical_json(value), encoding="utf-8")
    os.replace(temporary, path)


def write_text(path: Path, value: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + ".tmp")
    temporary.write_text(value, encoding="utf-8")
    os.replace(temporary, path)


def validate_ref(ref: str) -> None:
    if not REF_RE.fullmatch(ref) or ".." in ref or ref.endswith("/"):
        raise AuditError(f"unsafe upstream ref: {ref!r}")


def ensure_upstream(repo: Path, upstream_url: str, upstream_ref: str, skip_fetch: bool) -> str:
    validate_ref(upstream_ref)
    previous = run_git(repo, "remote", "get-url", REMOTE_NAME, check=False)
    if previous.returncode:
        run_git(repo, "remote", "add", REMOTE_NAME, upstream_url)
    elif previous.stdout.strip() != upstream_url:
        run_git(repo, "remote", "set-url", REMOTE_NAME, upstream_url)

    remote_ref = f"{REMOTE_NAME}/{upstream_ref}"
    if not skip_fetch:
        run_git(
            repo,
            "fetch",
            "--no-tags",
            "--prune",
            "--filter=blob:none",
            REMOTE_NAME,
            f"+refs/heads/{upstream_ref}:refs/remotes/{REMOTE_NAME}/{upstream_ref}",
        )
    git_text(repo, "rev-parse", "--verify", remote_ref)
    return remote_ref


def parse_name_status(raw: str) -> list[dict[str, str]]:
    entries: list[dict[str, str]] = []
    for line in filter(None, raw.splitlines()):
        parts = line.split("\t")
        status = parts[0]
        if status[:1] in {"R", "C"} and len(parts) >= 3:
            entries.append({"status": status, "old_path": parts[1], "path": parts[2]})
        elif len(parts) >= 2:
            entries.append({"status": status, "path": parts[1]})
        else:
            entries.append({"status": status, "path": "TOKEN_VAZIO"})
    return entries


def parse_numstat(raw: str) -> dict[str, dict[str, str]]:
    values: dict[str, dict[str, str]] = {}
    for line in filter(None, raw.splitlines()):
        parts = line.split("\t")
        if len(parts) >= 3:
            values[parts[-1]] = {"additions": parts[0], "deletions": parts[1]}
    return values


def load_policy(path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        raise AuditError(f"boundary policy is missing: {path}")
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        raise AuditError(f"boundary policy is not valid JSON: {error}") from error
    if value.get("version") != 1 or not isinstance(value.get("rules"), list):
        raise AuditError("boundary policy must contain version=1 and a rules list")
    rules: list[dict[str, Any]] = []
    for index, item in enumerate(value["rules"]):
        if not isinstance(item, dict):
            raise AuditError(f"boundary policy rule {index} is not an object")
        pattern = item.get("pattern")
        classification = item.get("classification")
        rationale = item.get("rationale")
        if not all(isinstance(part, str) and part for part in (pattern, classification, rationale)):
            raise AuditError(f"boundary policy rule {index} is incomplete")
        rules.append(item)
    return rules


def classify_path(path: str, rules: list[dict[str, Any]]) -> dict[str, Any]:
    for rule in rules:
        if fnmatch.fnmatchcase(path, rule["pattern"]):
            return {
                "classification": rule["classification"],
                "rationale": rule["rationale"],
                "approved_by_policy": True,
                "review_required": bool(rule.get("review_required", False)),
            }
    return {
        "classification": "unapproved_upstream_surface",
        "rationale": "No checked-in RmR boundary policy rule permits this path.",
        "approved_by_policy": False,
        "review_required": True,
    }


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def count_sources(module_dir: Path) -> dict[str, int]:
    if not module_dir.is_dir():
        return {"java": 0, "kotlin": 0, "cpp": 0, "tests": 0}
    files = [path for path in module_dir.rglob("*") if path.is_file()]
    return {
        "java": sum(path.suffix == ".java" for path in files),
        "kotlin": sum(path.suffix == ".kt" for path in files),
        "cpp": sum(path.suffix in {".c", ".cc", ".cpp", ".cxx", ".h", ".hpp"} for path in files),
        "tests": sum("/src/test/" in path.as_posix() or "/src/androidTest/" in path.as_posix() for path in files),
    }


def module_inventory(repo: Path) -> dict[str, Any]:
    settings_candidates = (repo / "settings.gradle", repo / "settings.gradle.kts")
    settings = "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in settings_candidates if path.is_file())
    modules: list[dict[str, Any]] = []
    for gradle_path, relative_dir in EXPECTED_MODULES:
        module_dir = repo / relative_dir
        build_files = [name for name in ("build.gradle", "build.gradle.kts") if (module_dir / name).is_file()]
        build_text = "\n".join((module_dir / name).read_text(encoding="utf-8", errors="replace") for name in build_files)
        native_declared = "externalNativeBuild" in build_text or any(module_dir.rglob("CMakeLists.txt"))
        registered = gradle_path in settings
        modules.append(
            {
                "gradle_path": gradle_path,
                "directory": relative_dir,
                "directory_present": module_dir.is_dir(),
                "build_files": build_files,
                "registered_in_settings": registered,
                "native_build_declared": native_declared,
                "source_counts": count_sources(module_dir),
                "status": "STATIC_EVIDENCE" if module_dir.is_dir() and build_files and registered else "TOKEN_VAZIO",
            }
        )
    return {"expected_modules": modules}


def artifact_inventory(repo: Path, artifact_root: str | None) -> list[dict[str, Any]]:
    root = (repo / artifact_root).resolve() if artifact_root else repo / "rmr"
    if not root.is_dir():
        return []
    artifacts: list[dict[str, Any]] = []
    for artifact in sorted(root.glob("**/build/outputs/aar/*.aar")):
        artifacts.append(
            {
                # Artifact roots may be outside the source checkout in AndroidX.
                # Keep the stable module-relative path rather than a runner-specific
                # absolute path.
                "path": f"{root.name}/{artifact.relative_to(root).as_posix()}",
                "bytes": artifact.stat().st_size,
                "sha256": sha256(artifact),
            }
        )
    return artifacts


def legal_inventory(repo: Path) -> dict[str, Any]:
    candidates = (
        "LICENSE.txt",
        "LICENSE",
        "rmr/AUTHORSHIP_AND_LICENSE.md",
        "rmr/rafaelia/LEGAL_NOTICE.md",
    )
    files: list[dict[str, Any]] = []
    restrictive_signals: list[str] = []
    root_apache = False
    for relative in candidates:
        path = repo / relative
        if not path.is_file():
            continue
        contents = path.read_text(encoding="utf-8", errors="replace")
        lowered = contents.lower()
        if relative in {"LICENSE.txt", "LICENSE"} and "apache license" in lowered and "version 2.0" in lowered:
            root_apache = True
        signals = [word for word in ("proprietary", "commercial", "penalt", "restriction") if word in lowered]
        if signals:
            restrictive_signals.append(relative)
        files.append({"path": relative, "sha256": sha256(path), "signals": signals})
    return {
        "files": files,
        "root_apache_2_detected": root_apache,
        "files_with_restrictive_language_signals": restrictive_signals,
        "legal_review_required": root_apache and bool(restrictive_signals),
        "conclusion": "TOKEN_VAZIO: this inventory is not a licence compatibility or enforcement conclusion.",
    }


def parse_observations(values: list[str]) -> dict[str, str]:
    observations: dict[str, str] = {}
    for raw in values:
        if "=" not in raw:
            raise AuditError("--observation must be NAME=STATUS")
        name, status = raw.split("=", 1)
        name = name.strip()
        status = status.strip().upper()
        if not name or not re.fullmatch(r"[a-z0-9_]+", name):
            raise AuditError(f"invalid observation name: {name!r}")
        if status not in {"PASSED", "FAILED", "TOKEN_VAZIO"}:
            raise AuditError(f"invalid observation status for {name}: {status!r}")
        observations[name] = status
    return observations


def observed_status(observations: dict[str, str], key: str, fallback: str) -> str:
    value = observations.get(key)
    if value == "PASSED":
        return "OBSERVED_IN_CI"
    if value == "FAILED":
        return "FAILED_IN_CI"
    if value == "TOKEN_VAZIO":
        return "TOKEN_VAZIO"
    return fallback


def packaging_status(observations: dict[str, str], artifacts: list[dict[str, Any]]) -> str:
    observation = observations.get("rmr_release_aar_build")
    if observation == "FAILED":
        return "FAILED_IN_CI"
    if observation == "PASSED":
        # A successful Gradle command alone is not packaging evidence: the
        # expected AARs must be present and hashable in the audit inventory.
        return "OBSERVED_IN_CI" if artifacts else "TOKEN_VAZIO"
    return "BUILD_ARTIFACT_EVIDENCE" if artifacts else "TOKEN_VAZIO"


def build_claims(
    modules: dict[str, Any], artifacts: list[dict[str, Any]], legal: dict[str, Any], observations: dict[str, str]
) -> list[dict[str, str]]:
    all_registered = all(item["status"] == "STATIC_EVIDENCE" for item in modules["expected_modules"])
    return [
        {
            "id": "rmr_module_registration",
            "status": "STATIC_EVIDENCE" if all_registered else "TOKEN_VAZIO",
            "evidence": "settings.gradle and module build-file inspection for the declared RmR module set.",
        },
        {
            "id": "rmr_release_aar_packaging",
            "status": packaging_status(observations, artifacts),
            "evidence": "AAR file hashes are inventory evidence only; they do not prove runtime semantics.",
        },
        {
            "id": "rmr_core_tests",
            "status": observed_status(observations, "rmr_core_tests", "TOKEN_VAZIO"),
            "evidence": "A successful Gradle test observation is required.",
        },
        {
            "id": "rafaelia_tests",
            "status": observed_status(observations, "rafaelia_tests", "TOKEN_VAZIO"),
            "evidence": "A successful Gradle test observation is required.",
        },
        {
            "id": "androidx_upstream_behavioral_compatibility",
            "status": "TOKEN_VAZIO",
            "evidence": "Tree comparison and package assembly cannot prove behavioural compatibility with AndroidX upstream.",
        },
        {
            "id": "rmr_optimization_or_performance_gain",
            "status": "TOKEN_VAZIO",
            "evidence": "Requires reproducible workloads, an upstream control, and statistically reported samples.",
        },
        {
            "id": "license_compatibility_conclusion",
            "status": "LEGAL_REVIEW_REQUIRED" if legal["legal_review_required"] else "TOKEN_VAZIO",
            "evidence": "The audit inventories notices and language signals; a qualified legal review remains required.",
        },
    ]


def render_markdown(summary: dict[str, Any], delta: dict[str, Any], claims: list[dict[str, str]]) -> str:
    lines = [
        "# AndroidX RmR — upstream functional audit",
        "",
        f"- Audit status: `{summary['status']}`",
        f"- Fork HEAD: `{summary.get('head', 'TOKEN_VAZIO')}`",
        f"- Upstream: `{summary.get('upstream', 'TOKEN_VAZIO')}`",
        f"- Merge base: `{summary.get('merge_base', 'TOKEN_VAZIO')}`",
        f"- Changed paths: `{delta.get('changed_path_count', 0)}`",
        f"- Unapproved upstream-surface paths: `{delta.get('unapproved_path_count', 0)}`",
        "",
        "## Boundary classifications",
        "",
    ]
    for classification, count in sorted(delta.get("classifications", {}).items()):
        lines.append(f"- `{classification}`: {count} path(s)")
    if not delta.get("classifications"):
        lines.append("- `TOKEN_VAZIO`: no comparable delta was produced.")
    lines.extend(["", "## Claim evidence states", ""])
    for claim in claims:
        lines.append(f"- `{claim['id']}` — `{claim['status']}`. {claim['evidence']}")
    lines.extend(
        [
            "",
            "`TOKEN_VAZIO` is an explicit absence-of-evidence marker, not approval or success.",
            "The report inventories legal signals but does not make a legal compatibility conclusion.",
            "",
        ]
    )
    return "\n".join(lines)


def error_reports(out_dir: Path, error: str) -> None:
    summary = {
        "report_version": REPORT_VERSION,
        "status": "TOKEN_VAZIO",
        "error": error,
        "meaning": "The upstream comparison could not be established; compatibility, performance, and legal conclusions remain unproven.",
    }
    delta = {
        "report_version": REPORT_VERSION,
        "status": "TOKEN_VAZIO",
        "error": error,
        "changed_path_count": 0,
        "unapproved_path_count": 0,
        "paths": [],
        "classifications": {},
    }
    claims = [{"id": "upstream_comparison", "status": "TOKEN_VAZIO", "evidence": "Fetch, reference resolution, policy loading, or merge-base calculation failed."}]
    write_json(out_dir / "androidx_audit_summary.json", summary)
    write_json(out_dir / "androidx_upstream_delta.json", delta)
    write_json(out_dir / "rmr_capability_inventory.json", {"report_version": REPORT_VERSION, "status": "TOKEN_VAZIO", "error": error})
    write_json(out_dir / "rmr_claims.json", claims)
    write_text(out_dir / "androidx_upstream_delta.md", render_markdown(summary, delta, claims))


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=".", help="AndroidX checkout to audit (default: current directory)")
    parser.add_argument("--upstream-url", default="https://github.com/androidx/androidx.git")
    parser.add_argument("--upstream-ref", default="androidx-main")
    parser.add_argument("--policy", default="rmr/audit/androidx_boundary_allowlist.json")
    parser.add_argument("--artifact-root", default="rmr")
    parser.add_argument("--out-dir", default="artifacts/androidx-upstream-audit")
    parser.add_argument("--skip-fetch", action="store_true")
    parser.add_argument("--enforce-boundary", action="store_true")
    parser.add_argument("--require-clean-tree", action="store_true")
    parser.add_argument("--fail-on-whitespace", action="store_true")
    parser.add_argument("--review-after-days", type=int, default=30)
    parser.add_argument("--fail-on-drift", action="store_true")
    parser.add_argument("--observation", action="append", default=[], metavar="NAME=STATUS")
    args = parser.parse_args()

    repo = Path(args.repo).resolve()
    out_dir = Path(args.out_dir).resolve()
    try:
        if args.review_after_days < 0:
            raise AuditError("--review-after-days must be non-negative")
        observations = parse_observations(args.observation)
        git_text(repo, "rev-parse", "--is-inside-work-tree")
        rules = load_policy((repo / args.policy).resolve())
        remote_ref = ensure_upstream(repo, args.upstream_url, args.upstream_ref, args.skip_fetch)
        head = git_text(repo, "rev-parse", "HEAD")
        upstream = git_text(repo, "rev-parse", remote_ref)
        merge_base = git_text(repo, "merge-base", "HEAD", remote_ref)
        clean_status = git_text(repo, "status", "--porcelain=v1", "--untracked-files=all")
        changes = parse_name_status(git_text(repo, "diff", "--name-status", "-M", merge_base, "HEAD"))
        numstat = parse_numstat(git_text(repo, "diff", "--numstat", "-M", merge_base, "HEAD"))
        diff_check = run_git(repo, "diff", "--check", merge_base, "HEAD", check=False)
        classifications: dict[str, int] = {}
        paths: list[dict[str, Any]] = []
        for entry in changes:
            boundary = classify_path(entry["path"], rules)
            classifications[boundary["classification"]] = classifications.get(boundary["classification"], 0) + 1
            paths.append({**entry, **numstat.get(entry["path"], {"additions": "TOKEN_VAZIO", "deletions": "TOKEN_VAZIO"}), **boundary})
        unapproved_paths = [item for item in paths if not item["approved_by_policy"]]

        upstream_epoch_text = git_text(repo, "show", "-s", "--format=%ct", remote_ref)
        try:
            upstream_epoch = int(upstream_epoch_text)
        except ValueError as error:
            raise AuditError(f"invalid upstream commit timestamp: {upstream_epoch_text!r}") from error
        now = datetime.datetime.now(datetime.timezone.utc)
        upstream_date = datetime.datetime.fromtimestamp(upstream_epoch, datetime.timezone.utc)
        drift_days = max(0, (now.date() - upstream_date.date()).days)
        drift_status = "CURRENT" if drift_days <= args.review_after_days else "REVIEW_REQUIRED"
        modules = module_inventory(repo)
        artifacts = artifact_inventory(repo, args.artifact_root)
        legal = legal_inventory(repo)
        claims = build_claims(modules, artifacts, legal, observations)
        delta = {
            "report_version": REPORT_VERSION,
            "status": "COMPARABLE",
            "base": merge_base,
            "head": head,
            "upstream": upstream,
            "changed_path_count": len(paths),
            "unapproved_path_count": len(unapproved_paths),
            "paths": paths,
            "classifications": {key: classifications[key] for key in sorted(classifications)},
            "diff_check": {
                "status": "PASS" if diff_check.returncode == 0 else "REVIEW_REQUIRED",
                "output": (diff_check.stdout + diff_check.stderr).strip() or "TOKEN_VAZIO",
            },
        }
        violations: list[str] = []
        if args.enforce_boundary and unapproved_paths:
            violations.append(f"{len(unapproved_paths)} changed path(s) fall outside the checked-in RmR boundary policy")
        if args.require_clean_tree and clean_status:
            violations.append("working tree is not clean")
        if args.fail_on_whitespace and diff_check.returncode:
            violations.append("git diff --check reported whitespace errors")
        if args.fail_on_drift and drift_status != "CURRENT":
            violations.append(f"upstream reference is {drift_days} day(s) old; review threshold is {args.review_after_days}")
        summary = {
            "report_version": REPORT_VERSION,
            "status": "PASS" if not violations else "FAIL",
            "head": head,
            "upstream": upstream,
            "merge_base": merge_base,
            "upstream_ref": args.upstream_ref,
            "upstream_url": args.upstream_url,
            "changed_path_count": len(paths),
            "unapproved_path_count": len(unapproved_paths),
            "working_tree_clean_before_report": not bool(clean_status),
            "upstream_commit_timestamp": upstream_date.isoformat(),
            "upstream_drift_days": drift_days,
            "upstream_drift_status": drift_status,
            "review_after_days": args.review_after_days,
            "observations": observations,
            "gate_violations": violations,
            "evidence_model": {
                "OBSERVED_IN_CI": "A workflow command was executed and reported PASSED.",
                "STATIC_EVIDENCE": "The source tree or Gradle metadata contains the inspected evidence.",
                "BUILD_ARTIFACT_EVIDENCE": "A deterministic AAR inventory with hashes is present.",
                "LEGAL_REVIEW_REQUIRED": "Notices/signals require a qualified legal review; no conclusion is implied.",
                "TOKEN_VAZIO": "No sufficient evidence was supplied; it is not a pass.",
            },
        }
        modules.update({"report_version": REPORT_VERSION, "status": "COMPARABLE", "artifacts": artifacts, "legal_inventory": legal, "observations": observations})

        write_json(out_dir / "androidx_audit_summary.json", summary)
        write_json(out_dir / "androidx_upstream_delta.json", delta)
        write_json(out_dir / "rmr_capability_inventory.json", modules)
        write_json(out_dir / "rmr_claims.json", claims)
        write_text(out_dir / "androidx_upstream_delta.md", render_markdown(summary, delta, claims))
        print(f"AndroidX RmR upstream audit: {summary['status']} ({len(paths)} changed paths)")
        return 0 if not violations else 1
    except AuditError as error:
        error_reports(out_dir, str(error))
        print(f"AndroidX RmR upstream audit: TOKEN_VAZIO: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
