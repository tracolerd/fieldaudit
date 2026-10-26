package com.tracolerd.fieldaudit.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
@Dao
interface ChecklistDao {
    @Query("SELECT * FROM inspections ORDER BY lastUpdated DESC")
    fun getAllInspections(): Flow<List<InspectionEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: InspectionEntity)
    @Query("UPDATE inspections SET status = :newStatus WHERE id = :inspectionId")
    suspend fun updateSyncStatus(inspectionId: String, newStatus: String)
}