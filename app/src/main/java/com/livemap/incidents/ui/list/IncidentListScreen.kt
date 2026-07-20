package com.livemap.incidents.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemap.incidents.data.model.Incident
import com.livemap.incidents.ui.common.EmptyState
import com.livemap.incidents.ui.common.ErrorState
import com.livemap.incidents.ui.common.IncidentListSkeleton
import com.livemap.incidents.ui.common.SeverityBadge
import com.livemap.incidents.ui.common.relativeAge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncidentListScreen(
    onIncidentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IncidentListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh(isPullToRefresh = true) },
        modifier = modifier.fillMaxSize(),
    ) {
        when (val s = state) {
            ListUiState.Loading -> IncidentListSkeleton()

            is ListUiState.Error -> ErrorState(
                message = s.message,
                onRetry = { viewModel.refresh() },
            )

            is ListUiState.Content -> if (s.incidents.isEmpty()) {
                EmptyState()
            } else {
                IncidentList(
                    state = s,
                    listState = listState,
                    onIncidentClick = onIncidentClick,
                    onLoadMore = viewModel::loadMore,
                )
            }
        }
    }
}

@Composable
private fun IncidentList(
    state: ListUiState.Content,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onIncidentClick: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    // Infinite scroll: ask for another page once the last visible item nears the end.
    val shouldLoadMore by remember(state.incidents.size, state.canLoadMore) {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.canLoadMore && lastVisible >= state.incidents.lastIndex - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(state.incidents, key = Incident::id) { incident ->
            IncidentRow(incident = incident, onClick = { onIncidentClick(incident.id) })
            HorizontalDivider()
        }

        if (state.canLoadMore) {
            item(key = "loading-more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Loading more… (${state.incidents.size} of ${state.totalCount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

@Composable
private fun IncidentRow(incident: Incident, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SeverityBadge(incident.severity)
            Text(
                text = incident.reportedAt.relativeAge(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }
        Text(text = incident.title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "${incident.category.label} · ${incident.city}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        )
    }
}

private const val LOAD_MORE_THRESHOLD = 5
