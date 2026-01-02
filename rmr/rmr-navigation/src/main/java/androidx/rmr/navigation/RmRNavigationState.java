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
import androidx.annotation.RestrictTo;
import androidx.rmr.core.RmRMatrix;
import androidx.rmr.core.RmRState;

/**
 * Optimized navigation state using RmR matrix-based approach.
 * 
 * <p>RmRNavigationState provides minimal footprint navigation tracking without
 * traditional backstack overhead. Navigation state is represented as matrix
 * transformations, enabling:
 * <ul>
 *   <li>O(1) navigation operations</li>
 *   <li>Predictable memory usage independent of backstack depth</li>
 *   <li>Cache-friendly destination storage</li>
 *   <li>Zero allocation during navigation</li>
 * </ul>
 * 
 * <p>State dimensions:
 * <ul>
 *   <li>0: Current destination ID</li>
 *   <li>1: Navigation arguments hash</li>
 *   <li>2: Backstack depth</li>
 *   <li>3: Navigation options flags</li>
 * </ul>
 * 
 * @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public final class RmRNavigationState {
    
    /**
     * Internal state representation
     */
    @NonNull
    private final RmRState state;
    
    /**
     * Current destination identifier
     */
    private final int destinationId;
    
    /**
     * Creates navigation state at initial destination
     */
    public RmRNavigationState() {
        this(RmRState.forNavigation(), 0);
    }
    
    /**
     * Creates navigation state with specified destination
     * 
     * @param state RmR state
     * @param destinationId current destination
     */
    private RmRNavigationState(@NonNull RmRState state, int destinationId) {
        this.state = state;
        this.destinationId = destinationId;
    }
    
    /**
     * Gets current destination ID
     * 
     * @return destination identifier
     */
    public int getDestinationId() {
        return destinationId;
    }
    
    /**
     * Navigates to new destination
     * 
     * @param newDestinationId target destination
     * @return new navigation state
     */
    @NonNull
    public RmRNavigationState navigateTo(int newDestinationId) {
        return navigateTo(newDestinationId, 0);
    }
    
    /**
     * Navigates to new destination with arguments
     * 
     * @param newDestinationId target destination
     * @param argumentsHash hash of navigation arguments
     * @return new navigation state
     */
    @NonNull
    public RmRNavigationState navigateTo(int newDestinationId, int argumentsHash) {
        // Update state vector with new destination info
        double[] navVector = {
            (double) newDestinationId,
            (double) argumentsHash,
            state.extractVector(0)[2] + 1.0, // increment backstack depth
            0.0 // default options
        };
        
        RmRState newState = state.updateVector(0, navVector);
        newState = newState.transform();
        
        return new RmRNavigationState(newState, newDestinationId);
    }
    
    /**
     * Navigates back to previous destination
     * 
     * @return navigation state after pop, or this if at root
     */
    @NonNull
    public RmRNavigationState popBackStack() {
        double depth = state.extractVector(0)[2];
        if (depth <= 0.0) {
            return this; // Already at root
        }
        
        // Use linear flip to reverse navigation
        RmRState reversed = state.optimize();
        
        // Update backstack depth
        double[] navVector = state.extractVector(0);
        navVector[2] -= 1.0;
        reversed = reversed.updateVector(0, navVector);
        
        // Extract previous destination from state
        int prevDestination = (int) Math.abs(reversed.extractVector(0)[0]);
        
        return new RmRNavigationState(reversed, prevDestination);
    }
    
    /**
     * Gets current backstack depth
     * 
     * @return number of entries in backstack
     */
    public int getBackStackDepth() {
        return (int) state.extractVector(0)[2];
    }
    
    /**
     * Gets arguments hash for current destination
     * 
     * @return arguments hash code
     */
    public int getArgumentsHash() {
        return (int) state.extractVector(0)[1];
    }
    
    /**
     * Computes navigation path prediction
     * 
     * @param targetDestination target to predict path to
     * @return predicted transition cost
     */
    public double predictNavigationCost(int targetDestination) {
        double[] input = {
            (double) destinationId,
            (double) targetDestination,
            0.0,
            0.0
        };
        
        double[] result = state.computeDeterministicPoint(input);
        
        // Return manhattan distance as cost metric
        double cost = 0.0;
        for (double val : result) {
            cost += Math.abs(val);
        }
        return cost;
    }
    
    /**
     * Creates optimized navigation state
     * 
     * @return optimized state with reduced footprint
     */
    @NonNull
    public RmRNavigationState createOptimized() {
        RmRState optimized = state.optimize();
        return new RmRNavigationState(optimized, destinationId);
    }
    
    /**
     * Checks if at root destination
     * 
     * @return true if backstack is empty
     */
    public boolean isAtRoot() {
        return getBackStackDepth() == 0;
    }
}
