# AGENTS.md — Scoped Rafaelia memory protocol

Applies only to rmr/rafaelia/**; start at ../001_PONTEIRO/START_HERE_ANDROIDX_LEASES_P0_V1.md.

- GitHub producer owns code and tests; Drive NEXUS/CURRENT_STATE own index/receipts; Supabase stores private event metadata.
- No destructive edit to upstream AndroidX, third-party code, licensing, existing exported JNI symbols or Java public API without explicit review.
- Keep freestanding C11 lease registry separate from JNI/posix allocator/OS mutex. BLAKE3 is a methodology reference, not imported code.
- P0: null, unknown, stale, overlap, capacity, double-free, ABA, races, quotas. Handles are not crypto capabilities.
- External locks MUST span both lease resolution AND use/free; use-after-free protection does not arise from generation alone.
- Safe mode opt-in may change private numeric jlong meaning. Document semantic compatibility separately from ABI.
- Measure source/profiler/CI/device states separately. Never call host compiler CI a physical-device PASS.
- No auto-merge, release, deletion, license replacement or background worker creation.
- One small index pointer per material change and append-only receipt with exact SHA, parent, gate and rollback.
