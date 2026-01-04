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
 * Tests for determinism guarantees in RmR core.
 * These tests verify that operations produce consistent, deterministic results.
 */
@RunWith(AndroidJUnit4.class)
public class RmRDeterminismTest {
    
    private static final double EPSILON = 1e-15;
    
    @Test
    public void testMatrixMultiplicationDeterminism() {
        // Same input should always produce same output
        RmRMatrix a = new RmRMatrix(4, 4);
        RmRMatrix b = new RmRMatrix(4, 4);
        
        // Initialize with specific values
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                a.set(i, j, i * 4.0 + j);
                b.set(i, j, (i + j) * 2.0);
            }
        }
        
        // Multiply multiple times
        RmRMatrix result1 = a.multiply(b);
        RmRMatrix result2 = a.multiply(b);
        RmRMatrix result3 = a.multiply(b);
        
        // All results must be identical
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals("Result must be deterministic", 
                           result1.get(i, j), result2.get(i, j), EPSILON);
                assertEquals("Result must be deterministic", 
                           result2.get(i, j), result3.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testLinearFlipDeterminism() {
        RmRMatrix matrix = new RmRMatrix(4, 4);
        
        // Initialize with non-zero values
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                matrix.set(i, j, (i + 1) * (j + 1) * 1.5);
            }
        }
        
        // Apply linear flip multiple times
        RmRMatrix flip1 = matrix.linearFlip();
        RmRMatrix flip2 = matrix.linearFlip();
        RmRMatrix flip3 = matrix.linearFlip();
        
        // All results must be identical
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals("Linear flip must be deterministic", 
                           flip1.get(i, j), flip2.get(i, j), EPSILON);
                assertEquals("Linear flip must be deterministic", 
                           flip2.get(i, j), flip3.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testStateTransformationDeterminism() {
        RmRState state = RmRState.forLifecycle();
        
        // Transform multiple times from same initial state
        RmRState transformed1 = state.transform();
        RmRState transformed2 = state.transform();
        RmRState transformed3 = state.transform();
        
        // Extract state matrices and compare
        RmRMatrix matrix1 = transformed1.getStateMatrix();
        RmRMatrix matrix2 = transformed2.getStateMatrix();
        RmRMatrix matrix3 = transformed3.getStateMatrix();
        
        for (int i = 0; i < matrix1.rows; i++) {
            for (int j = 0; j < matrix1.cols; j++) {
                assertEquals("State transformation must be deterministic",
                           matrix1.get(i, j), matrix2.get(i, j), EPSILON);
                assertEquals("State transformation must be deterministic",
                           matrix2.get(i, j), matrix3.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testVectorOperationsDeterminism() {
        double[] vector = {1.0, 2.0, 3.0, 4.0};
        
        // Normalize multiple times
        double[] norm1 = RmRUtils.normalize(vector);
        double[] norm2 = RmRUtils.normalize(vector);
        double[] norm3 = RmRUtils.normalize(vector);
        
        for (int i = 0; i < vector.length; i++) {
            assertEquals("Normalize must be deterministic",
                       norm1[i], norm2[i], EPSILON);
            assertEquals("Normalize must be deterministic",
                       norm2[i], norm3[i], EPSILON);
        }
    }
    
    @Test
    public void testDistanceMetricsDeterminism() {
        double[] a = {1.0, 2.0, 3.0, 4.0};
        double[] b = {5.0, 6.0, 7.0, 8.0};
        
        // Compute distances multiple times
        double euclidean1 = RmRUtils.euclideanDistance(a, b);
        double euclidean2 = RmRUtils.euclideanDistance(a, b);
        double euclidean3 = RmRUtils.euclideanDistance(a, b);
        
        assertEquals("Euclidean distance must be deterministic",
                   euclidean1, euclidean2, EPSILON);
        assertEquals("Euclidean distance must be deterministic",
                   euclidean2, euclidean3, EPSILON);
        
        double manhattan1 = RmRUtils.manhattanDistance(a, b);
        double manhattan2 = RmRUtils.manhattanDistance(a, b);
        double manhattan3 = RmRUtils.manhattanDistance(a, b);
        
        assertEquals("Manhattan distance must be deterministic",
                   manhattan1, manhattan2, EPSILON);
        assertEquals("Manhattan distance must be deterministic",
                   manhattan2, manhattan3, EPSILON);
        
        double cosine1 = RmRUtils.cosineSimilarity(a, b);
        double cosine2 = RmRUtils.cosineSimilarity(a, b);
        double cosine3 = RmRUtils.cosineSimilarity(a, b);
        
        assertEquals("Cosine similarity must be deterministic",
                   cosine1, cosine2, EPSILON);
        assertEquals("Cosine similarity must be deterministic",
                   cosine2, cosine3, EPSILON);
    }
    
    @Test
    public void testHashConsistency() {
        // Test that hash-like operations are consistent
        RmRMatrix matrix = new RmRMatrix(4, 4);
        
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                matrix.set(i, j, i * 4.0 + j);
            }
        }
        
        // Compute trace multiple times (acts like a hash)
        double trace1 = RmRUtils.trace(matrix);
        double trace2 = RmRUtils.trace(matrix);
        double trace3 = RmRUtils.trace(matrix);
        
        assertEquals("Trace must be deterministic", trace1, trace2, EPSILON);
        assertEquals("Trace must be deterministic", trace2, trace3, EPSILON);
        
        // Compute Frobenius norm multiple times
        double norm1 = RmRUtils.frobeniusNorm(matrix);
        double norm2 = RmRUtils.frobeniusNorm(matrix);
        double norm3 = RmRUtils.frobeniusNorm(matrix);
        
        assertEquals("Frobenius norm must be deterministic", norm1, norm2, EPSILON);
        assertEquals("Frobenius norm must be deterministic", norm2, norm3, EPSILON);
    }
    
    @Test
    public void testZeroHandlingDeterminism() {
        // Test deterministic handling of zero and near-zero values
        RmRMatrix matrix = new RmRMatrix(4, 4);
        
        // Set some values to zero
        matrix.set(0, 0, 0.0);
        matrix.set(1, 1, 1e-20); // Very small
        matrix.set(2, 2, 1.0);
        matrix.set(3, 3, 2.0);
        
        // Linear flip should handle zeros consistently
        RmRMatrix flip1 = matrix.linearFlip();
        RmRMatrix flip2 = matrix.linearFlip();
        
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals("Zero handling must be deterministic",
                           flip1.get(i, j), flip2.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testCrossRunDeterminism() {
        // Test that results are identical across multiple independent runs
        for (int run = 0; run < 10; run++) {
            RmRMatrix a = new RmRMatrix(3, 3);
            a.set(0, 0, 1.0); a.set(0, 1, 2.0); a.set(0, 2, 3.0);
            a.set(1, 0, 4.0); a.set(1, 1, 5.0); a.set(1, 2, 6.0);
            a.set(2, 0, 7.0); a.set(2, 1, 8.0); a.set(2, 2, 9.0);
            
            RmRMatrix b = RmRMatrix.identity(3);
            RmRMatrix result = a.multiply(b);
            
            // Every run should produce the same result
            assertEquals("Cross-run result must be deterministic", 
                       1.0, result.get(0, 0), EPSILON);
            assertEquals("Cross-run result must be deterministic",
                       5.0, result.get(1, 1), EPSILON);
            assertEquals("Cross-run result must be deterministic",
                       9.0, result.get(2, 2), EPSILON);
        }
    }
}
