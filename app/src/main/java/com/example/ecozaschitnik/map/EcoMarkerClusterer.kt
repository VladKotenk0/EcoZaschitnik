package com.example.ecozaschitnik.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.MotionEvent
import com.example.ecozaschitnik.ui.ThemeManager
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class EcoMarkerClusterer(context: Context) : RadiusMarkerClusterer(context) {

    private val appContext = context.applicationContext
    private val originalPositions = LinkedHashMap<Marker, GeoPoint>()
    private var spiderCenter: GeoPoint? = null
    private var spiderLegEnds: List<GeoPoint> = emptyList()

    init {
        setRadius(52)
        setMaxClusteringZoomLevel(19)
        mAnchorV = Marker.ANCHOR_CENTER
        mAnchorU = Marker.ANCHOR_CENTER
    }

    val isSpiderfied: Boolean
        get() = originalPositions.isNotEmpty()

    override fun buildClusterMarker(cluster: StaticCluster?, mapView: MapView?): Marker {
        val marker = super.buildClusterMarker(cluster, mapView)
        val count = cluster?.size ?: 0
        marker.icon = MapMarkerFactory.clusterIcon(appContext, count)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        return marker
    }

    override fun clusterer(mapView: MapView): ArrayList<StaticCluster> {
        if (originalPositions.isNotEmpty()) {
            val clusters = ArrayList<StaticCluster>(mItems.size)
            for (marker in mItems) {
                val cluster = StaticCluster(marker.position)
                cluster.add(marker)
                clusters.add(cluster)
            }
            return clusters
        }
        return super.clusterer(mapView)
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (!shadow) {
            drawSpider(canvas, mapView)
        }
        super.draw(canvas, mapView, shadow)
    }

    override fun onSingleTapConfirmed(event: MotionEvent, mapView: MapView): Boolean {
        for (cluster in reversedClusters()) {
            val clusterMarker = cluster.marker ?: continue
            if (!clusterMarker.onSingleTapConfirmed(event, mapView)) continue

            if (cluster.size > 1) {
                if (shouldSpiderfy(cluster, mapView)) {
                    focusCluster(cluster, mapView)
                    spiderfy(cluster, mapView)
                } else {
                    clearSpiderVisuals()
                    unspiderfy()
                    zoomOnCluster(mapView, cluster)
                    zoomFurtherIfNeeded(cluster, mapView)
                }
            }
            return true
        }

        if (isSpiderfied) {
            clearSpiderVisuals()
            unspiderfy()
            invalidate()
        }
        return false
    }

    fun unspiderfy() {
        if (originalPositions.isEmpty()) return
        for ((marker, position) in originalPositions) {
            marker.position = position
        }
        originalPositions.clear()
        clearSpiderVisuals()
    }

    private fun shouldSpiderfy(cluster: StaticCluster, mapView: MapView): Boolean {
        if (mapView.zoomLevelDouble >= mMaxClusteringZoomLevel - 0.5) return true

        val bbox = cluster.boundingBox ?: return false
        val latSpan = abs(bbox.latNorth - bbox.latSouth)
        val lonSpan = abs(bbox.lonEast - bbox.lonWest)
        return latSpan < SAME_POINT_THRESHOLD && lonSpan < SAME_POINT_THRESHOLD
    }

    private fun focusCluster(cluster: StaticCluster, mapView: MapView) {
        mapView.controller.animateTo(cluster.position)
        if (mapView.zoomLevelDouble < 15.5) {
            mapView.controller.setZoom(16.0)
        }
    }

    private fun spiderfy(cluster: StaticCluster, mapView: MapView) {
        unspiderfy()

        val center = cluster.position
        val count = cluster.size
        if (count <= 1) return

        val radiusMeters = spiderRadiusMeters(count, mapView)
        val angleStep = 360.0 / count
        val legEnds = ArrayList<GeoPoint>(count)

        for (index in 0 until count) {
            val marker = cluster.getItem(index)
            originalPositions[marker] = marker.position
            val bearing = angleStep * index - 90.0
            val offset = offsetGeoPoint(center, radiusMeters, bearing)
            marker.position = offset
            legEnds.add(offset)
        }

        spiderCenter = center
        spiderLegEnds = legEnds
        invalidate()
    }

    private fun drawSpider(canvas: Canvas, mapView: MapView) {
        val center = spiderCenter ?: return
        if (spiderLegEnds.isEmpty()) return

        val projection = mapView.projection
        val centerPoint = Point()
        projection.toPixels(center, centerPoint)
        val density = mapView.context.resources.displayMetrics.density
        val isNight = ThemeManager.isNight(mapView.context)

        val hubRadius = 14f * density
        val hubPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isNight) {
                Color.argb(89, 127, 182, 63)
            } else {
                Color.argb(89, 94, 140, 97)
            }
            style = Paint.Style.FILL
        }
        canvas.drawCircle(
            centerPoint.x.toFloat(),
            centerPoint.y.toFloat(),
            hubRadius,
            hubPaint,
        )

        val legPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isNight) Color.argb(160, 170, 180, 170) else Color.argb(170, 130, 140, 130)
            strokeWidth = 1.25f * density
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

        for (end in spiderLegEnds) {
            val endPoint = Point()
            projection.toPixels(end, endPoint)
            canvas.drawLine(
                centerPoint.x.toFloat(),
                centerPoint.y.toFloat(),
                endPoint.x.toFloat(),
                endPoint.y.toFloat(),
                legPaint,
            )
        }
    }

    private fun clearSpiderVisuals() {
        spiderCenter = null
        spiderLegEnds = emptyList()
    }

    private fun zoomFurtherIfNeeded(cluster: StaticCluster, mapView: MapView) {
        val bbox = cluster.boundingBox ?: return
        val latSpan = abs(bbox.latNorth - bbox.latSouth)
        val lonSpan = abs(bbox.lonEast - bbox.lonWest)
        if (latSpan >= SAME_POINT_THRESHOLD || lonSpan >= SAME_POINT_THRESHOLD) return

        val targetZoom = (mapView.zoomLevelDouble + 2.0)
            .coerceAtMost(mMaxClusteringZoomLevel.toDouble())
        if (targetZoom > mapView.zoomLevelDouble) {
            mapView.controller.setZoom(targetZoom)
        }
    }

    private fun spiderRadiusMeters(count: Int, mapView: MapView): Double {
        val density = mapView.context.resources.displayMetrics.density
        val radiusPx = (34f + count * 7f) * density
        return radiusPx * metersPerPixel(mapView)
    }

    private fun metersPerPixel(mapView: MapView): Double {
        if (mRadiusInMeters > 0.0 && mRadiusInPixels > 0) {
            return mRadiusInMeters / mRadiusInPixels
        }

        val rect = mapView.getIntrinsicScreenRect(null)
        val screenWidth = rect.right - rect.left
        val screenHeight = rect.bottom - rect.top
        val diagonalPixels = sqrt(
            (screenWidth * screenWidth + screenHeight * screenHeight).toDouble(),
        )
        if (diagonalPixels <= 0.0) return 1.0
        return mapView.boundingBox.diagonalLengthInMeters / diagonalPixels
    }

    private fun offsetGeoPoint(center: GeoPoint, distanceMeters: Double, bearingDegrees: Double): GeoPoint {
        val earthRadius = 6_378_137.0
        val bearing = Math.toRadians(bearingDegrees)
        val angularDistance = distanceMeters / earthRadius
        val lat1 = Math.toRadians(center.latitude)
        val lon1 = Math.toRadians(center.longitude)

        val lat2 = asin(
            sin(lat1) * cos(angularDistance) +
                cos(lat1) * sin(angularDistance) * cos(bearing),
        )
        val lon2 = lon1 + atan2(
            sin(bearing) * sin(angularDistance) * cos(lat1),
            cos(angularDistance) - sin(lat1) * sin(lat2),
        )

        return GeoPoint(Math.toDegrees(lat2), Math.toDegrees(lon2))
    }

    companion object {
        private const val SAME_POINT_THRESHOLD = 0.00005
    }
}
