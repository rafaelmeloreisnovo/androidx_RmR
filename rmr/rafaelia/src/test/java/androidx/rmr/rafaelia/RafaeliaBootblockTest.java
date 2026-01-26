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
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

public class RafaeliaBootblockTest {

    @Test
    public void vqfLoadIsDeterministic() {
        int[] first = RafaeliaBootblock.getVqfLoad();
        int[] second = RafaeliaBootblock.getVqfLoad();

        assertEquals(42, first.length);
        assertEquals(1, first[0]);
        assertEquals(42, first[41]);
        assertNotSame(first, second);
    }

    @Test
    public void sealsAreDefensiveCopies() {
        String[] first = RafaeliaBootblock.getSeals();
        String[] second = RafaeliaBootblock.getSeals();

        assertEquals(11, first.length);
        assertEquals("藏智界", first[0]);
        assertNotSame(first, second);
    }
}
