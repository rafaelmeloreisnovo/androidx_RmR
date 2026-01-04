# RmR Core Contract

**Version**: 1.0  
**Date**: 2026-01-04  
**Status**: IMMUTABLE

## Overview

This document defines the **immutable core contract** for the RmR (Rafael Melo Reis) system. These guarantees, invariants, and identities **MUST NEVER CHANGE** to maintain compatibility and system integrity across all versions.

## Core Identities

### RAFCODE-Φ

**RAFCODE-Φ** (Rafael Code - Phi) is the fundamental identity seal of the RmR system.

**Properties**:
- **Immutable**: The RAFCODE-Φ identity remains constant across all versions
- **Deterministic**: Computed using the golden ratio φ (phi) = 1.618033988749...
- **Verifiable**: Can be independently verified through mathematical computation
- **Unique**: Distinguishes RmR implementations from other systems

**Mathematical Definition**:
```
RAFCODE-Φ = Hash(RmR_Identity || φ || Timestamp_Epoch)
where φ = (1 + √5) / 2
```

**Usage**: RAFCODE-Φ serves as a cryptographic seal to verify authentic RmR implementations and ensure no tampering has occurred in core components.

**Guarantee**: RAFCODE-Φ computation algorithm SHALL NEVER be modified.

### Bitraf Seed

**Bitraf Seed** is the deterministic initialization seed for all RmR matrix operations.

**Properties**:
- **Deterministic**: Always produces the same seed for the same inputs
- **Reproducible**: Any implementation must produce identical seeds
- **Collision-resistant**: Different inputs produce different seeds
- **Privacy-preserving**: Seed does not leak information about internal state

**Definition**:
```java
long bitrafSeed = computeBitrafSeed(inputVector);
// Where seed = XOR(hash(vector[0]), hash(vector[1]), ..., hash(vector[n]))
```

**Guarantee**: Bitraf seed computation SHALL NEVER change to ensure deterministic behavior across versions and implementations.

## Invariants

### Matrix Operations

1. **Determinism Invariant**
   - Matrix operations are **strictly deterministic**
   - Same input ALWAYS produces same output
   - No randomness, no non-deterministic behavior
   - Guarantee: `multiply(A, B) == multiply(A, B)` for any matrices A, B

2. **Identity Invariant**
   - Identity matrix multiplied by any matrix returns the original matrix
   - Guarantee: `multiply(I, M) == M` for any matrix M and identity I

3. **Associativity Invariant**
   - Matrix multiplication is associative
   - Guarantee: `multiply(multiply(A, B), C) == multiply(A, multiply(B, C))`

4. **Dimension Consistency**
   - Matrix operations maintain dimensional correctness
   - Guarantee: `multiply(A[m×k], B[k×n]) -> C[m×n]`

5. **Data Integrity**
   - Raw data arrays are row-major order (C-style)
   - Index formula: `data[row * cols + col]`
   - Guarantee: Memory layout NEVER changes

6. **Bounds Checking Invariant**
   - Unsafe methods (`get`, `set`) perform NO bounds checking for performance
   - Safe methods (`getChecked`, `setChecked`) ALWAYS validate bounds
   - Guarantee: Unsafe methods require caller to ensure:
     * `0 <= row < rows`
     * `0 <= col < cols`
     * `row * cols + col` does not overflow Integer.MAX_VALUE
   - Guarantee: Safe methods throw `IndexOutOfBoundsException` for invalid indices

7. **Overflow Prevention**
   - Matrix constructor validates `rows * cols` will not overflow
   - Throws `IllegalArgumentException` if dimensions would cause overflow
   - Guarantee: All RmRMatrix instances have valid, non-overflowing dimensions

### State Management

1. **State Immutability**
   - State transformations produce NEW state objects
   - Original state objects are NEVER modified
   - Guarantee: Functional state transformations only

2. **Transformation Closure**
   - State transformations always produce valid states
   - No transformation can produce invalid/corrupted state
   - Guarantee: `transform(validState) -> validState`

3. **Linear Flip Consistency**
   - Linear flip operation is well-defined: `-1/x` for non-zero elements
   - Zero elements remain zero (safe handling of division by zero)
   - Guarantee: `linearFlip(linearFlip(M))` approximates original M (within epsilon)

### Vector Operations

1. **Vector Length Preservation**
   - Vector operations maintain length consistency
   - Guarantee: Operations on vectors of length N produce results of length N

2. **Distance Metrics**
   - Euclidean distance: `sqrt(sum((a[i] - b[i])^2))`
   - Manhattan distance: `sum(abs(a[i] - b[i]))`
   - Cosine similarity: `dot(a, b) / (norm(a) * norm(b))`
   - Guarantee: Distance metric formulas NEVER change

### Hardware Optimization

1. **Cache Line Alignment**
   - Cache line size = 64 bytes (industry standard)
   - Structures aligned to cache boundaries
   - Guarantee: Cache alignment strategy remains consistent

2. **SIMD Vectorization**
   - Vector widths remain consistent with hardware capabilities
   - ARM NEON: 128-bit (2 doubles)
   - x86 SSE: 128-bit (2 doubles)
   - x86 AVX: 256-bit (4 doubles)
   - x86 AVX-512: 512-bit (8 doubles)
   - Guarantee: SIMD width constants NEVER change

3. **Block Size Optimization**
   - Default block size = 64 (L1 cache friendly)
   - Calculated based on L1 cache characteristics
   - Guarantee: Block size calculation algorithm remains stable

## API Stability Guarantees

### Public API (MUST NEVER BREAK)

#### Core Classes
- `RmRMatrix` - Matrix representation and operations
- `RmRState` - State container
- `RmRMatrixOps` - Hardware-optimized operations
- `RmRHardware` - Hardware detection
- `RmRUtils` - Utility methods

#### Core Methods (Immutable Signatures)
```java
// RmRMatrix
public double get(int row, int col)  // UNSAFE: no bounds checking
public void set(int row, int col, double value)  // UNSAFE: no bounds checking
public double getChecked(int row, int col)  // SAFE: validates bounds
public void setChecked(int row, int col, double value)  // SAFE: validates bounds
public RmRMatrix multiply(RmRMatrix other)
public RmRMatrix add(RmRMatrix other)
public RmRMatrix transpose()
public RmRMatrix linearFlip()
public static RmRMatrix identity(int size)

// RmRState
public RmRMatrix getStateMatrix()
public RmRMatrix getTransformMatrix()
public RmRState transform()
public RmRState transform(RmRMatrix customTransform)
public RmRState accumulate(RmRState other)
public static RmRState forLifecycle()
public static RmRState forNavigation()
public static RmRState forPreferences()
public static RmRState forDatabase()

// RmRMatrixOps
public static void multiply(RmRMatrix a, RmRMatrix b, RmRMatrix result)
public static void transpose(RmRMatrix input, RmRMatrix output)
public static double dotProduct(double[] a, double[] b)

// RmRHardware
public static Architecture getArchitecture()
public static SimdCapability getSimdCapability()
public static int getCoreCount()
public static int getVectorWidth()
```

**Guarantee**: These signatures SHALL NEVER change. New methods may be added, but existing methods remain stable.

### Internal Implementation (MAY CHANGE)

#### Allowed Changes
- Performance optimizations that maintain same behavior
- Bug fixes that don't affect correct usage
- Additional internal helper methods
- Platform-specific optimizations

#### Restrictions
- MUST maintain deterministic behavior
- MUST NOT break API contracts
- MUST preserve mathematical correctness
- MUST NOT introduce non-determinism

## Security and Privacy

### No Telemetry Guarantee

**The RmR core modules contain ZERO telemetry, analytics, or "phone-home" functionality.**

- No network connections
- No data collection
- No usage tracking
- No external reporting

**Verification**: Run `tools/audit_placeholders.py` to verify code integrity.

**Exception**: The `rafaelia` module contains usage authorization validation (see rafaelia/LEGAL_NOTICE.md), but this operates locally only.

### Data Privacy

1. **No Data Leakage**
   - Matrix operations do not leak internal state
   - State transformations are cryptographically safe
   - No side channels through timing or memory access patterns

2. **Memory Safety**
   - No buffer overflows in native code
   - Bounds checking where appropriate
   - Safe memory management practices

## Versioning and Compatibility

### Semantic Versioning

RmR follows semantic versioning: MAJOR.MINOR.PATCH

- **MAJOR**: Breaking changes (avoid whenever possible)
- **MINOR**: New features, backward compatible
- **PATCH**: Bug fixes, backward compatible

### Compatibility Guarantee

**Backward Compatibility**: Code compiled against RmR version X.Y.Z will work with any version X.Y'.Z' where Y' >= Y.

**Forward Compatibility**: NOT guaranteed. Newer features may not be available in older versions.

### Migration Path

If a breaking change is absolutely necessary:
1. Deprecation warning in version N
2. Maintained deprecated API for at least 2 major versions
3. Clear migration guide provided
4. Automated migration tools when possible

## Testing Requirements

### Mandatory Tests

1. **Determinism Tests**
   - Verify same input produces same output
   - Test across multiple runs
   - Test across different machines

2. **Invariant Tests**
   - Verify all mathematical invariants hold
   - Matrix identity, associativity, etc.
   - Vector operation correctness

3. **Hardware Tests**
   - Verify hardware detection accuracy
   - Test SIMD optimizations produce correct results
   - Test cache alignment benefits

4. **Thread-Safety Tests**
   - Verify concurrent access patterns
   - Test lock-free operations
   - Validate memory barriers

### Continuous Validation

- All tests run on every commit
- Cross-platform testing (ARM, x86, x86_64)
- Performance regression testing
- Memory leak detection

## Auditing and Verification

### Placeholder Audit

**Script**: `tools/audit_placeholders.py`

**Purpose**: Ensures core modules contain no placeholder code (TODO, FIXME, HACK, etc.)

**Enforcement**: CI build FAILS if placeholders are found in core modules

**Usage**:
```bash
python3 tools/audit_placeholders.py
# Exit 0: All checks passed
# Exit 1: Placeholders found
```

### Integrity Verification

Future versions will include cryptographic verification of core module integrity using RAFCODE-Φ seals.

## Change Process

### Core Contract Changes

Changes to this contract require:
1. **Unanimous consensus** from core maintainers
2. **Public review period** of at least 30 days
3. **Impact assessment** on existing code
4. **Migration guide** for affected users
5. **Version bump** reflecting breaking changes

### Adding New Invariants

New invariants may be added if they:
1. Do not conflict with existing invariants
2. Strengthen correctness guarantees
3. Aid in verification and testing
4. Are mathematically sound

## References

### Mathematical Foundations
- Linear Algebra: Horn & Johnson, "Matrix Analysis"
- Numerical Computation: Golub & Van Loan, "Matrix Computations"
- Cache Optimization: Intel, "Cache Blocking Techniques"

### Standards Compliance
- IEEE 754: Floating-point arithmetic
- C++17: Native implementation standard
- Java Language Specification: Java API requirements

### Legal Framework
- Apache License 2.0: Base license for RmR core
- rafaelia/LEGAL_NOTICE.md: Additional restrictions for rafaelia module

---

**This document is part of the RmR core specification and is considered IMMUTABLE.**  
**Any changes require formal review process and version incrementation.**  
**Last Updated**: 2026-01-04  
**Version**: 1.0
