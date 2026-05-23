package com.runner.ui.history

data class RunActivity(
    val id: String,
    val date: String,
    val duration: String,
    val distanceKm: String,
    val paceMinKm: String,
    val positions: List<Position>
)
