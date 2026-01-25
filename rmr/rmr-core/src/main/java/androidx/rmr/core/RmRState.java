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

public final class RmRState {
    private static final int DEFAULT_DIMENSION = 4;

    private final RmRMatrix matrix;

    public RmRState(@NonNull RmRMatrix matrix) {
        this.matrix = matrix;
    }

    @NonNull
    public RmRMatrix getMatrix() {
        return matrix;
    }

    @NonNull
    public static RmRState forLifecycle() {
        return new RmRState(RmRMatrix.identity(DEFAULT_DIMENSION));
    }

    @NonNull
    public static RmRState forNavigation() {
        return new RmRState(RmRMatrix.identity(DEFAULT_DIMENSION));
    }

    @NonNull
    public static RmRState forPreference() {
        return new RmRState(RmRMatrix.identity(DEFAULT_DIMENSION));
    }

    @NonNull
    public static RmRState forRoom() {
        return new RmRState(RmRMatrix.identity(DEFAULT_DIMENSION));
    }

    @NonNull
    public double[] computeDeterministicPoint(@NonNull double[] inputVector) {
        int rows = matrix.getRows();
        int cols = matrix.getCols();
        if (inputVector.length != cols) {
            throw new IllegalArgumentException("Input vector length must match matrix columns.");
        }
        double[] output = new double[rows];
        double[] data = matrix.getDataUnsafe();
        double[] localInput = inputVector;
        if (cols == 4) {
            double in0 = localInput[0];
            double in1 = localInput[1];
            double in2 = localInput[2];
            double in3 = localInput[3];
            for (int row = 0; row < rows; row++) {
                int rowOffset = row * 4;
                output[row] = data[rowOffset] * in0
                        + data[rowOffset + 1] * in1
                        + data[rowOffset + 2] * in2
                        + data[rowOffset + 3] * in3;
            }
            return output;
        }
        for (int row = 0; row < rows; row++) {
            double sum = 0.0;
            int rowOffset = row * cols;
            for (int col = 0; col < cols; col++) {
                sum += data[rowOffset + col] * localInput[col];
            }
            output[row] = sum;
        }
        return output;
    }

    @NonNull
    public RmRState transform() {
        return new RmRState(matrix.linearFlip());
    }

    @NonNull
    public RmRState accumulate(@NonNull RmRState other) {
        return new RmRState(matrix.add(other.matrix));
    }
}
