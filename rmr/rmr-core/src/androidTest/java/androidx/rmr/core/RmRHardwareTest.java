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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class RmRHardwareTest {

    @Test
    public void testSimdLevelAlwaysResolves() {
        RmRHardware.SimdLevel level = RmRHardware.getSimdLevel();
        assertNotNull("SIMD level should always resolve", level);
    }

    @Test
    public void testAbiFallbackArm64IsNeon() {
        RmRHardware.SimdLevel level = RmRHardware.getSimdLevelFromAbi(
                new String[] {"arm64-v8a"});
        assertEquals("arm64 fallback should be NEON", RmRHardware.SimdLevel.NEON, level);
    }

    @Test
    public void testAbiFallbackX86_64DoesNotAssumeAvx() {
        RmRHardware.SimdLevel level = RmRHardware.getSimdLevelFromAbi(
                new String[] {"x86_64"});
        assertEquals("x86_64 fallback should be conservative SSE",
                RmRHardware.SimdLevel.SSE, level);
    }

    @Test
    public void testNativeMappingDoesNotInflateUnknownValues() {
        RmRHardware.SimdLevel level = RmRHardware.simdLevelFromNativeValue(99);
        assertNull("Unknown native values should not map to AVX", level);
    }
}
