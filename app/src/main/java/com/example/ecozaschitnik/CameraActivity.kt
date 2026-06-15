package com.example.ecozaschitnik

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat

class CameraActivity : BaseEcoActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var previewView: PreviewView

    // Запрос разрешения на камеру
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera()
            } else {
                Toast.makeText(this, "Разрешение камеры отклонено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        bindToolbar(getString(R.string.title_camera))
        applyContentBottomInset(findViewById(R.id.bottomPanel))

        previewView = findViewById(R.id.cameraPreview)
        val btnShutter: ImageButton = findViewById(R.id.btnShutter)

        // Проверяем, есть ли уже разрешение
        if (hasCameraPermission()) {
            startCamera()
        } else {
            askCameraPermission()
        }

        btnShutter.setOnClickListener {
            takePhoto()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun askCameraPermission() {
        requestPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Превью камеры
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            // Модуль для съёмки фото
            imageCapture = ImageCapture.Builder().build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "Ошибка запуска камеры: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Имя файла для галереи
        val name = java.text.SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            java.util.Locale.US
        ).format(System.currentTimeMillis())

        // Настройки для сохранения в MediaStore (галерея)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Папка Pictures/EcoZaschitnik
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "Pictures/EcoZaschitnik"
                )
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Ошибка снимка: ${exc.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Фото сохранено в галерею",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}
