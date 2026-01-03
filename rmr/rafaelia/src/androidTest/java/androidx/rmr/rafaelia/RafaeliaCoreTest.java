/*
 * Copyright (C) 2026 Rafael Melo Reis (RmR)
 *
 * Licensed under the Apache License, Version 2.0 with Additional Restrictions.
 * See LEGAL_NOTICE.md for complete terms and automatic penalty provisions.
 */

package androidx.rmr.rafaelia;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.ByteBuffer;

/**
 * Tests for RafaeliaCore bare-metal operations.
 * 
 * Note: These tests validate basic functionality.
 * Full performance benchmarks should be run separately.
 */
@RunWith(AndroidJUnit4.class)
public class RafaeliaCoreTest {
    
    @Test
    public void testCreateInstance() {
        // Should create instance with direct memory
        RafaeliaCore core = RafaeliaCore.create(1024);
        assertNotNull(core);
        
        ByteBuffer buffer = core.getDirectMemory();
        assertNotNull(buffer);
        assertTrue(buffer.isDirect());
        assertTrue(buffer.capacity() >= 1024);
    }
    
    @Test
    public void testVectorAddition() {
        // Test SIMD vector addition
        int length = 100;
        float[] a = new float[length];
        float[] b = new float[length];
        float[] result = new float[length];
        
        // Initialize arrays
        for (int i = 0; i < length; i++) {
            a[i] = i;
            b[i] = i * 2;
        }
        
        // Perform addition
        RafaeliaCore.vectorAdd(a, b, result, length);
        
        // Verify results
        for (int i = 0; i < length; i++) {
            assertEquals("Vector add at index " + i, 
                        i + (i * 2), result[i], 0.0001f);
        }
    }
    
    @Test
    public void testVectorMultiplication() {
        // Test SIMD vector multiplication
        int length = 100;
        float[] a = new float[length];
        float[] b = new float[length];
        float[] result = new float[length];
        
        // Initialize arrays
        for (int i = 0; i < length; i++) {
            a[i] = i + 1;
            b[i] = 2;
        }
        
        // Perform multiplication
        RafaeliaCore.vectorMultiply(a, b, result, length);
        
        // Verify results
        for (int i = 0; i < length; i++) {
            assertEquals("Vector multiply at index " + i,
                        (i + 1) * 2, result[i], 0.0001f);
        }
    }
    
    @Test
    public void testMatrixMultiplication() {
        // Test cache-optimized matrix multiplication
        int size = 4;
        float[] a = new float[size * size];
        float[] b = new float[size * size];
        float[] result = new float[size * size];
        
        // Initialize identity matrix for a
        for (int i = 0; i < size; i++) {
            a[i * size + i] = 1.0f;
        }
        
        // Initialize b with sequential values
        for (int i = 0; i < size * size; i++) {
            b[i] = i + 1;
        }
        
        // Multiply: identity * b = b
        RafaeliaCore.matrixMultiply(a, b, result, size, size, size);
        
        // Verify result equals b
        for (int i = 0; i < size * size; i++) {
            assertEquals("Matrix multiply at index " + i,
                        b[i], result[i], 0.0001f);
        }
    }
    
    @Test
    public void testCpuFeatureDetection() {
        // Test CPU feature detection
        int features = RafaeliaCore.getCpuFeatures();
        
        // At minimum, should report some features (or 0 if none available)
        assertTrue("CPU features should be non-negative", features >= 0);
        
        // Log detected features for debugging
        System.out.println("Detected CPU features: 0x" + Integer.toHexString(features));
    }
    
    @Test
    public void testMemoryCopy() {
        // Test optimized memory copy
        int size = 256;
        ByteBuffer src = ByteBuffer.allocateDirect(size);
        ByteBuffer dst = ByteBuffer.allocateDirect(size);
        
        // Fill source with pattern
        for (int i = 0; i < size; i++) {
            src.put(i, (byte)(i & 0xFF));
        }
        
        // Copy using optimized method
        RafaeliaCore.optimizedMemoryCopy(src, 0, dst, 0, size);
        
        // Verify copy
        for (int i = 0; i < size; i++) {
            assertEquals("Memory copy at index " + i,
                        src.get(i), dst.get(i));
        }
    }
    
    @Test
    public void testCacheAlignment() {
        // Test that allocated memory is properly sized
        RafaeliaCore core = RafaeliaCore.create(100);
        ByteBuffer buffer = core.getDirectMemory();
        
        // Should be aligned to cache line (64 bytes)
        // Size should be at least the requested size
        assertTrue("Buffer should be at least requested size",
                  buffer.capacity() >= 100);
        
        // Size should be multiple of cache line for optimal performance
        assertTrue("Buffer should be aligned",
                  buffer.capacity() % 64 == 0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testVectorOperationBoundsCheck() {
        // Test that operations validate array bounds
        float[] a = new float[10];
        float[] b = new float[10];
        float[] result = new float[10];
        
        // This should throw because length > array size
        RafaeliaCore.vectorAdd(a, b, result, 20);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMatrixOperationBoundsCheck() {
        // Test that matrix operations validate array bounds
        float[] a = new float[16];
        float[] b = new float[16];
        float[] result = new float[16];
        
        // This should throw because arrays are too small for 8x8 matrix
        RafaeliaCore.matrixMultiply(a, b, result, 8, 8, 8);
    }
}
