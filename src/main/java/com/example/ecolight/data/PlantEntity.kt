package com.example.ecolight.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plant_requests")
data class PlantRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val plantName: String,
    val lightLevel: Float, // Average light level
    val minLightLevel: Float? = null,
    val maxLightLevel: Float? = null,
    val analysisDurationSeconds: Long = 0,
    val lightReadingsJson: String = "", // Stored as comma separated values
    val timestamp: Long,
    val imageUri: String? = null,
    val isSuitable: Boolean? = null,
    val recommendation: String = ""
)