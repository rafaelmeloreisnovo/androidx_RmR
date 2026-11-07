/* SPDX-License-Identifier: Apache-2.0 */
#include "../../main/cpp/rmr_zero_f64.h"

/* Compiles the inline-ASM contract as it is consumed by the JNI C++17 TU. */
extern "C" void rmr_zero_f64_cpp_smoke(double *ptr, unsigned int count) {
    rmr_zero_f64(ptr, count);
}
