package com.example.ecozaschitnik.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecozaschitnik.ai.AiRepository
import com.example.ecozaschitnik.data.DumpRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ReportViewModel(
    private val dumpRepository: DumpRepository = DumpRepository(),
    private val aiRepository: AiRepository = AiRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    fun submitReport(
        name: String,
        userDescription: String,
        coordinatesText: String,
        lat: Double,
        lon: Double,
        hasPhoto: Boolean,
    ) {
        val current = _uiState.value
        if (current is ReportUiState.Loading || current is ReportUiState.Success) return

        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading
            try {
                val fullDescription = buildString {
                    append("Название свалки: $name.\n")
                    append("Краткое описание от пользователя: $userDescription.\n")
                    if (coordinatesText.isNotEmpty()) {
                        append("Текстовое описание места: $coordinatesText.\n")
                    }
                    if (hasPhoto) {
                        append("К отчёту прикреплено фото свалки для проверки на месте.\n")
                    }
                    append("Координаты места: $lat, $lon.")
                }

                withContext(Dispatchers.IO) {
                    val aiReport = aiRepository.createReport(fullDescription, lat, lon)
                    dumpRepository.saveReport(
                        name = name,
                        userDescription = userDescription,
                        aiReport = aiReport,
                        lat = lat,
                        lon = lon,
                        coordinatesText = coordinatesText,
                        hasPhoto = hasPhoto,
                    )
                    aiReport
                }.also { aiReport ->
                    _uiState.value = ReportUiState.Success(aiReport)
                }
            } catch (e: Throwable) {
                _uiState.value = ReportUiState.Error(mapSubmitError(e))
            }
        }
    }

    fun startNewReport() {
        _uiState.value = ReportUiState.Idle
    }

    private fun mapSubmitError(error: Throwable): String = when (error) {
        is UnknownHostException, is SocketTimeoutException, is IOException ->
            "Нет связи с сервером. Проверьте интернет и попробуйте снова."
        else -> error.message ?: "Неизвестная ошибка при отправке отчёта"
    }
}

sealed class ReportUiState {
    data object Idle : ReportUiState()
    data object Loading : ReportUiState()
    data class Success(val aiReport: String) : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}
