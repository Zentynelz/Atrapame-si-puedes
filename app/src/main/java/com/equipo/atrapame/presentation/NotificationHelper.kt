package com.equipo.atrapame.presentation

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.equipo.atrapame.R

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "game_notifications_v3"
        const val CHANNEL_NAME = "Game Notifications"
        private const val NOTIFICATION_ID_WIN = 1
        private const val NOTIFICATION_ID_LOSE = 2
        const val PERMISSION_REQUEST_CODE = 100
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW // Silenciar notificacion de sistema
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = context.getString(R.string.notification_channel_description)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showVictoryNotification(moves: Int, time: String) {
        if (!hasNotificationPermission()) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_victory_title))
            .setContentText(context.getString(R.string.notification_victory_text, moves, time))
            .setPriority(NotificationCompat.PRIORITY_LOW) // Usa baja prioridad para que no vibre/suene
            .setSilent(true) // Fuerza a que sea silente y tranquilizadora
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_WIN, notification)
        }
        
        // Reproducir sonido relajante caracteristico
        try {
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 100) // Suave beep
            Handler(Looper.getMainLooper()).postDelayed({
                toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 200) // Segundo beep aciendo acorde
                Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, 250)
            }, 120)
        } catch(e: Exception) {}
    }

    fun showDefeatNotification() {
        if (!hasNotificationPermission()) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notification_defeat_title))
            .setContentText(context.getString(R.string.notification_defeat_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID_LOSE, notification)
        }
    }

    fun showCustomNotification(title: String, message: String) {
        if (!hasNotificationPermission()) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(System.currentTimeMillis().toInt(), notification) // ID único
        }
    }

    fun requestNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission()) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
