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
 * 
 * LEGAL COMPLIANCE NOTICE:
 * This implementation follows Apache License 2.0 requirements.
 * For bare-metal optimized version with additional restrictions,
 * see the rafaelia module (rafaelia/LEGAL_NOTICE.md).
 */

package androidx.rmr.core;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/**
 * Matrix-based state representation for minimal footprint and high performance.
 * 
 * <p>RmR (Rafael Melo Reis) Core provides optimized, low-level bare metal computation
 * using matrix operations instead of traditional function calls. This approach minimizes
 * memory footprint and maximizes execution speed by treating all state as deterministic
 * points in a matrix space.
 * 
 * <p>Variables are matrices that assume deterministic points to facilitate calculations,
 * following a linear flip solution with solubility patterns.</p>
 * 
 * <p><b>Low-Level Optimization:</b> This class uses direct array access and avoids
 * object allocations in hot paths. For even lower-level native SIMD operations,
 * use the rafaelia module.</p>
 * 
 * @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RmRMatrix {
    
    /**
     * Matrix dimensions - fixed size for optimal memory layout
     */
    private static final int DEFAULT_ROWS = 4;
    private static final int DEFAULT_COLS = 4;
    
    /**
     * Raw matrix data - direct access for bare metal performance
     * Layout: row-major order for cache locality
     */
    @NonNull
    public final double[] data;
    
    /**
     * Matrix dimensions
     */
    public final int rows;
    public final int cols;
    
    /**
     * Creates a matrix with default dimensions (4x4)
     */
    public RmRMatrix() {
        this(DEFAULT_ROWS, DEFAULT_COLS);
    }
    
    /**
     * Creates a matrix with specified dimensions
     * 
     * @param rows number of rows
     * @param cols number of columns
     */
    public RmRMatrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.data = new double[rows * cols];
    }
    
    /**
     * Creates a matrix from existing data
     * 
     * @param rows number of rows
     * @param cols number of columns
     * @param data raw matrix data in row-major order
     */
    public RmRMatrix(int rows, int cols, @NonNull double[] data) {
        if (data.length != rows * cols) {
            throw new IllegalArgumentException("Data length must equal rows * cols");
        }
        this.rows = rows;
        this.cols = cols;
        this.data = data.clone();
    }
    
    /**
     * Direct element access - no bounds checking for performance
     * 
     * @param row row index
     * @param col column index
     * @return element value
     */
    public double get(int row, int col) {
        return data[row * cols + col];
    }
    
    /**
     * Direct element mutation - no bounds checking for performance
     * 
     * @param row row index
     * @param col column index
     * @param value new value
     */
    public void set(int row, int col, double value) {
        data[row * cols + col] = value;
    }
    
    /**
     * Matrix multiplication - core operation for state transformations
     * Uses hardware-optimized implementation from RmRMatrixOps.
     * 
     * @param other matrix to multiply with
     * @return result matrix
     */
    @NonNull
    public RmRMatrix multiply(@NonNull RmRMatrix other) {
        if (this.cols != other.rows) {
            throw new IllegalArgumentException("Matrix dimensions incompatible for multiplication");
        }
        
        RmRMatrix result = new RmRMatrix(this.rows, other.cols);
        
        // Use hardware-optimized multiplication
        RmRMatrixOps.multiply(this, other, result);
        
        return result;
    }
    
    /**
     * Element-wise addition - for state accumulation
     * 
     * @param other matrix to add
     * @return result matrix
     */
    @NonNull
    public RmRMatrix add(@NonNull RmRMatrix other) {
        if (this.rows != other.rows || this.cols != other.cols) {
            throw new IllegalArgumentException("Matrix dimensions must match for addition");
        }
        
        RmRMatrix result = new RmRMatrix(this.rows, this.cols);
        for (int i = 0; i < data.length; i++) {
            result.data[i] = this.data[i] + other.data[i];
        }
        
        return result;
    }
    
    /**
     * Transpose - for state transformations
     * Uses hardware-optimized implementation from RmRMatrixOps.
     * 
     * @return transposed matrix
     */
    @NonNull
    public RmRMatrix transpose() {
        RmRMatrix result = new RmRMatrix(this.cols, this.rows);
        
        // Use hardware-optimized transpose
        RmRMatrixOps.transpose(this, result);
        
        return result;
    }
    
    /**
     * Creates identity matrix - for initialization
     * 
     * @param size dimension of square matrix
     * @return identity matrix
     */
    @NonNull
    public static RmRMatrix identity(int size) {
        RmRMatrix result = new RmRMatrix(size, size);
        for (int i = 0; i < size; i++) {
            result.set(i, i, 1.0);
        }
        return result;
    }
    
    /**
     * Deterministic point calculation - core RmR concept
     * Maps state variables to deterministic points in matrix space
     * 
     * @param stateVector input state vector
     * @return deterministic point coordinates
     */
    @NonNull
    public double[] calculateDeterministicPoint(@NonNull double[] stateVector) {
        if (stateVector.length != this.cols) {
            throw new IllegalArgumentException("State vector length must match matrix columns");
        }
        
        double[] result = new double[this.rows];
        for (int i = 0; i < this.rows; i++) {
            double sum = 0.0;
            for (int j = 0; j < this.cols; j++) {
                sum += this.get(i, j) * stateVector[j];
            }
            result[i] = sum;
        }
        
        return result;
    }
    
    /**
     * Linear flip solution - RmR optimization technique
     * Applies flip transformation for solubility
     * 
     * @return flipped matrix
     */
    @NonNull
    public RmRMatrix linearFlip() {
        RmRMatrix result = new RmRMatrix(this.rows, this.cols);
        // Use tolerance threshold to avoid overflow with very small values
        final double epsilon = 1e-15;
        for (int i = 0; i < data.length; i++) {
            // Flip: negate and invert (if non-zero and above threshold)
            if (Math.abs(data[i]) > epsilon) {
                result.data[i] = -1.0 / data[i];
            } else {
                result.data[i] = 0.0;
            }
        }
        return result;
    }
    
    /**
     * Clone this matrix
     * 
     * @return cloned matrix
     */
    @NonNull
    public RmRMatrix clone() {
        return new RmRMatrix(this.rows, this.cols, this.data);
    }
}
