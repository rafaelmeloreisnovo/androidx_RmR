# START HERE — AndroidX RmR inline Assembly, isolated tranche V2

**Date:** 2026-11-07. **Status:** IMPLEMENTED_UNTESTED_REMOTE; no release and no performance claim.
**Producer:** rafaelmeloreisnovo/androidx_RmR, branch refactor/rmr-inline-asm-armv7-aarch64-20261107.
**Authority:** RmR author-owned rmr/rmr-core/**; original AndroidX sources and existing licenses unchanged.

## Source / artifact / execution / evidence / claim

- SOURCE: prior scoped freestanding shape gate and original JNI matrix implementation.
- ARTIFACT: new rmr/rmr-core/src/main/cpp/rmr_zero_f64.h with a C-compatible freestanding zero-fill primitive, integrated into four matrix output initialization paths formerly using memset; native test; scoped CI.
- EXECUTION: locally compiled with host C11, ARMv7 NEON and AArch64 targets. No full Android build was executed.
- EVIDENCE: host test exited 0; Clang 17 generated ARMv7 veor/vst1.64 and AArch64 stp xzr instructions in disassembly.
- CLAIM: only an implemented source patch and local compile/test evidence. Remote CI, real-device behavior, performance and memory reductions are TOKEN_VAZIO. claim_allowed=false.

## Constraints and scope

1. The caller must own at least count writable, appropriately aligned doubles. The kernel does not prove native pointer lifetime or concurrent ownership.
2. The freestanding kernel has no libc, heap, malloc, JNI, OS calls, or syscalls. The Android JNI/ART/Gradle integration is not bare metal and remains necessary.
3. AArch64 uses stp xzr,xzr; ARMv7 NEON uses veor q0,q0,q0 and vst1.64; other architectures use a scalar C fallback.
4. Preserve unsigned dimension computation after the existing JNI shape validation. Do not change public APIs, matrix order or arithmetic accumulation.
5. Do not introduce fast-math, runtime ISA guessing, hex opcodes or speculative performance claims.
6. Original upstream AndroidX licenses/provenance unchanged, new RmR files labeled Apache-2.0.

## Tests and gates

- HOST_C11: local gcc -std=c11 -ffreestanding -fno-builtin -O2 -Wall -Wextra -Werror -pedantic; exit code 0; lengths 0..8, exact zero bytes, odd tails, sentinel preservation.
- CROSS_ARM32: local Clang 17 compilation to armv7a-linux-androideabi21 with -mfpu=neon -mfloat-abi=softfp; assembly generated.
- CROSS_ARM64: local Clang 17 compilation to aarch64-linux-android21; assembly generated.
- REMOTE_CI: TOKEN_VAZIO; do not promote until exact-head readback.
- JNI/JVM/AAR/REAL_ANDROID_DEVICE: TOKEN_VAZIO.
- BENCHMARK_SAME_DEVICE, PUBLIC_API_DIFF, AAR_BINARY_PARITY: TOKEN_VAZIO.

## Prioritized next tranches

P0: JNI negative paths and exact-head CI, ARMv7/AArch64 instrumented runtime parity.
P0: replace unsafe raw native address handles in RAFAELIA with capacity-checked ownership.
P1: benchmark this kernel against the previous memset implementation; revert if slower or larger without other justified benefit.
P1: profile matrix/vector kernels and add narrowly scoped pure-C/ASM replacements only when proven equivalent.
P2: release gates for provenance, public API/ABI and reproducible AAR hashes.

## Rollback and μRECEIPT

No default-branch mutation. Close the draft PR or revert this series; leave the prior source history and pointers intact.

ANDROIDX-RMR-INLINEASM-20261107-V2 | parent=rmr/001_PONTEIRO/START_HERE_ANDROIDX_REFACTOR_V1.md | kind=isolated_implementation | delta=ARMv7_NEON+AArch64_zero_f64_4_call_sites+host_test+cross_compile_CI | routes=P,I,E | evidence=local_host_exit_0+cross_compiled_objects_and_disassembly | gap=remote_CI_device_benchmark_TOKEN_VAZIO | next=exact_head_checks+real_arm32_arm64 | claim_allowed=false

R3 = <source patch and local compile/test PASS, CI/device/benchmarks unresolved, verify and benchmark before merge>.
