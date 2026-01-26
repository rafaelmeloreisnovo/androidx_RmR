/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.rmr.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RmRMatrixTest {

    @Test
    public void identityBuildsDiagonal() {
        RmRMatrix identity = RmRMatrix.identity(3);

        assertEquals(1.0, identity.get(0, 0), 0.0);
        assertEquals(1.0, identity.get(1, 1), 0.0);
        assertEquals(1.0, identity.get(2, 2), 0.0);
        assertEquals(0.0, identity.get(0, 1), 0.0);
        assertEquals(0.0, identity.get(1, 2), 0.0);
    }

    @Test
    public void addHandlesSelf() {
        RmRMatrix matrix = new RmRMatrix(1, 2);
        matrix.set(0, 0, 3.0);
        matrix.set(0, 1, -2.5);

        RmRMatrix doubled = matrix.add(matrix);

        assertEquals(6.0, doubled.get(0, 0), 0.0);
        assertEquals(-5.0, doubled.get(0, 1), 0.0);
    }

    @Test
    public void linearFlipPreservesZeros() {
        RmRMatrix matrix = new RmRMatrix(1, 2);
        matrix.set(0, 0, 0.0);
        matrix.set(0, 1, 2.0);

        RmRMatrix flipped = matrix.linearFlip();

        assertEquals(0.0, flipped.get(0, 0), 0.0);
        assertEquals(-0.5, flipped.get(0, 1), 0.0);
    }

    @Test
    public void linearFlipPreservesNaN() {
        RmRMatrix matrix = new RmRMatrix(1, 1);
        matrix.set(0, 0, Double.NaN);

        RmRMatrix flipped = matrix.linearFlip();

        assertTrue(Double.isNaN(flipped.get(0, 0)));
    }
}
