package com.pal.ussd.data.model

data class TransferRequest(
    val recipientPhone: String,
    val amount: String
)

data class StepStatus(
    val index: Int,
    val label: String,
    val state: StepState
)

enum class StepState { PENDING, RUNNING, DONE, FAILED }
