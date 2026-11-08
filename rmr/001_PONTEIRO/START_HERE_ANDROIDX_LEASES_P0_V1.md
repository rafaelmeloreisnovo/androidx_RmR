# 001_PONTEIRO · RAFAELIA owned-memory P0 V1

STATE: DRAFT/HOLD; CLAIM_ALLOWED=false; MERGE_ALLOWED=false.
START: READ → SOURCE → BUILD → TEST → EVIDENCE → ROLLBACK.

## Fast human + IA route

1. Source = rmr/rafaelia/src/main/cpp/rafaelia_core.cpp, androidx-main@8a75a3aba3e1452bea1feb45e64cba421a916c57. Legacy JNI/C++ and upstream preserved.
2. Pure leaf = rmr/rafaelia/src/main/cpp/portable/rmr_lease_v1.[ch]: C11 fixed 128 slots, generation-tagged handle, capacity, stale/ABA/duplicate rejection, no heap/libc/JNI/syscalls.
3. Android adapter = same rafaelia_core.cpp with optional RMR_SAFE_LEASES: posix_memalign/free in adapter only, pthread mutex covering use/free, 32 MiB per allocation + 64 MiB total; 16,777,216 scalar-matrix-operation upper bound.
4. High-frequency path = Java RafaeliaCore.optimizedMemoryCopy(ByteBuffer,…): pass LIVE objects through nativeCopyDirectBuffers with offsets, capacity and read-only checks, memmove for overlap. JVM fallback snapshots aliased views.
5. Build = -DRMR_SAFE_LEASES=ON (candidate opt-in); OFF default preserves existing native-jlong behavior and symbols. The direct ByteBuffer repair applies to both modes.
6. Tests = sh rmr/rafaelia/src/test/native/check_lease_v1.sh; Java rmr/rafaelia/src/test/java/androidx/rmr/rafaelia/RafaeliaMemoryCopyTest.java; .github/workflows/rmr-rafaelia-lease-p0.yml.
7. Scope evidence = independent host C11, sanitizers, no undefined imports in pure object, compile-only ARM A32/Thumb2/A64, host JNI both modes. NOT_DEVICE_RUN until physical evidence.
8. Rollback = close DRAFT PR or choose RMR_SAFE_LEASES=OFF in a controlled build. Direct-ByteBuffer repair rollback would require a separate reviewed code revert.

## Threats / capacity / anti-zombie

- P0: nativeFreeAligned formerly accepted an arbitrary address; copy/set/vectors/matrices/prefetch formerly trusted raw jlong addresses and unknown lifetimes. Safe profile rejects stale/unknown, checks bytes and serializes close/use.
- Default raw JNI mode still exists for compatibility: global P0 is NOT CLOSED. The old nativeGetDirectBufferAddress still exposes raw address privately but optimizedMemoryCopy no longer uses it.
- jlong private numeric semantics change in opt-in safe mode from pointer to opaque generation token. Android Java signatures stay intact, BUT callers interpreting those longs as addresses are not compatible. Not cryptographically unforgeable.
- ByteBuffer reachability is maintained during JNI synchronous call by argument references. Does not prevent hostile concurrent writes, external frees, JVM/Android bugs or unauthorized reflection.
- Finite quotas reduce unbounded memory and compute time; do NOT demonstrate lack of OS zombie processes. No process lifecycle or live profiler was examined; access frequency and performance are TOKEN_VAZIO.
- JVM fallback snapshots into byte[length], therefore incurs heap allocation. It can throw OOME without partially modifying dst; not zero-allocation end-to-end.
- Upstream AndroidX and historical license grants unchanged; ownership/legal validity not certified here.

## Falsifiers / gates before promotion

G1 C11 duplicate registration, stale double-close, ABA/generation, offset overflow, null, slot exhaustion, retired generation.
G2 zero pure-object imports; compile with strict flags GCC+Clang; A32 Thumb2 AArch64 object build, optional ASan+UBSan.
G3 hosted C++20 JNI adapter compile, RMR_SAFE_LEASES OFF and ON; JDK headers + Android log stub != linked NDK .so.
G4 Java host fallback tests for heap alias/views, direct offsets, negative capacities; physical Android JNI direct test mandatory.
G5 device ARMv7 & arm64 release/close race, ABI/AAR parity, full native correctness, old behavior, benchmark & independent source/license review.
Claim allowed=false unless G1–G5 appropriate to specific claim and reviewer gate succeeds.
IMPORTANT: current C11 candidate PR#74 and Q16 PR#73 are separate branches, not merged here.

## Source custody

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
IMPLEMENTED_UNTESTED != PASS; TOKEN_VAZIO != 0.
μWRITE successor = one receipt per material delta in authorized NEXUS shard, then CURRENT_STATE pointer, with parent/exact_SHA/gates/rollback.
R3 = <F_ok:bounded host source and modular bridge,F_gap:physical device/runtime/perf,F_next:CI exact head→Android tests→review>.
