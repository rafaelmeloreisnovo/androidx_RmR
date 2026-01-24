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

fun computeBitrafSeed(@NonNull inputVector: DoubleArray): Long {
    return RmRUtils.computeBitrafSeed(inputVector)
}

fun distanceEuclidean(@NonNull a: DoubleArray, @NonNull b: DoubleArray): Double {
    return RmRUtils.distanceEuclidean(a, b)
}

fun distanceManhattan(@NonNull a: DoubleArray, @NonNull b: DoubleArray): Double {
    return RmRUtils.distanceManhattan(a, b)
}

fun cosineSimilarity(@NonNull a: DoubleArray, @NonNull b: DoubleArray): Double {
    return RmRUtils.cosineSimilarity(a, b)
}

fun hashToIndex(@NonNull key: String, capacity: Int): Int {
    return RmRUtils.hashToIndex(key, capacity)
}
