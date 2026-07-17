package com.livemap.incidents.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemap.incidents.data.model.Incident

/**
 * Temporary verification screen for the data layer: shows the loaded incident count and a
 * preview list. Replaced by the map screen in a later feature.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (val s = state) {
            HomeUiState.Loading -> {
                CircularProgressIndicator()
                Text("Loading incidents…", style = MaterialTheme.typography.bodyMedium)
            }

            is HomeUiState.Error -> Text(
                text = "Error: ${s.message}",
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )

            is HomeUiState.Content -> {
                Text(
                    text = "${s.incidents.size} incidents loaded",
                    style = MaterialTheme.typography.titleLarge,
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(s.incidents.take(20), key = Incident::id) { incident ->
                        IncidentRow(incident)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun IncidentRow(incident: Incident) {
    Column {
        Text(
            text = "${incident.id} · ${incident.title}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = "${incident.category.label} · ${incident.severity.name.lowercase()} · ${incident.city}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
    }
}
