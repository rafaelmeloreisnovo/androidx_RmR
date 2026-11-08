/* SPDX-License-Identifier: Apache-2.0 */
#include "../../main/cpp/rmr_q16_geom.h"
#include "../../main/cpp/rmr_native_arena.h"
extern "C" int rmr_q16_cpp_smoke(void) {
    int32_t out = 0;
    return rmr_q16_triangle_height(RMR_Q16_ONE, &out) &&
           out == RMR_Q16_SQRT3_OVER2;
}
