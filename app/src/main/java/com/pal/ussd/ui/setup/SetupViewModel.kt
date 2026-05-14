package com.pal.ussd.ui.setup

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.pal.ussd.data.storage.SecurePinStorage

class SetupViewModel(app: Application) : AndroidViewModel(app) {

    private val storage = SecurePinStorage(app)

    private val _saved = MutableLiveData<Boolean>()
    val saved: LiveData<Boolean> = _saved

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadExisting(): Triple<String, String, String> = Triple(
        storage.getMenuPin() ?: "",
        storage.getTransferPin() ?: "",
        storage.getAccountIndex()
    )

    fun save(menuPin: String, transferPin: String, accountIndex: String) {
        when {
            menuPin.isBlank() -> { _error.value = "يرجى إدخال الرقم السري للقائمة"; return }
            transferPin.isBlank() -> { _error.value = "يرجى إدخال رمز التأكيد"; return }
            accountIndex.isBlank() -> { _error.value = "يرجى إدخال رقم الحساب"; return }
        }
        storage.saveMenuPin(menuPin)
        storage.saveTransferPin(transferPin)
        storage.saveAccountIndex(accountIndex)
        _saved.value = true
    }
}
