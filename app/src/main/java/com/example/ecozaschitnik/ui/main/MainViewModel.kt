package com.example.ecozaschitnik.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecozaschitnik.data.DumpRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val dumpRepository: DumpRepository = DumpRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadError = null) }
            try {
                val dumps = dumpRepository.getAll()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        stats = computeStats(dumps),
                        recentReports = dumps
                            .sortedByDescending { dump -> dump.timestamp ?: 0L }
                            .take(RECENT_LIMIT),
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loadError = e.message,
                    )
                }
            }
        }
    }

    private fun computeStats(dumps: List<com.example.ecozaschitnik.DumpPoint>): MainStats {
        val monthMs = 30L * 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        return MainStats(
            total = dumps.size,
            recent30Days = dumps.count { dump ->
                val ts = dump.timestamp ?: return@count false
                now - ts <= monthMs
            },
            withPhoto = dumps.count { it.hasPhotoAttachment() },
        )
    }

    companion object {
        private const val RECENT_LIMIT = 3
    }
}
