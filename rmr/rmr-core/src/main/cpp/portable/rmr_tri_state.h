/* Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 * Two-bit ternary data encoding; no implicit harmonic/physics model.
 */
#ifndef RMR_TRI_STATE_H
#define RMR_TRI_STATE_H
#if !defined(__UINT64_TYPE__) || !defined(__UINT32_TYPE__)
#error "Require fixed width compiler unsigned types"
#endif
typedef __UINT64_TYPE__ rmr_u64;
typedef __UINT32_TYPE__ rmr_u32;
#define RMR_TRI_ABSENT 0u
#define RMR_TRI_PRESENT 1u
#define RMR_TRI_EMPTY 2u
#define RMR_TRI_INVALID 3u
#define RMR_TRI21_COUNT 21u
#define RMR_TRI40_COUNT 40u
#define RMR_TRI21_LANE_BITS ((rmr_u64)0x15555555555ULL)
#define RMR_TRI21_USED_BITS (((rmr_u64)1 << 42) - 1u)
#define RMR_TRI_STATUS_OK 0
#define RMR_TRI_STATUS_NULL 1
#define RMR_TRI_STATUS_RANGE 2
#define RMR_TRI_STATUS_ENCODING 3
#ifdef __cplusplus
extern "C" {
#endif
int rmr_tri21_validate(rmr_u64 packed);
int rmr_tri21_fill(rmr_u64 *packed, unsigned state);
int rmr_tri21_put(rmr_u64 *packed, unsigned row, unsigned col, unsigned state);
int rmr_tri21_get(rmr_u64 packed, unsigned row, unsigned col, unsigned *out_state);
int rmr_tri21_compare_scalar(rmr_u64 left, rmr_u64 right, unsigned out_counts[9]);
int rmr_tri21_compare_bitsliced(rmr_u64 left, rmr_u64 right, unsigned out_counts[9]);
/* 5x8 = 40 states / 80 bits, without an implied 3x7 projection. */
typedef struct { rmr_u64 words[2]; } rmr_tri40;
int rmr_tri40_validate(const rmr_tri40 *grid);
int rmr_tri40_fill(rmr_tri40 *grid, unsigned state);
int rmr_tri40_put(rmr_tri40 *grid, unsigned row, unsigned col, unsigned state);
int rmr_tri40_get(const rmr_tri40 *grid, unsigned row, unsigned col, unsigned *out_state);
#ifdef __cplusplus
}
#endif
#endif
