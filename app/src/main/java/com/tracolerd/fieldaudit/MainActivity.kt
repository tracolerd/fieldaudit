package com.tracolerd.fieldaudit
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tracolerd.fieldaudit.ui.InspectionViewModel
class MainActivity : ComponentActivity() {
    private val viewModel: InspectionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    InspectionScreen(viewModel)
                }
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionScreen(viewModel: InspectionViewModel) {
    val inspections by viewModel.inspections.collectAsState()
    var textInput by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("FieldAudit: Offline Inspections") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Inspection Title (Offline Draft)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        viewModel.createNewDraft(textInput)
                        textInput = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Offline Draft")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Saved Local Drafts (${inspections.size})", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(inspections) { inspection ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = inspection.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Status: ${inspection.status}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "UUID: ${inspection.id}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}