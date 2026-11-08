/* Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * No libc, heap, syscalls, JNI, dynamic loader or external provider. */
#include <stdint.h>
#include "rmr_matrix_f64.h"
#include "../rmr_matrix_contract.h"

/* A true overlap check must examine the entire touched range, not only ==. */
static int rmr_overlap_doubles(
        const double *a, unsigned long long a_count,
        const double *b, unsigned long long b_count) {
    const uintptr_t a_start = (uintptr_t)a;
    const uintptr_t b_start = (uintptr_t)b;
    const uintptr_t max_address = (uintptr_t)~(uintptr_t)0;
    const uintptr_t max_elements = max_address / (uintptr_t)sizeof(double);
    if (a_count > (unsigned long long)max_elements ||
            b_count > (unsigned long long)max_elements) {
        return -1;
    }
    const uintptr_t a_bytes = (uintptr_t)a_count * (uintptr_t)sizeof(double);
    const uintptr_t b_bytes = (uintptr_t)b_count * (uintptr_t)sizeof(double);
    if (a_start > max_address - a_bytes || b_start > max_address - b_bytes) {
        return -1;
    }
    return a_start < b_start + b_bytes && b_start < a_start + a_bytes;
}

int rmr_matrix_f64_multiply(
        const double *a, int a_length, int a_rows, int a_cols,
        const double *b, int b_length, int b_rows, int b_cols,
        double *out, int out_length) {
    if (a == (const double *)0 || b == (const double *)0 ||
            out == (double *)0 ||
            !rmr_matrix_multiply_shape_valid(a_rows, a_cols, b_rows, b_cols,
                    a_length, b_length, out_length)) {
        return RMR_MATRIX_F64_INVALID;
    }
    const unsigned long long a_count =
            (unsigned long long)(unsigned int)a_rows * (unsigned int)a_cols;
    const unsigned long long b_count =
            (unsigned long long)(unsigned int)b_rows * (unsigned int)b_cols;
    const unsigned long long out_count =
            (unsigned long long)(unsigned int)a_rows * (unsigned int)b_cols;
    const int overlap_a = rmr_overlap_doubles(out, out_count, a, a_count);
    const int overlap_b = rmr_overlap_doubles(out, out_count, b, b_count);
    if (overlap_a < 0 || overlap_b < 0) {
        return RMR_MATRIX_F64_INVALID;
    }
    if (overlap_a != 0 || overlap_b != 0) {
        return RMR_MATRIX_F64_ALIAS;
    }
    /* All validation precedes the first destination write. */
    for (unsigned int i = 0; i < (unsigned int)out_count; ++i) {
        out[i] = 0.0;
    }
    /* Keep the legacy 64x64x64 scalar accumulation order for parity. */
    const int block = 64;
    for (int ii = 0; ii < a_rows;) {
        const int i_step = a_rows - ii < block ? a_rows - ii : block;
        const int i_end = ii + i_step;
        for (int kk = 0; kk < a_cols;) {
            const int k_step = a_cols - kk < block ? a_cols - kk : block;
            const int k_end = kk + k_step;
            for (int jj = 0; jj < b_cols;) {
                const int j_step = b_cols - jj < block ? b_cols - jj : block;
                const int j_end = jj + j_step;
                for (int i = ii; i < i_end; ++i) {
                    for (int k = kk; k < k_end; ++k) {
                        const double aik = a[i * a_cols + k];
                        const int b_offset = k * b_cols;
                        const int out_offset = i * b_cols;
                        for (int j = jj; j < j_end; ++j) {
                            out[out_offset + j] += aik * b[b_offset + j];
                        }
                    }
                }
                jj = j_end;
            }
            kk = k_end;
        }
        ii = i_end;
    }
    return RMR_MATRIX_F64_OK;
}
