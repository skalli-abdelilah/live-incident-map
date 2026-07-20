package com.livemap.incidents.ui.map

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Creates a [MapView] and forwards the host's lifecycle events to it.
 *
 * [MapView] is a classic Android View with a manual lifecycle contract — skipping these calls
 * leaks the GL surface. Keeping the instance in [remember] also means configuration-independent
 * recompositions never rebuild the map, so the user's pan/zoom survives them.
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapLibre.getInstance(context) // must run before any MapView is constructed
        MapView(context).apply {
            id = View.generateViewId()
            onCreate(null)
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }
    return mapView
}

/** Suspends until the map instance is ready, replacing the callback API. */
suspend fun MapView.awaitMap(): MapLibreMap = suspendCoroutine { continuation ->
    getMapAsync { map -> continuation.resume(map) }
}

/** Suspends until the given style has finished loading. */
suspend fun MapLibreMap.awaitStyle(uri: String): Style = suspendCoroutine { continuation ->
    setStyle(Style.Builder().fromUri(uri)) { style -> continuation.resume(style) }
}
