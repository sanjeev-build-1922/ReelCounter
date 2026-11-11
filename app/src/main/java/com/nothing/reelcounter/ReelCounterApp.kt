package com.nothing.reelcounter

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.nothing.reelcounter.data.ReelDatabase
import com.nothing.reelcounter.data.ReelRepository

class ReelCounterApp : Application() {

    val database: ReelDatabase by lazy {
        ReelDatabase.getInstance(this)
    }

    val repository: ReelRepository by lazy {
        ReelRepository(
            reelDao = database.reelDao(),
            appContext = applicationContext
        )
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                HUD_CHANNEL_ID,
                getString(R.string.hud_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.hud_notification_channel_desc)
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val HUD_CHANNEL_ID = "reel_counter_hud_channel"

        @Volatile
        lateinit var instance: ReelCounterApp
            private set
    }
}
