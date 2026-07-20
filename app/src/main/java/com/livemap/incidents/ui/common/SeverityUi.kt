package com.livemap.incidents.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livemap.incidents.data.model.Severity
import com.livemap.incidents.ui.theme.SeverityCritical
import com.livemap.incidents.ui.theme.SeverityHigh
import com.livemap.incidents.ui.theme.SeverityLow
import com.livemap.incidents.ui.theme.SeverityMedium
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Single source of truth for severity colour across map, list and detail. */
fun Severity.color(): Color = when (this) {
    Severity.LOW -> SeverityLow
    Severity.MEDIUM -> SeverityMedium
    Severity.HIGH -> SeverityHigh
    Severity.CRITICAL -> SeverityCritical
}

/** Coloured dot + label, reused wherever severity is shown. */
@Composable
fun SeverityBadge(severity: Severity, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(severity.color()),
        )
        Text(
            text = severity.apiValue.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = severity.color(),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

private val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm").withZone(ZoneId.systemDefault())

fun Instant.formatReported(): String = dateTimeFormatter.format(this)

/** Compact relative age, e.g. "3h ago" — what an operator scans for first. */
fun Instant.relativeAge(now: Instant = Instant.now()): String {
    val minutes = java.time.Duration.between(this, now).toMinutes()
    return when {
        minutes < 0 -> "scheduled"
        minutes < 1 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}
