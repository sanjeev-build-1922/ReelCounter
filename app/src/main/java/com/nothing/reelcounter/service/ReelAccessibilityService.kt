package com.nothing.reelcounter.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.nothing.reelcounter.ReelCounterApp
import com.nothing.reelcounter.data.ReelPlatform

/**
 * Core AccessibilityService detecting reel/short-form video swipes in real time.
 * Coordinates FloatingHudService so the minimal overlay appears ONLY while actively in
 * Instagram Reels or YouTube Shorts, and disappears immediately when leaving.
 */
class ReelAccessibilityService : AccessibilityService() {

    private val repository by lazy { ReelCounterApp.instance.repository }

    private var lastRecordedSwipeElapsedMs: Long = 0L
    private var lastInstagramPageIndex: Int = -1
    private var lastYoutubePageIndex: Int = -1

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository.setAccessibilityConnected(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        val platform = ReelPlatform.fromPackageName(packageName)

        // Strict foreground window monitoring: if leaving Instagram/YouTube, hide overlay immediately
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (platform != null) {
                repository.setActiveForegroundPlatform(platform)
                syncFloatingHudVisibility(show = true)
            } else if (!isSystemOverlayOrKeyboardPackage(packageName)) {
                repository.setActiveForegroundPlatform(null)
                syncFloatingHudVisibility(show = false)
            }
        }

        // Only process scroll telemetry if inside Instagram or YouTube
        if (platform == null) return

        if (repository.activeForegroundPlatform.value != platform) {
            repository.setActiveForegroundPlatform(platform)
            syncFloatingHudVisibility(show = true)
        }

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                handleViewScrolled(event, platform)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handleWindowContentChanged(event, platform)
            }
            else -> Unit
        }
    }

    private fun handleViewScrolled(event: AccessibilityEvent, platform: ReelPlatform) {
        val nowElapsed = SystemClock.elapsedRealtime()
        if (nowElapsed - lastRecordedSwipeElapsedMs < DEBOUNCE_COOLDOWN_MS) {
            return
        }

        val isVerticalSwipe = isVerticalPagerOrReelScroll(event, platform)
        if (!isVerticalSwipe) return

        commitReelSwipe(platform, nowElapsed)
    }

    private fun handleWindowContentChanged(event: AccessibilityEvent, platform: ReelPlatform) {
        val nowElapsed = SystemClock.elapsedRealtime()
        if (nowElapsed - lastRecordedSwipeElapsedMs < DEBOUNCE_COOLDOWN_MS) {
            return
        }

        val sourceNode = event.source ?: return
        try {
            val viewId = sourceNode.viewIdResourceName?.lowercase().orEmpty()
            val className = event.className?.toString()?.lowercase().orEmpty()

            val isReelContainerTransition = when (platform) {
                ReelPlatform.INSTAGRAM -> {
                    (viewId.contains("clips_viewer_view_pager") ||
                        viewId.contains("reel_viewer") ||
                        viewId.contains("clips_video_container")) &&
                        (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0)
                }
                ReelPlatform.YOUTUBE -> {
                    (viewId.contains("reel_recycler") ||
                        viewId.contains("reel_player_page_container") ||
                        viewId.contains("shorts_page_container")) &&
                        (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_SUBTREE != 0)
                }
            }

            if (isReelContainerTransition && className.contains("viewpager")) {
                commitReelSwipe(platform, nowElapsed)
            }
        } finally {
            @Suppress("DEPRECATION")
            sourceNode.recycle()
        }
    }

    private fun isVerticalPagerOrReelScroll(
        event: AccessibilityEvent,
        platform: ReelPlatform
    ): Boolean {
        val className = event.className?.toString()?.lowercase().orEmpty()
        val deltaY = event.scrollDeltaY
        val deltaX = event.scrollDeltaX

        if (deltaX != 0 && kotlin.math.abs(deltaX) > kotlin.math.abs(deltaY)) {
            return false
        }

        val sourceNode = event.source
        var viewId = ""
        var isShortFormContext = false

        if (sourceNode != null) {
            try {
                viewId = sourceNode.viewIdResourceName?.lowercase().orEmpty()
                isShortFormContext = matchesShortFormViewId(viewId, platform) ||
                    hasShortFormAncestorOrChild(sourceNode, platform)
            } finally {
                @Suppress("DEPRECATION")
                sourceNode.recycle()
            }
        }

        val fromIndex = event.fromIndex
        val toIndex = event.toIndex
        val hasPageAdvance = fromIndex >= 0 && toIndex >= 0 && fromIndex != toIndex
        val hasSignificantVerticalDelta = kotlin.math.abs(deltaY) >= MIN_VERTICAL_SCROLL_DELTA_PX

        val isPagerOrRecycler = className.contains("viewpager") ||
            className.contains("recyclerview") ||
            className.contains("scrollable")

        if (!isPagerOrRecycler && !isShortFormContext) {
            return false
        }

        if (hasPageAdvance) {
            when (platform) {
                ReelPlatform.INSTAGRAM -> {
                    if (toIndex == lastInstagramPageIndex && !hasSignificantVerticalDelta) {
                        return false
                    }
                    lastInstagramPageIndex = toIndex
                }
                ReelPlatform.YOUTUBE -> {
                    if (toIndex == lastYoutubePageIndex && !hasSignificantVerticalDelta) {
                        return false
                    }
                    lastYoutubePageIndex = toIndex
                }
            }
        }

        return when {
            isShortFormContext && (hasPageAdvance || hasSignificantVerticalDelta) -> true
            className.contains("viewpager") && (hasPageAdvance || hasSignificantVerticalDelta) -> true
            isPagerOrRecycler && hasPageAdvance && hasSignificantVerticalDelta -> true
            else -> false
        }
    }

    private fun matchesShortFormViewId(viewId: String, platform: ReelPlatform): Boolean {
        if (viewId.isEmpty()) return false
        return when (platform) {
            ReelPlatform.INSTAGRAM -> {
                viewId.contains("clips") ||
                    viewId.contains("reel") ||
                    viewId.contains("media_container")
            }
            ReelPlatform.YOUTUBE -> {
                viewId.contains("reel") ||
                    viewId.contains("shorts") ||
                    viewId.contains("slim_video")
            }
        }
    }

    private fun hasShortFormAncestorOrChild(
        node: AccessibilityNodeInfo,
        platform: ReelPlatform
    ): Boolean {
        var currentParent: AccessibilityNodeInfo? = node.parent
        var depth = 0
        while (currentParent != null && depth < 3) {
            val parentId = currentParent.viewIdResourceName?.lowercase().orEmpty()
            if (matchesShortFormViewId(parentId, platform)) {
                @Suppress("DEPRECATION")
                currentParent.recycle()
                return true
            }
            val nextParent = currentParent.parent
            @Suppress("DEPRECATION")
            currentParent.recycle()
            currentParent = nextParent
            depth++
        }
        return false
    }

    private fun commitReelSwipe(platform: ReelPlatform, elapsedMs: Long) {
        lastRecordedSwipeElapsedMs = elapsedMs
        repository.recordReelSwipeAsync(platform = platform, count = 1)

        if (repository.hapticsEnabled.value) {
            triggerClickHaptic()
        }
    }

    private fun triggerClickHaptic() {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator == null || !vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                vibrator.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
        }
    }

    private fun syncFloatingHudVisibility(show: Boolean) {
        if (!repository.hudEnabled.value || !Settings.canDrawOverlays(this)) {
            FloatingHudService.stopHud(this)
            return
        }
        if (show) {
            FloatingHudService.showHud(this)
        } else {
            FloatingHudService.stopHud(this)
        }
    }

    private fun isSystemOverlayOrKeyboardPackage(packageName: String): Boolean {
        return packageName.startsWith("com.android.systemui") ||
            packageName.contains("inputmethod") ||
            packageName.contains("keyboard") ||
            packageName == applicationContext.packageName
    }

    override fun onInterrupt() {
        repository.setAccessibilityConnected(false)
        repository.setActiveForegroundPlatform(null)
        FloatingHudService.stopHud(this)
    }

    override fun onDestroy() {
        repository.setAccessibilityConnected(false)
        repository.setActiveForegroundPlatform(null)
        FloatingHudService.stopHud(this)
        super.onDestroy()
    }

    companion object {
        const val DEBOUNCE_COOLDOWN_MS = 1300L
        private const val MIN_VERTICAL_SCROLL_DELTA_PX = 180

        fun isServiceEnabled(context: Context): Boolean {
            val expectedComponentName =
                "${context.packageName}/${ReelAccessibilityService::class.java.canonicalName}"
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ).orEmpty()
            return enabledServices.split(':').any {
                it.equals(expectedComponentName, ignoreCase = true) ||
                    it.endsWith(ReelAccessibilityService::class.java.simpleName)
            }
        }
    }
}
