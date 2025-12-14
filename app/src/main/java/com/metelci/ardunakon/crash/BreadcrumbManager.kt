package com.metelci.ardunakon.crash

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

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

object BreadcrumbManager {
    private const val MAX_BREADCRUMBS = 50
    private val breadcrumbs = ConcurrentLinkedDeque<Breadcrumb>()

    fun leave(tag: String, message: String) {
        breadcrumbs.add(Breadcrumb(System.currentTimeMillis(), tag, message))
        while (breadcrumbs.size > MAX_BREADCRUMBS) {
            breadcrumbs.poll()
        }
    }

    fun getBreadcrumbs(): String {
        return breadcrumbs.joinToString("\n") { it.toString() }
    }

    fun clear() {
        breadcrumbs.clear()
    }
}
