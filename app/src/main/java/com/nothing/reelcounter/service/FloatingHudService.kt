package com.nothing.reelcounter.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.nothing.reelcounter.MainActivity
import com.nothing.reelcounter.R
import com.nothing.reelcounter.ReelCounterApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

/**
 * Ultra-minimal WindowManager overlay HUD.
 * Strips out verbose labels ("REELS:") and renders only the live count (e.g. "042")
 * with a subtle red dot in a compact Nothing OS translucent capsule.
 * Appears ONLY when Instagram or YouTube is open in foreground; otherwise hides/removes itself.
 */
class FloatingHudService : Service() {

    private val repository by lazy { ReelCounterApp.instance.repository }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var pillContainer: LinearLayout? = null
    private var statusDotView: View? = null
    private var counterTextView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var isOverlayAttached = false
    private var latestCount = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundWithSpecialUse()
        observeTelemetry()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this) || !repository.hudEnabled.value) {
            removeOverlayView()
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent?.action) {
            ACTION_SHOW_HUD -> {
                ensureOverlayAttached()
                pillContainer?.visibility = View.VISIBLE
            }
            ACTION_HIDE_HUD -> {
                pillContainer?.visibility = View.GONE
                // If requested to hide, also stop foreground service when outside target apps to save battery
                removeOverlayView()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_STOP_HUD -> {
                removeOverlayView()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (repository.activeForegroundPlatform.value != null) {
                    ensureOverlayAttached()
                    pillContainer?.visibility = View.VISIBLE
                } else {
                    removeOverlayView()
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithSpecialUse() {
        val notification = buildNotification(latestCount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeTelemetry() {
        serviceScope.launch {
            repository.observeTodayCount().collectLatest { count ->
                latestCount = count
                updatePillText(count)
            }
        }

        serviceScope.launch {
            repository.hudEnabled.collectLatest { enabled ->
                if (!enabled) {
                    removeOverlayView()
                    stopSelf()
                }
            }
        }

        // Hide immediately if active foreground platform becomes null
        serviceScope.launch {
            repository.activeForegroundPlatform.collectLatest { platform ->
                if (platform == null) {
                    pillContainer?.visibility = View.GONE
                    removeOverlayView()
                    stopSelf()
                } else {
                    ensureOverlayAttached()
                    pillContainer?.visibility = View.VISIBLE
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ensureOverlayAttached() {
        if (isOverlayAttached || !Settings.canDrawOverlays(this)) return

        val density = resources.displayMetrics.density
        val savedPos = repository.getHudPosition()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = savedPos?.first ?: (14 * density).toInt()
            y = savedPos?.second ?: (60 * density).toInt()
        }
        layoutParams = params

        // Ultra-minimal Nothing OS translucent capsule
        val pillBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 20f * density
            setColor(Color.parseColor("#E6050505")) // Deep AMOLED translucent black
            setStroke((1f * density).toInt(), Color.parseColor("#33FFFFFF"))
        }

        val dotDrawable = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#D71921")) // Nothing Red Accent Dot
        }

        val dotView = View(this).apply {
            background = dotDrawable
            val size = (6 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = (6 * density).toInt()
                gravity = Gravity.CENTER_VERTICAL
            }
        }
        statusDotView = dotView

        // Minimal bold number display (larger font, no "REELS:" text clutter)
        val textView = TextView(this).apply {
            text = formatPillLabel(latestCount)
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f) // Enlarged, clean monospace number
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            letterSpacing = 0.08f
            includeFontPadding = false
        }
        counterTextView = textView

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = pillBackground
            val padH = (11 * density).toInt()
            val padV = (5 * density).toInt()
            setPadding(padH, padV, padH, padV)
            elevation = 6f * density
            addView(dotView)
            addView(textView)
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { _, event ->
            val currentParams = layoutParams ?: return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = currentParams.x
                    initialY = currentParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (initialTouchX - event.rawX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 8 || abs(dy) > 8) {
                        isDragging = true
                    }
                    currentParams.x = (initialX + dx).coerceAtLeast(0)
                    currentParams.y = (initialY + dy).coerceAtLeast(0)
                    try {
                        windowManager?.updateViewLayout(container, currentParams)
                    } catch (_: Exception) {
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        openDashboardActivity()
                    } else {
                        repository.saveHudPosition(currentParams.x, currentParams.y)
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(container, params)
            pillContainer = container
            isOverlayAttached = true
        } catch (_: Exception) {
            isOverlayAttached = false
        }
    }

    private fun updatePillText(count: Int) {
        counterTextView?.text = formatPillLabel(count)
        val limit = repository.dailyLimit.value
        val dotColor = if (count >= limit) {
            Color.parseColor("#D71921")
        } else {
            Color.parseColor("#FFFFFF")
        }
        (statusDotView?.background as? GradientDrawable)?.setColor(dotColor)
    }

    private fun formatPillLabel(count: Int): String {
        return String.format(Locale.US, "%03d", count)
    }

    private fun openDashboardActivity() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(launchIntent)
    }

    private fun buildNotification(count: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ReelCounterApp.HUD_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("REEL // COUNTER")
            .setContentText(formatPillLabel(count))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun removeOverlayView() {
        if (isOverlayAttached && pillContainer != null) {
            try {
                windowManager?.removeViewImmediate(pillContainer)
            } catch (_: Exception) {
            }
        }
        pillContainer = null
        counterTextView = null
        statusDotView = null
        isOverlayAttached = false
    }

    override fun onDestroy() {
        serviceScope.cancel()
        removeOverlayView()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 1042
        const val ACTION_SHOW_HUD = "com.nothing.reelcounter.action.SHOW_HUD"
        const val ACTION_HIDE_HUD = "com.nothing.reelcounter.action.HIDE_HUD"
        const val ACTION_STOP_HUD = "com.nothing.reelcounter.action.STOP_HUD"

        fun showHud(context: Context) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, FloatingHudService::class.java).apply {
                action = ACTION_SHOW_HUD
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun hideHud(context: Context) {
            val intent = Intent(context, FloatingHudService::class.java).apply {
                action = ACTION_HIDE_HUD
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }

        fun stopHud(context: Context) {
            val intent = Intent(context, FloatingHudService::class.java).apply {
                action = ACTION_STOP_HUD
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }
    }
}
