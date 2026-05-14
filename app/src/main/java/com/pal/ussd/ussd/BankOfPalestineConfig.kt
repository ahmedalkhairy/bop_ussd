package com.pal.ussd.ussd

import com.pal.ussd.data.model.PinType
import com.pal.ussd.data.model.TransferField
import com.pal.ussd.data.model.UssdStep

/**
 * Defines the complete USSD flow for Bank of Palestine friend transfer.
 *
 * Recorded flow:
 *   *267#
 *   → PIN القائمة
 *   → # (المزيد)
 *   → 3 (دفع الى)
 *   → 1 (صديق)
 *   → رقم جوال المستقبل
 *   → 1 (تأكيد الاسم)
 *   → المبلغ
 *   → رقم الحساب
 *   → رمز التأكيد
 *   → رسالة الانتهاء
 */
object BankOfPalestineConfig {

    const val INITIAL_CODE = "*267#"

    val TRANSFER_TO_FRIEND_STEPS: List<UssdStep> = listOf(
        UssdStep.CachedPin(PinType.MENU_PIN),           // PIN للدخول للقائمة
        UssdStep.Fixed("#"),                             // المزيد
        UssdStep.Fixed("3"),                             // دفع الى
        UssdStep.Fixed("1"),                             // صديق
        UssdStep.UserInput(TransferField.RECIPIENT),     // رقم جوال المستقبل
        UssdStep.Fixed("1"),                             // تأكيد الاسم
        UssdStep.UserInput(TransferField.AMOUNT),        // المبلغ
        UssdStep.AccountSelection,                       // اختيار الحساب
        UssdStep.CachedPin(PinType.TRANSFER_PIN)        // رمز التأكيد
    )

    val STEP_LABELS: List<String> = listOf(
        "إدخال الرقم السري",
        "الانتقال لقائمة المزيد",
        "اختيار دفع الى",
        "اختيار دفع لصديق",
        "إرسال رقم المستقبل",
        "تأكيد اسم المستقبل",
        "إدخال المبلغ",
        "اختيار الحساب",
        "إدخال رمز التأكيد"
    )
}
