package com.example.ecozaschitnik.ui.main

import com.example.ecozaschitnik.DumpPoint

data class MainStats(
    val total: Int = 0,
    val recent30Days: Int = 0,
    val withPhoto: Int = 0,
)

data class MainUiState(
    val isLoading: Boolean = true,
    val stats: MainStats = MainStats(),
    val recentReports: List<DumpPoint> = emptyList(),
    val loadError: String? = null,
)
