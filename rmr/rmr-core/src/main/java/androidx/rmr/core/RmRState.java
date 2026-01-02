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
 * Optimized state container using matrix representation.
 * 
 * <p>RmRState provides minimal footprint state management using matrices
 * instead of traditional object-oriented state. All state variables are
 * represented as deterministic points in a matrix space, enabling:
 * <ul>
 *   <li>Low-level bare metal performance</li>
 *   <li>Minimal memory footprint</li>
 *   <li>Cache-friendly data layout</li>
 *   <li>Predictable computation cost</li>
 * </ul>
 * 
 * @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RmRState {
    
    /**
     * State matrix - represents all state as deterministic points
     */
    @NonNull
    private final RmRMatrix stateMatrix;
    
    /**
     * Transformation matrix - for state transitions
     */
    @NonNull
    private final RmRMatrix transformMatrix;
    
    /**
     * Creates state with default 4x4 matrices
     */
    public RmRState() {
        this(new RmRMatrix(), RmRMatrix.identity(4));
    }
    
    /**
     * Creates state with specified matrices
     * 
     * @param stateMatrix initial state matrix
     * @param transformMatrix transformation matrix
     */
    public RmRState(@NonNull RmRMatrix stateMatrix, @NonNull RmRMatrix transformMatrix) {
        this.stateMatrix = stateMatrix;
        this.transformMatrix = transformMatrix;
    }
    
    /**
     * Gets current state matrix
     * 
     * @return state matrix (immutable view)
     */
    @NonNull
    public RmRMatrix getStateMatrix() {
        return stateMatrix.clone();
    }
    
    /**
     * Gets transformation matrix
     * 
     * @return transform matrix (immutable view)
     */
    @NonNull
    public RmRMatrix getTransformMatrix() {
        return transformMatrix.clone();
    }
    
    /**
     * Applies transformation to current state
     * 
     * @return new state after transformation
     */
    @NonNull
    public RmRState transform() {
        RmRMatrix newState = transformMatrix.multiply(stateMatrix);
        return new RmRState(newState, transformMatrix);
    }
    
    /**
     * Applies custom transformation matrix
     * 
     * @param customTransform transformation to apply
     * @return new state after transformation
     */
    @NonNull
    public RmRState transform(@NonNull RmRMatrix customTransform) {
        RmRMatrix newState = customTransform.multiply(stateMatrix);
        return new RmRState(newState, customTransform);
    }
    
    /**
     * Accumulates state with another state (additive)
     * 
     * @param other state to accumulate
     * @return new accumulated state
     */
    @NonNull
    public RmRState accumulate(@NonNull RmRState other) {
        RmRMatrix newState = this.stateMatrix.add(other.stateMatrix);
        return new RmRState(newState, this.transformMatrix);
    }
    
    /**
     * Extracts state vector at specified position
     * 
     * @param position position to extract (0-indexed)
     * @return state vector
     */
    @NonNull
    public double[] extractVector(int position) {
        if (position < 0 || position >= stateMatrix.rows) {
            throw new IllegalArgumentException("Invalid position");
        }
        
        double[] vector = new double[stateMatrix.cols];
        for (int j = 0; j < stateMatrix.cols; j++) {
            vector[j] = stateMatrix.get(position, j);
        }
        return vector;
    }
    
    /**
     * Updates state vector at specified position
     * 
     * @param position position to update
     * @param vector new vector values
     * @return new state with updated vector
     */
    @NonNull
    public RmRState updateVector(int position, @NonNull double[] vector) {
        if (position < 0 || position >= stateMatrix.rows) {
            throw new IllegalArgumentException("Invalid position");
        }
        if (vector.length != stateMatrix.cols) {
            throw new IllegalArgumentException("Vector length mismatch");
        }
        
        RmRMatrix newState = stateMatrix.clone();
        for (int j = 0; j < stateMatrix.cols; j++) {
            newState.set(position, j, vector[j]);
        }
        return new RmRState(newState, this.transformMatrix);
    }
    
    /**
     * Computes deterministic point from input vector
     * Uses state matrix as transformation basis
     * 
     * @param input input vector
     * @return deterministic point coordinates
     */
    @NonNull
    public double[] computeDeterministicPoint(@NonNull double[] input) {
        return stateMatrix.calculateDeterministicPoint(input);
    }
    
    /**
     * Applies linear flip optimization
     * 
     * @return optimized state
     */
    @NonNull
    public RmRState optimize() {
        RmRMatrix flipped = stateMatrix.linearFlip();
        return new RmRState(flipped, transformMatrix);
    }
    
    /**
     * Creates minimal footprint state for lifecycle events
     * Pre-optimized matrices for common lifecycle states
     * 
     * @return lifecycle-optimized state
     */
    @NonNull
    public static RmRState forLifecycle() {
        // 4 lifecycle states: CREATE, START, RESUME, PAUSE
        RmRMatrix state = new RmRMatrix(4, 4);
        RmRMatrix transform = RmRMatrix.identity(4);
        
        // Initialize with optimal lifecycle transition matrix
        for (int i = 0; i < 4; i++) {
            state.set(i, i, 1.0);
            if (i < 3) {
                transform.set(i, i + 1, 1.0);
            }
        }
        
        return new RmRState(state, transform);
    }
    
    /**
     * Creates minimal footprint state for navigation
     * Pre-optimized matrices for navigation state
     * 
     * @return navigation-optimized state
     */
    @NonNull
    public static RmRState forNavigation() {
        // 4 navigation dimensions: destination, arguments, backstack, options
        RmRMatrix state = new RmRMatrix(4, 4);
        RmRMatrix transform = RmRMatrix.identity(4);
        
        // Initialize with optimal navigation patterns
        state.set(0, 0, 1.0); // current destination
        transform.set(0, 1, 1.0); // transition to next
        
        return new RmRState(state, transform);
    }
    
    /**
     * Creates minimal footprint state for preferences
     * Pre-optimized matrices for preference storage
     * 
     * @return preference-optimized state
     */
    @NonNull
    public static RmRState forPreferences() {
        // 4 preference dimensions: key, value, type, dirty
        RmRMatrix state = new RmRMatrix(4, 4);
        RmRMatrix transform = RmRMatrix.identity(4);
        
        return new RmRState(state, transform);
    }
    
    /**
     * Creates minimal footprint state for database operations
     * Pre-optimized matrices for Room/DB access
     * 
     * @return database-optimized state
     */
    @NonNull
    public static RmRState forDatabase() {
        // 4 database dimensions: query, result, transaction, cache
        RmRMatrix state = new RmRMatrix(4, 4);
        RmRMatrix transform = RmRMatrix.identity(4);
        
        return new RmRState(state, transform);
    }
}
