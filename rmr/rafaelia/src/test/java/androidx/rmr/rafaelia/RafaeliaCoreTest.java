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

import static org.junit.Assert.assertEquals;

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
}
