/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 *
 * Freestanding capacity/lifetime registry. Caller owns storage and lock.
 * IDs are generation-tagged safety tokens, NOT cryptographic capabilities.
 */
#ifndef RMR_LEASE_V1_H
#define RMR_LEASE_V1_H
#if !defined(__UINT64_TYPE__) || !defined(__UINT32_TYPE__) || !defined(__SIZE_TYPE__)
#error "RMR lease v1 requires compiler integer-width definitions"
#endif
typedef __UINT64_TYPE__ rmr_lease_id;
typedef __UINT32_TYPE__ rmr_lease_generation;
typedef __SIZE_TYPE__ rmr_lease_size;
#define RMR_LEASE_SLOTS 128u

typedef struct {
    void *base;
    rmr_lease_size capacity;
    rmr_lease_generation generation;
    unsigned char live;
} rmr_lease_slot;

typedef struct {
    rmr_lease_slot slots[RMR_LEASE_SLOTS];
} rmr_lease_registry;

enum {
    RMR_LEASE_OK = 0,
    RMR_LEASE_INVALID = 1,
    RMR_LEASE_FULL = 2,
    RMR_LEASE_STALE = 3,
    RMR_LEASE_BOUNDS = 4
};

#ifdef __cplusplus
extern "C" {
#endif
void rmr_lease_init(rmr_lease_registry *registry);
int rmr_lease_open(rmr_lease_registry *registry, void *base,
                   rmr_lease_size capacity, rmr_lease_id *out_id);
int rmr_lease_view(rmr_lease_registry *registry, rmr_lease_id id,
                   rmr_lease_size offset, rmr_lease_size bytes, void **out_ptr);
int rmr_lease_close(rmr_lease_registry *registry, rmr_lease_id id,
                    void **out_base, rmr_lease_size *out_capacity);
#ifdef __cplusplus
}
#endif
#endif
