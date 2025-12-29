package com.metelci.ardunakon.crash

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Single breadcrumb entry with timestamped context for crash reports.
 *
 * @property timestamp Epoch time in milliseconds.
 * @property tag Short category tag.
 * @property message Human-readable message.
 */
data class Breadcrumb(
    val timestamp: Long,
    val tag: String,
    val message: String
) {
    override fun toString(): String {
        val dateParams = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
        return "[$dateParams] $tag: $message"
    }
}

/**
 * Collects and formats breadcrumb trail for crash diagnostics.
 */
object BreadcrumbManager {
    private const val MAX_BREADCRUMBS = 50
    private val breadcrumbs = ConcurrentLinkedDeque<Breadcrumb>()

    /**
     * Adds a breadcrumb entry, discarding oldest entries beyond the limit.
     *
     * @param tag Category tag for the event.
     * @param message Descriptive message for the event.
     */
    fun leave(tag: String, message: String) {
        breadcrumbs.add(Breadcrumb(System.currentTimeMillis(), tag, message))
        while (breadcrumbs.size > MAX_BREADCRUMBS) {
            breadcrumbs.poll()
        }
    }

    /**
     * Returns formatted breadcrumbs suitable for crash logs.
     *
     * @return Newline-separated breadcrumb entries.
     */
    fun getBreadcrumbs(): String {
        return breadcrumbs.joinToString("\n") { it.toString() }
    }

    /**
     * Clears all stored breadcrumbs.
     */
    fun clear() {
        breadcrumbs.clear()
    }
}
