# START HERE — AndroidX RmR: Q16 mathematical kernels and bounded native arena V1

**State:** experimental isolated source + scoped local object/host gates. **Claim allowed:** false.
**Producer:** rafaelmeloreisnovo/androidx_RmR. **Base:** androidx-main, unchanged; delivery by separate DRAFT PR.
**Boundary:** only new files in rmr/rmr-core and a scoped CI gate. No legacy AndroidX edits, JNI cutover, AAR build, device runtime or release.

## Authority and sources

- Canonical dispatch: RAFAELIA START HERE Ω V2.2, current HOTSTATE, and 001_PONTEIRO/ATLAS. Read CURRENT_STATE before history.
- Mathematical basis: classical equilateral altitude h=(sqrt(3)/2)a, finite recurrence r[n+1]=q*r[n], q=sqrt(3)/2, and Q16.16 quantization. Project's private Matem-tica- and TeoremasTesesTeorias formal indices remain source-of-truth; no private code or paper content is copied here.
- Memory methodology: papers' private Research Memory Unit framework distinguishes physical zero, unknown semantics, absent evidence, and actual recovery.
- Prior art checked in alphaXiv collections for fixed-point/geometry, freestanding ELF ABI, and system-memory provenance. Bibliographic membership is not a device test, a novelty proof or permission to publish private text.
- Authorship: this new RmR work carries Apache-2.0; inherited AndroidX and upstream notices remain unchanged.

## Micro-modules

| Path | Contract |
| --- | --- |
| rmr_q16_mul.h | Signed Q16.16 checked 32x32->64 multiply; ARMv7 SMULL/AArch64 SMULL; scalar fallback; truncation toward zero; reject overflow without touching output |
| rmr_q16_geom.h | Constant sqrt(3)/2=56756 Q16; altitude and contracting radial step; finite quantized behavior only |
| rmr_native_arena.h | Caller-owned byte region; power-of-two alignment, capacity and range checks, generational handles, no heap and no syscalls |
| rmr_q16_arena_test.c | Host positive/negative tests: altitude, 3/4 quantization, overflow, 42 finite steps, alignment, exhaustion, stale handle |
| rmr_q16_cpp_smoke.cpp | Cross-language compile guard for existing C++ JNI translation units |
| rmr-q16-native-arena.yml | Host test + separate A32/Thumb2/A64 object gates and global-undefined-symbol rejection |

## Safety boundary

- An arena slice is NOT an unforgeable capability or proof of native ownership. The caller guarantees the byte region is live and exclusive until reset; do not call init again while handles exist.
- A borrowed pointer returned by resolve can become stale after reset. The arena is not thread-safe and does not clear memory, prevent double-free outside this allocator, or validate arbitrary jlong pointers.
- Q16 truncation introduces quantization. q²=49152 for this chosen representation, not exact symbolic equality over real numbers. No geodetic or physical claim.
- No new native export is called by Android/ART yet. C11 source freestanding does NOT mean a standalone Android application is freestanding.
- TOKEN_VAZIO != 0. NO_SPACE, STALE, BAD_ARG, OUT_OF_RANGE and GEN_EXHAUSTED are distinct operational error states.

## Gates and remaining risks

LOCAL_HOST_C11=PASS_SCOPED; LOCAL_C11_ARMv7_A32_THUMB2_A64=OBJECT_COMPILED; LOCAL_CXX17_ARMv7_A32_THUMB2_A64=OBJECT_COMPILED.
REMOTE_EXACT_HEAD_CI=TOKEN_VAZIO until readback. PHYSICAL_ARM_DEVICE=NOT_RUN. JNI_NEGATIVE_HARNESS=NOT_RUN. AAR_PARITY=NOT_RUN.
Remaining P0: legacy rmr/rafaelia/src/main/cpp/rafaelia_core.cpp exports raw jlong pointers and uses memcpy/memset without caller-bound native ownership; this tranche does NOT certify or rewrite them.
Before integration, replace the raw-pointer surface with owned handle APIs and prove object lifetime/capacity, aliased copies, negative JNI paths, and device parity.
Rollback: close this DRAFT PR; no default branch changes. If independently merged later, use reviewed revert commit.

## μRECEIPT proposal (promote only after provider readback)

ANDROIDX-RMR-Q16-ARENA-20261107-S1 | parent=rmr/001_PONTEIRO/START_HERE_ANDROIDX_INLINE_ASM_V2.md | kind=isolated_implementation | source=Matem-tica- formal Q16 + papers memory methodology | artifact=3 headers + 2 tests + CI + pointer | routes=L,O,T,P,I,E | evidence=local_host_and_cross_object_scope | gap=remote_CI_JNI_hardware_missing | next=readback exact HEAD then device and JNI negative gates | claim_allowed=false

R3 = <bounded source and local compiler evidence, remote/JNI/device gap, exact-head CI then scoped owned-handle integration>.
