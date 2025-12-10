package com.example.ecolight

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ecolight.data.AppDatabase
import com.example.ecolight.data.PlantRequest
import com.example.ecolight.ui.theme.EcolightTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val database = AppDatabase.getDatabase(applicationContext)
        val viewModelFactory = MainViewModelFactory(application, database.plantDao())
        val viewModel: MainViewModel by viewModels { viewModelFactory }

        setContent {
            EcolightTheme {
                MainScreen(viewModel)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentLight by viewModel.currentLightLevel.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val analysisProgress by viewModel.analysisProgress.collectAsState()
    val timeRemaining by viewModel.analysisTimeRemaining.collectAsState()
    val requestList by viewModel.allRequests.collectAsState(initial = emptyList())
    
    var plantName by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDurationSeconds by remember { mutableIntStateOf(0) } // 0 means instant

    // State for displaying details dialog
    var showDetailsDialog by remember { mutableStateOf(false) }
    var selectedRequest by remember { mutableStateOf<PlantRequest?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri }
    )

    if (showDetailsDialog && selectedRequest != null) {
        PlantDetailsDialog(
            request = selectedRequest!!,
            onDismiss = { showDetailsDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Sensor de Luz Ambiental",
                style = MaterialTheme.typography.headlineMedium
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Nivel de Luz Actual", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "$currentLight lx",
                        style = MaterialTheme.typography.displayMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = plantName,
                onValueChange = { plantName = it },
                label = { Text("Nombre de la Planta") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSearching && !isAnalyzing
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { 
                         photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    enabled = !isSearching && !isAnalyzing
                ) {
                    Text("Subir Foto")
                }
                
                if (selectedImageUri != null) {
                    Text("Foto seleccionada", color = MaterialTheme.colorScheme.primary)
                }
            }
            
            if (selectedImageUri != null && plantName.isBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        plantName = listOf("Monstera", "Cactus", "Helecho", "Ficus", "Orquídea").random()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSearching && !isAnalyzing
                ) {
                    Text("Identificar Planta (Simulado)")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Duración del Análisis:", style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.Start))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DurationChip("Instantáneo", 0, selectedDurationSeconds) { selectedDurationSeconds = it }
                DurationChip("30m", 1800, selectedDurationSeconds) { selectedDurationSeconds = it }
                DurationChip("1h", 3600, selectedDurationSeconds) { selectedDurationSeconds = it }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isAnalyzing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { analysisProgress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Analizando luz promedio... ${timeRemaining / 60} min ${timeRemaining % 60} s restantes")
                    Text("Mantén el dispositivo en el lugar de la planta.", style = MaterialTheme.typography.bodySmall)
                }
            } else if (isSearching) {
                CircularProgressIndicator()
                Text("Buscando información de la planta...", style = MaterialTheme.typography.bodySmall)
            } else {
                Button(
                    onClick = {
                        if (plantName.isNotBlank()) {
                            val savedPath = selectedImageUri?.let { uri ->
                                 saveImageToInternalStorage(context, uri)
                            }
                            // Start analysis with selected duration
                            viewModel.startAnalysis(plantName, savedPath, selectedDurationSeconds)
                            
                            plantName = ""
                            selectedImageUri = null
                            // Reset duration to instant? Or keep last selection? keeping it seems fine.
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = plantName.isNotBlank()
                ) {
                    val buttonText = when {
                        selectedDurationSeconds == 0 -> "Analizar Ahora"
                        selectedDurationSeconds >= 3600 -> "Iniciar Análisis (${selectedDurationSeconds / 3600} h)"
                        selectedDurationSeconds >= 60 -> "Iniciar Análisis (${selectedDurationSeconds / 60} m)"
                        else -> "Iniciar Análisis (${selectedDurationSeconds} s)"
                    }
                    Text(buttonText)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Historial",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.Start)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                items(requestList) { request ->
                    PlantRequestItem(
                        request = request,
                        onClick = { 
                            selectedRequest = request
                            showDetailsDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DurationChip(
    label: String,
    seconds: Int,
    selectedSeconds: Int,
    onSelect: (Int) -> Unit
) {
    FilterChip(
        selected = seconds == selectedSeconds,
        onClick = { onSelect(seconds) },
        label = { Text(label) }
    )
}

@Composable
fun PlantRequestItem(
    request: PlantRequest,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            if (request.imageUri != null) {
                AsyncImage(
                    model = File(request.imageUri),
                    contentDescription = "Plant Image",
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            
            Column {
                Text(text = request.plantName, style = MaterialTheme.typography.titleMedium)
                Text(text = "Luz medida: ${request.lightLevel.toInt()} lx", style = MaterialTheme.typography.labelLarge)
                if (request.analysisDurationSeconds > 0) {
                     val durationText = when {
                        request.analysisDurationSeconds >= 3600 -> "Promedio durante ${request.analysisDurationSeconds / 3600} h"
                        request.analysisDurationSeconds >= 60 -> "Promedio durante ${request.analysisDurationSeconds / 60} m"
                        else -> "Promedio durante ${request.analysisDurationSeconds}s"
                    }
                    Text(text = durationText, style = MaterialTheme.typography.labelSmall)
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(request.timestamp))
                Text(text = date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun PlantDetailsDialog(
    request: PlantRequest,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Detalles del Análisis",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (request.imageUri != null) {
                    AsyncImage(
                        model = File(request.imageUri),
                        contentDescription = "Plant Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(text = "Planta: ${request.plantName}", style = MaterialTheme.typography.titleMedium)
                Text(text = "Luz Promedio: ${request.lightLevel.toInt()} lx")
                if (request.analysisDurationSeconds > 0) {
                    val durationText = when {
                        request.analysisDurationSeconds >= 3600 -> "${request.analysisDurationSeconds / 3600} horas"
                        request.analysisDurationSeconds >= 60 -> "${request.analysisDurationSeconds / 60} minutos"
                        else -> "${request.analysisDurationSeconds} segundos"
                    }
                    Text(text = "Duración: $durationText")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Recomendación:", style = MaterialTheme.typography.titleSmall)
                Text(text = request.recommendation, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                if (request.lightReadingsJson.isNotEmpty()) {
                    Text("Gráfico de Luz:", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    LightGraph(readingsJson = request.lightReadingsJson)
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
fun LightGraph(readingsJson: String) {
    val readings = remember(readingsJson) {
        try {
            if (readingsJson.isBlank()) emptyList() 
            else readingsJson.split(",").mapNotNull { it.toFloatOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    if (readings.isEmpty()) {
        Text("No hay datos suficientes para el gráfico.", style = MaterialTheme.typography.bodySmall)
        return
    }

    val maxReading = readings.maxOrNull() ?: 100f
    val minReading = readings.minOrNull() ?: 0f
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (readings.size < 2) return@Canvas

            val width = size.width
            val height = size.height
            val stepX = width / (readings.size - 1)
            
            // Normalize points to fit in height
            val range = (maxReading - minReading).coerceAtLeast(1f) // avoid div by zero
            
            val path = Path()
            readings.forEachIndexed { index, reading ->
                val x = index * stepX
                // Invert Y because canvas origin is top-left
                val normalizedY = (reading - minReading) / range
                val y = height - (normalizedY * height)
                
                if (index == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = Color(0xFF4CAF50), // Green color
                style = Stroke(width = 3.dp.toPx())
            )
        }
        
        // Simple labels
        Text(
            text = "${maxReading.toInt()} lx",
            modifier = Modifier.align(Alignment.TopStart),
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            text = "${minReading.toInt()} lx",
            modifier = Modifier.align(Alignment.BottomStart),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val fileName = "plant_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}