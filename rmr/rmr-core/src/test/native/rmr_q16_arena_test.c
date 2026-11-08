/* SPDX-License-Identifier: Apache-2.0 */
#include "../../main/cpp/rmr_q16_geom.h"
#include "../../main/cpp/rmr_native_arena.h"
static int run(void) {
    int32_t q = INT32_C(777);
    if (rmr_q16_mul_checked(RMR_Q16_SQRT3_OVER2, RMR_Q16_SQRT3_OVER2, &q) != 1 ||
        q != RMR_Q16_THREE_QUARTERS) return __LINE__;
    if (!rmr_q16_triangle_height(RMR_Q16_ONE, &q) ||
        q != RMR_Q16_SQRT3_OVER2) return __LINE__;
    if (!rmr_q16_mul_checked(-RMR_Q16_ONE, RMR_Q16_ONE / 2, &q) ||
        q != -RMR_Q16_ONE / 2) return __LINE__;
    q = INT32_C(777);
    if (rmr_q16_mul_checked(INT32_MAX, INT32_MAX, &q) || q != 777) return __LINE__;
    if (rmr_q16_mul_checked(1, 1, (int32_t *)0)) return __LINE__;
    if (rmr_q16_triangle_height(-1, &q)) return __LINE__;
    int32_t r = RMR_Q16_ONE;
    for (int i = 0; i < 42; ++i) {
        int32_t next = -1;
        if (!rmr_q16_radial_step(r, &next) || next < 0 || next > r) return __LINE__;
        r = next;
    }
    unsigned char bytes[49];
    rmr_native_arena arena;
    rmr_arena_slice first = {0U, 0U, 0U}, second = first;
    if (rmr_arena_init(&arena, bytes, 49U) != RMR_ARENA_OK) return __LINE__;
    if (rmr_arena_alloc(&arena, 4U, 4U, &first) != RMR_ARENA_OK) return __LINE__;
    if (rmr_arena_alloc(&arena, 8U, 8U, &second) != RMR_ARENA_OK) return __LINE__;
    void *p = (void *)0;
    if (rmr_arena_resolve(&arena, first, 4U, &p) != RMR_ARENA_OK ||
        (((uintptr_t)p) & 3U) != 0U) return __LINE__;
    p = (void *)0;
    if (rmr_arena_resolve(&arena, second, 8U, &p) != RMR_ARENA_OK ||
        (((uintptr_t)p) & 7U) != 0U) return __LINE__;
    p = (void *)0;
    if (rmr_arena_resolve(&arena, first, 5U, &p) != RMR_ARENA_OUT_OF_RANGE || p)
        return __LINE__;
    /* Simulate corrupt bookkeeping: resolution must never escape capacity. */
    const size_t saved_used = arena.used;
    arena.used = arena.capacity + 1U;
    p = (void *)0;
    if (rmr_arena_resolve(&arena, first, 4U, &p) != RMR_ARENA_OUT_OF_RANGE || p)
        return __LINE__;
    arena.used = saved_used;
    if (rmr_arena_alloc(&arena, 50U, 1U, &second) != RMR_ARENA_NO_SPACE)
        return __LINE__;
    if (rmr_arena_alloc(&arena, 1U, 3U, &second) != RMR_ARENA_BAD_ARG)
        return __LINE__;
    if (rmr_arena_reset(&arena) != RMR_ARENA_OK) return __LINE__;
    if (rmr_arena_resolve(&arena, first, 1U, &p) != RMR_ARENA_STALE)
        return __LINE__;
    arena.generation = UINT32_MAX;
    if (rmr_arena_reset(&arena) != RMR_ARENA_GEN_EXHAUSTED)
        return __LINE__;
    return 0;
}
int main(void) { return run(); }
