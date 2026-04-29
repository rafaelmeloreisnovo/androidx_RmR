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

import androidx.rmr.core.RmRHardware
import androidx.rmr.core.RmRMatrix
import androidx.rmr.core.RmRMatrixOps

fun multiply(left: RmRMatrix, right: RmRMatrix): RmRMatrix =
    RmRMatrixOps.multiply(left, right)

fun add(left: RmRMatrix, right: RmRMatrix): RmRMatrix =
    RmRMatrixOps.add(left, right)

fun linearFlip(matrix: RmRMatrix): RmRMatrix =
    RmRMatrixOps.linearFlip(matrix)

fun ensureNativeLoaded(): Boolean =
    RmRHardware.ensureNativeLoaded()
