package com.tracolerd.fieldaudit.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "inspections")
data class InspectionEntity(
    @PrimaryKey
    val id: String, // Stable Client-Generated UUID
    val title: String,
    val status: String, // DRAFT, PENDING_SYNC, SYNCED, CONFLICT
    val localVersion: Int,
    val serverVersion: Int,
    val lastUpdated: Long
)