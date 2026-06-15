package com.example.ecozaschitnik



import android.Manifest

import android.content.pm.PackageManager

import android.location.Location

import android.os.Bundle

import android.view.View

import android.widget.Button

import android.widget.TextView

import android.widget.Toast

import androidx.activity.result.contract.ActivityResultContracts

import androidx.core.content.ContextCompat

import androidx.lifecycle.lifecycleScope

import com.example.ecozaschitnik.location.AddressGeocoder

import com.example.ecozaschitnik.map.EcoMapTiles

import com.example.ecozaschitnik.map.EcoMapViewConfig

import com.example.ecozaschitnik.map.MapMarkerFactory

import com.google.android.gms.location.FusedLocationProviderClient

import com.google.android.gms.location.LocationServices

import com.google.android.material.floatingactionbutton.FloatingActionButton

import org.osmdroid.config.Configuration

import org.osmdroid.util.GeoPoint

import org.osmdroid.views.MapView

import org.osmdroid.views.overlay.Marker

import org.osmdroid.views.overlay.Polygon

import kotlinx.coroutines.launch

import java.util.Locale



class LocationActivity : BaseEcoActivity() {



    private lateinit var fusedLocation: FusedLocationProviderClient

    private lateinit var tvAddress: TextView

    private lateinit var tvCoordinates: TextView

    private lateinit var mapView: MapView

    private lateinit var fabMyLocation: FloatingActionButton



    private var lastLocation: Location? = null

    private var locationMarker: Marker? = null

    private var accuracyCircle: Polygon? = null



    private val requestPermissionLauncher =

        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->

            if (granted) {

                getLocation()

            } else {

                Toast.makeText(this, "Разрешение на геолокацию отклонено", Toast.LENGTH_SHORT).show()

            }

        }



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)



        val ctx = applicationContext

        val prefs = ctx.getSharedPreferences("osmdroid_prefs", MODE_PRIVATE)

        Configuration.getInstance().load(ctx, prefs)

        Configuration.getInstance().userAgentValue = packageName



        setContentView(R.layout.activity_location)



        bindToolbar(getString(R.string.title_location))

        applyContentBottomInset(findViewById(R.id.mapCard))



        fusedLocation = LocationServices.getFusedLocationProviderClient(this)

        tvAddress = findViewById(R.id.tvAddress)

        tvCoordinates = findViewById(R.id.tvCoordinates)

        mapView = findViewById(R.id.locationMapView)

        val btnGetLocation: Button = findViewById(R.id.btnGetLocation)

        fabMyLocation = findViewById(R.id.fabMyLocation)



        setupMap()



        btnGetLocation.setOnClickListener { askPermissionAndGetLocation() }

        fabMyLocation.setOnClickListener { goToMyLocation() }

    }



    private fun setupMap() {

        applyMapStyle()

        EcoMapViewConfig.setup(mapView)

        mapView.controller.setZoom(EcoMapTiles.defaultZoom)

        mapView.controller.setCenter(GeoPoint(EcoMapTiles.defaultLat, EcoMapTiles.defaultLon))

    }



    private fun applyMapStyle() {

        mapView.setTileSource(EcoMapTiles.forTheme(this))

    }



    private fun askPermissionAndGetLocation() {

        val fine = Manifest.permission.ACCESS_FINE_LOCATION

        val coarse = Manifest.permission.ACCESS_COARSE_LOCATION

        val fineGranted = ContextCompat.checkSelfPermission(this, fine) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(this, coarse) == PackageManager.PERMISSION_GRANTED



        when {

            fineGranted || coarseGranted -> getLocation()

            else -> requestPermissionLauncher.launch(fine)

        }

    }



    private fun goToMyLocation() {

        val loc = lastLocation

        if (loc != null) {

            centerMapOn(loc, animate = true)

        } else {

            askPermissionAndGetLocation()

        }

    }



    private fun getLocation() {

        fusedLocation.lastLocation

            .addOnSuccessListener { location ->

                if (location != null) {

                    lastLocation = location

                    showCoordinates(location)

                    resolveAddress(location)

                    updateMapWithLocation(location)

                } else {

                    tvAddress.visibility = View.GONE

                    tvCoordinates.text = "Не удалось получить геолокацию"

                }

            }

            .addOnFailureListener {

                tvAddress.visibility = View.GONE

                tvCoordinates.text = "Ошибка при получении геолокации"

            }

    }



    private fun showCoordinates(location: Location) {

        tvCoordinates.text = String.format(

            Locale.getDefault(),

            "%.6f, %.6f · ±%d м",

            location.latitude,

            location.longitude,

            location.accuracy.toInt(),

        )

    }



    private fun resolveAddress(location: Location) {

        tvAddress.text = getString(R.string.location_resolving_address)

        tvAddress.visibility = View.VISIBLE



        lifecycleScope.launch {

            val address = AddressGeocoder.resolve(

                this@LocationActivity,

                location.latitude,

                location.longitude,

            )

            if (!address.isNullOrBlank()) {

                tvAddress.text = address

                tvAddress.visibility = View.VISIBLE

                locationMarker?.snippet = address

                mapView.invalidate()

            } else {

                tvAddress.visibility = View.GONE

            }

        }

    }



    private fun updateMapWithLocation(location: Location) {

        val point = GeoPoint(location.latitude, location.longitude)



        if (locationMarker == null) {

            locationMarker = Marker(mapView).apply {

                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                title = getString(R.string.map_you_here)

                icon = MapMarkerFactory.youAreHereIcon(this@LocationActivity)

            }

            mapView.overlays.add(locationMarker)

        }

        locationMarker?.position = point



        val radius = location.accuracy.toDouble()

        if (accuracyCircle == null) {

            accuracyCircle = Polygon(mapView).apply {

                fillPaint.apply {

                    color = 0x333388FF

                    style = android.graphics.Paint.Style.FILL

                }

                outlinePaint.apply {

                    color = 0xFF3388FF.toInt()

                    strokeWidth = 2f

                    style = android.graphics.Paint.Style.STROKE

                }

            }

            mapView.overlays.add(0, accuracyCircle)

        }

        accuracyCircle?.points = Polygon.pointsAsCircle(point, radius)



        centerMapOn(location, animate = true)

        mapView.invalidate()

    }



    private fun centerMapOn(location: Location, animate: Boolean) {

        val point = GeoPoint(location.latitude, location.longitude)

        if (animate) {

            mapView.controller.animateTo(point)

        } else {

            mapView.controller.setCenter(point)

        }

        mapView.controller.setZoom(16.0)

    }



    override fun onResume() {

        super.onResume()

        applyMapStyle()

        mapView.onResume()

    }



    override fun onPause() {

        super.onPause()

        mapView.onPause()

    }

}


