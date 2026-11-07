/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 *
 * Pure freestanding shape gate. No libc, allocation, JNI or OS calls.
 * This verifies dimension arithmetic and declared array lengths only;
 * it does not establish native pointer ownership or memory lifetime.
 */
#ifndef RMR_MATRIX_CONTRACT_H
#define RMR_MATRIX_CONTRACT_H

static inline int rmr_matrix_multiply_shape_valid(
        int a_rows, int a_cols, int b_rows, int b_cols,
        int a_length, int b_length, int result_length) {
    if (a_rows <= 0 || a_cols <= 0 || b_rows <= 0 || b_cols <= 0 ||
            a_length < 0 || b_length < 0 || result_length < 0 ||
            a_cols != b_rows) {
        return 0;
    }

    /* Widen before multiplying: never multiply potentially overflowing ints. */
    const unsigned long long a_count =
            (unsigned long long)(unsigned int)a_rows * (unsigned int)a_cols;
    const unsigned long long b_count =
            (unsigned long long)(unsigned int)b_rows * (unsigned int)b_cols;
    const unsigned long long result_count =
            (unsigned long long)(unsigned int)a_rows * (unsigned int)b_cols;

    /* Kernels index by signed 32-bit int; ensure every index fits. */
    const unsigned long long max_indexed_count = 0x7fffffffULL;
    if (a_count > max_indexed_count || b_count > max_indexed_count ||
            result_count > max_indexed_count) {
        return 0;
    }
    return (unsigned long long)(unsigned int)a_length >= a_count &&
            (unsigned long long)(unsigned int)b_length >= b_count &&
            (unsigned long long)(unsigned int)result_length >= result_count;
}

#endif /* RMR_MATRIX_CONTRACT_H */
