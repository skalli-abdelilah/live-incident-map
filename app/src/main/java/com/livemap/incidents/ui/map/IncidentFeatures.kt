package com.livemap.incidents.ui.map

import com.livemap.incidents.data.model.Incident
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

/**
 * Converts incidents into a GeoJSON [FeatureCollection].
 *
 * Handing MapLibre a GeoJSON source (rather than one Android view per marker) is what makes
 * thousands of points viable: clustering and rendering happen inside the map engine, so the
 * cost is independent of the number of incidents on the JVM side.
 */
fun List<Incident>.toFeatureCollection(): FeatureCollection =
    FeatureCollection.fromFeatures(map { it.toFeature() })

private fun Incident.toFeature(): Feature =
    Feature.fromGeometry(Point.fromLngLat(lng, lat)).apply {
        addStringProperty(MapDefaults.PROP_ID, id)
        addStringProperty(MapDefaults.PROP_TITLE, title)
        addStringProperty(MapDefaults.PROP_SEVERITY, severity.apiValue)
        addStringProperty(MapDefaults.PROP_CATEGORY, category.apiValue)
        addStringProperty(MapDefaults.PROP_CITY, city)
    }
