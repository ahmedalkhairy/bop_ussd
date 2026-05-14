package com.pal.ussd.ui.progress

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pal.ussd.data.model.StepStatus
import com.pal.ussd.data.model.TransferRequest
import com.pal.ussd.data.storage.SecurePinStorage
import com.pal.ussd.ussd.BankOfPalestineConfig
import com.pal.ussd.ussd.UssdEngine
import kotlinx.coroutines.launch

class ProgressViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = UssdEngine(app, SecurePinStorage(app))

    private val _steps = MutableLiveData<List<StepStatus>>()
    val steps: LiveData<List<StepStatus>> = _steps

    private val _finished = MutableLiveData<Boolean?>()
    val finished: LiveData<Boolean?> = _finished

    private val _isRunning = MutableLiveData(false)
    val isRunning: LiveData<Boolean> = _isRunning

    fun startTransfer(recipient: String, amount: String) {
        if (_isRunning.value == true) return
        _isRunning.value = true

        val request = TransferRequest(recipient, amount)

        viewModelScope.launch {
            engine.execute(
                request = request,
                steps = BankOfPalestineConfig.TRANSFER_TO_FRIEND_STEPS,
                labels = BankOfPalestineConfig.STEP_LABELS,
                initialCode = BankOfPalestineConfig.INITIAL_CODE
            ).collect { stepList ->
                _steps.postValue(stepList)

                val allDone = stepList.all { it.state == com.pal.ussd.data.model.StepState.DONE }
                val anyFailed = stepList.any { it.state == com.pal.ussd.data.model.StepState.FAILED }

                when {
                    allDone -> { _finished.postValue(true); _isRunning.postValue(false) }
                    anyFailed -> { _finished.postValue(false); _isRunning.postValue(false) }
                }
            }
        }
    }
}
