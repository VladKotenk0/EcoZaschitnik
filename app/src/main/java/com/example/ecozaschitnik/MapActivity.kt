package com.example.ecozaschitnik

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.ecozaschitnik.map.EcoMapTiles
import com.example.ecozaschitnik.map.EcoMapViewConfig
import com.example.ecozaschitnik.map.EcoMarkerClusterer
import com.example.ecozaschitnik.map.MapMarkerFactory
import com.example.ecozaschitnik.ui.EcoUi
import com.example.ecozaschitnik.ui.main.ReportStatus
import com.example.ecozaschitnik.ui.main.reportStatus
import com.example.ecozaschitnik.ui.map.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MapActivity : BaseEcoActivity() {

    private val viewModel: MapViewModel by viewModels()

    private lateinit var mapView: MapView
    private lateinit var markerClusterer: EcoMarkerClusterer
    private val markersByDumpId = mutableMapOf<String, Marker>()
    private lateinit var mapEventsOverlay: MapEventsOverlay
    private lateinit var emptyState: LinearLayout
    private lateinit var fabMyLocation: FloatingActionButton

    private lateinit var dumpDetailPanel: View
    private lateinit var viewDetailAccent: View
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailBadge: TextView
    private lateinit var tvDetailPhotoBadge: TextView
    private lateinit var tvDetailDate: TextView
    private lateinit var tvDetailDescription: TextView
    private lateinit var tvDetailAiReport: TextView
    private lateinit var tvDetailCoords: TextView
    private lateinit var ivDetailPhoto: ImageView
    private lateinit var btnDeleteDump: Button

    private var userLocationMarker: Marker? = null
    private var selectedDumpId: String? = null
    private var pendingFocusDump: DumpPoint? = null
    private var pendingFocusId: String? = null
    private var pendingFocusPoint: GeoPoint? = null
    private var mapDidFit = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 2001
        const val EXTRA_FOCUS_REPORT = "focus_report"
        @Deprecated("Use EXTRA_FOCUS_REPORT")
        const val EXTRA_FOCUS_LAT = "focus_lat"
        @Deprecated("Use EXTRA_FOCUS_REPORT")
        const val EXTRA_FOCUS_LON = "focus_lon"
        @Deprecated("Use EXTRA_FOCUS_REPORT")
        const val EXTRA_FOCUS_ID = "focus_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)
        Configuration.getInstance().load(ctx, prefs)
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_map)

        bindToolbar(getString(R.string.title_map))
        EcoUi.applyBottomInset(findViewById(R.id.mapCard))

        readFocusFromIntent()
        bindDetailPanel()

        mapView = findViewById(R.id.mapView)
        emptyState = findViewById(R.id.emptyState)
        markerClusterer = EcoMarkerClusterer(this)

        val btnAdminLogin: Button = findViewById(R.id.btnAdminLogin)
        val btnAdminLogout: Button = findViewById(R.id.btnAdminLogout)
        fabMyLocation = findViewById(R.id.fabMyLocation)

        fabMyLocation.setOnClickListener { centerOnMyLocation() }

        btnAdminLogin.setOnClickListener {
            UserRoleManager.setRole(this, UserRole.ADMIN)
            Toast.makeText(this, "Теперь вы администратор", Toast.LENGTH_SHORT).show()
            updateAdminActions()
            refreshMarkers(viewModel.dumps.value)
        }

        btnAdminLogout.setOnClickListener {
            UserRoleManager.setRole(this, UserRole.USER)
            Toast.makeText(this, "Режим администратора отключён", Toast.LENGTH_SHORT).show()
            updateAdminActions()
            refreshMarkers(viewModel.dumps.value)
        }

        applyMapStyle()

        EcoMapViewConfig.setup(mapView)
        mapView.controller.setZoom(EcoMapTiles.defaultZoom)
        mapView.controller.setCenter(GeoPoint(EcoMapTiles.defaultLat, EcoMapTiles.defaultLon))

        if (hasPendingFocus()) {
            mapDidFit = true
            applyPendingFocusPreview()
        }

        mapView.overlays.add(markerClusterer)

        mapView.addMapListener(object : MapListener {
            override fun onScroll(event: ScrollEvent?): Boolean = false

            override fun onZoom(event: ZoomEvent?): Boolean {
                collapseSpiderfiedCluster()
                return false
            }
        })

        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                collapseSpiderfiedCluster()
                hideDetailPanel()
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                p ?: return false

                if (!UserRoleManager.isAdmin(this@MapActivity)) {
                    Toast.makeText(
                        this@MapActivity,
                        "Добавлять точки с карты может только администратор. " +
                            "Обычный пользователь создаёт отчёты.",
                        Toast.LENGTH_LONG,
                    ).show()
                    return true
                }

                AlertDialog.Builder(this@MapActivity)
                    .setTitle("Добавить свалку")
                    .setMessage("Добавить свалку в точке:\n${p.latitude}, ${p.longitude}?")
                    .setPositiveButton("Добавить") { _, _ ->
                        viewModel.addDump(p.latitude, p.longitude)
                    }
                    .setNegativeButton("Отмена", null)
                    .show()

                return true
            }
        }

        mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(0, mapEventsOverlay)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.dumps.collect { dumps ->
                        refreshMarkers(dumps)
                    }
                }
                launch {
                    viewModel.message.collect { msg ->
                        if (msg != null) {
                            Toast.makeText(this@MapActivity, msg, Toast.LENGTH_SHORT).show()
                            viewModel.clearMessage()
                        }
                    }
                }
            }
        }

        viewModel.loadDumps()
    }

    override fun onResume() {
        super.onResume()
        applyMapStyle()
        mapView.onResume()
        viewModel.loadDumps()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    private fun applyMapStyle() {
        mapView.setTileSource(EcoMapTiles.forTheme(this))
    }

    private fun bindDetailPanel() {
        dumpDetailPanel = findViewById(R.id.dumpDetailPanel)
        viewDetailAccent = dumpDetailPanel.findViewById(R.id.viewDetailAccent)
        tvDetailTitle = dumpDetailPanel.findViewById(R.id.tvDetailTitle)
        tvDetailBadge = dumpDetailPanel.findViewById(R.id.tvDetailBadge)
        tvDetailPhotoBadge = dumpDetailPanel.findViewById(R.id.tvDetailPhotoBadge)
        tvDetailDate = dumpDetailPanel.findViewById(R.id.tvDetailDate)
        tvDetailDescription = dumpDetailPanel.findViewById(R.id.tvDetailDescription)
        tvDetailAiReport = dumpDetailPanel.findViewById(R.id.tvDetailAiReport)
        tvDetailCoords = dumpDetailPanel.findViewById(R.id.tvDetailCoords)
        ivDetailPhoto = dumpDetailPanel.findViewById(R.id.ivDetailPhoto)
        btnDeleteDump = dumpDetailPanel.findViewById(R.id.btnDeleteDump)

        dumpDetailPanel.findViewById<ImageButton>(R.id.btnCloseDetail).setOnClickListener {
            hideDetailPanel()
        }

        dumpDetailPanel.setOnClickListener { }

        btnDeleteDump.setOnClickListener {
            val dumpId = selectedDumpId ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("Удалить свалку?")
                .setMessage("Вы уверены, что хотите удалить эту свалку?")
                .setPositiveButton("Удалить") { _, _ ->
                    viewModel.deleteDump(dumpId)
                    hideDetailPanel()
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        updateAdminActions()
    }

    private fun updateAdminActions() {
        btnDeleteDump.visibility = if (UserRoleManager.isAdmin(this)) View.VISIBLE else View.GONE
    }

    private fun refreshMarkers(dumps: List<DumpPoint>) {
        val focusId = pendingFocusDump?.id ?: pendingFocusId ?: selectedDumpId

        collapseSpiderfiedCluster()
        markerClusterer.getItems().clear()
        markersByDumpId.clear()

        emptyState.visibility = if (dumps.isEmpty()) View.VISIBLE else View.GONE

        for (dump in dumps) {
            val isSelected = dump.id == focusId
            val sourceText = dump.reportText.ifBlank { dump.description }
            val preview = sourceText.take(200).let {
                if (sourceText.length > 200) "$it…" else it
            }

            val marker = Marker(mapView).apply {
                position = GeoPoint(dump.lat, dump.lon)
                title = dump.title
                snippet = preview
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = MapMarkerFactory.dotIcon(this@MapActivity, isSelected)
                relatedObject = dump
                setOnMarkerClickListener { clickedMarker, _ ->
                    val dumpPoint = clickedMarker.relatedObject as? DumpPoint
                        ?: return@setOnMarkerClickListener true
                    openReportOnMap(dumpPoint, animateMap = true, showPopup = true)
                    true
                }
            }

            markersByDumpId[dump.id] = marker
            markerClusterer.add(marker)
        }

        markerClusterer.invalidate()

        if (!mapView.overlays.contains(mapEventsOverlay)) {
            mapView.overlays.add(0, mapEventsOverlay)
        }

        if (!mapDidFit && dumps.isNotEmpty() && !hasPendingFocus()) {
            fitAllMarkers(dumps)
            mapDidFit = true
        }

        mapView.invalidate()

        if (focusId != null) {
            dumps.firstOrNull { it.id == focusId }?.let { dump ->
                openReportOnMap(dump, animateMap = hasPendingFocus(), showPopup = hasPendingFocus())
                clearPendingFocus()
                return
            }
        }

        if (hasPendingFocus()) {
            pendingFocusDump?.let { dump ->
                openReportOnMap(dump, animateMap = true, showPopup = false)
            } ?: pendingFocusPoint?.let { point ->
                mapView.controller.animateTo(point)
                mapView.controller.setZoom(17.0)
            }
        }
    }

    private fun fitAllMarkers(dumps: List<DumpPoint>) {
        if (dumps.isEmpty()) return
        val points = dumps.map { GeoPoint(it.lat, it.lon) }
        val bbox = BoundingBox.fromGeoPoints(points)
        mapView.post {
            mapView.zoomToBoundingBox(bbox.increaseByScale(1.12f), true)
        }
    }

    private fun openReportOnMap(dump: DumpPoint, animateMap: Boolean, showPopup: Boolean) {
        if (selectedDumpId != dump.id) {
            selectedDumpId?.let { previousId ->
                markersByDumpId[previousId]?.icon =
                    MapMarkerFactory.dotIcon(this, selected = false)
            }
            selectedDumpId = dump.id
            markersByDumpId[dump.id]?.icon = MapMarkerFactory.dotIcon(this, selected = true)
        }

        bindDetailPanel(dump)
        dumpDetailPanel.visibility = View.VISIBLE

        if (animateMap) {
            mapView.controller.animateTo(GeoPoint(dump.lat, dump.lon))
            if (mapView.zoomLevelDouble < 16.0) {
                mapView.controller.setZoom(17.0)
            }
        }

        markerClusterer.invalidate()
        mapView.invalidate()

        if (showPopup) {
            revealMarkerPopup(dump.id)
        }
    }

    private fun selectDump(dump: DumpPoint, animateMap: Boolean) {
        openReportOnMap(dump, animateMap = animateMap, showPopup = true)
    }

    private fun bindDetailPanel(dump: DumpPoint) {
        tvDetailTitle.text = dump.title

        when (dump.reportStatus()) {
            ReportStatus.NEW -> {
                tvDetailBadge.setText(R.string.badge_new)
                tvDetailBadge.setBackgroundResource(R.drawable.bg_badge_new)
                tvDetailBadge.setTextColor(getColor(R.color.badge_new_text))
                viewDetailAccent.setBackgroundColor(getColor(R.color.badge_new_text))
            }
            ReportStatus.RECENT -> {
                tvDetailBadge.setText(R.string.badge_recent)
                tvDetailBadge.setBackgroundResource(R.drawable.bg_badge_recent)
                tvDetailBadge.setTextColor(getColor(R.color.badge_recent_text))
                viewDetailAccent.setBackgroundColor(getColor(R.color.badge_recent_text))
            }
            ReportStatus.OLD -> {
                tvDetailBadge.setText(R.string.badge_old)
                tvDetailBadge.setBackgroundResource(R.drawable.bg_badge_old)
                tvDetailBadge.setTextColor(getColor(R.color.badge_old_text))
                viewDetailAccent.setBackgroundColor(getColor(R.color.badge_old_text))
            }
        }

        tvDetailPhotoBadge.visibility =
            if (dump.hasPhotoAttachment()) View.VISIBLE else View.GONE

        tvDetailDate.text = formatDetailDate(dump.timestamp)

        val description = dump.description.ifBlank { "—" }
        tvDetailDescription.text = description

        val aiText = dump.reportText.ifBlank { description }
        tvDetailAiReport.text = aiText

        tvDetailCoords.text = dump.coordinatesText.ifBlank {
            String.format(Locale.US, "%.5f, %.5f", dump.lat, dump.lon)
        }

        val photoUrl = dump.photoUrl
        if (!photoUrl.isNullOrBlank()) {
            ivDetailPhoto.visibility = View.VISIBLE
            ivDetailPhoto.load(photoUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_thumb_placeholder)
                error(R.drawable.bg_thumb_placeholder)
            }
        } else {
            ivDetailPhoto.visibility = View.GONE
            ivDetailPhoto.setImageDrawable(null)
        }

        updateAdminActions()
    }

    private fun hideDetailPanel() {
        selectedDumpId?.let { id ->
            markersByDumpId[id]?.icon = MapMarkerFactory.dotIcon(this, selected = false)
        }
        selectedDumpId = null
        dumpDetailPanel.visibility = View.GONE
        collapseSpiderfiedCluster()
        markerClusterer.invalidate()
        mapView.invalidate()
    }

    private fun collapseSpiderfiedCluster() {
        if (!markerClusterer.isSpiderfied) return
        markerClusterer.unspiderfy()
        markerClusterer.invalidate()
        mapView.invalidate()
    }

    private fun readFocusFromIntent() {
        pendingFocusDump = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_FOCUS_REPORT, DumpPoint::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_FOCUS_REPORT)
        }

        pendingFocusDump?.let { dump ->
            pendingFocusId = dump.id
            pendingFocusPoint = GeoPoint(dump.lat, dump.lon)
            return
        }

        if (!intent.hasExtra(EXTRA_FOCUS_LAT) || !intent.hasExtra(EXTRA_FOCUS_LON)) return
        pendingFocusPoint = GeoPoint(
            intent.getDoubleExtra(EXTRA_FOCUS_LAT, 0.0),
            intent.getDoubleExtra(EXTRA_FOCUS_LON, 0.0),
        )
        pendingFocusId = intent.getStringExtra(EXTRA_FOCUS_ID)
    }

    private fun hasPendingFocus(): Boolean =
        pendingFocusDump != null || pendingFocusId != null || pendingFocusPoint != null

    private fun applyPendingFocusPreview() {
        val dump = pendingFocusDump ?: return
        selectedDumpId = dump.id
        bindDetailPanel(dump)
        dumpDetailPanel.visibility = View.VISIBLE
        mapView.post {
            mapView.controller.setCenter(GeoPoint(dump.lat, dump.lon))
            mapView.controller.setZoom(17.0)
        }
    }

    private fun revealMarkerPopup(dumpId: String) {
        mapView.post {
            markerClusterer.invalidate()
            mapView.post {
                val cluster = findClusterContaining(dumpId)
                if (cluster != null && cluster.size > 1) {
                    markerClusterer.zoomOnCluster(mapView, cluster)
                    mapView.postDelayed({ showMarkerPopup(dumpId) }, 400)
                } else {
                    showMarkerPopup(dumpId)
                }
            }
        }
    }

    private fun findClusterContaining(dumpId: String): StaticCluster? {
        for (cluster in markerClusterer.reversedClusters()) {
            for (i in 0 until cluster.size) {
                val dump = cluster.getItem(i).relatedObject as? DumpPoint ?: continue
                if (dump.id == dumpId) return cluster
            }
        }
        return null
    }

    private fun showMarkerPopup(dumpId: String) {
        markersByDumpId[dumpId]?.showInfoWindow()
    }

    private fun clearPendingFocus() {
        pendingFocusDump = null
        pendingFocusId = null
        pendingFocusPoint = null
    }

    private fun formatDetailDate(timestamp: Long?): String {
        if (timestamp == null) return ""
        return SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru")).format(Date(timestamp))
    }

    private fun showUserLocationMarker(point: GeoPoint) {
        userLocationMarker?.let { mapView.overlays.remove(it) }
        userLocationMarker = Marker(mapView).apply {
            position = point
            title = getString(R.string.map_you_here)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = MapMarkerFactory.youAreHereIcon(this@MapActivity)
        }
        mapView.overlays.add(userLocationMarker)
        mapView.invalidate()
    }

    private fun centerOnMyLocation() {
        val fine = Manifest.permission.ACCESS_FINE_LOCATION
        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
        val fineGranted =
            ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED
        val coarseGranted =
            ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(fine, coarse),
                LOCATION_PERMISSION_REQUEST,
            )
            return
        }

        LocationServices.getFusedLocationProviderClient(this)
            .lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val point = GeoPoint(location.latitude, location.longitude)
                    showUserLocationMarker(point)
                    mapView.controller.animateTo(point)
                    mapView.controller.setZoom(16.0)
                } else {
                    Toast.makeText(this, "Не удалось определить местоположение", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Ошибка геолокации", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        ) {
            centerOnMyLocation()
        }
    }
}
