package com.pal.ussd.ui.transfer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pal.ussd.data.storage.SecurePinStorage

class TransferViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = SecurePinStorage(app)

    private val _validationError = MutableLiveData<String?>()
    val validationError: LiveData<String?> = _validationError

    private val _navigateToProgress = MutableLiveData<Pair<String, String>?>()
    val navigateToProgress: LiveData<Pair<String, String>?> = _navigateToProgress

    fun isConfigured() = storage.isConfigured()

    fun submitTransfer(recipient: String, amount: String) {
        when {
            recipient.isBlank() || amount.isBlank() -> {
                _validationError.value = "يرجى ملء جميع الحقول"
            }
            !recipient.matches(Regex("^0[0-9]{9}$")) -> {
                _validationError.value = "رقم الجوال غير صحيح (10 أرقام يبدأ بـ 0)"
            }
            amount.toDoubleOrNull() == null || amount.toDouble() <= 0 -> {
                _validationError.value = "يرجى إدخال مبلغ صحيح"
            }
            else -> {
                _validationError.value = null
                _navigateToProgress.value = Pair(recipient, amount)
            }
        }
    }

    fun onNavigated() { _navigateToProgress.value = null }
}
