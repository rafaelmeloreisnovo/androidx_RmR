#!/usr/bin/env python3
"""
Audit script to ensure no placeholders exist in core modules.

This script enforces code quality by failing (exit 1) if it finds any
TODO, FIXME, PLACEHOLDER, REPLACE_ME, or CHANGEME markers in the
rmr-core and rafaelia-core modules.

Usage:
    python tools/audit_placeholders.py
    
Exit codes:
    0: No placeholders found - all checks passed
    1: Placeholders found - build should fail
"""

import os
import sys
import re
from pathlib import Path
from typing import List, Tuple

# Directories to audit (relative to repository root)
AUDIT_DIRS = [
    "rmr/rmr-core/src/main",
    "rmr/rafaelia/src/main",
]

# Placeholder patterns to search for (case-insensitive)
PLACEHOLDER_PATTERNS = [
    r'\bTODO\b',
    r'\bFIXME\b',
    r'\bPLACEHOLDER\b',
    r'\bREPLACE_ME\b',
    r'\bCHANGEME\b',
    r'\bXXX\b',
    r'\bHACK\b',
]

# File extensions to check
FILE_EXTENSIONS = ['.java', '.kt', '.cpp', '.h', '.hpp', '.c', '.cc']

def find_placeholders(file_path: Path) -> List[Tuple[int, str, str]]:
    """
    Search for placeholder patterns in a file.
    
    Args:
        file_path: Path to the file to check
        
    Returns:
        List of tuples: (line_number, pattern_found, line_content)
    """
    findings = []
    
    try:
        with open(file_path, 'r', encoding='utf-8', errors='ignore') as f:
            for line_num, line in enumerate(f, start=1):
                # Check each pattern
                for pattern in PLACEHOLDER_PATTERNS:
                    if re.search(pattern, line, re.IGNORECASE):
                        findings.append((line_num, pattern, line.strip()))
                        break  # Only report first match per line
    except Exception as e:
        print(f"Warning: Could not read {file_path}: {e}", file=sys.stderr)
    
    return findings

def audit_directory(base_dir: Path, audit_subdir: str) -> List[Tuple[Path, int, str, str]]:
    """
    Audit a directory for placeholder patterns.
    
    Args:
        base_dir: Base directory of the repository
        audit_subdir: Subdirectory to audit (relative to base_dir)
        
    Returns:
        List of findings: (file_path, line_number, pattern, line_content)
    """
    audit_path = base_dir / audit_subdir
    
    if not audit_path.exists():
        print(f"Warning: Directory {audit_path} does not exist", file=sys.stderr)
        return []
    
    all_findings = []
    
    # Walk through all files in the directory
    for root, dirs, files in os.walk(audit_path):
        for file in files:
            # Check if file has one of the target extensions
            if any(file.endswith(ext) for ext in FILE_EXTENSIONS):
                file_path = Path(root) / file
                findings = find_placeholders(file_path)
                
                for line_num, pattern, line_content in findings:
                    all_findings.append((file_path, line_num, pattern, line_content))
    
    return all_findings

def main():
    """Main entry point for the audit script."""
    # Get repository root (assume script is in tools/ subdirectory)
    script_dir = Path(__file__).parent
    repo_root = script_dir.parent
    
    print("=" * 80)
    print("RmR Core Placeholder Audit")
    print("=" * 80)
    print()
    print(f"Repository root: {repo_root}")
    print(f"Auditing directories: {', '.join(AUDIT_DIRS)}")
    print(f"Searching for patterns: {', '.join(PLACEHOLDER_PATTERNS)}")
    print()
    
    all_findings = []
    
    # Audit each directory
    for audit_dir in AUDIT_DIRS:
        findings = audit_directory(repo_root, audit_dir)
        all_findings.extend(findings)
    
    # Report findings
    if all_findings:
        print("❌ AUDIT FAILED - Placeholders found:")
        print()
        
        # Group by file
        findings_by_file = {}
        for file_path, line_num, pattern, line_content in all_findings:
            if file_path not in findings_by_file:
                findings_by_file[file_path] = []
            findings_by_file[file_path].append((line_num, pattern, line_content))
        
        # Print grouped findings
        for file_path, findings in sorted(findings_by_file.items()):
            rel_path = file_path.relative_to(repo_root)
            print(f"📄 {rel_path}:")
            for line_num, pattern, line_content in findings:
                print(f"   Line {line_num} [{pattern}]: {line_content[:80]}")
            print()
        
        print(f"Total placeholders found: {len(all_findings)}")
        print()
        print("Core modules MUST NOT contain placeholder markers.")
        print("Please remove or resolve all placeholders before committing.")
        print()
        return 1  # Exit with error code
    else:
        print("✅ AUDIT PASSED")
        print()
        print("No placeholders found in core modules.")
        print("All checks passed successfully.")
        print()
        return 0  # Exit success

if __name__ == "__main__":
    sys.exit(main())
