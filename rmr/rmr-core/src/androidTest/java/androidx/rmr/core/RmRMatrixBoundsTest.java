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
 * Tests for RmRMatrix bounds checking methods.
 * Verifies that checked methods validate indices while unsafe methods do not.
 */
@RunWith(AndroidJUnit4.class)
public class RmRMatrixBoundsTest {
    
    @Test
    public void testCheckedGetWithValidIndices() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.set(1, 2, 42.0);
        
        // Should work fine with valid indices
        double value = matrix.getChecked(1, 2);
        assertEquals(42.0, value, 0.0);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testCheckedGetWithNegativeRow() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.getChecked(-1, 0);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testCheckedGetWithNegativeCol() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.getChecked(0, -1);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testCheckedGetWithRowOutOfBounds() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.getChecked(3, 0);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testCheckedGetWithColOutOfBounds() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.getChecked(0, 3);
    }
    
    @Test
    public void testCheckedSetWithValidIndices() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        
        // Should work fine with valid indices
        matrix.setChecked(1, 2, 42.0);
        assertEquals(42.0, matrix.get(1, 2), 0.0);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testCheckedSetWithNegativeRow() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.setChecked(-1, 0, 42.0);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testCheckedSetWithNegativeCol() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.setChecked(0, -1, 42.0);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testCheckedSetWithRowOutOfBounds() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.setChecked(3, 0, 42.0);
    }
    
    @Test(expected = IndexOutOfBoundsException.class)
    public void testCheckedSetWithColOutOfBounds() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.setChecked(0, 3, 42.0);
    }
    
    @Test
    public void testUnsafeGetWithValidIndices() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        matrix.set(1, 2, 42.0);
        
        // Unsafe method should work with valid indices
        double value = matrix.get(1, 2);
        assertEquals(42.0, value, 0.0);
    }
    
    @Test
    public void testUnsafeSetWithValidIndices() {
        RmRMatrix matrix = new RmRMatrix(3, 3);
        
        // Unsafe method should work with valid indices
        matrix.set(1, 2, 42.0);
        assertEquals(42.0, matrix.get(1, 2), 0.0);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeRows() {
        new RmRMatrix(-1, 3);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeCols() {
        new RmRMatrix(3, -1);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithOverflowDimensions() {
        // This should overflow: Integer.MAX_VALUE * 2
        new RmRMatrix(Integer.MAX_VALUE, 2);
    }
    
    @Test
    public void testConstructorWithLargeDimensions() {
        // Large but valid dimensions (won't overflow)
        int size = 1000;
        RmRMatrix matrix = new RmRMatrix(size, size);
        
        assertEquals(size, matrix.rows);
        assertEquals(size, matrix.cols);
        assertEquals(size * size, matrix.data.length);
    }
    
    @Test
    public void testConstructorWithZeroDimensions() {
        // Zero dimensions should be allowed
        RmRMatrix matrix = new RmRMatrix(0, 0);
        
        assertEquals(0, matrix.rows);
        assertEquals(0, matrix.cols);
        assertEquals(0, matrix.data.length);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithDataLengthMismatch() {
        double[] data = new double[10];
        // 3 * 3 = 9, but data has length 10
        new RmRMatrix(3, 3, data);
    }
    
    @Test
    public void testConstructorWithValidData() {
        double[] data = {1, 2, 3, 4, 5, 6};
        RmRMatrix matrix = new RmRMatrix(2, 3, data);
        
        assertEquals(2, matrix.rows);
        assertEquals(3, matrix.cols);
        
        // Verify data is copied
        assertEquals(1.0, matrix.get(0, 0), 0.0);
        assertEquals(2.0, matrix.get(0, 1), 0.0);
        assertEquals(3.0, matrix.get(0, 2), 0.0);
        assertEquals(4.0, matrix.get(1, 0), 0.0);
        assertEquals(5.0, matrix.get(1, 1), 0.0);
        assertEquals(6.0, matrix.get(1, 2), 0.0);
    }
    
    @Test
    public void testCheckedMethodsEdgeCases() {
        RmRMatrix matrix = new RmRMatrix(5, 5);
        
        // Test all valid edge indices
        matrix.setChecked(0, 0, 1.0);
        matrix.setChecked(0, 4, 2.0);
        matrix.setChecked(4, 0, 3.0);
        matrix.setChecked(4, 4, 4.0);
        
        assertEquals(1.0, matrix.getChecked(0, 0), 0.0);
        assertEquals(2.0, matrix.getChecked(0, 4), 0.0);
        assertEquals(3.0, matrix.getChecked(4, 0), 0.0);
        assertEquals(4.0, matrix.getChecked(4, 4), 0.0);
    }
}
