package com.tracolerd.fieldaudit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.tracolerd.fieldaudit.BuildConfig
import com.tracolerd.fieldaudit.data.AppDatabase
import com.tracolerd.fieldaudit.data.InspectionEntity
import com.tracolerd.fieldaudit.data.InspectionRepository
import com.tracolerd.fieldaudit.data.SyncWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

class InspectionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: InspectionRepository

    init {
        val dao = AppDatabase.getDatabase(application).checklistDao()
        repository = InspectionRepository(dao)
    }

    val inspections: StateFlow<List<InspectionEntity>> = repository.allInspections
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createNewDraft(title: String) {
        viewModelScope.launch {
            val newInspection = InspectionEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                status = "PENDING_SYNC",
                localVersion = 1,
                serverVersion = 0,
                lastUpdated = System.currentTimeMillis()
            )
            repository.insert(newInspection)
            enqueueSync()
        }
    }

    fun retrySync(inspection: InspectionEntity) {
        viewModelScope.launch {
            repository.updateStatus(inspection.id, "PENDING_SYNC")
            enqueueSync()
        }
    }

    // Phase 7: Explicit conflict-resolution action. The current API does not yet
    // return the full server record on 409, so KEEP_SERVER only changes local
    // sync state; a later issue must implement a real server-snapshot refresh.
    fun resolveConflict(inspection: InspectionEntity, choice: String) {
        viewModelScope.launch {
            val updatedStatus = if (choice == "KEEP_LOCAL") "PENDING_SYNC" else "SYNCED"
            repository.updateStatus(inspection.id, updatedStatus)
            if (updatedStatus == "PENDING_SYNC") enqueueSync()
        }
    }

    private fun enqueueSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(
                androidx.work.workDataOf(SyncWorker.KEY_API_BASE_URL to BuildConfig.API_BASE_URL)
            )
            .build()

        WorkManager.getInstance(getApplication())
            .enqueueUniqueWork("field-audit-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}
