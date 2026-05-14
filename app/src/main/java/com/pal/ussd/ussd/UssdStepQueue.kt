package com.pal.ussd.ussd

import java.util.concurrent.LinkedBlockingQueue

/**
 * Singleton queue shared between UssdEngine and UssdAccessibilityService.
 * Engine puts the next value to send; AccessibilityService reads it when
 * the system USSD dialog appears.
 */
object UssdStepQueue {

    private val queue = LinkedBlockingQueue<String>()

    /** Callbacks invoked on the main thread after each USSD dialog response */
    var onResponse: ((response: String) -> Unit)? = null
    var onError: ((reason: String) -> Unit)? = null

    fun push(value: String) = queue.offer(value)

    fun poll(): String? = queue.poll()

    fun clear() = queue.clear()
}
