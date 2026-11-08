/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 * SPDX-License-Identifier: Apache-2.0
 */
package androidx.rmr.rafaelia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.nio.ByteBuffer;
import org.junit.Test;

public class RafaeliaMemoryCopyTest {
    @Test
    public void overlappingHeapBufferUsesMemmoveSemantics() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        for (int i = 0; i < 8; i++) buffer.put(i, (byte) i);
        RafaeliaCore.optimizedMemoryCopy(buffer, 0, buffer, 2, 6);
        for (int i = 0; i < 2; i++) assertEquals((byte) i, buffer.get(i));
        for (int i = 2; i < 8; i++) assertEquals((byte) (i - 2), buffer.get(i));
    }

    @Test
    public void overlappingHeapAliasesViaDifferentViewsRemainCorrect() {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        for (int i = 0; i < 9; i++) buffer.put(i, (byte) (i + 10));
        RafaeliaCore.optimizedMemoryCopy(buffer.duplicate(), 0,
                buffer.slice(), 1, 8);
        assertEquals((byte) 10, buffer.get(0));
        for (int i = 1; i < 9; i++) {
            assertEquals((byte) (9 + i), buffer.get(i));
        }
    }

    @Test
    public void nativeOrFallbackRespectsDirectBufferOffsets() {
        ByteBuffer from = ByteBuffer.allocateDirect(8);
        ByteBuffer to = ByteBuffer.allocateDirect(8);
        for (int i = 0; i < 8; i++) from.put(i, (byte) (i + 1));
        RafaeliaCore.optimizedMemoryCopy(from, 2, to, 3, 4);
        for (int i = 0; i < 4; i++) assertEquals((byte) (i + 3), to.get(i + 3));
    }

    @Test
    public void readOnlyDestinationRejectedWithoutWriting() {
        ByteBuffer src = ByteBuffer.allocate(4);
        ByteBuffer readonly = ByteBuffer.allocate(4).asReadOnlyBuffer();
        try {
            RafaeliaCore.optimizedMemoryCopy(src, 0, readonly, 0, 1);
            fail("Read-only destination must reject");
        } catch (IllegalArgumentException expected) {
            // No mutation.
        }
    }

    @Test
    public void capacityOverflowRejected() {
        ByteBuffer src = ByteBuffer.allocate(4);
        ByteBuffer dst = ByteBuffer.allocate(4);
        try {
            RafaeliaCore.optimizedMemoryCopy(src, Integer.MAX_VALUE,
                    dst, 0, Integer.MAX_VALUE);
            fail("Oversized ranges must reject");
        } catch (IndexOutOfBoundsException expected) {
            // No mutation.
        }
    }
}
