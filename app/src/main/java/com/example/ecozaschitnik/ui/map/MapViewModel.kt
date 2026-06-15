package com.example.ecozaschitnik.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecozaschitnik.DumpPoint
import com.example.ecozaschitnik.data.DumpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MapViewModel(
    private val dumpRepository: DumpRepository = DumpRepository(),
) : ViewModel() {

    private val _dumps = MutableStateFlow<List<DumpPoint>>(emptyList())
    val dumps: StateFlow<List<DumpPoint>> = _dumps.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadDumps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _dumps.value = dumpRepository.getAll()
            } catch (e: Exception) {
                _message.value = "Не удалось загрузить точки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addDump(lat: Double, lon: Double) {
        viewModelScope.launch {
            try {
                dumpRepository.addPoint(
                    lat = lat,
                    lon = lon,
                    title = "Свалка",
                    description = "Добавлено администратором с карты",
                )
                loadDumps()
                _message.value = "Точка добавлена"
            } catch (e: Exception) {
                _message.value = "Ошибка добавления: ${e.message}"
            }
        }
    }

    fun deleteDump(id: String) {
        viewModelScope.launch {
            try {
                dumpRepository.delete(id)
                loadDumps()
                _message.value = "Точка удалена"
            } catch (e: Exception) {
                _message.value = "Ошибка удаления: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
