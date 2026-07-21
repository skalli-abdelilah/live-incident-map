package com.livemap.incidents.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemap.incidents.data.model.Severity
import com.livemap.incidents.ui.common.ErrorState
import com.livemap.incidents.ui.common.NewIncidentsPill
import com.livemap.incidents.ui.common.OfflineBanner
import com.livemap.incidents.ui.common.color
import com.livemap.incidents.ui.filters.FilterButton
import com.livemap.incidents.ui.filters.FilterViewModel

@Composable
fun MapScreen(
    onIncidentClick: (String) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = hiltViewModel(),
    filterViewModel: FilterViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filters by filterViewModel.filters.collectAsStateWithLifecycle()
    val unseenCount by viewModel.unseenCount.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        when (val s = state) {
            MapUiState.Loading -> LoadingState()

            is MapUiState.Error -> ErrorState(message = s.message, onRetry = viewModel::refresh)

            is MapUiState.Content -> {
                IncidentMap(
                    incidents = s.incidents,
                    onIncidentClick = onIncidentClick,
                    modifier = Modifier.fillMaxSize(),
                )

                // The empty state is overlaid rather than replacing the map, so the
                // operator keeps their spatial context while adjusting filters.
                if (s.incidents.isEmpty()) EmptyOverlay()

                Column(modifier = Modifier.align(Alignment.TopStart)) {
                    OfflineBanner(isOffline = isOffline)
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterButton(activeCount = filters.activeCount, onClick = onOpenFilters)
                        IncidentCountBadge(
                            count = s.incidents.size,
                            totalCount = s.totalCount,
                            isFiltered = s.isFiltered,
                        )
                    }
                }

                SeverityLegend(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))

                // Purely informational: the new incidents are already on the map. Tapping
                // only clears the counter — the camera is never moved on the user's behalf.
                NewIncidentsPill(
                    count = unseenCount,
                    onClick = viewModel::acknowledgeNewIncidents,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = "Loading incidents…",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/** Overlaid rather than replacing the map, so the operator keeps their spatial context. */
@Composable
private fun EmptyOverlay() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 3.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        ) {
            Text(
                text = "No incidents match your filters",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun IncidentCountBadge(
    count: Int,
    totalCount: Int,
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 3.dp,
    ) {
        Text(
            // When filtered, show the denominator so the operator knows how much is hidden.
            text = if (isFiltered) "$count of $totalCount incidents" else "$count incidents",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SeverityLegend(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Severity.entries.forEach { severity ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(severity.color()),
                    )
                    Text(
                        text = severity.apiValue,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }
    }
}
