/* Copyright (C) 2026 Rafael Melo Reis (RmR) - SPDX-License-Identifier: Apache-2.0 */
#ifndef RMR_NATIVE_ARENA_H
#define RMR_NATIVE_ARENA_H
#include <stddef.h>
#include <stdint.h>
/* Caller owns base[0..capacity). Do not reinitialize while handles exist.
 * Single-threaded; not an OS allocator and not a security boundary.
 * Returned pointers are borrowed and valid only before next reset.
 */
typedef enum {
    RMR_ARENA_OK = 0, RMR_ARENA_BAD_ARG = 1,
    RMR_ARENA_NO_SPACE = 2, RMR_ARENA_STALE = 3,
    RMR_ARENA_OUT_OF_RANGE = 4, RMR_ARENA_GEN_EXHAUSTED = 5
} rmr_arena_status;
typedef struct {
    unsigned char *base;
    size_t capacity;
    size_t used;
    uint32_t generation;
} rmr_native_arena;
typedef struct {
    size_t offset;
    size_t bytes;
    uint32_t generation;
} rmr_arena_slice;
static inline rmr_arena_status rmr_arena_init(
        rmr_native_arena *a, void *base, size_t capacity) {
    if (!a || !base || capacity == 0U) return RMR_ARENA_BAD_ARG;
    a->base = (unsigned char *)base;
    a->capacity = capacity;
    a->used = 0U;
    a->generation = UINT32_C(1);
    return RMR_ARENA_OK;
}
static inline rmr_arena_status rmr_arena_reset(rmr_native_arena *a) {
    if (!a || !a->base || a->generation == 0U) return RMR_ARENA_BAD_ARG;
    if (a->generation == UINT32_MAX) return RMR_ARENA_GEN_EXHAUSTED;
    a->generation++;
    a->used = 0U;
    return RMR_ARENA_OK;
}
static inline rmr_arena_status rmr_arena_alloc(
        rmr_native_arena *a, size_t bytes, size_t alignment,
        rmr_arena_slice *out) {
    if (!a || !out || !a->base || !a->generation ||
        a->used > a->capacity || bytes == 0U || alignment == 0U ||
        (alignment & (alignment - 1U)) != 0U) return RMR_ARENA_BAD_ARG;
    /* Alignment arithmetic is performed on the absolute address. */
    const uintptr_t mask = (uintptr_t)(alignment - 1U);
    if ((size_t)mask != alignment - 1U) return RMR_ARENA_BAD_ARG;
    const uintptr_t current = (uintptr_t)(a->base + a->used);
    const size_t pad = (size_t)((-(uintptr_t)current) & mask);
    if (pad > a->capacity - a->used) return RMR_ARENA_NO_SPACE;
    const size_t offset = a->used + pad;
    if (bytes > a->capacity - offset) return RMR_ARENA_NO_SPACE;
    const rmr_arena_slice candidate = {offset, bytes, a->generation};
    a->used = offset + bytes;
    *out = candidate;
    return RMR_ARENA_OK;
}
static inline rmr_arena_status rmr_arena_resolve(
        const rmr_native_arena *a, rmr_arena_slice slice,
        size_t needed, void **out) {
    if (!a || !out || !a->base || a->generation == 0U)
        return RMR_ARENA_BAD_ARG;
    if (slice.generation != a->generation) return RMR_ARENA_STALE;
    /* Reject corrupted bookkeeping before deriving borrowed pointers. */
    if (a->used > a->capacity) return RMR_ARENA_OUT_OF_RANGE;
    if (slice.offset > a->used || slice.bytes == 0U ||
        slice.bytes > a->used - slice.offset || needed > slice.bytes)
        return RMR_ARENA_OUT_OF_RANGE;
    *out = (void *)(a->base + slice.offset);
    return RMR_ARENA_OK;
}
#endif
