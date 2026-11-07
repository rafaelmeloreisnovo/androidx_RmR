/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 *
 * Pure freestanding zero-fill kernel: no libc, heap, syscall or JNI.
 * The caller must own at least count writable doubles. This is not a
 * bounds or lifetime validator. Only ordinary writable memory is supported.
 */
#ifndef RMR_ZERO_F64_H
#define RMR_ZERO_F64_H

static inline void rmr_zero_f64(double *dst, unsigned int count) {
    if (dst == (double *)0 || count == 0U) return;

#if defined(__aarch64__) && (defined(__GNUC__) || defined(__clang__))
    /* AArch64: two 64-bit zero stores per iteration, no libc call. */
    unsigned int pairs = count >> 1;
    if (pairs != 0U) {
        unsigned char *cursor = (unsigned char *)dst;
        __asm__ __volatile__(
            "1:\n\t"
            "stp xzr, xzr, [%[cursor]], #16\n\t"
            "subs %w[pairs], %w[pairs], #1\n\t"
            "b.ne 1b"
            : [cursor] "+r"(cursor), [pairs] "+r"(pairs)
            : : "cc", "memory");
    }
    if ((count & 1U) != 0U) dst[count - 1U] = 0.0;
#elif defined(__arm__) && defined(__ARM_NEON) && (defined(__GNUC__) || defined(__clang__))
    /* ARMv7 NEON: zero Q0 once, store 128 bits each iteration. */
    unsigned int pairs = count >> 1;
    if (pairs != 0U) {
        unsigned char *cursor = (unsigned char *)dst;
        __asm__ __volatile__(
            "veor q0, q0, q0\n\t"
            "1:\n\t"
            "vst1.64 {d0-d1}, [%[cursor]]!\n\t"
            "subs %[pairs], %[pairs], #1\n\t"
            "bne 1b"
            : [cursor] "+r"(cursor), [pairs] "+r"(pairs)
            : : "q0", "cc", "memory");
    }
    if ((count & 1U) != 0U) dst[count - 1U] = 0.0;
#else
    /* Deterministic portable fallback. No separate OS/runtime requirement. */
    for (unsigned int i = 0U; i < count; ++i) dst[i] = 0.0;
#endif
}

#endif /* RMR_ZERO_F64_H */
