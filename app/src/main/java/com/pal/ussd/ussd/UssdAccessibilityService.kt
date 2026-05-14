package com.pal.ussd.ussd

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Monitors the system USSD dialog (shown by the phone app) and automatically
 * fills in the next queued value and clicks OK.
 *
 * Why AccessibilityService?
 * TelephonyManager.sendUssdRequest() opens a USSD session for the first code,
 * but subsequent inputs to an ongoing session are shown as system AlertDialogs
 * by the phone app on most Android OEMs. This service intercepts those dialogs
 * and feeds the pre-queued values without user interaction.
 */
class UssdAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return

        // USSD dialogs are typically AlertDialogs from the phone/dialer package
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (isUssdDialog(root)) {
                mainHandler.postDelayed({ handleUssdDialog(root) }, 300)
            }
        }
    }

    private fun isUssdDialog(root: AccessibilityNodeInfo): Boolean {
        val pkg = root.packageName?.toString() ?: return false
        return pkg.contains("phone") || pkg.contains("dialer") || pkg.contains("telecom")
    }

    private fun handleUssdDialog(root: AccessibilityNodeInfo) {
        val nextValue = UssdStepQueue.poll()

        if (nextValue == null) {
            // No more steps — just dismiss or let the user see the final message
            val response = extractDialogText(root)
            UssdStepQueue.onResponse?.invoke(response)
            clickOkButton(root) // dismiss
            return
        }

        val editText = findEditText(root)
        if (editText != null) {
            // Dialog has an input field (e.g. PIN, phone number, amount)
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, nextValue)
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            mainHandler.postDelayed({
                val response = extractDialogText(root)
                UssdStepQueue.onResponse?.invoke(response)
                clickOkButton(root)
            }, 200)
        } else {
            // Dialog has only buttons (menu options — just tap the numbered option)
            val response = extractDialogText(root)
            UssdStepQueue.onResponse?.invoke(response)
            clickButtonByText(root, nextValue)
        }
    }

    private fun extractDialogText(root: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        traverseForText(root, sb)
        return sb.toString().trim()
    }

    private fun traverseForText(node: AccessibilityNodeInfo, sb: StringBuilder) {
        if (!node.text.isNullOrEmpty()) sb.append(node.text).append("\n")
        for (i in 0 until node.childCount) {
            traverseForText(node.getChild(i) ?: continue, sb)
        }
    }

    private fun findEditText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.className?.contains("EditText") == true) return root
        for (i in 0 until root.childCount) {
            val found = findEditText(root.getChild(i) ?: continue)
            if (found != null) return found
        }
        return null
    }

    private fun clickOkButton(root: AccessibilityNodeInfo) {
        // Try common OK button texts
        for (label in listOf("موافق", "OK", "Send", "إرسال", "نعم", "Yes", "تأكيد")) {
            if (clickButtonByText(root, label)) return
        }
        // Fallback: click the first button
        clickFirstButton(root)
    }

    private fun clickButtonByText(root: AccessibilityNodeInfo, text: String): Boolean {
        if ((root.className?.contains("Button") == true || root.isClickable) &&
            root.text?.toString()?.trim() == text
        ) {
            root.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        for (i in 0 until root.childCount) {
            if (clickButtonByText(root.getChild(i) ?: continue, text)) return true
        }
        return false
    }

    private fun clickFirstButton(root: AccessibilityNodeInfo): Boolean {
        if (root.className?.contains("Button") == true && root.isClickable) {
            root.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        for (i in 0 until root.childCount) {
            if (clickFirstButton(root.getChild(i) ?: continue)) return true
        }
        return false
    }

    override fun onInterrupt() {}
}
