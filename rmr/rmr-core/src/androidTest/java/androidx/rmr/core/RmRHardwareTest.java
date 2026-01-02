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

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

/**
 * Tests for RmRHardware class.
 */
@RunWith(AndroidJUnit4.class)
public class RmRHardwareTest {
    
    @Test
    public void testArchitectureDetection() {
        RmRHardware.Architecture arch = RmRHardware.getArchitecture();
        assertNotNull("Architecture should be detected", arch);
        assertNotEquals("Architecture should not be UNKNOWN on Android", 
                       RmRHardware.Architecture.UNKNOWN, arch);
    }
    
    @Test
    public void testSimdCapabilityDetection() {
        RmRHardware.SimdCapability simd = RmRHardware.getSimdCapability();
        assertNotNull("SIMD capability should be detected", simd);
        
        // On ARM64, NEON should always be available
        RmRHardware.Architecture arch = RmRHardware.getArchitecture();
        if (arch == RmRHardware.Architecture.ARM64) {
            assertEquals("ARM64 should have NEON support", 
                        RmRHardware.SimdCapability.NEON, simd);
            assertTrue("ARM64 should report NEON support", 
                      RmRHardware.hasNeonSupport());
        }
    }
    
    @Test
    public void testCoreCountDetection() {
        int cores = RmRHardware.getCoreCount();
        assertTrue("Core count should be positive", cores > 0);
        assertTrue("Core count should be reasonable (1-256)", cores <= 256);
    }
    
    @Test
    public void testSimdSupportFlag() {
        boolean hasSimd = RmRHardware.hasSimdSupport();
        RmRHardware.SimdCapability simd = RmRHardware.getSimdCapability();
        
        if (simd == RmRHardware.SimdCapability.NONE) {
            assertFalse("hasSimdSupport should be false when SIMD is NONE", hasSimd);
        } else {
            assertTrue("hasSimdSupport should be true when SIMD is available", hasSimd);
        }
    }
    
    @Test
    public void testVectorWidth() {
        int vectorWidth = RmRHardware.getVectorWidth();
        assertTrue("Vector width should be positive", vectorWidth > 0);
        assertTrue("Vector width should be power of 2", 
                  Integer.bitCount(vectorWidth) == 1);
        assertTrue("Vector width should be reasonable (1-8)", 
                  vectorWidth >= 1 && vectorWidth <= 8);
    }
    
    @Test
    public void testCacheLineAlignment() {
        assertEquals("Cache line size should be 64 bytes", 
                    64, RmRHardware.CACHE_LINE_SIZE);
        
        assertTrue("0 should be cache-line aligned", 
                  RmRHardware.isCacheLineAligned(0));
        assertTrue("64 should be cache-line aligned", 
                  RmRHardware.isCacheLineAligned(64));
        assertTrue("128 should be cache-line aligned", 
                  RmRHardware.isCacheLineAligned(128));
        assertFalse("32 should not be cache-line aligned", 
                   RmRHardware.isCacheLineAligned(32));
    }
    
    @Test
    public void testAlignToCacheLine() {
        assertEquals("0 should align to 0", 0, RmRHardware.alignToCacheLine(0));
        assertEquals("1 should align to 64", 64, RmRHardware.alignToCacheLine(1));
        assertEquals("64 should align to 64", 64, RmRHardware.alignToCacheLine(64));
        assertEquals("65 should align to 128", 128, RmRHardware.alignToCacheLine(65));
        assertEquals("128 should align to 128", 128, RmRHardware.alignToCacheLine(128));
    }
    
    @Test
    public void testOptimalBlockSizeCalculation() {
        int blockSize = RmRHardware.calculateOptimalBlockSize(8); // 8 bytes for double
        assertTrue("Block size should be positive", blockSize > 0);
        assertTrue("Block size should be power of 2", 
                  Integer.bitCount(blockSize) == 1);
        assertTrue("Block size should be reasonable (8-256)", 
                  blockSize >= 8 && blockSize <= 256);
    }
    
    @Test
    public void testParallelizationThreshold() {
        // Should not parallelize small workloads
        assertFalse("Should not parallelize 100 operations", 
                   RmRHardware.shouldParallelize(100));
        
        // Should parallelize large workloads
        assertTrue("Should parallelize 1000000 operations", 
                  RmRHardware.shouldParallelize(1000000));
    }
    
    @Test
    public void testHardwareDescription() {
        String desc = RmRHardware.getHardwareDescription();
        assertNotNull("Hardware description should not be null", desc);
        assertTrue("Description should contain architecture", 
                  desc.contains("Architecture:"));
        assertTrue("Description should contain SIMD info", 
                  desc.contains("SIMD:"));
        assertTrue("Description should contain core count", 
                  desc.contains("Cores:"));
    }
    
    @Test
    public void testDoublesPerCacheLine() {
        assertEquals("8 doubles should fit in 64-byte cache line", 
                    8, RmRHardware.DOUBLES_PER_CACHE_LINE);
    }
    
    @Test
    public void testOptimalBlockSize() {
        assertTrue("Optimal block size should be reasonable", 
                  RmRHardware.OPTIMAL_BLOCK_SIZE >= 16 && 
                  RmRHardware.OPTIMAL_BLOCK_SIZE <= 128);
    }
}
