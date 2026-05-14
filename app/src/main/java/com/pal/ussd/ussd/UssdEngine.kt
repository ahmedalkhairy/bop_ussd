package com.pal.ussd.ussd

import android.annotation.SuppressLint
import android.content.Context
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
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Executes a multi-step USSD session by sending each step sequentially.
 * Each step waits for a USSD response before sending the next input.
 * Uses a Channel to bridge the async TelephonyManager callback into coroutine flow.
 */
class UssdEngine(
    private val context: Context,
    private val storage: SecurePinStorage
) {
    private val telephonyManager by lazy {
        context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    }

    // Receives USSD responses from the telephony callback
    private val responseChannel = Channel<UssdResult>(capacity = 1)

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Emits [StepStatus] updates as each step executes.
     * Terminates with a final DONE or FAILED status on the last step.
     */
    @SuppressLint("MissingPermission")
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

        // Step -1: Dial the initial USSD code
        val callback = buildCallback()
        sendUssd(initialCode, callback)

        // Wait for the initial menu response
        val initialResponse = withTimeoutOrNull(STEP_TIMEOUT_MS) { responseChannel.receive() }
        if (initialResponse is UssdResult.Failed || initialResponse == null) {
            statuses[0] = statuses[0].copy(state = StepState.FAILED)
            emit(statuses.toList())
            return@flow
        }

        // Execute each step
        steps.forEachIndexed { index, step ->
            statuses[index] = statuses[index].copy(state = StepState.RUNNING)
            emit(statuses.toList())

            val value = resolveStepValue(step, request)
            if (value == null) {
                statuses[index] = statuses[index].copy(state = StepState.FAILED)
                emit(statuses.toList())
                return@flow
            }

            sendUssd(value, callback)

            val response = withTimeoutOrNull(STEP_TIMEOUT_MS) { responseChannel.receive() }

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
    }

    @SuppressLint("MissingPermission")
    private fun sendUssd(code: String, callback: TelephonyManager.UssdResponseCallback) {
        mainHandler.post {
            telephonyManager.sendUssdRequest(code, callback, mainHandler)
        }
    }

    private fun buildCallback() = object : TelephonyManager.UssdResponseCallback() {
        override fun onReceiveUssdResponse(
            telephonyManager: TelephonyManager,
            request: String,
            response: CharSequence
        ) {
            responseChannel.trySend(UssdResult.Success(response.toString()))
        }

        override fun onReceiveUssdResponseFailed(
            telephonyManager: TelephonyManager,
            request: String,
            failureCode: Int
        ) {
            responseChannel.trySend(UssdResult.Failed("Error code: $failureCode"))
        }
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
        private const val STEP_TIMEOUT_MS = 15_000L
    }
}
