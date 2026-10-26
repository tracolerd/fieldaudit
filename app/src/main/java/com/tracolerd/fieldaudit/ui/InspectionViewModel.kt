package com.tracolerd.fieldaudit.ui
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tracolerd.fieldaudit.data.AppDatabase
import com.tracolerd.fieldaudit.data.InspectionEntity
import com.tracolerd.fieldaudit.data.InspectionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
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
                status = "DRAFT",
                localVersion = 1,
                serverVersion = 0,
                lastUpdated = System.currentTimeMillis()
            )
            repository.insert(newInspection)
        }
    }
}