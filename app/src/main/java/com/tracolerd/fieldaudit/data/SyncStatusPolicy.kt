package com.tracolerd.fieldaudit.data

enum class SyncResponseDisposition {
    ACCEPTED,
    CONFLICT,
    PERMANENT_FAILURE,
    RETRY,
}

internal fun classifySyncStatus(statusCode: Int): SyncResponseDisposition = when {
    statusCode in 200..299 -> SyncResponseDisposition.ACCEPTED
    statusCode == 409 -> SyncResponseDisposition.CONFLICT
    statusCode == 408 || statusCode == 429 -> SyncResponseDisposition.RETRY
    statusCode in 400..499 -> SyncResponseDisposition.PERMANENT_FAILURE
    else -> SyncResponseDisposition.RETRY
}
