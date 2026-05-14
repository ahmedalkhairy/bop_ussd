package com.pal.ussd.data.model

/**
 * Represents a single step in a USSD session flow.
 * Each step defines what value to send to the USSD session.
 */
sealed class UssdStep {
    /** Send a hardcoded value (menu option number or symbol) */
    data class Fixed(val value: String) : UssdStep()

    /** Send a value entered by the user in the transfer form */
    data class UserInput(val field: TransferField) : UssdStep()

    /** Send a PIN retrieved from secure storage */
    data class CachedPin(val type: PinType) : UssdStep()

    /** Send a pre-configured account index from settings */
    object AccountSelection : UssdStep()
}

enum class TransferField { RECIPIENT, AMOUNT }

enum class PinType {
    MENU_PIN,      // PIN for entering the USSD main menu
    TRANSFER_PIN   // PIN for confirming the transaction
}
