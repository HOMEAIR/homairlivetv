package com.homeair.live.data

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String? = null,
    val startEpochMs: Long,
    val endEpochMs: Long
) {
    fun progressAt(nowMs: Long): Float {
        if (endEpochMs <= startEpochMs) return 0f
        return ((nowMs - startEpochMs).toFloat() / (endEpochMs - startEpochMs)).coerceIn(0f, 1f)
    }
}

data class EpgSchedule(
    val programsByChannel: Map<String, List<EpgProgram>>
) {
    fun current(channelId: String, nowMs: Long = System.currentTimeMillis()): EpgProgram? =
        programsByChannel[channelId]?.firstOrNull { nowMs in it.startEpochMs until it.endEpochMs }

    fun next(channelId: String, nowMs: Long = System.currentTimeMillis()): EpgProgram? =
        programsByChannel[channelId]?.firstOrNull { it.startEpochMs > nowMs }
}
