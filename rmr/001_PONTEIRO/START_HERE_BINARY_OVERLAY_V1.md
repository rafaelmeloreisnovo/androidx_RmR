# START HERE — RAFAELIA binary overlay / linker V1

STATUS = DRAFT/HOLD; claim_allowed=false. Scope: build composition, NOT new runtime logic.
Parent implementation: PR#75 (RMR_SAFE_LEASES OFF by default), producer head 28a88a636bc4d0e1741ec19f5bc75030cb69c520.
Reference architecture: BLAKE3 rmr/portable/CONTRACT.md — methodology only, no transplanted cryptographic code.

## 1. One-page human / IA navigation

SOURCE → front-end C/C++ → LLVM IR → machine code → ELF sections → relocations/symbols → lld → shared .so.
One byte has values 0..255; that fact alone NEVER proves two behaviors equivalent or two pointers interchangeable.
Code address, buffer address, physical alignment and object lifetime are four independent properties.

Existing leaf map:
- rmr_lease_v1.c: independent C11 registry, capacity and generation; data is mutable, must NOT be folded across owners.
- rafaelia_core.cpp: JNI, memory copy, vector/matrix math, branching for safe lease; function sectioning splits its compiled functions WITHOUT moving or duplicating source.
- rafaelia_cpu_detect.cpp: cold feature discovery; compact code preference is testable.
- rafaelia_memory.cpp, rafaelia_vector.cpp, rafaelia_matrix.cpp: comments/includes only at observed source ref, no runtime kernels to deduplicate; do NOT silently remove as they could later gain code.
- Android JNI wrappers and external dependencies remain; no claim that the complete Android .so is freestanding.

## 2. Composition: each flag has one responsibility

CMake: `-DRMR_BINARY_LAYOUT_EXPERIMENT=ON` is opt-in, OFF by default.
`-DRMR_BINARY_LAYOUT_TRACE=ON` prints lld retained/discarded and identical-code sections; requires EXPERIMENT=ON.

| Leaf/phase | Mechanism | Meaning | Unsafe extrapolation |
|---|---|---|---|
| Every existing function | `-ffunction-sections` | Individual `.text.<function>` entries | Does not make function calls free |
| Existing initialized constants/data | `-fdata-sections` | Isolated data sections | Does not establish ownership |
| Linker liveness | `--gc-sections` | Drop unreachable input sections | Export roots must remain |
| Linker safe identity | `--icf=safe` | Fold eligible identical code addresses | Do not use `--icf=all` for address-significant functions |
| Cold CPU discovery | `-Oz -fno-unroll-loops` | Favor compact instructions | Must check same CPU feature result |
| Cold C11 lease registry | `-Oz -fno-unroll-loops` | Favor compact checks | Never remove capacity/generation/locks |
| Link map | `-Map,<path>` | Verify sections, symbols, alignment and padding | Map alone is not device runtime |
| Warnings | existing `-Wall -Wextra -Werror` | Fail on known diagnostic regression | Suppressing a warning does not fix a bug |

Existing `-O3 -funroll-loops` for hot JNI/SIMD stays unchanged in this experiment; Release LTO remains the producer policy.
Never force 64-byte alignment on every function: padding can INCREASE binary size. ABI alignment and data/pointer alignment are different concerns.
`void` declares a return type / pointer genericity and does NOT instruct lld to fold. Comments do not emit instructions except where they are preprocessor pragmas or affect actual source compilation.

## 3. Source-level invariants

- Correctness and lifetime outrank size. No overlap/union of live lease slots or mutable `.data/.bss`.
- No hidden symbol rename or ABI change. JNI-exported entrypoints must remain addressable from Android.
- No manual assembly interleaving / overlapping executable addresses. Thumb2 and AArch64 instruction encodings are different.
- No zero-import/zero-dependency claim for Android JNI; freestanding only applies to the pure C11 leaf.
- Function duplication may be removed only when behavior, ABI, unwind requirements and observable function address semantics permit it.

## 4. Exact reproducibility path

Run `sh rmr/rafaelia/src/test/native/check_lease_v1.sh` (GCC / Clang host, C11 negatives).
The same existing C source + test are linked via lld as:
  (a) plain; (b) -ffunction-sections / -fdata-sections / --gc-sections;
  (c) b + --icf=safe; (d) c + -Os -fno-unroll-loops.
Every variant runs the same negative suite, checks all four public registry functions remain, and prints ELF size and .text alignment.
This is a *bundled, scoped host experiment*: do NOT infer causal benefit for Android, device speed, or security from a reduction in bytes.
For Android native opt-in: `-DRMR_SAFE_LEASES=ON -DRMR_BINARY_LAYOUT_EXPERIMENT=ON`; produce NDK ARMv7/arm64 link maps, compare two variants with identical SHA/toolchain/ABI.
Ensure `nm -D` JNI exports identical, `readelf -d` dependencies unchanged, no new unexpected undefined symbols; actual .so section sizes, link map, and device test receipts needed.
No APK release, merge, user device action or claimed performance increase until reviewed.

## 5. R3

F_ok: section-based binary layout experiment defined, existing runtime code preserved.
F_gap: exact-HEAD linker metrics/physical Android ABIs/size and CPU runtime equality are TOKEN_VAZIO until tests.
F_next: inspect workflow, measure host bytes and ABI link maps, compare before/after without regressing P0.
Rollback: leave option OFF, or close DRAFT PR; upstream AndroidX untouched.
