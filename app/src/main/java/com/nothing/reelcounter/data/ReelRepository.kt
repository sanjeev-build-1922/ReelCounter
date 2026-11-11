package com.nothing.reelcounter.data

import android.content.Context
import android.content.SharedPreferences
import com.nothing.reelcounter.widget.ReelGlanceWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DailyBreakdown(
    val instagramCount: Int = 0,
    val youtubeCount: Int = 0
) {
    val total: Int get() = instagramCount + youtubeCount

    val instagramRatio: Float
        get() = if (total > 0) instagramCount.toFloat() / total.toFloat() else 0f

    val youtubeRatio: Float
        get() = if (total > 0) youtubeCount.toFloat() / total.toFloat() else 0f
}

class ReelRepository(
    private val reelDao: ReelDao,
    private val appContext: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _hudEnabled = MutableStateFlow(prefs.getBoolean(KEY_HUD_ENABLED, true))
    val hudEnabled: StateFlow<Boolean> = _hudEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean(KEY_HAPTICS_ENABLED, true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _dailyLimit = MutableStateFlow(prefs.getInt(KEY_DAILY_LIMIT, 100))
    val dailyLimit: StateFlow<Int> = _dailyLimit.asStateFlow()

    private val _activeForegroundPlatform = MutableStateFlow<ReelPlatform?>(null)
    val activeForegroundPlatform: StateFlow<ReelPlatform?> = _activeForegroundPlatform.asStateFlow()

    private val _accessibilityConnected = MutableStateFlow(false)
    val accessibilityConnected: StateFlow<Boolean> = _accessibilityConnected.asStateFlow()

    fun todayDateKey(): String {
        return LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun observeTodayCount(): Flow<Int> {
        return reelDao.observeDailyTotal(todayDateKey())
    }

    suspend fun getTodayCountSnapshot(): Int {
        return reelDao.getDailyTotalSnapshot(todayDateKey())
    }

    fun observeTodayBreakdown(): Flow<DailyBreakdown> {
        return reelDao.observeDailyBreakdown(todayDateKey()).map { tuples ->
            var ig = 0
            var yt = 0
            for (tuple in tuples) {
                when (ReelPlatform.fromId(tuple.platform)) {
                    ReelPlatform.INSTAGRAM -> ig += tuple.total
                    ReelPlatform.YOUTUBE -> yt += tuple.total
                }
            }
            DailyBreakdown(instagramCount = ig, youtubeCount = yt)
        }
    }

    fun observeAllTimeTotal(): Flow<Int> {
        return reelDao.observeAllTimeTotal()
    }

    fun observeRecentLogs(limit: Int = 25): Flow<List<ReelEntity>> {
        return reelDao.observeRecentLogs(limit)
    }

    fun observeRecentDailyTotals(days: Int = 7): Flow<List<DailyCountTuple>> {
        return reelDao.observeRecentDailyTotals(days)
    }

    suspend fun recordReelSwipe(platform: ReelPlatform, count: Int = 1) {
        val now = System.currentTimeMillis()
        val entity = ReelEntity(
            timestamp = now,
            platform = platform.id,
            count = count,
            dateKey = todayDateKey()
        )
        reelDao.insertReel(entity)
        ReelGlanceWidget.updateAllWidgets(appContext)
    }

    fun recordReelSwipeAsync(platform: ReelPlatform, count: Int = 1) {
        scope.launch {
            recordReelSwipe(platform, count)
        }
    }

    suspend fun resetTodayCount() {
        reelDao.clearDay(todayDateKey())
        ReelGlanceWidget.updateAllWidgets(appContext)
    }

    suspend fun clearAllHistory() {
        reelDao.clearAll()
        ReelGlanceWidget.updateAllWidgets(appContext)
    }

    fun setHudEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HUD_ENABLED, enabled).apply()
        _hudEnabled.value = enabled
    }

    fun setHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTICS_ENABLED, enabled).apply()
        _hapticsEnabled.value = enabled
    }

    fun setDailyLimit(limit: Int) {
        val coerced = limit.coerceIn(10, 1000)
        prefs.edit().putInt(KEY_DAILY_LIMIT, coerced).apply()
        _dailyLimit.value = coerced
    }

    fun setActiveForegroundPlatform(platform: ReelPlatform?) {
        _activeForegroundPlatform.value = platform
    }

    fun setAccessibilityConnected(connected: Boolean) {
        _accessibilityConnected.value = connected
    }

    fun saveHudPosition(x: Int, y: Int) {
        prefs.edit()
            .putInt(KEY_HUD_X, x)
            .putInt(KEY_HUD_Y, y)
            .putBoolean(KEY_HUD_HAS_CUSTOM_POS, true)
            .apply()
    }

    fun getHudPosition(): Pair<Int, Int>? {
        if (!prefs.getBoolean(KEY_HUD_HAS_CUSTOM_POS, false)) return null
        return prefs.getInt(KEY_HUD_X, 24) to prefs.getInt(KEY_HUD_Y, 140)
    }

    companion object {
        private const val PREFS_NAME = "reel_counter_prefs"
        private const val KEY_HUD_ENABLED = "hud_enabled"
        private const val KEY_HAPTICS_ENABLED = "haptics_enabled"
        private const val KEY_DAILY_LIMIT = "daily_limit"
        private const val KEY_HUD_X = "hud_x"
        private const val KEY_HUD_Y = "hud_y"
        private const val KEY_HUD_HAS_CUSTOM_POS = "hud_has_custom_pos"
    }
}
