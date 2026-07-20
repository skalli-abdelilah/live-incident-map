package com.livemap.incidents.ui.map

import android.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.livemap.incidents.data.model.Severity
import com.livemap.incidents.ui.theme.SeverityCritical
import com.livemap.incidents.ui.theme.SeverityHigh
import com.livemap.incidents.ui.theme.SeverityLow
import com.livemap.incidents.ui.theme.SeverityMedium
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.FeatureCollection

/** Severity → colour, shared with the rest of the UI via the theme palette. */
fun Severity.markerColor(): Int = when (this) {
    Severity.LOW -> SeverityLow.toArgb()
    Severity.MEDIUM -> SeverityMedium.toArgb()
    Severity.HIGH -> SeverityHigh.toArgb()
    Severity.CRITICAL -> SeverityCritical.toArgb()
}

/**
 * Registers the clustered incident source and its three layers on the style.
 *
 * Clustering is configured on the *source* (`withCluster`), so MapLibre groups points itself
 * as the camera moves — no per-frame work on the main thread. The layers then style the two
 * kinds of features the source emits: clusters (which carry `point_count`) and individual
 * incidents (which don't).
 */
fun Style.addIncidentLayers() {
    addSource(
        GeoJsonSource(
            MapDefaults.SOURCE_ID,
            FeatureCollection.fromFeatures(emptyList()),
            GeoJsonOptions()
                .withCluster(true)
                .withClusterRadius(CLUSTER_RADIUS_DP)
                .withClusterMaxZoom(CLUSTER_MAX_ZOOM),
        ),
    )

    // Individual incidents: colour and size driven by the severity property.
    addLayer(
        CircleLayer(MapDefaults.LAYER_UNCLUSTERED, MapDefaults.SOURCE_ID)
            .withProperties(
                PropertyFactory.circleColor(severityColorExpression()),
                PropertyFactory.circleRadius(severityRadiusExpression()),
                PropertyFactory.circleStrokeWidth(1.5f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
                PropertyFactory.circleOpacity(0.95f),
            )
            .withFilter(Expression.not(Expression.has(POINT_COUNT))),
    )

    // Cluster bubbles: size and colour scale with how many incidents they contain.
    addLayer(
        CircleLayer(MapDefaults.LAYER_CLUSTERS, MapDefaults.SOURCE_ID)
            .withProperties(
                PropertyFactory.circleColor(clusterColorExpression()),
                PropertyFactory.circleRadius(clusterRadiusExpression()),
                PropertyFactory.circleOpacity(0.9f),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(Color.WHITE),
            )
            .withFilter(Expression.has(POINT_COUNT)),
    )

    // Count label drawn on top of each cluster bubble.
    addLayer(
        SymbolLayer(MapDefaults.LAYER_CLUSTER_COUNT, MapDefaults.SOURCE_ID)
            .withProperties(
                PropertyFactory.textField(Expression.get(POINT_COUNT_ABBREVIATED)),
                PropertyFactory.textFont(arrayOf(MapDefaults.GLYPH_FONT)),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(Color.WHITE),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true),
            )
            .withFilter(Expression.has(POINT_COUNT)),
    )
}

/**
 * Severity string → palette colour.
 *
 * Built with `switchCase` (condition, output, …, default) rather than `match`, whose
 * overloads are ambiguous about where the default argument sits — getting that wrong
 * silently produces an invalid expression and the spec default (black) is drawn instead.
 */
private fun severityColorExpression(): Expression = Expression.switchCase(
    isSeverity(Severity.CRITICAL), Expression.color(Severity.CRITICAL.markerColor()),
    isSeverity(Severity.HIGH), Expression.color(Severity.HIGH.markerColor()),
    isSeverity(Severity.MEDIUM), Expression.color(Severity.MEDIUM.markerColor()),
    Expression.color(Severity.LOW.markerColor()), // default
)

/** More severe incidents render larger, so they read first at a glance. */
private fun severityRadiusExpression(): Expression = Expression.switchCase(
    isSeverity(Severity.CRITICAL), Expression.literal(10f),
    isSeverity(Severity.HIGH), Expression.literal(8f),
    isSeverity(Severity.MEDIUM), Expression.literal(6.5f),
    Expression.literal(5f), // default
)

private fun isSeverity(severity: Severity): Expression = Expression.eq(
    Expression.get(MapDefaults.PROP_SEVERITY),
    Expression.literal(severity.apiValue),
)

private fun clusterColorExpression(): Expression = Expression.step(
    Expression.toNumber(Expression.get(POINT_COUNT)),
    Expression.color(SeverityLow.toArgb()),
    Expression.stop(25, Expression.color(SeverityMedium.toArgb())),
    Expression.stop(100, Expression.color(SeverityHigh.toArgb())),
    Expression.stop(400, Expression.color(SeverityCritical.toArgb())),
)

private fun clusterRadiusExpression(): Expression = Expression.step(
    Expression.toNumber(Expression.get(POINT_COUNT)),
    Expression.literal(16f),
    Expression.stop(25, Expression.literal(22f)),
    Expression.stop(100, Expression.literal(28f)),
    Expression.stop(400, Expression.literal(34f)),
)

private const val POINT_COUNT = "point_count"
private const val POINT_COUNT_ABBREVIATED = "point_count_abbreviated"
private const val CLUSTER_RADIUS_DP = 60
private const val CLUSTER_MAX_ZOOM = 12
