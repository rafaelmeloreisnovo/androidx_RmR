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

package androidx.rmr.rafaelia;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import org.junit.Test;

public class RafaeliaCoreTest {

    @Test
    public void optimizedMemoryCopyFallsBackSafely() {
        ByteBuffer src = ByteBuffer.allocateDirect(4);
        ByteBuffer dst = ByteBuffer.allocateDirect(4);
        src.put(0, (byte) 10);
        src.put(1, (byte) 20);
        src.put(2, (byte) 30);
        src.put(3, (byte) 40);

        RafaeliaCore.optimizedMemoryCopy(src, 0, dst, 0, 4);

        assertEquals(10, dst.get(0));
        assertEquals(20, dst.get(1));
        assertEquals(30, dst.get(2));
        assertEquals(40, dst.get(3));
    }

    @Test
    public void optimizedMemoryCopyHandlesZeroLength() {
        ByteBuffer src = ByteBuffer.allocateDirect(1);
        ByteBuffer dst = ByteBuffer.allocateDirect(1);
        src.put(0, (byte) 1);

        RafaeliaCore.optimizedMemoryCopy(src, 0, dst, 0, 0);

        assertEquals(0, dst.get(0));
    }

    @Test
    public void optimizedMemoryCopySkipsNativeWhenUnavailable() throws Exception {
        Field nativeAvailableField = RafaeliaCore.class.getDeclaredField("sNativeAvailable");
        nativeAvailableField.setAccessible(true);
        boolean originalNativeAvailable = (boolean) nativeAvailableField.get(null);
        nativeAvailableField.set(null, false);
        try {
            ByteBuffer src = ByteBuffer.allocateDirect(4);
            ByteBuffer dst = ByteBuffer.allocateDirect(4);
            src.put(0, (byte) 7);
            src.put(1, (byte) 8);
            src.put(2, (byte) 9);
            src.put(3, (byte) 10);

            RafaeliaCore.optimizedMemoryCopy(src, 0, dst, 0, 4);

            assertEquals(7, dst.get(0));
            assertEquals(8, dst.get(1));
            assertEquals(9, dst.get(2));
            assertEquals(10, dst.get(3));
        } finally {
            nativeAvailableField.set(null, originalNativeAvailable);
        }
    }
    @Test
    public void createAlignsSizeAndRejectsInvalidAllocationSizes() throws Exception {
        Field validatedField = RafaeliaCore.class.getDeclaredField("sValidated");
        validatedField.setAccessible(true);
        boolean originalValidated = (boolean) validatedField.get(null);
        validatedField.set(null, true);
        try {
            assertEquals(64, RafaeliaCore.create(1).getDirectMemory().capacity());

            try {
                RafaeliaCore.create(-1);
                fail("Negative allocation sizes must be rejected");
            } catch (IllegalArgumentException expected) {
                // Expected.
            }

            try {
                RafaeliaCore.create(Integer.MAX_VALUE);
                fail("Overflowing aligned allocation sizes must be rejected");
            } catch (IllegalArgumentException expected) {
                // Expected.
            }
        } finally {
            validatedField.set(null, originalValidated);
        }
    }

    @Test
    public void vectorOperationsHandlePartialLengthAndAliasedOutput() {
        float[] sum = new float[] {1.0f, -2.0f, 99.0f};
        RafaeliaCore.vectorAdd(sum, new float[] {4.0f, 3.0f, 7.0f}, sum, 2);
        assertArrayEquals(new float[] {5.0f, 1.0f, 99.0f}, sum, 0.0f);

        float[] product = new float[] {0.0f, 0.0f, Float.NaN};
        RafaeliaCore.vectorMultiply(
                new float[] {2.0f, -3.0f, 9.0f},
                new float[] {4.0f, 5.0f, 2.0f},
                product,
                2);
        assertEquals(8.0f, product[0], 0.0f);
        assertEquals(-15.0f, product[1], 0.0f);
        assertTrue(Float.isNaN(product[2]));
    }

    @Test(expected = IllegalArgumentException.class)
    public void vectorAddRejectsInsufficientInputLength() {
        RafaeliaCore.vectorAdd(new float[1], new float[1], new float[1], 2);
    }

    @Test
    public void matrixMultiplyHandlesRectangularMatricesAndResetsOutput() {
        float[] result = new float[] {Float.NaN, Float.NaN, Float.NaN, Float.NaN};
        RafaeliaCore.matrixMultiply(
                new float[] {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f},
                new float[] {7.0f, 8.0f, 9.0f, 10.0f, 11.0f, 12.0f},
                result,
                2,
                3,
                2);
        assertArrayEquals(new float[] {58.0f, 64.0f, 139.0f, 154.0f}, result, 0.0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void optimizedMemoryCopyRejectsReadOnlyDestination() {
        ByteBuffer src = ByteBuffer.allocateDirect(1);
        ByteBuffer dst = ByteBuffer.allocateDirect(1).asReadOnlyBuffer();
        RafaeliaCore.optimizedMemoryCopy(src, 0, dst, 0, 1);
    }

}
