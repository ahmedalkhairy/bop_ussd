package com.pal.ussd.data.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores sensitive values (PINs, account index) using AES-256 encryption
 * backed by the Android Keystore. Data survives app restarts but is
 * bound to this device and cannot be backed up.
 */
class SecurePinStorage(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveMenuPin(pin: String) = prefs.edit().putString(KEY_MENU_PIN, pin).apply()
    fun getMenuPin(): String? = prefs.getString(KEY_MENU_PIN, null)

    fun saveTransferPin(pin: String) = prefs.edit().putString(KEY_TRANSFER_PIN, pin).apply()
    fun getTransferPin(): String? = prefs.getString(KEY_TRANSFER_PIN, null)

    fun saveAccountIndex(index: String) = prefs.edit().putString(KEY_ACCOUNT_INDEX, index).apply()
    fun getAccountIndex(): String = prefs.getString(KEY_ACCOUNT_INDEX, "1") ?: "1"

    fun isConfigured(): Boolean = getMenuPin() != null && getTransferPin() != null

    fun clear() = prefs.edit().clear().apply()

    companion object {
        private const val KEY_MENU_PIN = "menu_pin"
        private const val KEY_TRANSFER_PIN = "transfer_pin"
        private const val KEY_ACCOUNT_INDEX = "account_index"
    }
}
