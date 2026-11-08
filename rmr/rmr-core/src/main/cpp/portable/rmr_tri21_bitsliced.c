/* Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * Parallel 21-lane bit-slice classification in one u64; no NEON required.
 * No heap, OS, JNI, dynamic allocation, or math-library imports.
 */
#include "rmr_tri_state.h"

static unsigned rmr_popcount64(rmr_u64 value) {
    value -= (value >> 1) & (rmr_u64)0x5555555555555555ULL;
    value = (value & (rmr_u64)0x3333333333333333ULL) +
            ((value >> 2) & (rmr_u64)0x3333333333333333ULL);
    value = (value + (value >> 4)) & (rmr_u64)0x0f0f0f0f0f0f0f0fULL;
    value += value >> 8;
    value += value >> 16;
    value += value >> 32;
    return (unsigned)(value & 0x7fu);
}

int rmr_tri21_compare_bitsliced(rmr_u64 left, rmr_u64 right, unsigned out_counts[9]) {
    if (out_counts == (unsigned *)0) return RMR_TRI_STATUS_NULL;
    if (((left | right) & ~RMR_TRI21_USED_BITS) != 0u)
        return RMR_TRI_STATUS_ENCODING;
    const rmr_u64 al = left & RMR_TRI21_LANE_BITS;
    const rmr_u64 ah = (left >> 1) & RMR_TRI21_LANE_BITS;
    const rmr_u64 bl = right & RMR_TRI21_LANE_BITS;
    const rmr_u64 bh = (right >> 1) & RMR_TRI21_LANE_BITS;
    if (((al & ah) | (bl & bh)) != 0u) return RMR_TRI_STATUS_ENCODING;
    const rmr_u64 aa[3] = {
        (~al & ~ah) & RMR_TRI21_LANE_BITS,
        (al & ~ah) & RMR_TRI21_LANE_BITS,
        (~al & ah) & RMR_TRI21_LANE_BITS
    };
    const rmr_u64 bb[3] = {
        (~bl & ~bh) & RMR_TRI21_LANE_BITS,
        (bl & ~bh) & RMR_TRI21_LANE_BITS,
        (~bl & bh) & RMR_TRI21_LANE_BITS
    };
    unsigned counts[9];
    for (unsigned i = 0; i < 3u; ++i)
        for (unsigned j = 0; j < 3u; ++j)
            counts[i * 3u + j] = rmr_popcount64(aa[i] & bb[j]);
    for (unsigned i = 0; i < 9u; ++i) out_counts[i] = counts[i];
    return RMR_TRI_STATUS_OK;
}
