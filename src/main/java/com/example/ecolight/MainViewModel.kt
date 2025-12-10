package com.example.ecolight

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ecolight.data.PlantDao
import com.example.ecolight.data.PlantRequest
import com.example.ecolight.data.PlantRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val plantDao: PlantDao
) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    private val _currentLightLevel = MutableStateFlow(0f)
    val currentLightLevel: StateFlow<Float> = _currentLightLevel.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisProgress = MutableStateFlow(0f)
    val analysisProgress: StateFlow<Float> = _analysisProgress.asStateFlow()
    
    private val _analysisTimeRemaining = MutableStateFlow(0L)
    val analysisTimeRemaining: StateFlow<Long> = _analysisTimeRemaining.asStateFlow()

    private var lightReadings = mutableListOf<Float>()
    private var analysisJob: Job? = null

    val allRequests = plantDao.getAllRequests()

    init {
        startListening()
    }

    private fun startListening() {
        lightSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
            val lux = event.values[0]
            _currentLightLevel.value = lux
            if (_isAnalyzing.value) {
                lightReadings.add(lux)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    fun startAnalysis(name: String, imageUri: String?, durationSeconds: Int) {
        if (_isAnalyzing.value) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            lightReadings.clear()
            
            val startTime = System.currentTimeMillis()
            val endTime = startTime + (durationSeconds * 1000)
            
            // Keep updating progress while analyzing
            while (System.currentTimeMillis() < endTime) {
                val currentTime = System.currentTimeMillis()
                val elapsed = currentTime - startTime
                _analysisProgress.value = elapsed.toFloat() / (durationSeconds * 1000)
                _analysisTimeRemaining.value = (endTime - currentTime) / 1000
                delay(100) // Update UI every 100ms
            }
            
            _isAnalyzing.value = false
            _analysisProgress.value = 0f
            
            // Once analysis is done, process results
            processAnalysisResults(name, imageUri, durationSeconds)
        }
    }

    private fun processAnalysisResults(name: String, imageUri: String?, durationSeconds: Int) {
        viewModelScope.launch {
            _isSearching.value = true
            try {
                // Calculate average light from readings
                val avgLight = if (lightReadings.isNotEmpty()) {
                    lightReadings.average().toFloat()
                } else {
                    _currentLightLevel.value // Fallback if no readings captured
                }
                
                // Convert readings to CSV string for storage
                // We can downsample if there are too many points to save space
                val readingsString = lightReadings.joinToString(",")
                
                // Search for plant data online (simulated)
                val plantInfo = PlantRepository.searchPlantRequirements(name)
                
                val suitability: Boolean
                val recommendation: String

                if (plantInfo != null) {
                    suitability = avgLight >= plantInfo.minLux && avgLight <= plantInfo.maxLux
                    
                    recommendation = if (suitability) {
                        "Excelente! La luz promedio ($avgLight lx) durante $durationSeconds segundos es ideal para ${plantInfo.name} (${plantInfo.minLux}-${plantInfo.maxLux} lx). ${plantInfo.description}"
                    } else if (avgLight < plantInfo.minLux) {
                        "Muy poca luz promedio ($avgLight lx). ${plantInfo.name} necesita al menos ${plantInfo.minLux} lx. ${plantInfo.description}"
                    } else {
                        "Demasiada luz promedio ($avgLight lx). ${plantInfo.name} prefiere menos de ${plantInfo.maxLux} lx. ${plantInfo.description}"
                    }
                } else {
                    // Fallback to generic logic if plant not found
                    suitability = calculateSuitability(name, avgLight)
                    recommendation = getRecommendation(name, avgLight) + " (Promedio de $durationSeconds s - Datos específicos no encontrados)"
                }
                
                val request = PlantRequest(
                    plantName = plantInfo?.name ?: name,
                    lightLevel = avgLight,
                    analysisDurationSeconds = durationSeconds.toLong(),
                    lightReadingsJson = readingsString,
                    timestamp = System.currentTimeMillis(),
                    imageUri = imageUri,
                    isSuitable = suitability,
                    recommendation = recommendation
                )
                plantDao.insertRequest(request)
            } finally {
                _isSearching.value = false
            }
        }
    }

    private fun calculateSuitability(name: String, light: Float): Boolean {
        val plant = name.lowercase().trim()
        return when {
            plant.contains("cactus") || plant.contains("suculenta") -> light > 5000
            plant.contains("helecho") || plant.contains("fern") -> light < 2000
            else -> light > 500 // Generic rule
        }
    }

    private fun getRecommendation(name: String, light: Float): String {
         val plant = name.lowercase().trim()
         return when {
            plant.contains("cactus") -> if (light > 5000) "Luz adecuada para Cactus." else "Muy poca luz para un Cactus. Necesita sol directo."
            plant.contains("helecho") -> if (light < 2000) "Luz adecuada para Helecho." else "Demasiada luz para un Helecho. Prefiere sombra."
            else -> if (light > 500) "Luz aceptable para la mayoría de plantas de interior." else "Luz baja, busca plantas de sombra."
        }
    }
}

class MainViewModelFactory(
    private val application: Application,
    private val plantDao: PlantDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application, plantDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}