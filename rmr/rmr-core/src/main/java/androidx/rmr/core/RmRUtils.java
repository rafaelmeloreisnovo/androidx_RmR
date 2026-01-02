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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Utility class demonstrating RmR integration patterns.
 * 
 * <p>Provides helper methods for common RmR operations and demonstrates
 * best practices for using the matrix-based state management system.</p>
 * 
 * @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RmRUtils {
    
    private RmRUtils() {
        // Utility class - no instantiation
    }
    
    /**
     * Creates a state vector from integer values.
     * Useful for encoding discrete state into matrix form.
     * 
     * @param values integer values to encode
     * @return double array for matrix operations
     */
    @NonNull
    public static double[] encodeIntegers(@NonNull int[] values) {
        double[] result = new double[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (double) values[i];
        }
        return result;
    }
    
    /**
     * Decodes state vector to integer values.
     * 
     * @param values double array from matrix
     * @return decoded integer values
     */
    @NonNull
    public static int[] decodeIntegers(@NonNull double[] values) {
        int[] result = new int[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = (int) Math.round(values[i]);
        }
        return result;
    }
    
    /**
     * Computes cosine similarity between two state vectors.
     * Useful for determining state similarity.
     * 
     * @param a first state vector
     * @param b second state vector
     * @return similarity score (-1 to 1, higher is more similar)
     */
    public static double cosineSimilarity(@NonNull double[] a, @NonNull double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }
        
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
    
    /**
     * Normalizes a state vector to unit length.
     * Useful for scale-independent state comparison.
     * 
     * @param vector state vector to normalize
     * @return normalized vector
     */
    @NonNull
    public static double[] normalize(@NonNull double[] vector) {
        double norm = 0.0;
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        
        if (norm == 0.0) {
            return vector.clone();
        }
        
        double[] result = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            result[i] = vector[i] / norm;
        }
        return result;
    }
    
    /**
     * Interpolates between two state vectors.
     * Useful for smooth state transitions.
     * 
     * @param start starting state
     * @param end ending state
     * @param t interpolation parameter (0 to 1)
     * @return interpolated state
     */
    @NonNull
    public static double[] interpolate(@NonNull double[] start, @NonNull double[] end, double t) {
        if (start.length != end.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }
        
        t = Math.max(0.0, Math.min(1.0, t)); // Clamp to [0, 1]
        
        double[] result = new double[start.length];
        for (int i = 0; i < start.length; i++) {
            result[i] = start[i] + t * (end[i] - start[i]);
        }
        return result;
    }
    
    /**
     * Computes Manhattan distance between two state vectors.
     * Useful for measuring state difference.
     * 
     * @param a first state vector
     * @param b second state vector
     * @return distance (0 is identical)
     */
    public static double manhattanDistance(@NonNull double[] a, @NonNull double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }
        
        double distance = 0.0;
        for (int i = 0; i < a.length; i++) {
            distance += Math.abs(a[i] - b[i]);
        }
        return distance;
    }
    
    /**
     * Computes Euclidean distance between two state vectors.
     * Useful for geometric state comparison.
     * 
     * @param a first state vector
     * @param b second state vector
     * @return distance (0 is identical)
     */
    public static double euclideanDistance(@NonNull double[] a, @NonNull double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have same length");
        }
        
        double sumSquares = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sumSquares += diff * diff;
        }
        return Math.sqrt(sumSquares);
    }
    
    /**
     * Creates rotation matrix for 2D state transformations.
     * Useful for circular state transitions.
     * 
     * @param angle rotation angle in radians
     * @return 2x2 rotation matrix
     */
    @NonNull
    public static RmRMatrix createRotationMatrix(double angle) {
        RmRMatrix matrix = new RmRMatrix(2, 2);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        
        matrix.set(0, 0, cos);
        matrix.set(0, 1, -sin);
        matrix.set(1, 0, sin);
        matrix.set(1, 1, cos);
        
        return matrix;
    }
    
    /**
     * Creates scaling matrix for state amplification.
     * Useful for state magnitude adjustments.
     * 
     * @param scaleX scale factor for first dimension
     * @param scaleY scale factor for second dimension
     * @return 2x2 scaling matrix
     */
    @NonNull
    public static RmRMatrix createScaleMatrix(double scaleX, double scaleY) {
        RmRMatrix matrix = new RmRMatrix(2, 2);
        matrix.set(0, 0, scaleX);
        matrix.set(1, 1, scaleY);
        return matrix;
    }
    
    /**
     * Checks if a matrix is approximately identity.
     * Useful for detecting neutral transformations.
     * 
     * @param matrix matrix to check
     * @param epsilon tolerance for comparison
     * @return true if approximately identity
     */
    public static boolean isApproximatelyIdentity(@NonNull RmRMatrix matrix, double epsilon) {
        if (matrix.rows != matrix.cols) {
            return false;
        }
        
        for (int i = 0; i < matrix.rows; i++) {
            for (int j = 0; j < matrix.cols; j++) {
                double expected = (i == j) ? 1.0 : 0.0;
                if (Math.abs(matrix.get(i, j) - expected) > epsilon) {
                    return false;
                }
            }
        }
        return true;
    }
    
    /**
     * Computes trace of a matrix (sum of diagonal elements).
     * Useful for state invariant calculations.
     * 
     * @param matrix square matrix
     * @return trace value
     */
    public static double trace(@NonNull RmRMatrix matrix) {
        if (matrix.rows != matrix.cols) {
            throw new IllegalArgumentException("Matrix must be square");
        }
        
        double sum = 0.0;
        for (int i = 0; i < matrix.rows; i++) {
            sum += matrix.get(i, i);
        }
        return sum;
    }
    
    /**
     * Computes Frobenius norm of a matrix.
     * Useful for measuring matrix magnitude.
     * 
     * @param matrix input matrix
     * @return Frobenius norm
     */
    public static double frobeniusNorm(@NonNull RmRMatrix matrix) {
        double sumSquares = 0.0;
        for (int i = 0; i < matrix.rows; i++) {
            for (int j = 0; j < matrix.cols; j++) {
                double value = matrix.get(i, j);
                sumSquares += value * value;
            }
        }
        return Math.sqrt(sumSquares);
    }
}
