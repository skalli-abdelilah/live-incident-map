package com.livemap.incidents.ui.filters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.livemap.incidents.data.model.IncidentCategory
import com.livemap.incidents.data.model.Severity
import com.livemap.incidents.ui.common.color

/**
 * Filter bottom sheet: category (multi-select), severity (multi-select) and a date range.
 *
 * Every tap writes straight through to the shared filter repository, so the map and list
 * behind the sheet update live — the user sees the effect of a filter before dismissing it,
 * and there is no "apply" step to get out of sync with.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel = hiltViewModel(),
) {
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activePreset = viewModel.activePreset(filters)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Filters", style = MaterialTheme.typography.titleLarge)
                if (filters.isActive) {
                    TextButton(onClick = viewModel::clear) { Text("Clear all") }
                }
            }

            FilterGroup(title = "Category") {
                IncidentCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category in filters.categories,
                        onClick = { viewModel.toggleCategory(category) },
                        label = { Text(category.label) },
                    )
                }
            }

            FilterGroup(title = "Severity") {
                Severity.entries.forEach { severity ->
                    FilterChip(
                        selected = severity in filters.severities,
                        onClick = { viewModel.toggleSeverity(severity) },
                        label = { Text(severity.apiValue.replaceFirstChar { it.uppercase() }) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(severity.color()),
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(),
                    )
                }
            }

            FilterGroup(title = "Reported") {
                DateRangePreset.entries.forEach { preset ->
                    FilterChip(
                        selected = activePreset == preset,
                        onClick = { viewModel.selectDateRange(preset) },
                        label = { Text(preset.label) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}
