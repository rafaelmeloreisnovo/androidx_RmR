/* Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0 */
#include "../../main/cpp/portable/rmr_matrix_f64.h"
#define CHECK(x) do { if (!(x)) return __LINE__; } while (0)

static int eq(const double *a, const double *b, int n) {
    for (int i = 0; i < n; ++i) {
        if (a[i] != b[i]) return 0;
    }
    return 1;
}

int main(void) {
    const double a[6] = {1, 2, 3, 4, 5, 6};
    const double b[6] = {7, 8, 9, 10, 11, 12};
    const double expected[4] = {58, 64, 139, 154};
    double out[5] = {101, 102, 103, 104, 105};
    CHECK(rmr_matrix_f64_multiply(a, 6, 2, 3, b, 6, 3, 2, out, 5) == 0);
    CHECK(eq(out, expected, 4));
    CHECK(out[4] == 105); /* trailing output untouched */

    const double sq[4] = {1, 2, 3, 4};
    double square[4] = {0, 0, 0, 0};
    const double sq_expected[4] = {7, 10, 15, 22};
    CHECK(rmr_matrix_f64_multiply(sq, 4, 2, 2, sq, 4, 2, 2, square, 4) == 0);
    CHECK(eq(square, sq_expected, 4)); /* both inputs may share */

    double data[16] = {1, 2, 3, 4, 5, 6, 77, 88, 0, 0, 0, 0, 0, 0, 0, 0};
    CHECK(rmr_matrix_f64_multiply(data, 6, 2, 3, b, 6, 3, 2, data, 16)
            == RMR_MATRIX_F64_ALIAS);
    CHECK(data[0] == 1 && data[6] == 77);
    CHECK(rmr_matrix_f64_multiply(data, 6, 2, 3, b, 6, 3, 2, data + 1, 15)
            == RMR_MATRIX_F64_ALIAS); /* partial overlap */
    CHECK(data[1] == 2);

    const double snap[5] = {58, 64, 139, 154, 105};
    CHECK(rmr_matrix_f64_multiply(a, 5, 2, 3, b, 6, 3, 2, out, 5)
            == RMR_MATRIX_F64_INVALID);
    CHECK(eq(out, snap, 5)); /* failure atomicity */
    CHECK(rmr_matrix_f64_multiply(a, 6, 2, 3, b, 5, 3, 2, out, 5)
            == RMR_MATRIX_F64_INVALID);
    CHECK(rmr_matrix_f64_multiply(a, 6, 2, 3, b, 6, 3, 2, out, 3)
            == RMR_MATRIX_F64_INVALID);
    CHECK(rmr_matrix_f64_multiply(a, 6, 2, 3, b, 6, 2, 3, out, 5)
            == RMR_MATRIX_F64_INVALID);
    CHECK(rmr_matrix_f64_multiply(a, 6, 0, 3, b, 6, 3, 2, out, 5)
            == RMR_MATRIX_F64_INVALID);
    CHECK(rmr_matrix_f64_multiply(a, 6, 2, 3, b, 6, 3, 2, (double *)0, 5)
            == RMR_MATRIX_F64_INVALID);
    CHECK(rmr_matrix_f64_multiply(a, 6, 2147483647, 2, b, 6, 2, 1, out, 5)
            == RMR_MATRIX_F64_INVALID);
    CHECK(eq(out, snap, 5));

    /* Multi-block boundary: 65 columns and nonzero top/right tails. */
    double big_a[65];
    double big_b[65];
    for (int i = 0; i < 65; ++i) { big_a[i] = 1.0; big_b[i] = 2.0; }
    double big_out[1] = {99};
    CHECK(rmr_matrix_f64_multiply(big_a, 65, 1, 65, big_b, 65, 65, 1,
            big_out, 1) == 0);
    CHECK(big_out[0] == 130.0);
    return 0;
}
