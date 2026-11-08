/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * Independent C11 freestanding candidate: caller-owned buffers, no JNI/libc/heap.
 */
#ifndef RMR_MATRIX_F64_H
#define RMR_MATRIX_F64_H
#ifdef __cplusplus
extern "C" {
#endif
enum {
    RMR_MATRIX_F64_OK = 0,
    RMR_MATRIX_F64_INVALID = 1,
    RMR_MATRIX_F64_ALIAS = 2
};
/*
 * Array lengths are in doubles, not bytes. The entire operation fails before
 * modifying the destination on invalid dimensions/capacity or any overlap.
 * Valid, disjoint output is overwritten (never accumulated across calls).
 * No concurrency, lifetime or true ownership is inferred from a raw pointer.
 */
int rmr_matrix_f64_multiply(
        const double *a, int a_length, int a_rows, int a_cols,
        const double *b, int b_length, int b_rows, int b_cols,
        double *out, int out_length);
#ifdef __cplusplus
}
#endif
#endif
