package com.livemap.incidents.ui.filters

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Filter affordance with a badge showing how many constraints are active. */
@Composable
fun FilterButton(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BadgedBox(
        modifier = modifier,
        badge = {
            if (activeCount > 0) {
                Badge { Text(activeCount.toString()) }
            }
        },
    ) {
        IconButton(onClick = onClick) {
            Icon(Icons.Default.FilterList, contentDescription = "Filters")
        }
    }
}
