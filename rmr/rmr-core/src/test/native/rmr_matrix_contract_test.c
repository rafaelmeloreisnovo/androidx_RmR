/* SPDX-License-Identifier: Apache-2.0 */
#include "../../main/cpp/rmr_matrix_contract.h"

#define EXPECT_OK(ar,ac,br,bc,al,bl,rl) \
    do { if (!rmr_matrix_multiply_shape_valid(ar,ac,br,bc,al,bl,rl)) return __LINE__; } while (0)
#define EXPECT_BAD(ar,ac,br,bc,al,bl,rl) \
    do { if (rmr_matrix_multiply_shape_valid(ar,ac,br,bc,al,bl,rl)) return __LINE__; } while (0)

int main(void) {
    EXPECT_OK(1, 1, 1, 1, 1, 1, 1);
    EXPECT_OK(2, 3, 3, 4, 6, 12, 8);
    EXPECT_OK(2, 3, 3, 4, 7, 13, 9); /* trailing capacity */
    EXPECT_BAD(2, 3, 4, 4, 6, 16, 8); /* incompatible inner dimension */
    EXPECT_BAD(2, 3, 3, 4, 5, 12, 8); /* undersized A */
    EXPECT_BAD(2, 3, 3, 4, 6, 11, 8); /* undersized B */
    EXPECT_BAD(2, 3, 3, 4, 6, 12, 7); /* undersized output */
    EXPECT_BAD(-1, 3, 3, 4, 6, 12, 8);
    EXPECT_BAD(0, 3, 3, 4, 6, 12, 8);
    EXPECT_BAD(2, 3, 3, 4, -1, 12, 8);
    EXPECT_BAD(2147483647, 2, 2, 1, 2147483647, 2, 2147483647);
    EXPECT_BAD(50000, 50000, 50000, 50000, 2147483647, 2147483647, 2147483647);
    return 0;
}
