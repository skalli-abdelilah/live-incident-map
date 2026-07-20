package com.livemap.incidents.ui.map

import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.livemap.incidents.data.model.Incident
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonSource

/**
 * Full-screen incident map.
 *
 * Camera handling is deliberate: the initial position is set **once**, when the style first
 * loads. Later data changes only call [GeoJsonSource.setGeoJson], which swaps the rendered
 * features without touching the camera — so incidents arriving live never yank the operator
 * away from the area they are looking at.
 */
@Composable
fun IncidentMap(
    incidents: List<Incident>,
    onIncidentClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mapView = rememberMapViewWithLifecycle()
    var style by remember { mutableStateOf<Style?>(null) }
    val currentOnIncidentClick by rememberUpdatedState(onIncidentClick)

    // The MapView itself cannot survive leaving composition (navigating to the detail screen)
    // or a configuration change, so the *camera* is hoisted into saveable state instead.
    // Without this the map silently re-frames Morocco every time the user comes back.
    var savedLat by rememberSaveable { mutableDoubleStateOf(Double.NaN) }
    var savedLng by rememberSaveable { mutableDoubleStateOf(Double.NaN) }
    var savedZoom by rememberSaveable { mutableDoubleStateOf(Double.NaN) }

    AndroidView(factory = { mapView }, modifier = modifier)

    // Runs once per MapView: acquire the map, load the style and layers, then frame Morocco.
    LaunchedEffect(mapView) {
        val map = mapView.awaitMap()

        val loadedStyle = map.awaitStyle(MapDefaults.STYLE_URI)
        loadedStyle.addIncidentLayers()
        style = loadedStyle

        // Framed *after* the style resolves: applying a style re-applies its own default
        // camera, which would otherwise discard a position set beforehand.
        // Restores where the user was, falling back to the country view on a cold start.
        val target = if (savedLat.isNaN()) {
            MapDefaults.MOROCCO_CENTER
        } else {
            LatLng(savedLat, savedLng)
        }
        val zoom = if (savedZoom.isNaN()) MapDefaults.MOROCCO_ZOOM else savedZoom
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, zoom))

        // Record the viewport whenever the user stops moving, so it can be restored later.
        map.addOnCameraIdleListener {
            val position = map.cameraPosition
            savedLat = position.target?.latitude ?: return@addOnCameraIdleListener
            savedLng = position.target?.longitude ?: return@addOnCameraIdleListener
            savedZoom = position.zoom
        }

        map.addOnMapClickListener { point ->
            map.handleTap(point, currentOnIncidentClick)
        }
    }

    // Re-runs whenever the incident set changes — data only, camera untouched.
    LaunchedEffect(style, incidents) {
        val source = style?.getSourceAs<GeoJsonSource>(MapDefaults.SOURCE_ID) ?: return@LaunchedEffect
        source.setGeoJson(incidents.toFeatureCollection())
    }
}

/**
 * Resolves a tap into either "zoom into this cluster" or "open this incident", by asking the
 * renderer which features sit under the touch point.
 */
private fun MapLibreMap.handleTap(point: LatLng, onIncidentClick: (String) -> Unit): Boolean {
    val screenPoint = projection.toScreenLocation(point)
    val touchArea = screenPoint.touchRect()

    // A cluster was tapped: zoom in so it breaks apart.
    if (queryRenderedFeatures(touchArea, MapDefaults.LAYER_CLUSTERS).isNotEmpty()) {
        animateCamera(
            CameraUpdateFactory.newLatLngZoom(point, cameraPosition.zoom + MapDefaults.CLUSTER_ZOOM_STEP),
        )
        return true
    }

    // An individual incident was tapped.
    val incidentId = queryRenderedFeatures(touchArea, MapDefaults.LAYER_UNCLUSTERED)
        .firstOrNull()
        ?.getStringProperty(MapDefaults.PROP_ID)
        ?: return false

    onIncidentClick(incidentId)
    return true
}

/** Generous hit box so small markers stay tappable with a finger. */
private fun PointF.touchRect(): RectF = RectF(
    x - TOUCH_SLOP_PX,
    y - TOUCH_SLOP_PX,
    x + TOUCH_SLOP_PX,
    y + TOUCH_SLOP_PX,
)

private const val TOUCH_SLOP_PX = 24f
