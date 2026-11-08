/* Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * Independent scalar oracle, no SIMD-specific assumptions.
 */
#include "rmr_tri_state.h"

int rmr_tri21_validate(rmr_u64 packed) {
    if ((packed & ~RMR_TRI21_USED_BITS) != 0u) return RMR_TRI_STATUS_ENCODING;
    for (unsigned pos = 0; pos < RMR_TRI21_COUNT; ++pos) {
        if (((packed >> (2u * pos)) & 3u) == RMR_TRI_INVALID)
            return RMR_TRI_STATUS_ENCODING;
    }
    return RMR_TRI_STATUS_OK;
}

int rmr_tri21_fill(rmr_u64 *packed, unsigned state) {
    if (packed == (rmr_u64 *)0) return RMR_TRI_STATUS_NULL;
    if (state > RMR_TRI_EMPTY) return RMR_TRI_STATUS_ENCODING;
    rmr_u64 value = 0;
    for (unsigned i = 0; i < RMR_TRI21_COUNT; ++i)
        value |= (rmr_u64)state << (2u * i);
    *packed = value;
    return RMR_TRI_STATUS_OK;
}

int rmr_tri21_put(rmr_u64 *packed, unsigned row, unsigned col, unsigned state) {
    if (packed == (rmr_u64 *)0) return RMR_TRI_STATUS_NULL;
    if (row >= 3u || col >= 7u) return RMR_TRI_STATUS_RANGE;
    if (state > RMR_TRI_EMPTY || rmr_tri21_validate(*packed) != RMR_TRI_STATUS_OK)
        return RMR_TRI_STATUS_ENCODING;
    const unsigned shift = 2u * (7u * row + col);
    const rmr_u64 mask = (rmr_u64)3 << shift;
    *packed = (*packed & ~mask) | ((rmr_u64)state << shift);
    return RMR_TRI_STATUS_OK;
}

int rmr_tri21_get(rmr_u64 packed, unsigned row, unsigned col, unsigned *out_state) {
    if (out_state == (unsigned *)0) return RMR_TRI_STATUS_NULL;
    if (row >= 3u || col >= 7u) return RMR_TRI_STATUS_RANGE;
    if (rmr_tri21_validate(packed) != RMR_TRI_STATUS_OK)
        return RMR_TRI_STATUS_ENCODING;
    *out_state = (unsigned)((packed >> (2u * (7u * row + col))) & 3u);
    return RMR_TRI_STATUS_OK;
}

int rmr_tri21_compare_scalar(rmr_u64 left, rmr_u64 right, unsigned out_counts[9]) {
    if (out_counts == (unsigned *)0) return RMR_TRI_STATUS_NULL;
    if (rmr_tri21_validate(left) != RMR_TRI_STATUS_OK ||
        rmr_tri21_validate(right) != RMR_TRI_STATUS_OK)
        return RMR_TRI_STATUS_ENCODING;
    unsigned counts[9] = {0};
    for (unsigned index = 0; index < RMR_TRI21_COUNT; ++index) {
        const unsigned a = (unsigned)((left >> (2u * index)) & 3u);
        const unsigned b = (unsigned)((right >> (2u * index)) & 3u);
        counts[a * 3u + b] += 1u;
    }
    for (unsigned i = 0; i < 9u; ++i) out_counts[i] = counts[i];
    return RMR_TRI_STATUS_OK;
}
