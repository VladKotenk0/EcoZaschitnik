package com.example.ecozaschitnik

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.ecozaschitnik.data.PhotoPreparer
import com.example.ecozaschitnik.location.AddressGeocoder
import com.example.ecozaschitnik.ui.report.ReportUiState
import com.example.ecozaschitnik.ui.report.ReportViewModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.io.File

class ReportActivity : BaseEcoActivity() {

    private val viewModel: ReportViewModel by viewModels()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLat: Double? = null
    private var currentLon: Double? = null

    private lateinit var etCoordinates: EditText
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var photoPreviewContainer: View
    private lateinit var btnRemovePhoto: ImageButton
    private lateinit var btnSend: Button
    private lateinit var btnNewReport: Button
    private lateinit var tvResult: TextView
    private lateinit var loadingOverlay: View
    private lateinit var etName: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnAttach: Button

    private var selectedPhotoFile: File? = null
    private var isPreparingPhoto = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@registerForActivityResult
        prepareAttachedPhoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        bindToolbar(getString(R.string.title_report))
        applyContentBottomInset(findViewById(R.id.scrollReport))

        etName = findViewById(R.id.etDumpName)
        etDescription = findViewById(R.id.etDescription)
        etCoordinates = findViewById(R.id.etCoordinates)
        ivPhotoPreview = findViewById(R.id.ivPhotoPreview)
        photoPreviewContainer = findViewById(R.id.photoPreviewContainer)
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto)
        btnAttach = findViewById(R.id.btnAttachPhoto)
        btnSend = findViewById(R.id.btnSendReport)
        btnNewReport = findViewById(R.id.btnNewReport)
        tvResult = findViewById(R.id.tvReportResult)
        loadingOverlay = findViewById(R.id.loadingOverlay)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationOrLoad()

        btnAttach.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRemovePhoto.setOnClickListener {
            clearAttachedPhoto()
        }

        btnSend.setOnClickListener {
            hideKeyboard()

            if (isPreparingPhoto) {
                Toast.makeText(this, getString(R.string.report_photo_preparing), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val name = etName.text.toString().trim()
            val userDescription = etDescription.text.toString().trim()
            val coordsText = etCoordinates.text.toString().trim()

            if (name.isEmpty() || userDescription.isEmpty()) {
                Toast.makeText(this, "Заполните название и описание!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val lat = currentLat
            val lon = currentLon

            if (lat == null || lon == null) {
                Toast.makeText(
                    this,
                    "Координаты ещё не определены. Попробуйте через пару секунд.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            setLoading(true)
            viewModel.submitReport(
                name = name,
                userDescription = userDescription,
                coordinatesText = coordsText,
                lat = lat,
                lon = lon,
                hasPhoto = selectedPhotoFile != null,
            )
        }

        btnNewReport.setOnClickListener {
            tvResult.text = ""
            viewModel.startNewReport()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        ReportUiState.Idle -> applyFormEditable(true)
                        ReportUiState.Loading -> setLoading(true)
                        is ReportUiState.Success -> {
                            setLoading(false)
                            tvResult.text = state.aiReport
                            clearForm(clearPhoto = true)
                            applyFormEditable(false)
                            btnSend.visibility = View.GONE
                            btnNewReport.visibility = View.VISIBLE
                            Toast.makeText(
                                this@ReportActivity,
                                getString(R.string.report_sent_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        is ReportUiState.Error -> {
                            setLoading(false)
                            applyFormEditable(true)
                            btnSend.visibility = View.VISIBLE
                            btnNewReport.visibility = View.GONE
                            tvResult.text = "Ошибка: ${state.message}"
                            AlertDialog.Builder(this@ReportActivity)
                                .setTitle(R.string.report_error_title)
                                .setMessage(state.message)
                                .setPositiveButton(android.R.string.ok, null)
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun prepareAttachedPhoto(sourceUri: Uri) {
        isPreparingPhoto = true
        btnAttach.isEnabled = false
        photoPreviewContainer.visibility = View.VISIBLE
        ivPhotoPreview.setImageDrawable(null)

        lifecycleScope.launch {
            try {
                val prepared = PhotoPreparer.prepareForUpload(this@ReportActivity, sourceUri)
                PhotoPreparer.deleteQuietly(selectedPhotoFile)
                selectedPhotoFile = prepared
                ivPhotoPreview.load(prepared) {
                    crossfade(true)
                    placeholder(R.drawable.bg_thumb_placeholder)
                    error(R.drawable.bg_thumb_placeholder)
                }
            } catch (e: Throwable) {
                clearAttachedPhoto()
                Toast.makeText(
                    this@ReportActivity,
                    getString(R.string.report_photo_prepare_error),
                    Toast.LENGTH_LONG,
                ).show()
            } finally {
                isPreparingPhoto = false
                if (viewModel.uiState.value !is ReportUiState.Loading &&
                    viewModel.uiState.value !is ReportUiState.Success
                ) {
                    btnAttach.isEnabled = true
                }
            }
        }
    }

    private fun clearAttachedPhoto() {
        PhotoPreparer.deleteQuietly(selectedPhotoFile)
        selectedPhotoFile = null
        ivPhotoPreview.setImageDrawable(null)
        photoPreviewContainer.visibility = View.GONE
    }

    private fun clearForm(clearPhoto: Boolean) {
        etName.text = null
        etDescription.text = null
        if (clearPhoto) {
            clearAttachedPhoto()
        }
    }

    override fun onDestroy() {
        val isSubmitting = viewModel.uiState.value is ReportUiState.Loading
        if (isFinishing && !isSubmitting) {
            PhotoPreparer.deleteQuietly(selectedPhotoFile)
        }
        super.onDestroy()
    }

    private fun applyFormEditable(editable: Boolean) {
        btnSend.isEnabled = editable
        btnSend.visibility = if (editable) View.VISIBLE else View.GONE
        btnAttach.isEnabled = editable
        btnRemovePhoto.isEnabled = editable
        etName.isEnabled = editable
        etDescription.isEnabled = editable
        if (editable) {
            btnNewReport.visibility = View.GONE
        }
    }

    private fun setLoading(loading: Boolean) {
        loadingOverlay.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) {
            btnSend.isEnabled = false
            btnAttach.isEnabled = false
            etName.isEnabled = false
            etDescription.isEnabled = false
        }
    }

    private fun requestLocationOrLoad() {
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
                LOCATION_PERMISSION_REQUEST
            )
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude

                etCoordinates.setText("${location.latitude}, ${location.longitude}")

                lifecycleScope.launch {
                    val addressText = AddressGeocoder.resolve(
                        this@ReportActivity,
                        location.latitude,
                        location.longitude,
                    )
                    if (!addressText.isNullOrEmpty()) {
                        etCoordinates.setText("$addressText (${location.latitude}, ${location.longitude})")
                    }
                }
            } else {
                etCoordinates.setText("Координаты не найдены")
            }
        }.addOnFailureListener {
            etCoordinates.setText("Ошибка получения координат")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults.any { it == PackageManager.PERMISSION_GRANTED }
        ) {
            requestLocationOrLoad()
        }
    }

    private fun hideKeyboard() {
        val view: View = currentFocus ?: return
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }
}
