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

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.zip.CRC32C
import kotlin.math.max

class BitStack(
    initialCapacityBytes: Int = DEFAULT_CAPACITY_BYTES,
    private val storage: BitStackStorage = BitStackStorage.InMemory,
    private val parityHook: ParityHook = ParityHook.NO_OP
) : Closeable {
    companion object {
        const val DEFAULT_CAPACITY_BYTES: Int = 64 * 1024
        private const val RECORD_HEADER_BYTES: Int = 8
    }

    private var buffer: ByteBuffer = ByteBuffer.allocateDirect(initialCapacityBytes)
        .order(ByteOrder.LITTLE_ENDIAN)
    private val stackCrc32c = CRC32C()
    private val recordCrc32c = CRC32C()
    private val lengthScratch = ByteArray(4)
    private var sizeBytes: Int = 0
    private var recordCount: Int = 0
    private val fileChannel: FileChannel? = when (storage) {
        is BitStackStorage.FileBacked -> openFile(storage.file)
        BitStackStorage.InMemory -> null
    }
    private val headerBuffer: ByteBuffer? = fileChannel?.let {
        ByteBuffer.allocateDirect(RECORD_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
    }

    val sizeInBytes: Int
        get() = sizeBytes

    val totalRecords: Int
        get() = recordCount

    fun record(event: ByteArray) {
        val checksum = computeChecksum(event)
        ensureCapacity(RECORD_HEADER_BYTES + event.size)
        buffer.putInt(event.size)
        buffer.putInt(checksum)
        buffer.put(event)
        sizeBytes = buffer.position()
        recordCount += 1
        updateStackChecksum(event.size, event)
        parityHook.onRecord(event, checksum)
        writeToFile(event.size, checksum, event)
    }

    fun snapshot(): Snapshot {
        val snapshotBuffer = ByteBuffer.allocateDirect(sizeBytes).order(ByteOrder.LITTLE_ENDIAN)
        val readOnly = buffer.duplicate()
        readOnly.flip()
        snapshotBuffer.put(readOnly)
        snapshotBuffer.flip()
        return Snapshot(
            buffer = snapshotBuffer.asReadOnlyBuffer(),
            recordCount = recordCount,
            sizeBytes = sizeBytes,
            checksum = stackCrc32c.value.toInt()
        )
    }

    fun verifySnapshot(snapshot: Snapshot): IntegrityReport {
        val data = snapshot.buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        data.rewind()
        val recomputedStackCrc = CRC32C()
        var checksumValid = true
        var records = 0
        while (data.remaining() >= RECORD_HEADER_BYTES) {
            val length = data.int
            val checksum = data.int
            if (length < 0 || data.remaining() < length) {
                checksumValid = false
                break
            }
            val payload = ByteArray(length)
            data.get(payload)
            recordCrc32c.reset()
            recordCrc32c.update(payload)
            if (recordCrc32c.value.toInt() != checksum) {
                checksumValid = false
            }
            updateCrcWithInt(recomputedStackCrc, length)
            recomputedStackCrc.update(payload)
            records += 1
        }
        if (data.hasRemaining()) {
            checksumValid = false
        }
        if (recomputedStackCrc.value.toInt() != snapshot.checksum) {
            checksumValid = false
        }
        if (records != snapshot.recordCount) {
            checksumValid = false
        }
        val parityCheck = parityHook.validate(snapshot)
        return IntegrityReport(checksumValid = checksumValid, parity = parityCheck)
    }

    override fun close() {
        fileChannel?.close()
    }

    private fun openFile(file: File): FileChannel {
        file.parentFile?.mkdirs()
        val raf = RandomAccessFile(file, "rw")
        return raf.channel.apply { position(size()) }
    }

    private fun ensureCapacity(neededBytes: Int) {
        if (buffer.remaining() >= neededBytes) {
            return
        }
        val newCapacity = max(buffer.capacity() * 2, sizeBytes + neededBytes)
        val newBuffer = ByteBuffer.allocateDirect(newCapacity).order(ByteOrder.LITTLE_ENDIAN)
        buffer.flip()
        newBuffer.put(buffer)
        buffer = newBuffer
    }

    private fun computeChecksum(event: ByteArray): Int {
        recordCrc32c.reset()
        recordCrc32c.update(event)
        return recordCrc32c.value.toInt()
    }

    private fun updateStackChecksum(length: Int, payload: ByteArray) {
        updateCrcWithInt(stackCrc32c, length)
        stackCrc32c.update(payload)
    }

    private fun updateCrcWithInt(crc32c: CRC32C, value: Int) {
        lengthScratch[0] = (value and 0xFF).toByte()
        lengthScratch[1] = ((value shr 8) and 0xFF).toByte()
        lengthScratch[2] = ((value shr 16) and 0xFF).toByte()
        lengthScratch[3] = ((value shr 24) and 0xFF).toByte()
        crc32c.update(lengthScratch)
    }

    private fun writeToFile(length: Int, checksum: Int, payload: ByteArray) {
        val channel = fileChannel ?: return
        val header = headerBuffer ?: return
        header.clear()
        header.putInt(length)
        header.putInt(checksum)
        header.flip()
        channel.write(header)
        channel.write(ByteBuffer.wrap(payload))
    }
}

sealed interface BitStackStorage {
    data object InMemory : BitStackStorage
    data class FileBacked(val file: File) : BitStackStorage
}

data class Snapshot(
    val buffer: ByteBuffer,
    val recordCount: Int,
    val sizeBytes: Int,
    val checksum: Int
)

data class IntegrityReport(
    val checksumValid: Boolean,
    val parity: ParityCheck
)

data class ParityCheck(
    val valid: Boolean,
    val message: String? = null
) {
    companion object {
        fun stubbed() = ParityCheck(valid = true, message = "parity hook stub")
    }
}

fun interface ParityHook {
    fun onRecord(event: ByteArray, checksum: Int)

    fun validate(snapshot: Snapshot): ParityCheck = ParityCheck.stubbed()

    companion object {
        val NO_OP = ParityHook { _, _ -> }
    }
}
