/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * Hosted POSIX allocator/mutex boundary for Android/JNI; not freestanding.
 * Registry metadata is fixed-size and never heap-allocated.
 */
#define _POSIX_C_SOURCE 200112L
#include "rafaelia_capabilities.h"
#include <pthread.h>
#include <stdlib.h>
#include <string.h>

#define RMR_CAP_SLOTS 64u
#define RMR_CAP_MAX_BYTES (16u * 1024u * 1024u)
#define RMR_CAP_MAX_GENERATION (UINT64_MAX >> 8)

struct cap_slot {
    uint8_t *ptr;
    size_t size;
    uint64_t generation;
};
static struct cap_slot registry[RMR_CAP_SLOTS];
static pthread_mutex_t registry_mutex = PTHREAD_MUTEX_INITIALIZER;

static int is_pow2(size_t v) { return v && ((v & (v - 1u)) == 0u); }
static int inside(size_t cap, size_t offset, size_t count) {
    return offset <= cap && count <= cap - offset;
}
static uint64_t encode_token(size_t i, uint64_t generation) {
    return (generation << 8) | (uint64_t)(i + 1u);
}
/* Caller holds registry_mutex. Tokens encode slot+monotonic generation. */
static struct cap_slot *find_locked(uint64_t token) {
    size_t idx;
    uint64_t generation;
    if ((token & UINT64_C(255)) == 0 || (token & UINT64_C(255)) > RMR_CAP_SLOTS)
        return NULL;
    idx = (size_t)((token & UINT64_C(255)) - 1u);
    generation = token >> 8;
    if (!generation || !registry[idx].ptr || registry[idx].generation != generation)
        return NULL;
    return &registry[idx];
}

uint64_t rmr_cap_allocate(size_t bytes, size_t alignment) {
    void *ptr = NULL;
    uint64_t token = 0;
    size_t i;
    if (bytes == 0 || bytes > RMR_CAP_MAX_BYTES || alignment < sizeof(void *) ||
        !is_pow2(alignment) || alignment > 4096u)
        return 0;
    if (posix_memalign(&ptr, alignment, bytes) != 0 || !ptr) return 0;
    memset(ptr, 0, bytes);
    if (pthread_mutex_lock(&registry_mutex) != 0) {
        free(ptr);
        return 0;
    }
    for (i = 0; i < RMR_CAP_SLOTS; ++i) {
        if (registry[i].ptr == NULL && registry[i].generation < RMR_CAP_MAX_GENERATION) {
            registry[i].ptr = (uint8_t *)ptr;
            registry[i].size = bytes;
            ++registry[i].generation;
            token = encode_token(i, registry[i].generation);
            break;
        }
    }
    pthread_mutex_unlock(&registry_mutex);
    if (!token) free(ptr);
    return token;
}

int rmr_cap_release(uint64_t token) {
    struct cap_slot *slot;
    if (pthread_mutex_lock(&registry_mutex) != 0) return 0;
    slot = find_locked(token);
    if (!slot) { pthread_mutex_unlock(&registry_mutex); return 0; }
    /* Invalidate before freeing, while all concurrent operations are blocked.
     * Increment generation only on next allocation; previous token stays stale. */
    uint8_t *ptr = slot->ptr;
    size_t length = slot->size;
    slot->ptr = NULL;
    slot->size = 0;
    /* Volatile best-effort wipe; not proof of hardware-cache erasure. */
    volatile uint8_t *wipe = (volatile uint8_t *)ptr;
    for (size_t i = 0; i < length; ++i) wipe[i] = 0;
    free(ptr);
    pthread_mutex_unlock(&registry_mutex);
    return 1;
}

int rmr_cap_fill(uint64_t token, size_t offset, size_t count, uint8_t value) {
    int ok = 0;
    if (pthread_mutex_lock(&registry_mutex) != 0) return 0;
    struct cap_slot *slot = find_locked(token);
    if (slot && inside(slot->size, offset, count)) {
        memset(slot->ptr + offset, value, count);
        ok = 1;
    }
    pthread_mutex_unlock(&registry_mutex);
    return ok;
}

int rmr_cap_copy(uint64_t src, size_t src_offset,
                 uint64_t dst, size_t dst_offset, size_t count) {
    int ok = 0;
    if (pthread_mutex_lock(&registry_mutex) != 0) return 0;
    struct cap_slot *source = find_locked(src);
    struct cap_slot *target = find_locked(dst);
    if (source && target && inside(source->size, src_offset, count) &&
        inside(target->size, dst_offset, count)) {
        memmove(target->ptr + dst_offset, source->ptr + src_offset, count);
        ok = 1;
    }
    pthread_mutex_unlock(&registry_mutex);
    return ok;
}

int rmr_cap_read(uint64_t token, size_t offset, uint8_t *value) {
    int ok = 0;
    if (!value || pthread_mutex_lock(&registry_mutex) != 0) return 0;
    struct cap_slot *slot = find_locked(token);
    if (slot && inside(slot->size, offset, 1u)) {
        *value = slot->ptr[offset];
        ok = 1;
    }
    pthread_mutex_unlock(&registry_mutex);
    return ok;
}
int rmr_cap_write(uint64_t token, size_t offset, uint8_t value) {
    int ok = 0;
    if (pthread_mutex_lock(&registry_mutex) != 0) return 0;
    struct cap_slot *slot = find_locked(token);
    if (slot && inside(slot->size, offset, 1u)) {
        slot->ptr[offset] = value;
        ok = 1;
    }
    pthread_mutex_unlock(&registry_mutex);
    return ok;
}
size_t rmr_cap_live_count(void) {
    size_t result = 0;
    if (pthread_mutex_lock(&registry_mutex) != 0) return SIZE_MAX;
    for (size_t i = 0; i < RMR_CAP_SLOTS; ++i) if (registry[i].ptr) ++result;
    pthread_mutex_unlock(&registry_mutex);
    return result;
}
