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

    public double get(int row, int col) {
        return data[row * cols + col];
    }

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
        double[] result = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i] + other.data[i];
        }
        return new RmRMatrix(rows, cols, result);
    }

    @NonNull
    public RmRMatrix multiply(@NonNull RmRMatrix other) {
        if (cols != other.rows) {
            throw new IllegalArgumentException("Incompatible matrix dimensions.");
        }
        double[] result = new double[rows * other.cols];
        for (int row = 0; row < rows; row++) {
            int rowOffset = row * cols;
            int resultOffset = row * other.cols;
            for (int col = 0; col < other.cols; col++) {
                double sum = 0.0;
                for (int k = 0; k < cols; k++) {
                    sum += data[rowOffset + k] * other.data[k * other.cols + col];
                }
                result[resultOffset + col] = sum;
            }
        }
        return new RmRMatrix(rows, other.cols, result);
    }

    @NonNull
    public RmRMatrix linearFlip() {
        double[] result = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            double value = data[i];
            result[i] = value == 0.0 ? 0.0 : -1.0 / value;
        }
        return new RmRMatrix(rows, cols, result);
    }

    @NonNull
    public double[] copyData() {
        double[] copy = new double[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        return copy;
    }

    @NonNull
    public static RmRMatrix identity(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Identity size must be positive.");
        }
        RmRMatrix identity = new RmRMatrix(size, size);
        for (int i = 0; i < size; i++) {
            identity.data[i * size + i] = 1.0;
        }
        return identity;
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
