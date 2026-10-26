package com.tracolerd.fieldaudit.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncStatusPolicyTest {
    @Test
    fun successfulResponsesAreAccepted() {
        assertEquals(SyncResponseDisposition.ACCEPTED, classifySyncStatus(200))
        assertEquals(SyncResponseDisposition.ACCEPTED, classifySyncStatus(201))
    }

    @Test
    fun conflictIsNotSilentlyRetried() {
        assertEquals(SyncResponseDisposition.CONFLICT, classifySyncStatus(409))
    }

    @Test
    fun clientErrorsArePermanentAndServerErrorsRetry() {
        assertEquals(SyncResponseDisposition.PERMANENT_FAILURE, classifySyncStatus(400))
        assertEquals(SyncResponseDisposition.PERMANENT_FAILURE, classifySyncStatus(422))
        assertEquals(SyncResponseDisposition.RETRY, classifySyncStatus(429))
        assertEquals(SyncResponseDisposition.RETRY, classifySyncStatus(500))
        assertEquals(SyncResponseDisposition.RETRY, classifySyncStatus(503))
    }
}
