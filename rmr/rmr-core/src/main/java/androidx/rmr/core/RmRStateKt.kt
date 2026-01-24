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

package androidx.rmr.core

import androidx.annotation.NonNull

@NonNull
fun forLifecycleState(): RmRState {
    return RmRState.forLifecycle()
}

@NonNull
fun forNavigationState(): RmRState {
    return RmRState.forNavigation()
}

@NonNull
fun forPreferenceState(): RmRState {
    return RmRState.forPreference()
}

@NonNull
fun forRoomState(): RmRState {
    return RmRState.forRoom()
}

@NonNull
fun computeDeterministicPoint(@NonNull state: RmRState, @NonNull inputVector: DoubleArray): DoubleArray {
    return state.computeDeterministicPoint(inputVector)
}

@NonNull
fun transformState(@NonNull state: RmRState): RmRState {
    return state.transform()
}

@NonNull
fun accumulateState(@NonNull state: RmRState, @NonNull other: RmRState): RmRState {
    return state.accumulate(other)
}
