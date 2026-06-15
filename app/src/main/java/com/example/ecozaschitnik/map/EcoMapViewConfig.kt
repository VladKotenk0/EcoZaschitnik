package com.example.ecozaschitnik.map

import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.CustomZoomButtonsDisplay
import org.osmdroid.views.MapView

object EcoMapViewConfig {

    /** Настройки карты: жесты и +/- в правом верхнем углу (не перекрывают легенду). */
    fun setup(mapView: MapView) {
        mapView.setMultiTouchControls(true)
        mapView.zoomController.apply {
            setVisibility(CustomZoomButtonsController.Visibility.ALWAYS)
            display.setPositions(
                true,
                CustomZoomButtonsDisplay.HorizontalPosition.RIGHT,
                CustomZoomButtonsDisplay.VerticalPosition.TOP,
            )
            display.setAdditionalPixelMargins(0f, 12f, 12f, 0f)
        }
    }
}
