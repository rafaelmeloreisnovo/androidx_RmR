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

package androidx.rmr.lifecycle;

import androidx.annotation.NonNull;
import androidx.rmr.core.RmRMatrix;

public final class RmRLifecycleState {
    public static final int STATE_INITIALIZED = 0;
    public static final int STATE_CREATED = 1;
    public static final int STATE_STARTED = 2;
    public static final int STATE_RESUMED = 3;

    private final int state;
    private final RmRMatrix stateMatrix;

    public RmRLifecycleState() {
        this(STATE_INITIALIZED);
    }

    public RmRLifecycleState(int state) {
        validateState(state);
        this.state = state;
        this.stateMatrix = buildStateMatrix(state);
    }

    public int getState() {
        return state;
    }

    public boolean isResumed() {
        return state == STATE_RESUMED;
    }

    @NonNull
    public RmRLifecycleState transitionNext() {
        if (state >= STATE_RESUMED) {
            return this;
        }
        return new RmRLifecycleState(state + 1);
    }

    @NonNull
    public RmRLifecycleState jumpToState(int targetState) {
        return new RmRLifecycleState(targetState);
    }

    @NonNull
    public double[] getStateVector() {
        return new double[] {state, stateMatrix.get(0, 1), stateMatrix.get(0, 2), stateMatrix.get(0, 3)};
    }

    @NonNull
    public RmRMatrix getStateMatrix() {
        return stateMatrix;
    }

    private static void validateState(int state) {
        if (state < STATE_INITIALIZED || state > STATE_RESUMED) {
            throw new IllegalArgumentException("Invalid lifecycle state.");
        }
    }

    private static RmRMatrix buildStateMatrix(int state) {
        RmRMatrix matrix = RmRMatrix.identity(4);
        matrix.set(0, 0, state);
        matrix.set(0, 1, state == STATE_CREATED ? 1.0 : 0.0);
        matrix.set(0, 2, state >= STATE_STARTED ? 1.0 : 0.0);
        matrix.set(0, 3, state == STATE_RESUMED ? 1.0 : 0.0);
        return matrix;
    }
}
