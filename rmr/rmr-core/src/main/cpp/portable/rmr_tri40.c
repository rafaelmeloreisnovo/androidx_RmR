/* Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * 5x8 storage only: 40 independent 2-bit lanes in two caller-owned words.
 */
#include "rmr_tri_state.h"

int rmr_tri40_validate(const rmr_tri40 *grid) {
    if (grid == (const rmr_tri40 *)0) return RMR_TRI_STATUS_NULL;
    if ((grid->words[1] >> 16) != 0u) return RMR_TRI_STATUS_ENCODING;
    for (unsigned i = 0; i < RMR_TRI40_COUNT; ++i) {
        const unsigned word = i >> 5;
        const unsigned shift = (i & 31u) * 2u;
        if (((grid->words[word] >> shift) & 3u) == RMR_TRI_INVALID)
            return RMR_TRI_STATUS_ENCODING;
    }
    return RMR_TRI_STATUS_OK;
}

int rmr_tri40_fill(rmr_tri40 *grid, unsigned state) {
    if (grid == (rmr_tri40 *)0) return RMR_TRI_STATUS_NULL;
    if (state > RMR_TRI_EMPTY) return RMR_TRI_STATUS_ENCODING;
    rmr_tri40 next = {{0, 0}};
    for (unsigned i = 0; i < RMR_TRI40_COUNT; ++i)
        next.words[i >> 5] |= (rmr_u64)state << (2u * (i & 31u));
    *grid = next;
    return RMR_TRI_STATUS_OK;
}

int rmr_tri40_put(rmr_tri40 *grid, unsigned row, unsigned col, unsigned state) {
    if (grid == (rmr_tri40 *)0) return RMR_TRI_STATUS_NULL;
    if (row >= 5u || col >= 8u) return RMR_TRI_STATUS_RANGE;
    if (state > RMR_TRI_EMPTY || rmr_tri40_validate(grid) != RMR_TRI_STATUS_OK)
        return RMR_TRI_STATUS_ENCODING;
    const unsigned index = row * 8u + col;
    const unsigned word = index >> 5;
    const unsigned shift = 2u * (index & 31u);
    const rmr_u64 mask = (rmr_u64)3 << shift;
    grid->words[word] = (grid->words[word] & ~mask) | ((rmr_u64)state << shift);
    return RMR_TRI_STATUS_OK;
}

int rmr_tri40_get(const rmr_tri40 *grid, unsigned row, unsigned col, unsigned *out_state) {
    if (grid == (const rmr_tri40 *)0 || out_state == (unsigned *)0)
        return RMR_TRI_STATUS_NULL;
    if (row >= 5u || col >= 8u) return RMR_TRI_STATUS_RANGE;
    if (rmr_tri40_validate(grid) != RMR_TRI_STATUS_OK) return RMR_TRI_STATUS_ENCODING;
    const unsigned index = row * 8u + col;
    *out_state = (unsigned)((grid->words[index >> 5] >> (2u * (index & 31u))) & 3u);
    return RMR_TRI_STATUS_OK;
}
