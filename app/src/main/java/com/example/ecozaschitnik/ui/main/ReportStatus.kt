package com.example.ecozaschitnik.ui.main

import com.example.ecozaschitnik.DumpPoint

enum class ReportStatus {
    NEW,
    RECENT,
    OLD,
}

private const val DAYS_NEW = 7L
private const val DAYS_RECENT = 30L

fun DumpPoint.reportStatus(): ReportStatus {
    val ts = timestamp ?: return ReportStatus.NEW
    val days = (System.currentTimeMillis() - ts) / (24 * 60 * 60 * 1000)
    return when {
        days <= DAYS_NEW -> ReportStatus.NEW
        days <= DAYS_RECENT -> ReportStatus.RECENT
        else -> ReportStatus.OLD
    }
}
