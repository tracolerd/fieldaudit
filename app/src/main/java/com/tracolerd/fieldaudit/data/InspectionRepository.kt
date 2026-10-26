package com.tracolerd.fieldaudit.data
import kotlinx.coroutines.flow.Flow
class InspectionRepository(private val dao: ChecklistDao) {
    val allInspections: Flow<List<InspectionEntity>> = dao.getAllInspections()
    suspend fun insert(inspection: InspectionEntity) {
        dao.insertInspection(inspection)
    }
    suspend fun updateStatus(id: String, status: String) {
        dao.updateSyncStatus(id, status)
    }
}