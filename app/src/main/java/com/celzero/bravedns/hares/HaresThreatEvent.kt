package com.celzero.bravedns.hares
data class HaresThreatEvent(val url: String, val level: Level, val reasons: List<String>, val timestamp: Long = System.currentTimeMillis()) {
    enum class Level { LOW, MEDIUM, HIGH }
}
