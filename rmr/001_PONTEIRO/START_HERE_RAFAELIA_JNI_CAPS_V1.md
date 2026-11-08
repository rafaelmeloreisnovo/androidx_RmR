# 001_PONTEIRO — RAFAELIA JNI Memory Safety

INTENT: Replace raw pointer use in rafaelia_core.cpp gradually, without breaking Android ABI.

CURRENT: rmr/NOVOexport/JNI_CAPABILITIES_V1.md.
READ: that doc -> src/main/cpp/rafaelia_capabilities.h -> src/main/cpp/rafaelia_capabilities.c -> src/main/cpp/rafaelia_core.cpp -> Java RafaeliaCore.OwnedRegion -> tests/capabilities/capabilities_host_test.c -> GitHub Actions RAFAELIA JNI capability safety.
CONTEXT: PR73 rmr_native_arena.h is independent and not yet integrated into the JNI boundary; PR74 C11 matrix separate. Preserve those branches/rights.
GATES: local 165 host checks GCC+Clang PASS_SCOPED; Android/NDK/JNI/device NOT_RUN; high risk legacy raw APIs remain. No claim of actual device memory safety.
OWNER: androidx_RmR producer. ROLLBACK: close draft PR / revert addition and tiny callsite cut. DELTA/receipt lives in linked Drive ledger only after readback.
R3 = <bounded native region & direct ByteBuffer JNI callsite, Android device evidence missing, JNI/ABI tests and later raw API migration>.
