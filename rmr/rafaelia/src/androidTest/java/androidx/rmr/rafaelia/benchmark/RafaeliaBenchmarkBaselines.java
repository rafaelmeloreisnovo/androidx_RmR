/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 with Additional Restrictions.
 * See LEGAL_NOTICE.md for complete terms and automatic penalty provisions.
 */

package androidx.rmr.rafaelia.benchmark;

/**
 * Baseline numbers and guardrail settings for Rafaelia microbenchmarks.
 */
final class RafaeliaBenchmarkBaselines {
    static final long RANDOM_SEED = 0x5EEDB00BL;

    static final int MATRIX_SIZE = 128;
    static final int VECTOR_LENGTH = 4096;
    static final int MEMORY_COPY_BYTES = 1 << 20; // 1 MiB

    static final long MATRIX_MULTIPLY_BASELINE_NS = 3_500_000L; // 3.5 ms
    static final long VECTOR_ADD_BASELINE_NS = 150_000L; // 0.15 ms
    static final long VECTOR_MULTIPLY_BASELINE_NS = 170_000L; // 0.17 ms
    static final long MEMORY_COPY_BASELINE_NS = 500_000L; // 0.5 ms

    static final double MAX_REGRESSION_RATIO = 1.20; // 20%

    private RafaeliaBenchmarkBaselines() {}
}
