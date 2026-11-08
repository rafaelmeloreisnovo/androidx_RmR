/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * No heap, libc, JNI, atomics, syscalls or unbounded dynamic allocation.
 * External caller must serialize all operations and retain owned backing.
 */
#include "rmr_lease_v1.h"
typedef char rmr_lease_id_is_64_bit[(sizeof(rmr_lease_id) == 8) ? 1 : -1];
typedef char rmr_lease_generation_is_32_bit[
    (sizeof(rmr_lease_generation) == 4) ? 1 : -1];

void rmr_lease_init(rmr_lease_registry *registry) {
    if (registry == (rmr_lease_registry *)0) return;
    for (unsigned int i = 0; i < RMR_LEASE_SLOTS; ++i) {
        registry->slots[i].base = (void *)0;
        registry->slots[i].capacity = 0;
        registry->slots[i].generation = 0;
        registry->slots[i].live = 0;
    }
}

int rmr_lease_open(rmr_lease_registry *registry, void *base,
                   rmr_lease_size capacity, rmr_lease_id *out_id) {
    if (registry == (rmr_lease_registry *)0 || base == (void *)0 ||
        out_id == (rmr_lease_id *)0 || capacity == 0) return RMR_LEASE_INVALID;
    *out_id = 0;
    for (unsigned int i = 0; i < RMR_LEASE_SLOTS; ++i) {
        rmr_lease_slot *slot = &registry->slots[i];
        /* No generation wrap: retire exhausted slots instead of ABA reuse. */
        if (slot->live != 0 || slot->generation == 0xffffffffu) continue;
        slot->generation += 1u;
        slot->base = base;
        slot->capacity = capacity;
        slot->live = 1;
        *out_id = ((rmr_lease_id)slot->generation << 32) |
                  (rmr_lease_id)(i + 1u);
        return RMR_LEASE_OK;
    }
    return RMR_LEASE_FULL;
}

static rmr_lease_slot *rmr_find(
        rmr_lease_registry *registry, rmr_lease_id id) {
    if (registry == (rmr_lease_registry *)0 || id == 0) return (rmr_lease_slot *)0;
    const rmr_lease_id raw = id & (rmr_lease_id)0xffffffffu;
    const rmr_lease_generation generation =
        (rmr_lease_generation)(id >> 32);
    if (raw == 0 || raw > (rmr_lease_id)RMR_LEASE_SLOTS ||
        generation == 0) return (rmr_lease_slot *)0;
    rmr_lease_slot *slot = &registry->slots[(unsigned int)(raw - 1u)];
    if (slot->generation != generation || slot->live == 0 ||
        slot->base == (void *)0) return (rmr_lease_slot *)0;
    return slot;
}

int rmr_lease_view(rmr_lease_registry *registry, rmr_lease_id id,
                   rmr_lease_size offset, rmr_lease_size bytes, void **out_ptr) {
    if (out_ptr == (void **)0) return RMR_LEASE_INVALID;
    *out_ptr = (void *)0;
    rmr_lease_slot *slot = rmr_find(registry, id);
    if (slot == (rmr_lease_slot *)0) return RMR_LEASE_STALE;
    /* Subtraction avoids addition overflow and checks the declared extent. */
    if (offset > slot->capacity || bytes > slot->capacity - offset)
        return RMR_LEASE_BOUNDS;
    *out_ptr = (void *)((unsigned char *)slot->base + offset);
    return RMR_LEASE_OK;
}

int rmr_lease_close(rmr_lease_registry *registry, rmr_lease_id id,
                    void **out_base, rmr_lease_size *out_capacity) {
    if (out_base == (void **)0 || out_capacity == (rmr_lease_size *)0)
        return RMR_LEASE_INVALID;
    *out_base = (void *)0;
    *out_capacity = 0;
    rmr_lease_slot *slot = rmr_find(registry, id);
    if (slot == (rmr_lease_slot *)0) return RMR_LEASE_STALE;
    *out_base = slot->base;
    *out_capacity = slot->capacity;
    slot->base = (void *)0;
    slot->capacity = 0;
    slot->live = 0;
    return RMR_LEASE_OK;
}
