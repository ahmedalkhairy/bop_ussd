package com.pal.ussd.ussd

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Sends the transfer parameters to MacroDroid via a broadcast intent.
 * MacroDroid listens for INTENT_ACTION and reads recipient/amount as extras.
 * PINs are stored securely inside MacroDroid's own macro variables — they
 * never leave MacroDroid and are never passed through this bridge.
 */
object MacroDroidBridge {

    const val INTENT_ACTION = "com.pal.ussd.START_TRANSFER"
    const val EXTRA_RECIPIENT = "recipient"
    const val EXTRA_AMOUNT = "amount"
    const val MACRODROID_PACKAGE = "com.arlosoft.macrodroid"

    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(MACRODROID_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun sendTransfer(context: Context, recipient: String, amount: String) {
        val intent = Intent(INTENT_ACTION).apply {
            putExtra(EXTRA_RECIPIENT, recipient)
            putExtra(EXTRA_AMOUNT, amount)
            // Target MacroDroid explicitly so other apps can't intercept
            `package` = MACRODROID_PACKAGE
        }
        context.sendBroadcast(intent)
    }
}
