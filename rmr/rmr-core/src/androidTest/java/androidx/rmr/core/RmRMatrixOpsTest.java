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
 * Tests for RmRMatrixOps class.
 */
@RunWith(AndroidJUnit4.class)
public class RmRMatrixOpsTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    public void testSmallMatrixMultiplication() {
        // Create 2x2 matrices
        RmRMatrix a = new RmRMatrix(2, 2);
        a.set(0, 0, 1.0); a.set(0, 1, 2.0);
        a.set(1, 0, 3.0); a.set(1, 1, 4.0);
        
        RmRMatrix b = new RmRMatrix(2, 2);
        b.set(0, 0, 5.0); b.set(0, 1, 6.0);
        b.set(1, 0, 7.0); b.set(1, 1, 8.0);
        
        RmRMatrix result = new RmRMatrix(2, 2);
        RmRMatrixOps.multiply(a, b, result);
        
        // Expected: [1*5+2*7, 1*6+2*8] = [19, 22]
        //           [3*5+4*7, 3*6+4*8] = [43, 50]
        assertEquals(19.0, result.get(0, 0), EPSILON);
        assertEquals(22.0, result.get(0, 1), EPSILON);
        assertEquals(43.0, result.get(1, 0), EPSILON);
        assertEquals(50.0, result.get(1, 1), EPSILON);
    }
    
    @Test
    public void testIdentityMultiplication() {
        RmRMatrix a = new RmRMatrix(4, 4);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                a.set(i, j, i * 4 + j);
            }
        }
        
        RmRMatrix identity = RmRMatrix.identity(4);
        RmRMatrix result = new RmRMatrix(4, 4);
        
        RmRMatrixOps.multiply(a, identity, result);
        
        // Result should equal a
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals(a.get(i, j), result.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testLargeMatrixMultiplication() {
        // Test with larger matrices to trigger blocked algorithm
        int size = 64;
        RmRMatrix a = new RmRMatrix(size, size);
        RmRMatrix b = RmRMatrix.identity(size);
        RmRMatrix result = new RmRMatrix(size, size);
        
        // Initialize a with some values
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                a.set(i, j, i + j);
            }
        }
        
        RmRMatrixOps.multiply(a, b, result);
        
        // Result should equal a
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                assertEquals(a.get(i, j), result.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testTransposeSmall() {
        RmRMatrix a = new RmRMatrix(2, 3);
        a.set(0, 0, 1.0); a.set(0, 1, 2.0); a.set(0, 2, 3.0);
        a.set(1, 0, 4.0); a.set(1, 1, 5.0); a.set(1, 2, 6.0);
        
        RmRMatrix result = new RmRMatrix(3, 2);
        RmRMatrixOps.transpose(a, result);
        
        assertEquals(1.0, result.get(0, 0), EPSILON);
        assertEquals(4.0, result.get(0, 1), EPSILON);
        assertEquals(2.0, result.get(1, 0), EPSILON);
        assertEquals(5.0, result.get(1, 1), EPSILON);
        assertEquals(3.0, result.get(2, 0), EPSILON);
        assertEquals(6.0, result.get(2, 1), EPSILON);
    }
    
    @Test
    public void testTransposeLarge() {
        // Test blocked transpose
        int size = 64;
        RmRMatrix a = new RmRMatrix(size, size);
        RmRMatrix result = new RmRMatrix(size, size);
        
        // Initialize with values
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                a.set(i, j, i * size + j);
            }
        }
        
        RmRMatrixOps.transpose(a, result);
        
        // Verify transpose
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                assertEquals(a.get(i, j), result.get(j, i), EPSILON);
            }
        }
    }
    
    @Test
    public void testDotProduct() {
        double[] a = {1.0, 2.0, 3.0, 4.0};
        double[] b = {5.0, 6.0, 7.0, 8.0};
        
        double result = RmRMatrixOps.dotProduct(a, b);
        
        // Expected: 1*5 + 2*6 + 3*7 + 4*8 = 5 + 12 + 21 + 32 = 70
        assertEquals(70.0, result, EPSILON);
    }
    
    @Test
    public void testDotProductOrthogonal() {
        double[] a = {1.0, 0.0, 0.0};
        double[] b = {0.0, 1.0, 0.0};
        
        double result = RmRMatrixOps.dotProduct(a, b);
        
        // Orthogonal vectors have dot product of 0
        assertEquals(0.0, result, EPSILON);
    }
    
    @Test
    public void testVectorAddition() {
        double[] a = {1.0, 2.0, 3.0};
        double[] b = {4.0, 5.0, 6.0};
        double[] result = new double[3];
        
        RmRMatrixOps.addVectors(a, b, result);
        
        assertEquals(5.0, result[0], EPSILON);
        assertEquals(7.0, result[1], EPSILON);
        assertEquals(9.0, result[2], EPSILON);
    }
    
    @Test
    public void testVectorScaling() {
        double[] vector = {1.0, 2.0, 3.0, 4.0};
        double[] result = new double[4];
        
        RmRMatrixOps.scaleVector(vector, 2.0, result);
        
        assertEquals(2.0, result[0], EPSILON);
        assertEquals(4.0, result[1], EPSILON);
        assertEquals(6.0, result[2], EPSILON);
        assertEquals(8.0, result[3], EPSILON);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMultiplyIncompatibleDimensions() {
        RmRMatrix a = new RmRMatrix(2, 3);
        RmRMatrix b = new RmRMatrix(4, 2);
        RmRMatrix result = new RmRMatrix(2, 2);
        
        RmRMatrixOps.multiply(a, b, result); // Should throw
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testTransposeWrongOutputDimensions() {
        RmRMatrix a = new RmRMatrix(2, 3);
        RmRMatrix result = new RmRMatrix(2, 3); // Should be 3x2
        
        RmRMatrixOps.transpose(a, result); // Should throw
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testDotProductDifferentLengths() {
        double[] a = {1.0, 2.0, 3.0};
        double[] b = {1.0, 2.0};
        
        RmRMatrixOps.dotProduct(a, b); // Should throw
    }
    
    @Test
    public void testIntegrationWithRmRMatrix() {
        // Test that RmRMatrix.multiply() uses optimized implementation
        RmRMatrix a = new RmRMatrix(3, 3);
        RmRMatrix b = RmRMatrix.identity(3);
        
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                a.set(i, j, i * 3 + j);
            }
        }
        
        RmRMatrix result = a.multiply(b);
        
        // Result should equal a
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals(a.get(i, j), result.get(i, j), EPSILON);
            }
        }
    }
}
