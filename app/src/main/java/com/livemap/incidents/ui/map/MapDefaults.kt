package com.livemap.incidents.ui.map

import org.maplibre.android.geometry.LatLng

/** Shared identifiers and camera defaults for the incident map. */
object MapDefaults {

    /** Roughly the geographic centre of Morocco, framed so the whole country is visible. */
    val MOROCCO_CENTER = LatLng(31.7917, -7.0926)
    const val MOROCCO_ZOOM = 4.9
    const val CLUSTER_ZOOM_STEP = 2.0

    const val STYLE_URI = "asset://map_style.json"

    const val SOURCE_ID = "incidents-source"
    const val LAYER_UNCLUSTERED = "incidents-unclustered"
    const val LAYER_CLUSTERS = "incidents-clusters"
    const val LAYER_CLUSTER_COUNT = "incidents-cluster-count"

    /** Only font stack confirmed available on the demotiles glyph server. */
    const val GLYPH_FONT = "Noto Sans Regular"

    // Feature property keys — kept in one place so the layers and click handling agree.
    const val PROP_ID = "id"
    const val PROP_TITLE = "title"
    const val PROP_SEVERITY = "severity"
    const val PROP_CATEGORY = "category"
    const val PROP_CITY = "city"
}
