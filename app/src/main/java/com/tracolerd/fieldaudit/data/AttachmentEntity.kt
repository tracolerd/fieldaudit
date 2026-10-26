package com.tracolerd.fieldaudit.data
import androidx.room.Entity
import androidx.room.PrimaryKey
@Entity(tableName = "attachments")
data class AttachmentEntity(
    @PrimaryKey
    val id: String,
    val inspectionId: String,
    val filePath: String,
    val isSynced: Boolean
)