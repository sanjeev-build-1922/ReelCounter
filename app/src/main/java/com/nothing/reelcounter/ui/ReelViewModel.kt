package com.nothing.reelcounter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nothing.reelcounter.data.DailyBreakdown
import com.nothing.reelcounter.data.DailyCountTuple
import com.nothing.reelcounter.data.ReelEntity
import com.nothing.reelcounter.data.ReelPlatform
import com.nothing.reelcounter.data.ReelRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReelDashboardUiState(
    val todayCount: Int = 0,
    val breakdown: DailyBreakdown = DailyBreakdown(),
    val allTimeTotal: Int = 0,
    val recentLogs: List<ReelEntity> = emptyList(),
    val recentDays: List<DailyCountTuple> = emptyList(),
    val hudEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val dailyLimit: Int = 100,
    val isAccessibilityEnabled: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val isNotificationPermissionGranted: Boolean = true
) {
    val estimatedMinutesToday: Int
        get() = ((todayCount * AVG_SECONDS_PER_REEL) / 60f).toInt()

    val limitProgress: Float
        get() = if (dailyLimit > 0) (todayCount.toFloat() / dailyLimit.toFloat()).coerceIn(0f, 1f) else 0f

    val isOverDailyLimit: Boolean
        get() = todayCount >= dailyLimit

    companion object {
        private const val AVG_SECONDS_PER_REEL = 22f
    }
}

private data class PermissionStates(
    val accessibility: Boolean = false,
    val overlay: Boolean = false,
    val notification: Boolean = true
)

class ReelViewModel(
    private val repository: ReelRepository
) : ViewModel() {

    private val permissionStates = MutableStateFlow(PermissionStates())

    private val coreTelemetryFlow = combine(
        repository.observeTodayCount(),
        repository.observeTodayBreakdown(),
        repository.observeAllTimeTotal(),
        repository.observeRecentLogs(limit = 15),
        repository.observeRecentDailyTotals(days = 7)
    ) { todayCount, breakdown, allTime, recentLogs, recentDays ->
        CoreTelemetry(
            todayCount = todayCount,
            breakdown = breakdown,
            allTimeTotal = allTime,
            recentLogs = recentLogs,
            recentDays = recentDays
        )
    }

    private val settingsFlow = combine(
        repository.hudEnabled,
        repository.hapticsEnabled,
        repository.dailyLimit,
        permissionStates
    ) { hud, haptics, limit, perms ->
        SettingsSnapshot(
            hudEnabled = hud,
            hapticsEnabled = haptics,
            dailyLimit = limit,
            permissions = perms
        )
    }

    val uiState: StateFlow<ReelDashboardUiState> = combine(
        coreTelemetryFlow,
        settingsFlow
    ) { telemetry, settings ->
        ReelDashboardUiState(
            todayCount = telemetry.todayCount,
            breakdown = telemetry.breakdown,
            allTimeTotal = telemetry.allTimeTotal,
            recentLogs = telemetry.recentLogs,
            recentDays = telemetry.recentDays,
            hudEnabled = settings.hudEnabled,
            hapticsEnabled = settings.hapticsEnabled,
            dailyLimit = settings.dailyLimit,
            isAccessibilityEnabled = settings.permissions.accessibility,
            isOverlayPermissionGranted = settings.permissions.overlay,
            isNotificationPermissionGranted = settings.permissions.notification
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ReelDashboardUiState()
    )

    fun refreshPermissions(
        isAccessibilityEnabled: Boolean,
        isOverlayGranted: Boolean,
        isNotificationGranted: Boolean
    ) {
        permissionStates.value = PermissionStates(
            accessibility = isAccessibilityEnabled,
            overlay = isOverlayGranted,
            notification = isNotificationGranted
        )
    }

    fun setHudEnabled(enabled: Boolean) {
        repository.setHudEnabled(enabled)
    }

    fun setHapticsEnabled(enabled: Boolean) {
        repository.setHapticsEnabled(enabled)
    }

    fun adjustDailyLimit(delta: Int) {
        val next = (repository.dailyLimit.value + delta).coerceIn(20, 500)
        repository.setDailyLimit(next)
    }

    fun simulateSwipe(platform: ReelPlatform) {
        viewModelScope.launch {
            repository.recordReelSwipe(platform = platform, count = 1)
        }
    }

    fun resetToday() {
        viewModelScope.launch {
            repository.resetTodayCount()
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }

    private data class CoreTelemetry(
        val todayCount: Int,
        val breakdown: DailyBreakdown,
        val allTimeTotal: Int,
        val recentLogs: List<ReelEntity>,
        val recentDays: List<DailyCountTuple>
    )

    private data class SettingsSnapshot(
        val hudEnabled: Boolean,
        val hapticsEnabled: Boolean,
        val dailyLimit: Int,
        val permissions: PermissionStates
    )

    class Factory(private val repository: ReelRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ReelViewModel::class.java)) {
                return ReelViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
