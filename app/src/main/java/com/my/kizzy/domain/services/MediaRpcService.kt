/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * MediaRpcService.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.my.kizzy.domain.services

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import com.google.gson.Gson
import com.my.kizzy.R
import com.my.kizzy.data.preference.Prefs
import com.my.kizzy.data.preference.Prefs.MEDIA_RPC_ENABLE_TIMESTAMPS
import com.my.kizzy.data.preference.Prefs.TOKEN
import com.my.kizzy.data.rpc.KizzyRPC
import com.my.kizzy.data.utils.Constants
import com.my.kizzy.data.utils.Log.logger
import com.my.kizzy.domain.use_case.get_current_data.get_media.getCurrentlyPlayingMedia
import com.my.kizzy.ui.screen.settings.rpc_settings.RpcButtons
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MediaRpcService : Service() {

    @Inject
    lateinit var kizzyRPC: KizzyRPC

    @Inject
    lateinit var scope: CoroutineScope

    private var wakeLock: WakeLock? = null

    @Suppress("DEPRECATION")
    @SuppressLint("WakelockTimeout")
    override fun onCreate() {
        super.onCreate()
        val token = Prefs[TOKEN, ""]
        if (token.isEmpty()) stopSelf()
        // TODO add time left later
        val time = System.currentTimeMillis()
        setupWakeLock()
        startForeground(NOTIFICATION_ID, getNotification())
        scope.launch {
            while (isActive) {
                val enableTimestamps = Prefs[MEDIA_RPC_ENABLE_TIMESTAMPS, false]
                val playingMedia = getCurrentlyPlayingMedia(this@MediaRpcService)
                getNotificationManager()?.notify(
                    NOTIFICATION_ID,
                    getNotification(playingMedia.details?:"")
                )
                val rpcButtonsString = Prefs[Prefs.RPC_BUTTONS_DATA, "{}"]
                val rpcButtons = Gson().fromJson(rpcButtonsString, RpcButtons::class.java)
                when (kizzyRPC.isRpcRunning()) {
                    true -> {
                        logger.d("MediaRPC", "Updating Rpc")
                        kizzyRPC.updateRPC(
                            name = playingMedia.name.ifEmpty { "YouTube" },
                            details = playingMedia.details,
                            state = playingMedia.state,
                            large_image = playingMedia.large_image,
                            small_image = playingMedia.small_image,
                            enableTimestamps = enableTimestamps,
                            time = time
                        )
                    }
                    false -> {
                        kizzyRPC.apply {
                            setName(playingMedia.name.ifEmpty { "YouTube" })
                            setDetails(playingMedia.details)
                            setStatus(Constants.DND)
                            if (Prefs[Prefs.USE_RPC_BUTTONS, false]) {
                                with(rpcButtons) {
                                    setButton1(button1.takeIf { it.isNotEmpty() })
                                    setButton1URL(button1Url.takeIf { it.isNotEmpty() })
                                    setButton2(button2.takeIf { it.isNotEmpty() })
                                    setButton2URL(button2Url.takeIf { it.isNotEmpty() })
                                }
                            }
                            build()
                        }
                    }
                }
                delay(5000)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getNotification(notificationTitle: String = "Browsing Home Page.."): Notification {
        getNotificationManager().apply {
            this?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Background Service",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        val builder = Notification.Builder(this, CHANNEL_ID)
        builder.setSmallIcon(R.drawable.ic_media_rpc)
        val intent = Intent(this, MediaRpcService::class.java)
        intent.action = ACTION_STOP_SERVICE
        val pendingIntent = PendingIntent.getService(
            this,
            0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(R.drawable.ic_media_rpc, "Exit", pendingIntent)
        builder.setContentText(notificationTitle.ifEmpty { "Browsing Home Page.." })
        return builder.build()
    }

    @SuppressLint("WakelockTimeout")
    private fun setupWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kizzy:MediaRPC")
        wakeLock?.acquire()
    }

    private fun getNotificationManager(): NotificationManager? {
        return getSystemService(
            NotificationManager::class.java
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            it.action?.let { ac ->
                if (ac == ACTION_STOP_SERVICE)
                    stopSelf()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        scope.cancel()
        kizzyRPC.closeRPC()
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val CHANNEL_ID = "MediaRPC"
        const val ACTION_STOP_SERVICE = "STOP_RPC"
        const val NOTIFICATION_ID = 8838
    }
}
