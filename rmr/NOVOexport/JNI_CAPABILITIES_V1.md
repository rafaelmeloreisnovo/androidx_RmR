# RAFAELIA — JNI Capabilities / Ownership / Lifetime V1

## Source of truth and route
Repository: rafaelmeloreisnovo/androidx_RmR; target branch androidx-main; review only via draft PR. READ -> rmr/001_PONTEIRO/START_HERE_RAFAELIA_JNI_CAPS_V1.md -> rmr/rafaelia/src/main/cpp/rafaelia_capabilities.{h,c} -> rmr/rafaelia/src/main/cpp/rafaelia_core.cpp -> Java RafaeliaCore -> tests/capabilities -> CI.

## Threat boundary
Original JNI accepts raw jlong addresses in nativeAllocateAligned/nativeFreeAligned/nativeMemoryCopy/nativeMemorySet/nativeVectorAdd/nativeVectorMultiply/nativeMatrixMultiply/nativePrefetch. These signatures remain for ABI but are LEGACY_UNSAFE, not proven bounds/lifetime safe. The old nativeGetDirectBufferAddress remains for binary compatibility but new optimizedMemoryCopy must pass ByteBuffer objects directly to JNI and query actual direct capacity during the same JNI call.

## New region model
- OwnedRegion Java AutoCloseable is explicit one-owner (not GC-finalized). Caller must close it, ideally with try-with-resources; native allocation uses hosted posix_memalign, not freestanding code.
- Opaque token encodes fixed registry slot (64 total) and monotonically increasing generation; stale/double release rejected, no generation wrap. Tokens are forgeable by code within the same process; NOT a security sandbox or cryptographic capability.
- Bounded 1..16 MiB allocation, power-of-two alignment >= sizeof(void*) and <= 4096, and per-operation offset/count checks via offset <= capacity and count <= capacity-offset.
- One native mutex serializes use, copy, and release; operation completes before close frees or fails after close. Overlap copy uses memmove. Allocations are zeroed; release performs best-effort volatile wipe, not proof of physical erasure.
- Registry metadata has fixed size, but underlying allocations still use heap and POSIX. No background process/thread, no watchdog, no persistent resource. Java wrapper requires explicit close to avoid leaks.
- No integer-address escape through new API; JNI validates negative sizes/offsets. Existing externally-owned ByteBuffer lifetime is retained through live jobject references while copying; API caller must not concurrently mutate/directly free the source or destination outside the JNI contract.

## Tests and gates
H0 GCC/Clang host ASan+UBSan: 165 negative/positive checks each observed locally; includes ABA reuse, double free, bounds, slot exhaustion and release race. This is SOURCE_TEST_PASS_SCOPED, not Android JNI proof.
H1 GitHub exact-HEAD dedicated CI: TOKEN_VAZIO until run readback.
H2 Android NDK armeabi-v7a + arm64-v8a compile/link: TOKEN_VAZIO.
H3 Java API/JNI instrumentation incl direct buffers, byte order, overlap, false-return fallback and concurrent close: TOKEN_VAZIO.
H4 installed Motorola E7 Power physical transaction + memory pressure/long-duration and lifecycle receipts: TOKEN_VAZIO.
H5 migrate remaining raw jlong vector/matrix/set/prefetch APIs only after capacity provenance and call-site audit: BLOCKED.
H6 legal/provenance: module claims Apache-2.0 plus additional restrictions in LEGAL_NOTICE; compatibility and enforceability require independent review; not resolved here.
Performance, zombie process frequency, ASAN device, and memory fault rate: TOKEN_VAZIO (no baseline). Do not claim percentage improvements.

## Rollback and approval
No existing ABI symbol removed, existing CMake source entries retained, new native methods additive. Close draft PR to leave base unchanged; if later merged, revert only this scoped tranche with successor receipt. No auto-merge, release, privilege/permission change, or legacy deletion. SOURCE != ARTIFACT != EXECUTION != EVIDENCE != CLAIM; claim_allowed=false until platform gates.
