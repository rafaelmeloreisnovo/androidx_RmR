/* Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * Compare independent scalar reference and bit-sliced implementation.
 */
#include "../../main/cpp/portable/rmr_tri_state.h"
#define CHECK(x) do { if (!(x)) return __LINE__; } while (0)
static rmr_u32 seed = 0x91ab73e3u;
static rmr_u32 next(void) { seed = seed * 1664525u + 1013904223u; return seed; }

int main(void) {
    rmr_u64 left = 0, right = 0, original = 0;
    rmr_tri40 grid40;
    unsigned scalar[9], fast[9], x = 7;
    CHECK(rmr_tri21_fill(&left, RMR_TRI_EMPTY) == 0);
    CHECK(rmr_tri21_fill(&right, RMR_TRI_ABSENT) == 0);
    CHECK(rmr_tri21_compare_scalar(left, right, scalar) == 0);
    CHECK(scalar[RMR_TRI_EMPTY * 3u + RMR_TRI_ABSENT] == 21u);
    CHECK(rmr_tri21_compare_bitsliced(left, right, fast) == 0);
    for (unsigned k = 0; k < 9; ++k) CHECK(fast[k] == scalar[k]);

    CHECK(rmr_tri21_put(&left, 2, 6, RMR_TRI_PRESENT) == 0);
    CHECK(rmr_tri21_get(left, 2, 6, &x) == 0 && x == RMR_TRI_PRESENT);
    original = left;
    CHECK(rmr_tri21_put(&left, 2, 7, RMR_TRI_PRESENT) == RMR_TRI_STATUS_RANGE);
    CHECK(left == original);
    CHECK(rmr_tri21_put(&left, 0, 0, RMR_TRI_INVALID) == RMR_TRI_STATUS_ENCODING);
    CHECK(left == original);
    CHECK(rmr_tri21_get(left, 3, 0, &x) == RMR_TRI_STATUS_RANGE && x == RMR_TRI_PRESENT);
    CHECK(rmr_tri21_compare_bitsliced(left | ((rmr_u64)1 << 63), right, fast) ==
          RMR_TRI_STATUS_ENCODING);
    CHECK(rmr_tri21_compare_scalar(left | ((rmr_u64)1 << 63), right, fast) ==
          RMR_TRI_STATUS_ENCODING);
    CHECK(rmr_tri21_compare_bitsliced(left | 3u, right, fast) == RMR_TRI_STATUS_ENCODING);
    CHECK(rmr_tri21_compare_scalar(left | 3u, right, fast) == RMR_TRI_STATUS_ENCODING);
    CHECK(rmr_tri21_compare_scalar(left, right, (unsigned *)0) == RMR_TRI_STATUS_NULL);

    for (unsigned t = 0; t < 2500u; ++t) {
        CHECK(rmr_tri21_fill(&left, RMR_TRI_EMPTY) == 0);
        CHECK(rmr_tri21_fill(&right, RMR_TRI_EMPTY) == 0);
        for (unsigned i = 0; i < 21u; ++i) {
            CHECK(rmr_tri21_put(&left, i / 7u, i % 7u, next() % 3u) == 0);
            CHECK(rmr_tri21_put(&right, i / 7u, i % 7u, next() % 3u) == 0);
        }
        CHECK(rmr_tri21_compare_scalar(left, right, scalar) == 0);
        CHECK(rmr_tri21_compare_bitsliced(left, right, fast) == 0);
        unsigned total = 0;
        for (unsigned i = 0; i < 9u; ++i) {
            CHECK(scalar[i] == fast[i]);
            total += fast[i];
        }
        CHECK(total == 21u);
    }
    CHECK(rmr_tri40_fill(&grid40, RMR_TRI_EMPTY) == 0);
    CHECK(rmr_tri40_put(&grid40, 4, 7, RMR_TRI_PRESENT) == 0);
    CHECK(rmr_tri40_put(&grid40, 4, 0, RMR_TRI_ABSENT) == 0);
    CHECK(rmr_tri40_get(&grid40, 4, 7, &x) == 0 && x == RMR_TRI_PRESENT);
    CHECK(rmr_tri40_get(&grid40, 4, 0, &x) == 0 && x == RMR_TRI_ABSENT);
    CHECK(rmr_tri40_validate(&grid40) == 0);
    original = grid40.words[1];
    CHECK(rmr_tri40_put(&grid40, 5, 0, RMR_TRI_PRESENT) == RMR_TRI_STATUS_RANGE);
    CHECK(grid40.words[1] == original);
    grid40.words[1] |= (rmr_u64)1 << 16;
    CHECK(rmr_tri40_validate(&grid40) == RMR_TRI_STATUS_ENCODING);
    grid40.words[1] = original;
    grid40.words[0] |= 3u;
    CHECK(rmr_tri40_validate(&grid40) == RMR_TRI_STATUS_ENCODING);
    return 0;
}
