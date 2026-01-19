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

package androidx.rmr.navigation;

import androidx.annotation.NonNull;
import androidx.rmr.core.RmRMatrix;

public final class RmRNavigationState {
    private final int backStackDepth;
    private final int currentDestinationId;
    private final RmRMatrix stateMatrix;

    public RmRNavigationState() {
        this(0, 0);
    }

    private RmRNavigationState(int backStackDepth, int currentDestinationId) {
        if (backStackDepth < 0) {
            throw new IllegalArgumentException("Back stack depth cannot be negative.");
        }
        this.backStackDepth = backStackDepth;
        this.currentDestinationId = currentDestinationId;
        this.stateMatrix = buildStateMatrix(backStackDepth, currentDestinationId);
    }

    public int getBackStackDepth() {
        return backStackDepth;
    }

    public int getCurrentDestinationId() {
        return currentDestinationId;
    }

    @NonNull
    public RmRNavigationState navigateTo(int destinationId) {
        return new RmRNavigationState(backStackDepth + 1, destinationId);
    }

    @NonNull
    public RmRNavigationState popBackStack() {
        if (backStackDepth == 0) {
            return this;
        }
        return new RmRNavigationState(backStackDepth - 1, currentDestinationId);
    }

    @NonNull
    public RmRMatrix getStateMatrix() {
        return stateMatrix;
    }

    @NonNull
    public double[] getStateVector() {
        return new double[] {backStackDepth, currentDestinationId, stateMatrix.get(0, 2), stateMatrix.get(0, 3)};
    }

    private static RmRMatrix buildStateMatrix(int depth, int destinationId) {
        RmRMatrix matrix = RmRMatrix.identity(4);
        matrix.set(0, 0, depth);
        matrix.set(0, 1, destinationId);
        matrix.set(0, 2, depth == 0 ? 0.0 : 1.0);
        matrix.set(0, 3, destinationId == 0 ? 0.0 : 1.0);
        return matrix;
    }
}
