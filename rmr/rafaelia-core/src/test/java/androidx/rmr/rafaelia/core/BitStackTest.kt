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
package androidx.rmr.rafaelia.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BitStackTest {
    @Test
    fun recordsTenThousandEventsAndValidatesIntegrity() {
        val parityHook = TrackingParityHook()
        BitStack(initialCapacityBytes = 1024, parityHook = parityHook).use { stack ->
            val payload = ByteArray(32) { it.toByte() }
            repeat(10_000) { index ->
                payload[0] = (index and 0xFF).toByte()
                stack.record(payload)
            }
            val snapshot = stack.snapshot()
            val report = stack.verifySnapshot(snapshot)
            assertEquals(10_000, snapshot.recordCount)
            assertTrue(report.checksumValid)
            assertTrue(report.parity.valid)
        }
    }

    private class TrackingParityHook : ParityHook {
        private var recordCount = 0

        override fun onRecord(event: ByteArray, checksum: Int) {
            recordCount += 1
        }

        override fun validate(snapshot: Snapshot): ParityCheck {
            val valid = recordCount == snapshot.recordCount
            return ParityCheck(valid = valid, message = "parity hook stub")
        }
    }
}
