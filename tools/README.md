# RmR Tools

This directory contains quality assurance and build tools for the RmR project.

## Tools Overview

### `audit_placeholders.py`

**Purpose**: Enforces code quality by detecting placeholder markers in core modules.

**Usage**:
```bash
python3 tools/audit_placeholders.py
```

**Exit Codes**:
- `0`: All checks passed (no placeholders found)
- `1`: Placeholders found (build should fail)

**Audited Directories**:
- `rmr/rmr-core/src/main` - Core matrix operations and state management
- `rmr/rafaelia/src/main` - Ultra low-level bare-metal optimization engine

**Detected Patterns**:
- `TODO` - Incomplete work markers
- `FIXME` - Code that needs fixing
- `PLACEHOLDER` - Temporary placeholder code
- `REPLACE_ME` - Code marked for replacement
- `CHANGEME` - Code marked for changes
- `XXX` - Warning markers
- `HACK` - Temporary workarounds

**File Types Checked**:
- `.java` - Java source files
- `.kt` - Kotlin source files
- `.cpp`, `.cc` - C++ source files
- `.h`, `.hpp` - C/C++ header files
- `.c` - C source files

**CI Integration**:
Add to your CI pipeline to ensure code quality:
```yaml
# GitHub Actions example
- name: Run placeholder audit
  run: python3 tools/audit_placeholders.py
```

**Rationale**: Core modules must be production-ready code with no placeholders. This ensures:
- Code completeness
- Production readiness
- Professional quality
- No forgotten work items

### `ci_build.sh`

**Purpose**: Comprehensive CI build script that runs all quality checks.

**Usage**:
```bash
bash tools/ci_build.sh
```

**Checks Performed**:
1. **Placeholder Audit** - Runs `audit_placeholders.py`
2. **Telemetry Check** - Ensures no telemetry/analytics code in core
3. **Reflection Check** - Detects reflection usage (performance concern)
4. **Contract Verification** - Ensures CORE_CONTRACT.md exists

**Exit Codes**:
- `0`: All checks passed
- `1`: One or more checks failed

**CI Integration**:
```yaml
# GitHub Actions example
- name: Run CI checks
  run: bash tools/ci_build.sh
```

**Output**: Color-coded results with detailed error messages

## Core Contract

The `CORE_CONTRACT.md` file in the `rmr/` directory defines immutable guarantees:

### Key Concepts

1. **RAFCODE-Φ** - Fundamental identity seal using golden ratio
2. **Bitraf Seed** - Deterministic initialization for matrix operations
3. **Invariants** - Mathematical properties that must always hold
4. **API Stability** - Guarantees about public API compatibility

### Invariant Categories

- **Matrix Operations**: Determinism, identity, associativity
- **State Management**: Immutability, transformation closure
- **Vector Operations**: Length preservation, distance metrics
- **Hardware Optimization**: Cache alignment, SIMD vectorization

## Development Workflow

### Before Committing

```bash
# 1. Run audit script
python3 tools/audit_placeholders.py

# 2. Run full CI checks
bash tools/ci_build.sh

# 3. Run tests
cd rmr/rmr-core && ../../gradlew test
```

### Adding New Core Code

1. Write production-ready code (no placeholders)
2. Add tests for determinism and invariants
3. Run audit script to verify
4. Update CORE_CONTRACT.md if adding new guarantees

### Fixing Audit Failures

If the audit script finds placeholders:

1. **Review the code** - Is it ready for production?
2. **Complete the implementation** - Replace placeholders with real code
3. **Add tests** - Ensure the code is tested
4. **Re-run audit** - Verify placeholders are removed

Example fix:
```java
// Before (FAILS audit)
public void someMethod() {
    // TODO: Implement this method
}

// After (PASSES audit)
public void someMethod() {
    // Implementation with proper logic
    validateInput();
    performCalculation();
    return result;
}
```

## Testing Guidelines

### Determinism Tests

Verify that operations produce consistent results:
- Same input always produces same output
- No random behavior
- Cross-run consistency

Example:
```java
@Test
public void testDeterminism() {
    RmRMatrix m = createMatrix();
    RmRMatrix r1 = m.multiply(identity);
    RmRMatrix r2 = m.multiply(identity);
    assertEquals(r1, r2); // Must be identical
}
```

### Invariant Tests

Verify mathematical properties hold:
- Identity: `I * M = M`
- Associativity: `(A * B) * C = A * (B * C)`
- Transpose: `(M^T)^T = M`

Example:
```java
@Test
public void testIdentityInvariant() {
    RmRMatrix m = createMatrix();
    RmRMatrix identity = RmRMatrix.identity(4);
    RmRMatrix result = identity.multiply(m);
    assertMatrixEquals(m, result); // I * M = M
}
```

### Thread-Safety Tests

Verify concurrent access is safe:
- No race conditions
- Proper synchronization
- Memory barriers

## Performance Considerations

### What the Tools Check

✅ **Checked by Tools**:
- No placeholders in core code
- No telemetry/analytics
- No reflection usage (performance killer)
- Code completeness

❌ **Not Checked** (manual review needed):
- Algorithm efficiency
- Memory allocation patterns
- Cache optimization effectiveness
- SIMD utilization

### Performance Checklist

Manual checks for optimization:
- [ ] Minimal heap allocations in hot paths
- [ ] Cache-friendly data layouts
- [ ] SIMD instructions utilized
- [ ] Branch prediction optimized
- [ ] Lock-free where possible

## Extending the Tools

### Adding New Audit Patterns

Edit `audit_placeholders.py`:
```python
PLACEHOLDER_PATTERNS = [
    r'\bTODO\b',
    r'\bFIXME\b',
    # Add new pattern here
    r'\bNEW_PATTERN\b',
]
```

### Adding New CI Checks

Edit `ci_build.sh`:
```bash
# Add new check
echo "Step N: Running new check..."
if new_check_command; then
    echo "✓ New check passed"
else
    echo "✗ New check failed"
    exit 1
fi
```

### Adding New Directories to Audit

Edit `audit_placeholders.py`:
```python
AUDIT_DIRS = [
    "rmr/rmr-core/src/main",
    "rmr/rafaelia/src/main",
    # Add new directory
    "rmr/new-module/src/main",
]
```

## Troubleshooting

### Audit Script Issues

**Problem**: Script not finding files
```bash
# Check directory structure
ls -la rmr/rmr-core/src/main
ls -la rmr/rafaelia/src/main
```

**Problem**: False positives
- Review the matched lines in the output
- Consider if the pattern is in a comment
- Update patterns if needed

### CI Script Issues

**Problem**: Permission denied
```bash
# Make script executable
chmod +x tools/ci_build.sh
# Or run with bash
bash tools/ci_build.sh
```

**Problem**: Python not found
```bash
# Install Python 3
sudo apt-get install python3
# Or use system Python
python tools/audit_placeholders.py
```

## Contributing

When adding new tools:
1. Add documentation to this README
2. Include usage examples
3. Add error handling
4. Test on multiple platforms
5. Update CI integration examples

## References

- [CORE_CONTRACT.md](../rmr/CORE_CONTRACT.md) - Core API guarantees
- [RmR README](../rmr/README.md) - Module overview
- [Contributing Guide](../CONTRIBUTING.md) - Contribution guidelines

---

**Maintained by**: RmR Core Team  
**See commit history for last updated date**
