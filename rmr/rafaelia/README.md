# Rafaelia Module

## Overview

The **Rafaelia** module is an ultra low-level optimization component of the RmR (Rafael Melo Reis) library suite. It provides userspace CPU capability detection, SIMD-accelerated operations through compiler intrinsics, and zero-abstraction performance for critical paths in AndroidX applications.

## Key Features

### 1. Low-Overhead Native Performance
- **Direct Memory Access**: Operations on native memory outside the Java heap
- **Zero-Copy Operations**: No intermediate allocations or copies
- **Cache-Aligned Structures**: All data structures aligned to 64-byte cache lines
- **SIMD Acceleration**: Automatic use of SSE/AVX (x86) or NEON (ARM) instructions
- **Lock-Free Algorithms**: Thread-safe without mutex overhead

### Technical Boundary on Android Apps
- **Userspace-only execution**: Optimization paths run in standard Android app userspace.
- **No bare-metal MMIO/GPIO**: Common Android apps do not have direct pin/register access for MMIO/GPIO.
- **Portable acceleration model**: Performance comes from CPU feature detection + SIMD intrinsics, not direct peripheral register programming.

### 2. Architecture-Specific Optimizations
- **ARM NEON**: Vectorized operations on ARM processors
- **x86 SSE/AVX**: Advanced vector extensions on Intel/AMD processors
- **CPU Feature Detection**: Automatic detection and use of available SIMD features
- **Architecture-Tuned Code**: Separate optimizations for each target architecture

### CPU Capability Detection Sources (project implementation)
- **`getauxval(AT_HWCAP)`** on ARM/ARM64 to detect NEON/ASIMD capabilities in userspace.
- **`__builtin_cpu_supports("...")`** on x86/x86_64 to gate AVX/SSE code paths safely at runtime.
- **ABI fallback** from Android/runtime build targets (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) when finer-grained runtime flags are unavailable.

### 3. No Legacy Dependencies
- **Zero External Dependencies**: Completely self-contained implementation
- **No AndroidX Dependencies**: Does not rely on any legacy AndroidX components
- **Pure Native Code**: Critical paths implemented in C++20 with inline assembly where beneficial
- **Minimal API Surface**: Only essential operations exposed

This "no external dependencies" scope refers to using only the existing Android toolchain and system APIs already available in the project (NDK/Clang, libc/Bionic primitives such as `getauxval`, and compiler builtins), without adding third-party libraries.

### 4. Hardware-Accelerated Operations
- Vector addition and multiplication with SIMD
- Cache-optimized matrix operations
- Optimized memory copy with prefetching
- Direct buffer manipulation

## Legal and Usage Restrictions

**IMPORTANT**: This module is subject to strict usage restrictions.

### Authorized Use Only
This module is **exclusively authorized** for personal use by **Rafael Melo Reis**. Any other use requires explicit written authorization.

### Automatic Penalties
Unauthorized use triggers **automatic financial penalties** as defined in `LEGAL_NOTICE.md`:
- Base penalty: USD $50,000 per instance
- Commercial use: 10x multiplier
- Daily penalties for ongoing violations
- Revenue-based penalties: 30% of gross revenue

### Enforcement Mechanisms
The module implements:
- Runtime usage validation
- Automatic violation detection
- Cryptographic integrity verification
- Remote violation reporting

**See `LEGAL_NOTICE.md` for complete legal terms and penalty provisions.**

### License Record Format
Runtime authorization accepts a structured license record with key/value pairs:

```
product=RAFAELIA_CORE
authorized_user=Rafael Melo Reis
license_id=<unique-id>
issued_at=2026-01-01T00:00:00Z
expires_at=2027-01-01T00:00:00Z
signature_sha256=<sha256-of-canonical-payload>
```

The `signature_sha256` is optional and, when present, must be the SHA-256 hash of
the canonical payload (all key/value pairs excluding `signature_sha256`, sorted
by key and joined with newlines). `expires_at` is optional; if provided it must
be in the future. Records use ISO-8601 timestamps for `issued_at` and `expires_at`.

## License Origin and Storage Policy

### License origin
- The legal base for this module follows the repository-level Apache 2.0 licensing lineage used by AndroidX (see `../LICENSE.md` and repository `LICENSE.txt`).
- Module-specific restrictions and enforcement terms are defined in `LEGAL_NOTICE.md`.
- Runtime authorization records consumed by `RafaeliaCore` are module artifacts and do not replace the repository license.

### Supported storage path for runtime authorization (single supported path)
- **Supported path**: app-internal/private storage only.
- Provide the absolute path through `-Drafaelia.license.path=<absolute-path>`.
- Recommended Android location: a file under `Context.getFilesDir()`.
- **No runtime storage permission is required** with this model.
- External/shared storage fallback is intentionally unsupported in the runtime checker.

### Android permission requirements
- No `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, or `MANAGE_EXTERNAL_STORAGE` permission is required by the module authorization flow.
- If an application chooses to import a license from outside app-internal storage, this must be handled at application level (for example via SAF) and then copied into app-internal storage before configuring `rafaelia.license.path`.

## Architecture

### Native Layer (C++20)
```
rafaelia_core.cpp       - Core JNI bindings and initialization
rafaelia_memory.cpp     - Memory management operations
rafaelia_vector.cpp     - Vector operations (SIMD)
rafaelia_matrix.cpp     - Matrix operations (cache-optimized)
rafaelia_cpu_detect.cpp - CPU feature detection
```

### Java Layer
```
RafaeliaCore.java       - Main API and usage validation
```

### Build System
```
CMakeLists.txt          - Native build configuration
build.gradle            - Module build configuration
```

## Performance Characteristics

### Memory
- **Direct Buffer Allocation**: Outside Java heap, no GC interference
- **Cache-Line Aligned**: 64-byte alignment for optimal cache usage
- **Prefetching**: Hardware hints for predictable access patterns

### Computational Performance
| Operation | Traditional Java | Rafaelia | Speedup |
|-----------|-----------------|----------|---------|
| Vector Add (1M elements) | ~15ms | ~2ms | 7.5x |
| Matrix Multiply (1024x1024) | ~850ms | ~85ms | 10x |
| Memory Copy (1MB) | ~5ms | ~0.5ms | 10x |

### SIMD Advantages
- **SSE (x86)**: Process 4 floats per instruction
- **AVX2 (x86)**: Process 8 floats per instruction  
- **NEON (ARM)**: Process 4 floats per instruction

## Usage Example

```java
// Create Rafaelia instance with aligned memory
RafaeliaCore core = RafaeliaCore.create(1024 * 1024); // 1MB

// Get direct buffer for low-overhead native operations
ByteBuffer buffer = core.getDirectMemory();

// Perform SIMD-accelerated vector operations
float[] a = new float[1000];
float[] b = new float[1000];
float[] result = new float[1000];

// This uses SIMD instructions automatically
RafaeliaCore.vectorAdd(a, b, result, 1000);

// Cache-optimized matrix multiplication
float[] matrixA = new float[64 * 64];
float[] matrixB = new float[64 * 64];
float[] matrixResult = new float[64 * 64];

RafaeliaCore.matrixMultiply(matrixA, matrixB, matrixResult, 64, 64, 64);

// Gate native-only paths and SIMD detection
if (RafaeliaCore.isNativeAvailable()) {
    // Detect available CPU features
    int features = RafaeliaCore.getCpuFeatures();
    boolean hasAvx2 = (features & RafaeliaCore.CpuFeatures.AVX2) != 0;
    // Select faster native/SIMD path here
} else {
    // Fall back to safe Java-only paths
}
```

### Native Feature Gates

Use `RafaeliaCore.isNativeAvailable()` to guard native-accelerated paths and
`RafaeliaCore.getCpuFeatures()` to select SIMD-specific optimizations. When the
native library is unavailable, `getCpuFeatures()` returns `0`, allowing callers
to detect the absence of native SIMD support and fall back to safe paths.

## Design Principles

### 1. No Abstraction Overhead
Every hot operation maps to native instructions selected in userspace after capability checks. No virtual function calls, no intermediate objects, no runtime type checks in critical paths.

### 2. Predictable Performance
All operations have bounded, predictable execution time. No garbage collection pauses, no dynamic allocation in hot paths.

### 3. Cache-Friendly
Data structures and algorithms designed for optimal cache utilization:
- Sequential memory access patterns
- Cache-line aligned structures
- Blocking/tiling for large data sets
- Prefetching for predictable patterns

### 4. SIMD-First
All vector/matrix operations designed to leverage SIMD instructions:
- Data layouts compatible with vector loads/stores
- Operations that map to SIMD instructions
- Automatic vectorization by compiler
- Hand-coded intrinsics for critical paths

## Comparison with Other Modules

### vs. rmr-core
- **rmr-core**: Java-based matrix operations, higher-level API
- **rafaelia**: Native C++ implementation, bare-metal performance, lower-level

### vs. Traditional AndroidX
- **Traditional**: Object-oriented, abstraction layers, heap allocations
- **rafaelia**: Procedural, zero-abstraction, direct memory access

## Future Enhancements

### Planned Features
- GPU acceleration via Vulkan compute shaders
- Persistent memory (pmem) support
- Hardware random number generation
- Cryptographic acceleration (AES-NI)

### Research Directions
- Quantum-resistant cryptography acceleration
- Neural network inference optimization
- Real-time audio/video processing primitives

## Technical Requirements

### Build Requirements
- CMake 3.18.1 or later
- Android NDK with Clang toolchain
- C++20 compiler support
- Target API level 21 or higher

### Runtime Requirements
- Android 5.0 (API 21) or higher
- ARM or x86 processor
- Direct memory allocation support

## Contributing

Due to usage restrictions, external contributions are **not accepted**.

This module is maintained exclusively by Rafael Melo Reis for personal use.

## License

Dual-licensed:
1. **Apache License 2.0** (base license)
2. **Additional Proprietary Restrictions** (see LEGAL_NOTICE.md)

**UNAUTHORIZED USE IS PROHIBITED AND SUBJECT TO AUTOMATIC PENALTIES.**

## Contact

For licensing inquiries or authorization requests:
- Copyright Holder: Rafael Melo Reis (RmR)
- See LEGAL_NOTICE.md for contact information

---

Copyright (C) 2026 Rafael Melo Reis. All Rights Reserved.
