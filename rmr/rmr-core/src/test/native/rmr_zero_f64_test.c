/* SPDX-License-Identifier: Apache-2.0 */
#include "../../main/cpp/rmr_zero_f64.h"

static int bytes_zero(const double *v, unsigned int n) {
    const unsigned char *p = (const unsigned char *)v;
    for (unsigned int i = 0; i < n * (unsigned int)sizeof(double); ++i) {
        if (p[i] != 0U) return 0;
    }
    return 1;
}

int main(void) {
    double v[9];
    for (unsigned int n = 0U; n <= 8U; ++n) {
        for (unsigned int i = 0U; i < 9U; ++i) v[i] = -7.0;
        rmr_zero_f64(v, n);
        if (!bytes_zero(v, n)) return __LINE__;
        for (unsigned int i = n; i < 9U; ++i)
            if (v[i] != -7.0) return __LINE__;
    }
    rmr_zero_f64((double *)0, 0U);
    rmr_zero_f64((double *)0, 7U);
    return 0;
}
