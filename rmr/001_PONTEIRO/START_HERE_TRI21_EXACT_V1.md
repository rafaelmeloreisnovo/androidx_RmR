# START HERE — RAFAELIA TRI21 EXACT V1

STATUS: independent mathematical-data candidate; NOT wired into JNI, CMake, Android, GPU or production. No performance/physics novelty claim.

## What has an explicit, falsifiable meaning

- ONE cell: exactly 2 bits: 00=ABSENT, 01=PRESENT, 10=EMPTY, 11=INVALID.
- EMPTY is a third **represented** state, not integer 0, not Boolean false, and not an asserted sample of a physical signal. UNKNOWN as a distinct fourth domain state is **not modeled**.
- 3x7 = row-major 21 cells = 42 used bits in a uint64 (top 22 bits MUST be zero).
- 21 opposed to 21: two independent 42-bit operands. Comparison is by **same index** only; there is no invented mirror/rotation/reversal geometry.
- 5x8 = row-major 40 cells = 80 used bits stored in two uint64 words (upper 48 bits of word[1] MUST be zero). It is a separate shape, NOT a derived projection of the 3x7 grid.
- Three 3x7 grids can be stored as three independent words; algebra connecting them is TOKEN_VAZIO until a human specifies a transform.
- A proposed -10..+10 coordinate label is an annotation, not a demonstrated physical coordinate, harmonic, group operation or conversion of bytes to frequencies.

## Specialist C leaves

SOURCE (producer): rmr/rmr-core/src/main/cpp/portable/
- rmr_tri_state.h: C ABI, 2-bit codes, domain and shape contracts.
- rmr_tri21_scalar.c: independent scalar oracle, read/write/validation, 9-cell confusion-count matrix.
- rmr_tri21_bitsliced.c: 21-lane SWAR (SIMD within a register) 64-bit state masks, count matrix, reserved-bit rejection and no partial mutation on error.
- rmr_tri40.c: independent 5x8 storage validation. Does not invent mapping to 21.
TEST: rmr/rmr-core/src/test/native/rmr_tri21_exact_test.c + check_tri21_exact.sh
CI: .github/workflows/rmr-tri21-exact.yml
ABI: C11; caller owns all storage, result out_counts has at least 9 unsigned slots; no malloc, libc calls, syscalls, JNI or GPU calls in the leaf.

## Gates, evidence and priority

1. 2500 deterministic paired frames; independent scalar and bit-slice must have identical 9 counts; sum of all buckets=21.
2. Fail closed on invalid 11, spare high bits, out-of-range rows/cols, NULL; no mutation of caller output on error.
3. GCC/Clang host full execution; Clang ASan/UBSan; ARMv7 A32 and Thumb2, AArch64 object compile and nm -u empty. ARM cross objects are NOT device execution.
4. Link into Android only after exact-head CI, ABI export check, physical ARMv7/ARM64 test and proof of a hot call site. It is likely 21 states may fit better as SWAR than a NEON/GPU transfer, but comparative speed is TOKEN_VAZIO.
5. Do NOT infer sine waves, harmonic frequency, angular momentum, torque/tension or GPU equivalence from bit patterns. Need sample times, unit definitions, fixed-point error budget, boundary and recurrence equations, calibration/provenance to authorize those operators.

RISK FENCES: separate from open PRs #73 (Q16), #74 (C11 matrix), #75 (native memory) and #77 (ELF linker). No user-device action; no upstream deletion, no auto-merge; the legacy Android binary is unchanged. Preserve original license/provenance.
ROLLBACK: close this DRAFT branch; no production option to disable because no runtime integration was added.

SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM.
IMPLEMENTED_UNTESTED != PASS. TOKEN_VAZIO != 0.
R3=<F_ok:typed 21/40 scalar-vs-bit-slice candidate,F_gap:semantic transform,physical JNI and benchmark,F_next:exact-head CI→real ARM profiling→explicit operator spec>.
