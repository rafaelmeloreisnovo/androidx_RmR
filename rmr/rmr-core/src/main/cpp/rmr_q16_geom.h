/* Copyright (C) 2026 Rafael Melo Reis (RmR) - SPDX-License-Identifier: Apache-2.0 */
#ifndef RMR_Q16_GEOM_H
#define RMR_Q16_GEOM_H
#include "rmr_q16_mul.h"
/* Standard equilateral-triangle altitude ratio sqrt(3)/2 rounded to Q16.16.
 * The square happens to truncate to 3/4 in Q16.16 at this precision;
 * this is a quantized identity, not a claim about novel geometry.
 */
#define RMR_Q16_ONE INT32_C(65536)
#define RMR_Q16_SQRT3_OVER2 INT32_C(56756)
#define RMR_Q16_THREE_QUARTERS INT32_C(49152)
static inline int rmr_q16_triangle_height(int32_t side, int32_t *height) {
    if (side < 0) return 0;
    return rmr_q16_mul_checked(side, RMR_Q16_SQRT3_OVER2, height);
}
static inline int rmr_q16_radial_step(int32_t radius, int32_t *next) {
    if (radius < 0) return 0;
    return rmr_q16_mul_checked(radius, RMR_Q16_SQRT3_OVER2, next);
}
#endif
