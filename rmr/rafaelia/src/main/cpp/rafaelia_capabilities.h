/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * New bounded native ownership contract; module license/provenance unchanged.
 * A capability token is NOT an address or an unforgeable security credential.
 */
#ifndef RAFAELIA_CAPABILITIES_H
#define RAFAELIA_CAPABILITIES_H
#include <stddef.h>
#include <stdint.h>
#ifdef __cplusplus
extern "C" {
#endif
uint64_t rmr_cap_allocate(size_t bytes, size_t alignment);
int rmr_cap_release(uint64_t token);
int rmr_cap_fill(uint64_t token, size_t offset, size_t count, uint8_t value);
int rmr_cap_copy(uint64_t src, size_t src_offset,
                 uint64_t dst, size_t dst_offset, size_t count);
int rmr_cap_read(uint64_t token, size_t offset, uint8_t *value);
int rmr_cap_write(uint64_t token, size_t offset, uint8_t value);
size_t rmr_cap_live_count(void); /* Diagnostics; does not enumerate handles. */
#ifdef __cplusplus
}
#endif
#endif
