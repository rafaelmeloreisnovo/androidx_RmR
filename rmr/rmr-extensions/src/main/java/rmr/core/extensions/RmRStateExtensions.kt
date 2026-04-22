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

package rmr.core.extensions

import androidx.rmr.core.RmRState

fun forLifecycleState(): RmRState =
    RmRState.forLifecycle()

fun forNavigationState(): RmRState =
    RmRState.forNavigation()

fun forPreferenceState(): RmRState =
    RmRState.forPreference()

fun forRoomState(): RmRState =
    RmRState.forRoom()

fun computeDeterministicPoint(state: RmRState, inputVector: DoubleArray): DoubleArray =
    state.computeDeterministicPoint(inputVector)

fun transformState(state: RmRState): RmRState =
    state.transform()

fun accumulateState(state: RmRState, other: RmRState): RmRState =
    state.accumulate(other)
