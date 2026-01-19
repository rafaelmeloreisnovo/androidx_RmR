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

    @NonNull
    public static RmRMatrix multiply(@NonNull RmRMatrix left, @NonNull RmRMatrix right) {
        return left.multiply(right);
    }

    @NonNull
    public static RmRMatrix add(@NonNull RmRMatrix left, @NonNull RmRMatrix right) {
        return left.add(right);
    }

    @NonNull
    public static RmRMatrix linearFlip(@NonNull RmRMatrix matrix) {
        return matrix.linearFlip();
    }
}
