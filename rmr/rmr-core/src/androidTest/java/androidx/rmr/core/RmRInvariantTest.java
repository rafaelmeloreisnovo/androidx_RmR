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
 * Tests for mathematical invariants in RmR core.
 * These tests verify that core mathematical properties hold.
 */
@RunWith(AndroidJUnit4.class)
public class RmRInvariantTest {
    
    private static final double EPSILON = 1e-10;
    
    // For operations that accumulate numerical errors (e.g., multiple matrix multiplications)
    // we use a larger epsilon to account for floating-point precision loss
    private static final double ACCUMULATED_ERROR_EPSILON = EPSILON * 100;
    
    @Test
    public void testIdentityInvariant() {
        // I * M = M for any matrix M
        RmRMatrix matrix = new RmRMatrix(4, 4);
        
        // Fill with test values
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                matrix.set(i, j, i * 4.0 + j + 1.0);
            }
        }
        
        RmRMatrix identity = RmRMatrix.identity(4);
        RmRMatrix result = identity.multiply(matrix);
        
        // Result should equal original matrix
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals("Identity invariant: I * M = M",
                           matrix.get(i, j), result.get(i, j), EPSILON);
            }
        }
        
        // Also test M * I = M
        RmRMatrix result2 = matrix.multiply(identity);
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals("Identity invariant: M * I = M",
                           matrix.get(i, j), result2.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testAssociativityInvariant() {
        // (A * B) * C = A * (B * C)
        RmRMatrix a = new RmRMatrix(3, 3);
        RmRMatrix b = new RmRMatrix(3, 3);
        RmRMatrix c = new RmRMatrix(3, 3);
        
        // Initialize matrices
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                a.set(i, j, i + j + 1.0);
                b.set(i, j, i - j + 2.0);
                c.set(i, j, i * j + 1.0);
            }
        }
        
        // Compute (A * B) * C
        RmRMatrix ab = a.multiply(b);
        RmRMatrix abc1 = ab.multiply(c);
        
        // Compute A * (B * C)
        RmRMatrix bc = b.multiply(c);
        RmRMatrix abc2 = a.multiply(bc);
        
        // Results should be equal
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                assertEquals("Associativity invariant: (A*B)*C = A*(B*C)",
                           abc1.get(i, j), abc2.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testTransposeTransposeInvariant() {
        // (M^T)^T = M
        RmRMatrix matrix = new RmRMatrix(3, 4);
        
        // Fill with test values
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                matrix.set(i, j, i * 4.0 + j);
            }
        }
        
        // Transpose twice
        RmRMatrix transpose1 = matrix.transpose();
        RmRMatrix transpose2 = transpose1.transpose();
        
        // Should get back original
        assertEquals("Dimensions should match", matrix.rows, transpose2.rows);
        assertEquals("Dimensions should match", matrix.cols, transpose2.cols);
        
        for (int i = 0; i < matrix.rows; i++) {
            for (int j = 0; j < matrix.cols; j++) {
                assertEquals("Transpose invariant: (M^T)^T = M",
                           matrix.get(i, j), transpose2.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testAdditionCommutativeInvariant() {
        // A + B = B + A
        RmRMatrix a = new RmRMatrix(4, 4);
        RmRMatrix b = new RmRMatrix(4, 4);
        
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                a.set(i, j, i * 2.0 + j);
                b.set(i, j, i + j * 3.0);
            }
        }
        
        RmRMatrix aPlusB = a.add(b);
        RmRMatrix bPlusA = b.add(a);
        
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals("Addition commutative invariant: A + B = B + A",
                           aPlusB.get(i, j), bPlusA.get(i, j), EPSILON);
            }
        }
    }
    
    @Test
    public void testVectorNormalizationInvariant() {
        // ||normalize(v)|| = 1 (unit length)
        double[] vector = {3.0, 4.0, 0.0}; // Length should be 5
        
        double[] normalized = RmRUtils.normalize(vector);
        
        // Compute norm of normalized vector
        double norm = 0.0;
        for (double v : normalized) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        
        assertEquals("Normalization invariant: ||normalize(v)|| = 1",
                   1.0, norm, EPSILON);
    }
    
    @Test
    public void testDotProductSymmetryInvariant() {
        // dot(a, b) = dot(b, a)
        double[] a = {1.0, 2.0, 3.0, 4.0};
        double[] b = {5.0, 6.0, 7.0, 8.0};
        
        double dotAB = RmRMatrixOps.dotProduct(a, b);
        double dotBA = RmRMatrixOps.dotProduct(b, a);
        
        assertEquals("Dot product symmetry: dot(a,b) = dot(b,a)",
                   dotAB, dotBA, EPSILON);
    }
    
    @Test
    public void testTraceInvariantUnderCyclicPermutation() {
        // tr(ABC) = tr(CAB) = tr(BCA)
        // This is a fundamental property of matrix trace
        RmRMatrix a = new RmRMatrix(3, 3);
        RmRMatrix b = new RmRMatrix(3, 3);
        RmRMatrix c = new RmRMatrix(3, 3);
        
        // Initialize small matrices for easier computation
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                a.set(i, j, i + 1.0);
                b.set(i, j, j + 1.0);
                c.set(i, j, i + j + 1.0);
            }
        }
        
        // Compute ABC, CAB, BCA
        RmRMatrix abc = a.multiply(b).multiply(c);
        RmRMatrix cab = c.multiply(a).multiply(b);
        RmRMatrix bca = b.multiply(c).multiply(a);
        
        double traceABC = RmRUtils.trace(abc);
        double traceCAB = RmRUtils.trace(cab);
        double traceBCA = RmRUtils.trace(bca);
        
        assertEquals("Trace cyclic invariant: tr(ABC) = tr(CAB)",
                   traceABC, traceCAB, ACCUMULATED_ERROR_EPSILON);
        assertEquals("Trace cyclic invariant: tr(ABC) = tr(BCA)",
                   traceABC, traceBCA, ACCUMULATED_ERROR_EPSILON);
    }
    
    @Test
    public void testDistanceTriangleInequality() {
        // d(a, c) <= d(a, b) + d(b, c)
        double[] a = {0.0, 0.0, 0.0};
        double[] b = {3.0, 4.0, 0.0};
        double[] c = {6.0, 8.0, 0.0};
        
        double distAB = RmRUtils.euclideanDistance(a, b);
        double distBC = RmRUtils.euclideanDistance(b, c);
        double distAC = RmRUtils.euclideanDistance(a, c);
        
        assertTrue("Triangle inequality: d(a,c) <= d(a,b) + d(b,c)",
                 distAC <= distAB + distBC + EPSILON);
    }
    
    @Test
    public void testCosineSimilarityBounds() {
        // -1 <= cosine(a, b) <= 1
        double[] a = {1.0, 2.0, 3.0};
        double[] b = {4.0, 5.0, 6.0};
        
        double similarity = RmRUtils.cosineSimilarity(a, b);
        
        assertTrue("Cosine similarity lower bound: >= -1",
                 similarity >= -1.0 - EPSILON);
        assertTrue("Cosine similarity upper bound: <= 1",
                 similarity <= 1.0 + EPSILON);
    }
    
    @Test
    public void testLinearFlipApproximateInversion() {
        // linearFlip(linearFlip(M)) ≈ M (for non-zero elements)
        RmRMatrix matrix = new RmRMatrix(3, 3);
        
        // Fill with non-zero values
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                matrix.set(i, j, (i + 1) * (j + 1) * 2.0);
            }
        }
        
        RmRMatrix flipped = matrix.linearFlip();
        RmRMatrix doubleFlipped = flipped.linearFlip();
        
        // Should approximately equal original (within reasonable tolerance)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                double original = matrix.get(i, j);
                double restored = doubleFlipped.get(i, j);
                double relativeError = Math.abs((original - restored) / original);
                assertTrue("Linear flip approximate inversion",
                         relativeError < 1e-10);
            }
        }
    }
    
    @Test
    public void testDimensionConsistencyInvariant() {
        // Verify that operations maintain correct dimensions
        RmRMatrix a = new RmRMatrix(3, 4);
        RmRMatrix b = new RmRMatrix(4, 5);
        
        RmRMatrix c = a.multiply(b);
        
        assertEquals("Multiplication dimension invariant: rows",
                   3, c.rows);
        assertEquals("Multiplication dimension invariant: cols",
                   5, c.cols);
        
        RmRMatrix d = a.transpose();
        assertEquals("Transpose dimension invariant: rows",
                   4, d.rows);
        assertEquals("Transpose dimension invariant: cols",
                   3, d.cols);
    }
    
    @Test
    public void testStateImmutabilityInvariant() {
        // State transformations should not modify original state
        RmRState original = RmRState.forLifecycle();
        RmRMatrix originalMatrix = original.getStateMatrix();
        
        // Store original values
        double[][] originalValues = new double[4][4];
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                originalValues[i][j] = originalMatrix.get(i, j);
            }
        }
        
        // Perform transformations
        RmRState transformed = original.transform();
        RmRState optimized = original.optimize();
        
        // Verify original unchanged
        RmRMatrix checkMatrix = original.getStateMatrix();
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                assertEquals("State immutability invariant",
                           originalValues[i][j], checkMatrix.get(i, j), EPSILON);
            }
        }
    }
}
