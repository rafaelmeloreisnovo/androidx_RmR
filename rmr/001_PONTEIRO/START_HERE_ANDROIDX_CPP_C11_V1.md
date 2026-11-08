# START HERE — AndroidX RmR C++ → C11 candidate V1

STATE: DRAFT/HOLD, CLAIM_ALLOWED=false, PROMOTION=false.
PRODUCER: rafaelmeloreisnovo/androidx_RmR
SOURCE BASE: androidx-main@8a75a3aba3e1452bea1feb45e64cba421a916c57
REFERENCE (method only): rafaelmeloreisnovo/BLAKE3, rmr/portable/CONTRACT.md, rmr/portable/AGENTS.md.
LICENSE: Preserve existing AndroidX/RmR Apache-2.0 and upstream notices. No upstream BLAKE3 algorithm or code transplanted.

## Intent and source boundaries

- Target source: rmr/rmr-core/src/main/cpp/rmr_matrix_native.cpp (existing full C++ SIMD/JNI implementation, NOT DELETED).
- New C11 pure leaf: rmr/rmr-core/src/main/cpp/portable/rmr_matrix_f64.c and .h.
- New C JNI adapter: rmr/rmr-core/src/main/cpp/rmr_matrix_jni_c.c (Android/JVM boundary; NOT freestanding).
- Build selector: -DRMR_MATRIX_C11_BACKEND=ON (candidate, opt-in). Default OFF preserves compiled C++ backend; no changed Gradle, AndroidX or Java API.
- Source priority: this repository owns code/ABI; Drive owns receipt/index; Supabase may hold private metadata only; alphaXiv bibliographic sources do not authorize code changes.

## Structure

Android Java/ART → C JNI adapter → C ABI → freestanding C11 scalar kernel.
Original C++ SIMD backend is preserved behind default-OFF candidate flag as the rollback profile.
ARMv7 Q16/SMULL inline-ASM is under separate DRAFT PR#73; do not join or claim it is executing here.

## Contracts and exclusions

- No malloc, libc calls, syscalls, runtime, exception mechanism, class, virtual method, JNI or dynamic loader inside the C leaf.
- Integer shape and declared array lengths validated in widened arithmetic before output writes.
- Input/output range overlap rejected, even if only partially overlapping; A==B input is permitted.
- Caller owns buffers and lifetime; pointer alone does not prove ownership, concurrent safety or mapped capacity.
- Uses legacy scalar blocked order, with checked block-step arithmetic and explicit no-fast-math/no-FP-contraction flags. Numerical parity is not yet certified.
- JNI boundary releases pinned source arrays read-only; commits output only when kernel reports success.
- Original C++ AArch64 NEON and x86 SSE/AVX may be faster; NO performance parity asserted before physical comparison.
- Existing raw jlong pointer operations in rmr/rafaelia/src/main/cpp/rafaelia_core.cpp remain unresolved P0 and are not affected by this work.

## Evidence gates

G1_HOST_C11: GCC and Clang native C11 test cases: dimensions, capacity, arithmetic overflow, exact matrices, partial alias, nonmutation on rejection, 65-element block tail.
G2_OBJECT_CLOSURE: nm -u pure core contains zero undefined imports; no CRT/heap/externally linked calls in leaf object.
G3_CROSS_COMPILE: Clang ARMv7-A A32, Thumb2, AArch64 C objects; no target runtime claim.
G4_JNI_HOST: C JNI adapter object build with JDK headers; still no real JVM calls unless instrumented.
G5_ANDROID_DEVICE: ARMv7 + ARM64 real native builds, negative JNI instrumentation, Java scalar comparison, 32/64 ABI symbols.
G6_NONREGRESSION: source/lockfile preservation, x86/ARM SIMD performance and numerical equality, same-device metrics, ABI/AAR diff, independent rights review.

Gate G1-G4 scoped only; G5-G6 = TOKEN_VAZIO until actual evidence.
SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
IMPLEMENTED_UNTESTED != PASS. Hosted source CI != device PASS.
Claim allowed false; merge/release human review HOLD.

## μRECEIPT candidate

MU-ANDROIDX-CPP-C11-BLAKE3-V1 | ts=2026-11-07 Brazil |
source=GitHub androidx-main@8a75a3aba3e1452bea1feb45e64cba421a916c57 |
parent=MU-ANDROIDX-RMR-Q16-ARENA-20261107-S1 (related workflow, not source ancestry) |
kind=ISOLATED_CODE_CANDIDATE |
delta=new opt-in C11 leaf and C JNI adapter, host negative tests, cross compile gate, no deletion |
routes=L:AndroidX/O:JNI-C/T:ARM-ABI/P:GitHub/C:RmR/R:legacy-to-C/I:001_PONTEIRO/E:CI/A:rollback |
evidence=local host GCC/Clang C11 + ARM A32/Thumb/A64 object compile, C JNI compile, no imported pure-core symbols |
gap=GitHub exact-head CI, Android device, ABI/parity/perf, P0 raw jlong, Drive ledger write |
next=inspect exact-HEAD CI, falsify parity, only then consider selecting C backend |
rollback=disable option or close draft PR; inherited source untouched.
