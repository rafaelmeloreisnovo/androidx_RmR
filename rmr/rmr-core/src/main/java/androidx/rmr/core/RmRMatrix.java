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

import java.util.Objects;

public final class RmRMatrix {
    private final int rows;
    private final int cols;
    private final double[] data;

    public RmRMatrix(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Matrix dimensions must be positive.");
        }
        if ((long) rows * (long) cols > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Matrix size overflows integer bounds.");
        }
        this.rows = rows;
        this.cols = cols;
        this.data = new double[rows * cols];
    }

    private RmRMatrix(int rows, int cols, double[] data) {
        this.rows = rows;
        this.cols = cols;
        this.data = data;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    /**
     * Returns the value at the given index without bounds checks.
     * Callers must ensure row/col are within bounds for performance-critical paths.
     */
    public double get(int row, int col) {
        return data[row * cols + col];
    }

    /**
     * Sets the value at the given index without bounds checks.
     * Callers must ensure row/col are within bounds for performance-critical paths.
     */
    public void set(int row, int col, double value) {
        data[row * cols + col] = value;
    }

    public double getChecked(int row, int col) {
        validateBounds(row, col);
        return data[row * cols + col];
    }

    public void setChecked(int row, int col, double value) {
        validateBounds(row, col);
        data[row * cols + col] = value;
    }

    @NonNull
    public RmRMatrix add(@NonNull RmRMatrix other) {
        ensureSameSize(other);
        double[] localData = data;
        int length = localData.length;
        if (length == 0) {
            return new RmRMatrix(rows, cols, new double[0]);
        }
        double[] result = new double[length];
        if (other == this) {
            for (int i = 0; i < length; i++) {
                result[i] = localData[i] + localData[i];
            }
        } else {
            double[] otherData = other.data;
            for (int i = 0; i < length; i++) {
                result[i] = localData[i] + otherData[i];
            }
        }
        return new RmRMatrix(rows, cols, result);
    }

    @NonNull
    public RmRMatrix multiply(@NonNull RmRMatrix other) {
        return RmRMatrixOps.multiply(this, other);
    }

    @NonNull
    public RmRMatrix linearFlip() {
        double[] localData = data;
        int length = localData.length;
        if (length == 0) {
            return new RmRMatrix(rows, cols, new double[0]);
        }
        double[] result = new double[length];
        for (int i = 0; i < length; i++) {
            double value = localData[i];
            result[i] = value == 0.0 ? 0.0 : -1.0 / value;
        }
        return new RmRMatrix(rows, cols, result);
    }

    @NonNull
    public double[] copyData() {
        return data.clone();
    }

    @NonNull
    public static RmRMatrix identity(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Identity size must be positive.");
        }
        RmRMatrix identity = new RmRMatrix(size, size);
        double[] identityData = identity.data;
        for (int i = 0; i < size; i++) {
            identityData[i * size + i] = 1.0;
        }
        return identity;
    }

    double[] getDataUnsafe() {
        return data;
    }

    static RmRMatrix wrap(int rows, int cols, double[] data) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Matrix dimensions must be positive.");
        }
        long expectedSize = (long) rows * (long) cols;
        if (expectedSize > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Matrix size overflows integer bounds.");
        }
        Objects.requireNonNull(data, "data");
        if (data.length != (int) expectedSize) {
            throw new IllegalArgumentException("Matrix data length mismatch.");
        }
        return new RmRMatrix(rows, cols, data);
    }

    @NonNull
    public static RmRMatrix allocateFromPool(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Matrix dimensions must be positive.");
        }
        long size = (long) rows * (long) cols;
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Matrix size overflows integer bounds.");
        }
        double[] buffer = DoubleArrayPool.acquire((int) size);
        return new RmRMatrix(rows, cols, buffer);
    }

    public void releaseToPool() {
        DoubleArrayPool.release(data);
    }

    public void addInto(@NonNull RmRMatrix other, @NonNull RmRMatrix out) {
        ensureSameSize(other);
        ensureSameSize(out);
        double[] localData = data;
        double[] outData = out.data;
        int length = localData.length;
        if (other == this) {
            for (int i = 0; i < length; i++) {
                outData[i] = localData[i] + localData[i];
            }
        } else {
            double[] otherData = other.data;
            for (int i = 0; i < length; i++) {
                outData[i] = localData[i] + otherData[i];
            }
        }
    }

    public void linearFlipInto(@NonNull RmRMatrix out) {
        ensureSameSize(out);
        double[] localData = data;
        double[] outData = out.data;
        int length = localData.length;
        for (int i = 0; i < length; i++) {
            double value = localData[i];
            outData[i] = value == 0.0 ? 0.0 : -1.0 / value;
        }
    }

    private void validateBounds(int row, int col) {
        if (row < 0 || row >= rows || col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException("Invalid matrix index.");
        }
    }

    private void ensureSameSize(RmRMatrix other) {
        if (rows != other.rows || cols != other.cols) {
            throw new IllegalArgumentException("Matrix sizes must match.");
        }
    }
}
