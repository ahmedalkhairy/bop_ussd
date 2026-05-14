package com.pal.ussd.ussd

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import com.pal.ussd.data.model.PinType
import com.pal.ussd.data.model.StepState
import com.pal.ussd.data.model.StepStatus
import com.pal.ussd.data.model.TransferField
import com.pal.ussd.data.model.TransferRequest
import com.pal.ussd.data.model.UssdStep
import com.pal.ussd.data.storage.SecurePinStorage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Hybrid USSD engine:
 *
 * Strategy A — TelephonyManager.sendUssdRequest()
 *   Works on stock Android and some OEMs. Sends the initial code and
 *   subsequent inputs programmatically.
 *
 * Strategy B — ACTION_CALL + AccessibilityService
 *   Dials *267# via the phone dialer (triggers the system USSD dialog),
 *   then UssdAccessibilityService fills each step automatically.
 *   Used as fallback when Strategy A fails or when Accessibility is enabled.
 */
class UssdEngine(
    private val context: Context,
    private val storage: SecurePinStorage
) {
    private val telephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }
    private val mainHandler = Handler(Looper.getMainLooper())
    private val responseChannel = Channel<UssdResult>(capacity = 1)

    fun execute(
        request: TransferRequest,
        steps: List<UssdStep>,
        labels: List<String>,
        initialCode: String
    ): Flow<List<StepStatus>> = flow {

        val statuses = labels.mapIndexed { i, label ->
            StepStatus(i, label, StepState.PENDING)
        }.toMutableList()
        emit(statuses.toList())

        // Resolve all step values upfront so we can push them to the queue
        val resolvedValues = steps.map { resolveStepValue(it, request) }
        if (resolvedValues.any { it == null }) {
            statuses[0] = statuses[0].copy(state = StepState.FAILED)
            emit(statuses.toList())
            return@flow
        }

        // Push all values to the queue for AccessibilityService
        UssdStepQueue.clear()
        resolvedValues.forEach { UssdStepQueue.push(it!!) }

        // Try Strategy A first (TelephonyManager)
        val usedTelephony = tryTelephonyStrategy(initialCode)

        if (!usedTelephony) {
            // Strategy B: open via phone dialer, AccessibilityService handles the rest
            dialViaIntent(initialCode)
        }

        // Now drive each step by listening to responses from the queue callbacks
        steps.forEachIndexed { index, _ ->
            statuses[index] = statuses[index].copy(state = StepState.RUNNING)
            emit(statuses.toList())

            if (usedTelephony) {
                // For strategy A, send each step via TelephonyManager
                val value = resolvedValues[index]!!
                sendUssdRequest(value)
            }
            // For strategy B, AccessibilityService handles it; we just wait for the callback

            val response = withTimeoutOrNull(STEP_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    UssdStepQueue.onResponse = { cont.resume(UssdResult.Success(it)) }
                    UssdStepQueue.onError = { cont.resume(UssdResult.Failed(it)) }
                }
            }

            when {
                response == null -> {
                    statuses[index] = statuses[index].copy(state = StepState.FAILED)
                    emit(statuses.toList())
                    return@flow
                }
                response is UssdResult.Failed -> {
                    statuses[index] = statuses[index].copy(state = StepState.FAILED)
                    emit(statuses.toList())
                    return@flow
                }
                else -> {
                    statuses[index] = statuses[index].copy(state = StepState.DONE)
                    emit(statuses.toList())
                }
            }
        }

        UssdStepQueue.onResponse = null
        UssdStepQueue.onError = null
    }

    @SuppressLint("MissingPermission")
    private fun tryTelephonyStrategy(initialCode: String): Boolean {
        return try {
            val callback = object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    tm: TelephonyManager, request: String, response: CharSequence
                ) {
                    UssdStepQueue.onResponse?.invoke(response.toString())
                }

                override fun onReceiveUssdResponseFailed(
                    tm: TelephonyManager, request: String, failureCode: Int
                ) {
                    UssdStepQueue.onError?.invoke("كود الخطأ: $failureCode")
                }
            }
            mainHandler.post {
                telephonyManager.sendUssdRequest(initialCode, callback, mainHandler)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendUssdRequest(value: String) {
        try {
            val callback = object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(
                    tm: TelephonyManager, request: String, response: CharSequence
                ) {
                    UssdStepQueue.onResponse?.invoke(response.toString())
                }

                override fun onReceiveUssdResponseFailed(
                    tm: TelephonyManager, request: String, failureCode: Int
                ) {
                    UssdStepQueue.onError?.invoke("كود الخطأ: $failureCode")
                }
            }
            mainHandler.post {
                telephonyManager.sendUssdRequest(value, callback, mainHandler)
            }
        } catch (_: Exception) {}
    }

    private fun dialViaIntent(code: String) {
        val encoded = code.replace("#", "%23")
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$encoded")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun resolveStepValue(step: UssdStep, request: TransferRequest): String? {
        return when (step) {
            is UssdStep.Fixed -> step.value
            is UssdStep.UserInput -> when (step.field) {
                TransferField.RECIPIENT -> request.recipientPhone
                TransferField.AMOUNT -> request.amount
            }
            is UssdStep.CachedPin -> when (step.type) {
                PinType.MENU_PIN -> storage.getMenuPin()
                PinType.TRANSFER_PIN -> storage.getTransferPin()
            }
            is UssdStep.AccountSelection -> storage.getAccountIndex()
        }
    }

    sealed class UssdResult {
        data class Success(val message: String) : UssdResult()
        data class Failed(val reason: String) : UssdResult()
    }

    companion object {
        private const val STEP_TIMEOUT_MS = 20_000L
    }
}
