/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 */
package androidx.rmr.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

public class RmRMatrixOpsAliasingTest {
    @Test
    public void multiplyIntoRejectsAliasedLeftBeforeMutating() {
        RmRMatrix left = new RmRMatrix(1, 1);
        RmRMatrix right = new RmRMatrix(1, 1);
        left.set(0, 0, 3.0);
        right.set(0, 0, 5.0);

        try {
            RmRMatrixOps.multiplyInto(left, right, left);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals(3.0, left.get(0, 0), 0.0);
            assertEquals(5.0, right.get(0, 0), 0.0);
        }
    }

    @Test
    public void multiplyIntoRejectsAliasedRightBeforeMutating() {
        RmRMatrix left = new RmRMatrix(1, 1);
        RmRMatrix right = new RmRMatrix(1, 1);
        left.set(0, 0, 2.0);
        right.set(0, 0, 7.0);

        try {
            RmRMatrixOps.multiplyInto(left, right, right);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertEquals(2.0, left.get(0, 0), 0.0);
            assertEquals(7.0, right.get(0, 0), 0.0);
        }
    }
}
