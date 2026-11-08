/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * Native independent negative checks; no Android/JNI or real allocations.
 */
#include "../../main/cpp/portable/rmr_lease_v1.h"
#define CHECK(x) do { if (!(x)) return __LINE__; } while (0)

int main(void) {
    rmr_lease_registry slots;
    unsigned char bytes[32] = {0};
    unsigned char another[16] = {0};
    void *out = (void *)0;
    void *released = (void *)0;
    rmr_lease_size capacity = 0;
    rmr_lease_id first = 0, next = 0, second = 0;

    rmr_lease_init(&slots);
    CHECK(rmr_lease_open(&slots, bytes, sizeof(bytes), &first) == RMR_LEASE_OK);
    CHECK(first != 0);
    CHECK(rmr_lease_view(&slots, first, 0, 32, &out) == RMR_LEASE_OK);
    CHECK(out == bytes);
    CHECK(rmr_lease_view(&slots, first, 16, 16, &out) == RMR_LEASE_OK);
    CHECK(out == bytes + 16);
    CHECK(rmr_lease_view(&slots, first, 32, 0, &out) == RMR_LEASE_OK);
    CHECK(out == bytes + 32);
    CHECK(rmr_lease_view(&slots, first, 33, 0, &out) == RMR_LEASE_BOUNDS);
    CHECK(out == (void *)0);
    CHECK(rmr_lease_view(&slots, first, 2, 31, &out) == RMR_LEASE_BOUNDS);
    CHECK(out == (void *)0);
    CHECK(rmr_lease_view(&slots, first, ~(rmr_lease_size)0, 4, &out)
          == RMR_LEASE_BOUNDS);
    CHECK(rmr_lease_open(&slots, bytes, sizeof(bytes), &next)
          == RMR_LEASE_INVALID);
    CHECK(next == 0);
    CHECK(rmr_lease_open(&slots, another, sizeof(another), &second)
          == RMR_LEASE_OK);
    CHECK(second != first);
    CHECK(rmr_lease_view(&slots, first ^ (1ULL << 32), 0, 1, &out)
          == RMR_LEASE_STALE);
    CHECK(rmr_lease_view(&slots, 0, 0, 1, &out) == RMR_LEASE_STALE);
    CHECK(rmr_lease_view(&slots, 0xffffffffULL, 0, 1, &out)
          == RMR_LEASE_STALE);
    CHECK(rmr_lease_close(&slots, first, &released, &capacity) == RMR_LEASE_OK);
    CHECK(released == bytes && capacity == 32);
    CHECK(rmr_lease_close(&slots, first, &released, &capacity) == RMR_LEASE_STALE);
    CHECK(released == (void *)0 && capacity == 0);
    CHECK(rmr_lease_view(&slots, first, 0, 1, &out) == RMR_LEASE_STALE);
    CHECK(out == (void *)0);
    CHECK(rmr_lease_open(&slots, bytes, sizeof(bytes), &next) == RMR_LEASE_OK);
    CHECK(next != first && (next & 0xffffffffULL) == (first & 0xffffffffULL));
    CHECK(rmr_lease_view(&slots, first, 0, 1, &out) == RMR_LEASE_STALE);
    CHECK(rmr_lease_view(&slots, next, 0, 1, &out) == RMR_LEASE_OK);
    CHECK(out == bytes);
    CHECK(rmr_lease_close(&slots, second, &released, &capacity) == RMR_LEASE_OK);
    CHECK(rmr_lease_close(&slots, next, &released, &capacity) == RMR_LEASE_OK);

    /* Exhaust capacity with distinct live pointers; avoid zero-size bases. */
    unsigned char pages[RMR_LEASE_SLOTS + 1u];
    rmr_lease_id handles[RMR_LEASE_SLOTS];
    rmr_lease_init(&slots);
    for (unsigned int i = 0; i < RMR_LEASE_SLOTS; ++i) {
        CHECK(rmr_lease_open(&slots, &pages[i], 1, &handles[i]) == RMR_LEASE_OK);
    }
    CHECK(rmr_lease_open(&slots, &pages[RMR_LEASE_SLOTS], 1, &next)
          == RMR_LEASE_FULL);
    CHECK(rmr_lease_close(&slots, handles[30], &released, &capacity)
          == RMR_LEASE_OK);
    CHECK(rmr_lease_open(&slots, &pages[RMR_LEASE_SLOTS], 1, &next)
          == RMR_LEASE_OK);
    CHECK(rmr_lease_close(&slots, handles[30], &released, &capacity)
          == RMR_LEASE_STALE);
    /* Generation exhaustion retires a slot rather than cycling to old tokens. */
    rmr_lease_init(&slots);
    slots.slots[0].generation = 0xffffffffu;
    CHECK(rmr_lease_open(&slots, bytes, 1, &next) == RMR_LEASE_OK);
    CHECK((next & 0xffffffffULL) == 2);
    CHECK(rmr_lease_open((rmr_lease_registry *)0, bytes, 1, &next)
          == RMR_LEASE_INVALID);
    CHECK(rmr_lease_open(&slots, (void *)0, 1, &next) == RMR_LEASE_INVALID);
    CHECK(rmr_lease_open(&slots, bytes, 0, &next) == RMR_LEASE_INVALID);
    CHECK(rmr_lease_view(&slots, next, 0, 1, (void **)0) == RMR_LEASE_INVALID);
    CHECK(rmr_lease_close(&slots, next, (void **)0, &capacity)
          == RMR_LEASE_INVALID);
    return 0;
}
