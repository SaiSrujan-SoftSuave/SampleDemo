package com.example.geofencinginitialdemo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

fun sendNotification(context: Context) {
    val channelId = "geofence_notification_channel"
    val channelName = "Geofence Notifications"
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
        notificationManager.createNotificationChannel(channel)
    }

    val notification = NotificationCompat.Builder(context, channelId)
        .setContentTitle("Geofence Alert")
        .setContentText("You have been away from the geofence for over 45 minutes.")
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .build()

    notificationManager.notify(0, notification)
}
