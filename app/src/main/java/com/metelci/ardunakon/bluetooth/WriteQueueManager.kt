package com.metelci.ardunakon.bluetooth

import android.util.Log
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*

/**
 * Manages the BLE write queue with bounded capacity and packet drop handling.
 * Prevents unbounded growth during connectivity issues and provides metrics.
 */
class WriteQueueManager(
    private val capacity: Int = 100,
    private val scope: CoroutineScope,
    private val tag: String = "WriteQueue"
) {
    private val queue = LinkedBlockingQueue<ByteArray>(capacity)
    private var writeJob: Job? = null

    // Metrics
    private val _packetsDropped = AtomicLong(0)
    private val _packetsFailed = AtomicLong(0)
    private val _packetsSent = AtomicLong(0)

    val packetsDropped: Long get() = _packetsDropped.get()
    val packetsFailed: Long get() = _packetsFailed.get()
    val packetsSent: Long get() = _packetsSent.get()

    // Callbacks
    var onPacketDropped: (() -> Unit)? = null
    var onWriteFailed: (() -> Unit)? = null
    var onLog: ((String, com.metelci.ardunakon.model.LogType) -> Unit)? = null

    /**
     * Starts the write queue processing loop.
     *
     * @param performWrite Suspend function to actually write data to the device
     * @param writeDelayMs Delay between writes to prevent overwhelming BLE stack
     * @param initialDelayMs Initial delay before processing (for BLE stack stability)
     */
    fun start(performWrite: suspend (ByteArray) -> Boolean, writeDelayMs: Long = 10L, initialDelayMs: Long = 200L) {
        writeJob?.cancel()
        queue.clear() // Clear stale packets from previous session

        writeJob = scope.launch {
            // Wait for BLE stack to stabilize after connection
            delay(initialDelayMs)

            while (isActive) {
                try {
                    // Take from queue (blocking call) on an interruptible dispatcher so cancellation can stop it.
                    val data = runInterruptible(Dispatchers.IO) { queue.take() }

                    // Perform actual write
                    val success = performWrite(data)
                    if (success) {
                        _packetsSent.incrementAndGet()
                    } else {
                        _packetsFailed.incrementAndGet()
                        onWriteFailed?.invoke()
                    }

                    // Small delay to prevent overwhelming BLE stack
                    delay(writeDelayMs)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: InterruptedException) {
                    // Queue interrupted, exit gracefully
                    break
                } catch (e: Exception) {
                    Log.e(tag, "Write queue error", e)
                }
            }
        }
    }

    /**
     * Stops the write queue processing.
     */
    fun stop() {
        writeJob?.cancel()
        writeJob = null
    }

    /**
     * Clears the queue and resets metrics.
     */
    fun clear() {
        queue.clear()
    }

    /**
     * Resets packet statistics.
     */
    fun resetMetrics() {
        _packetsSent.set(0)
        _packetsDropped.set(0)
        _packetsFailed.set(0)
    }

    /**
     * Enqueues data for writing. If queue is full, drops the oldest packet.
     *
     * @return true if enqueued successfully, false if a packet was dropped
     */
    fun enqueue(data: ByteArray): Boolean {
        if (queue.offer(data)) {
            return true
        }

        // Queue full - drop oldest packet
        queue.poll()
        queue.offer(data)
        _packetsDropped.incrementAndGet()
        onPacketDropped?.invoke()

        onLog?.invoke("⚠ Packet dropped (queue full)", com.metelci.ardunakon.model.LogType.WARNING)
        return false
    }

    /**
     * Returns current queue size.
     */
    fun queueSize(): Int = queue.size

    /**
     * Returns true if the queue is currently processing.
     */
    fun isRunning(): Boolean = writeJob?.isActive == true
}
