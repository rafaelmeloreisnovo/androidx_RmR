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

public final class RmRMatrixOps {
    private RmRMatrixOps() {
    }

    private static native void multiplyNative(
            double[] aData,
            int aRows,
            int aCols,
            double[] bData,
            int bRows,
            int bCols,
            double[] resultData);

    @NonNull
    public static RmRMatrix multiply(@NonNull RmRMatrix left, @NonNull RmRMatrix right) {
        if (left.getCols() != right.getRows()) {
            throw new IllegalArgumentException("Incompatible matrix dimensions.");
        }
        if (RmRHardware.ensureNativeLoaded()) {
            int rows = left.getRows();
            int cols = right.getCols();
            double[] result = new double[rows * cols];
            multiplyNative(
                    left.getDataUnsafe(),
                    rows,
                    left.getCols(),
                    right.getDataUnsafe(),
                    right.getRows(),
                    cols,
                    result);
            return RmRMatrix.wrap(rows, cols, result);
        }
        return multiplyScalar(left, right);
    }

    @NonNull
    public static RmRMatrix add(@NonNull RmRMatrix left, @NonNull RmRMatrix right) {
        return left.add(right);
    }

    @NonNull
    public static RmRMatrix linearFlip(@NonNull RmRMatrix matrix) {
        return matrix.linearFlip();
    }

    @NonNull
    private static RmRMatrix multiplyScalar(@NonNull RmRMatrix left, @NonNull RmRMatrix right) {
        int rows = left.getRows();
        int cols = left.getCols();
        int resultCols = right.getCols();
        double[] leftData = left.getDataUnsafe();
        double[] rightData = right.getDataUnsafe();
        double[] result = new double[rows * resultCols];
        if (resultCols == 1) {
            multiplyVector(leftData, rightData, result, rows, cols);
        } else if (shouldTransposeForMultiply(rows, cols, resultCols)) {
            multiplyWithTransposedRight(leftData, rightData, result, rows, cols, resultCols);
        } else {
            multiplyStandard(leftData, rightData, result, rows, cols, resultCols);
        }
        return RmRMatrix.wrap(rows, resultCols, result);
    }

    private static void multiplyStandard(
            double[] leftData,
            double[] rightData,
            double[] result,
            int rows,
            int cols,
            int resultCols) {
        for (int row = 0; row < rows; row++) {
            int rowOffset = row * cols;
            int resultOffset = row * resultCols;
            for (int k = 0; k < cols; k++) {
                double leftValue = leftData[rowOffset + k];
                int rightOffset = k * resultCols;
                for (int col = 0; col < resultCols; col++) {
                    result[resultOffset + col] += leftValue * rightData[rightOffset + col];
                }
            }
        }
    }

    private static void multiplyWithTransposedRight(
            double[] leftData,
            double[] rightData,
            double[] result,
            int rows,
            int cols,
            int resultCols) {
        double[] rightTransposed = new double[resultCols * cols];
        for (int row = 0; row < cols; row++) {
            int rightOffset = row * resultCols;
            for (int col = 0; col < resultCols; col++) {
                rightTransposed[col * cols + row] = rightData[rightOffset + col];
            }
        }
        for (int row = 0; row < rows; row++) {
            int leftOffset = row * cols;
            int resultOffset = row * resultCols;
            for (int col = 0; col < resultCols; col++) {
                int rightOffset = col * cols;
                double sum = 0.0;
                for (int k = 0; k < cols; k++) {
                    sum += leftData[leftOffset + k] * rightTransposed[rightOffset + k];
                }
                result[resultOffset + col] = sum;
            }
        }
    }

    private static void multiplyVector(double[] leftData, double[] rightData, double[] result, int rows, int cols) {
        for (int row = 0; row < rows; row++) {
            int rowOffset = row * cols;
            double sum = 0.0;
            for (int k = 0; k < cols; k++) {
                sum += leftData[rowOffset + k] * rightData[k];
            }
            result[row] = sum;
        }
    }

    private static boolean shouldTransposeForMultiply(int rows, int cols, int resultCols) {
        long workload = (long) rows * (long) cols * (long) resultCols;
        return workload >= 4096L;
    }
}
