package com.example.ecozaschitnik.map

import android.content.Context
import com.example.ecozaschitnik.ui.ThemeManager
import org.osmdroid.tileprovider.tilesource.XYTileSource

object EcoMapTiles {

    private fun cartoSource(name: String, path: String) = XYTileSource(
        name,
        0,
        20,
        256,
        ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/$path/",
            "https://b.basemaps.cartocdn.com/$path/",
            "https://c.basemaps.cartocdn.com/$path/",
            "https://d.basemaps.cartocdn.com/$path/",
        ),
        "© OpenStreetMap © CARTO",
    )

    /** Центр карты по умолчанию — Ярославль, как на сайте. */
    const val defaultLat = 57.64
    const val defaultLon = 39.97
    const val defaultZoom = 10.0

    fun forTheme(context: Context) =
        if (ThemeManager.isNight(context)) {
            cartoSource("CartoDB.Dark", "dark_all")
        } else {
            cartoSource("CartoDB.Light", "light_all")
        }
}
