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
import androidx.annotation.RestrictTo;
import androidx.rmr.core.RmRMatrix;
import androidx.rmr.core.RmRState;

/**
 * Optimized lifecycle state management using RmR matrix-based approach.
 * 
 * <p>RmRLifecycleState provides minimal footprint lifecycle tracking without
 * traditional callback overhead. All lifecycle states are represented as
 * deterministic points in a matrix space, enabling:
 * <ul>
 *   <li>Zero-overhead state transitions</li>
 *   <li>Predictable memory usage</li>
 *   <li>Cache-friendly access patterns</li>
 *   <li>Bare metal performance</li>
 * </ul>
 * 
 * <p>State indices:
 * <ul>
 *   <li>0: INITIALIZED</li>
 *   <li>1: CREATED</li>
 *   <li>2: STARTED</li>
 *   <li>3: RESUMED</li>
 * </ul>
 * 
 * @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RmRLifecycleState {
    
    /**
     * Lifecycle state constants as matrix indices
     */
    public static final int STATE_INITIALIZED = 0;
    public static final int STATE_CREATED = 1;
    public static final int STATE_STARTED = 2;
    public static final int STATE_RESUMED = 3;
    
    /**
     * Internal state representation
     */
    @NonNull
    private final RmRState state;
    
    /**
     * Current lifecycle position
     */
    private int currentState;
    
    /**
     * Creates lifecycle state starting at INITIALIZED
     */
    public RmRLifecycleState() {
        this(RmRState.forLifecycle(), STATE_INITIALIZED);
    }
    
    /**
     * Creates lifecycle state with specified initial state
     * 
     * @param state RmR state
     * @param currentState initial lifecycle state
     */
    private RmRLifecycleState(@NonNull RmRState state, int currentState) {
        this.state = state;
        this.currentState = currentState;
    }
    
    /**
     * Gets current lifecycle state index
     * 
     * @return state index (0-3)
     */
    public int getCurrentState() {
        return currentState;
    }
    
    /**
     * Transitions to next lifecycle state
     * 
     * @return new lifecycle state after transition
     */
    @NonNull
    public RmRLifecycleState transitionNext() {
        if (currentState >= STATE_RESUMED) {
            return this; // Already at final state
        }
        
        RmRState newState = state.transform();
        return new RmRLifecycleState(newState, currentState + 1);
    }
    
    /**
     * Transitions to previous lifecycle state
     * 
     * @return new lifecycle state after transition
     */
    @NonNull
    public RmRLifecycleState transitionPrevious() {
        if (currentState <= STATE_INITIALIZED) {
            return this; // Already at initial state
        }
        
        // Reverse transformation using linear flip
        RmRState optimized = state.optimize();
        return new RmRLifecycleState(optimized, currentState - 1);
    }
    
    /**
     * Jumps directly to specified state
     * 
     * @param targetState target state index
     * @return new lifecycle state at target
     */
    @NonNull
    public RmRLifecycleState jumpToState(int targetState) {
        if (targetState < STATE_INITIALIZED || targetState > STATE_RESUMED) {
            throw new IllegalArgumentException("Invalid state: " + targetState);
        }
        
        if (targetState == currentState) {
            return this;
        }
        
        // Calculate direct transformation
        int delta = targetState - currentState;
        RmRState newState = state;
        
        if (delta > 0) {
            // Forward transitions
            for (int i = 0; i < delta; i++) {
                newState = newState.transform();
            }
        } else {
            // Backward transitions
            for (int i = 0; i < -delta; i++) {
                newState = newState.optimize();
            }
        }
        
        return new RmRLifecycleState(newState, targetState);
    }
    
    /**
     * Gets state vector for current lifecycle position
     * 
     * @return state vector representing current state
     */
    @NonNull
    public double[] getStateVector() {
        return state.extractVector(currentState);
    }
    
    /**
     * Checks if lifecycle is at least in CREATED state
     * 
     * @return true if created or beyond
     */
    public boolean isAtLeastCreated() {
        return currentState >= STATE_CREATED;
    }
    
    /**
     * Checks if lifecycle is at least in STARTED state
     * 
     * @return true if started or beyond
     */
    public boolean isAtLeastStarted() {
        return currentState >= STATE_STARTED;
    }
    
    /**
     * Checks if lifecycle is in RESUMED state
     * 
     * @return true if resumed
     */
    public boolean isResumed() {
        return currentState == STATE_RESUMED;
    }
    
    /**
     * Computes deterministic point for lifecycle state
     * Useful for state prediction and optimization
     * 
     * @param input input parameters
     * @return deterministic lifecycle coordinates
     */
    @NonNull
    public double[] computeDeterministicPoint(@NonNull double[] input) {
        return state.computeDeterministicPoint(input);
    }
    
    /**
     * Creates optimized lifecycle state for minimal memory footprint
     * 
     * @return optimized state
     */
    @NonNull
    public RmRLifecycleState createOptimized() {
        RmRState optimized = state.optimize();
        return new RmRLifecycleState(optimized, currentState);
    }
}
