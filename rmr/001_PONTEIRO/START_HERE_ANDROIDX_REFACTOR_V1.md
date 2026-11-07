# START HERE — AndroidX RmR refactor, isolated safety tranche V1

**Status:** IMPLEMENTED_UNTESTED_REMOTE; no merge or release.
**Producer:** `rafaelmeloreisnovo/androidx_RmR`, branch `refactor/rmr-freestanding-p0-20261107`.
**Baseline:** `androidx-main@443754ad0cdad13832feffbb3f9ca5dd0e93b713` (observed before branch creation).
**Authority:** owner repository `rmr/**`; preserve upstream AndroidX and existing licenses.
**Method:** Define → Measure → Analyze → Improve → Control; one bounded, reversible tranche.

## Source, artifact, execution, evidence, claim

- SOURCE: `rmr/rmr-core/src/main/cpp/rmr_matrix_native.cpp`, `RmRMatrixOps.java`, documented boundary.
- ARTIFACT: header `rmr_matrix_contract.h`, updated JNI and Java adapters, C and JVM negative tests, scoped CI.
- EXECUTION: host C11 test locally prior to PR; remote CI and real device = not yet observed.
- EVIDENCE: host shape-gate test passed locally before GitHub commit; remote test = TOKEN_VAZIO; device runtime = TOKEN_VAZIO.
- CLAIM: only protection of checked shapes and explicit output alias rejection is implemented; no asserted performance gain, full freestanding AndroidX or complete native memory safety.

## Refactor boundary

```text
AndroidX legacy/upstream (unchanged)
     ↓ JNI / Gradle / Android-specific adapter
RmR matrix shape guard (pure C11, no libc/syscall/heap)
     ↓ checked dimensions/capacity
Existing native matrix kernel (C++/JNI, retained for compatibility)
```

**Security precondition:** validate dimensions, accessible Java array lengths and non-aliased result *before* any `GetDoubleArrayElements`. Kernel indexing uses signed 32-bit ints; 64-bit intermediate arithmetic checks bounds. The shape gate cannot prove native pointer lifetime, memory ownership or concurrency.

**Behavioral distinction:** `multiplyInto` previously zeroed an aliased result before reading input. It now rejects aliasing with `IllegalArgumentException` before mutation; ordinary disjoint output behavior remains unchanged.

## Follow-on tranches (not executed here)

1. P0: replace raw `jlong` native address APIs in `rmr/rafaelia/src/main/cpp/rafaelia_core.cpp` with owned/capacity-checked buffers and explicit lifetime; test on armeabi-v7a and arm64-v8a, with malicious lengths and aliasing.
2. P0: prove JNI negative paths via instrumented tests or a JNI harness; host shape tests alone are insufficient.
3. P1: separate freestanding pure C primitives from OS/JNI adapters, independent compile targets, no unnecessary heap or libc; ABIs verified independently.
4. P1: retain Gradle, Jetpack and upstream code as compatibility layers until each replaced API passes parity tests, licenses/provenance and rollback gates.
5. P1: audit `kotlin-js-store/yarn.lock` independently; no lockfile mutation in this tranche.
6. P2: reproducible AAR hashes, public API/ABI diff, dependency graph and measured CPU/memory benchmarks (same hardware, fixed inputs and baseline).

## Gates and falsifiers

- **HOST_C11:** compile `rmr/rmr-core/src/test/native/rmr_matrix_contract_test.c` with `-std=c11 -ffreestanding -Wall -Wextra -Werror`; run and expect exit 0.
- **JVM:** `:rmr:rmr-core:test` must pass, including output alias negative cases.
- **ANDROID_ABI:** Android native build for `armeabi-v7a` and `arm64-v8a` with exact head evidence; not yet run.
- **PROVENANCE:** retain Apache-2.0 headers, upstream attribution and `rmr/AUTHORSHIP_AND_LICENSE.md`; any claims of additional restrictions require legal review, not silent relicensing.
- **REGRESSION:** compare same-head upstream audit and AAR hashes. No release or merge without review.
- **ROLLBACK:** close draft PR / revert the exact commits; baseline remains immutable. No deletion of upstream source.

## μRECEIPT (provisional, until remote readback)

`ANDROIDX-RMR-SHAPE-P0-20261107 | source=androidx-main@443754ad | parent=none | kind=isolated_patch | delta=freestanding 64-bit shape gate + JNI lengths/alias validation + Java fail-closed + host/JVM tests | L9=P,E,I | evidence=local_host_exit_0 | gap=CI_device_runtime_TOKEN_VAZIO | next=exact-head CI+negative JNI/device tests | claim_allowed=false`

**R3** = <local shape test OK + isolated source patch, CI/device/provenance review pending, exact-head CI then address raw pointer APIs>.
