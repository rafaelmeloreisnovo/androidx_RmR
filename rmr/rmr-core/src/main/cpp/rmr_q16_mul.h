/* Copyright (C) 2026 Rafael Melo Reis (RmR) - SPDX-License-Identifier: Apache-2.0 */
#ifndef RMR_Q16_MUL_H
#define RMR_Q16_MUL_H
#include <stdint.h>
#include <limits.h>

/* Signed Q16.16, truncation toward zero; no saturation or implicit wrap.
 * Returns 1 on success. On rejection *out is untouched.
 * Freestanding leaf: no libc, heap, JNI, OS call or runtime ISA probe.
 */
static inline int rmr_q16_mul_checked(int32_t a, int32_t b, int32_t *out) {
    if (out == (int32_t *)0) return 0;
    int64_t product;
#if defined(__aarch64__) && (defined(__GNUC__) || defined(__clang__))
    __asm__ __volatile__("smull %0, %w1, %w2" : "=r"(product) : "r"(a), "r"(b));
#elif defined(__arm__) && (defined(__GNUC__) || defined(__clang__))
    uint32_t lo;
    int32_t hi;
    __asm__ __volatile__("smull %0, %1, %2, %3"
                         : "=&r"(lo), "=&r"(hi) : "r"(a), "r"(b));
    /* Compose without signed left shift or out-of-range unsigned cast. */
    product = (int64_t)hi * INT64_C(4294967296) + (int64_t)lo;
#else
    product = (int64_t)a * (int64_t)b;
#endif
    const int64_t q = product / INT64_C(65536);
    if (q < (int64_t)INT32_MIN || q > (int64_t)INT32_MAX) return 0;
    *out = (int32_t)q;
    return 1;
}
#endif
