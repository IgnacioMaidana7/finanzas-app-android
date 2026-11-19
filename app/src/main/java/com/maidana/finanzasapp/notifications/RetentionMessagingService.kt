package com.maidana.finanzasapp.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.maidana.finanzasapp.MainActivity
import com.maidana.finanzasapp.R
import com.maidana.finanzasapp.ui.navigation.Screen

internal enum class RetentionCampaign(val key: String, val screen: Screen) {
    MOVEMENTS("registro_movimientos", Screen.NewTransaction),
    GOALS("revision_metas", Screen.Goals);

    companion object {
        const val EXTRA_CAMPAIGN = "tp6_campaign"

        fun from(key: String?): RetentionCampaign? = entries.firstOrNull { it.key == key }
    }
}

internal fun createRetentionChannel(context: Context) {
    val channel = NotificationChannel(
        context.getString(R.string.notification_channel_id),
        context.getString(R.string.notification_channel_name),
        NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
        description = context.getString(R.string.notification_channel_description)
        lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
    }
    context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
}

class RetentionMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val campaign = RetentionCampaign.from(message.data[RetentionCampaign.EXTRA_CAMPAIGN])
        val content = message.notification ?: return
        val body = content.body ?: return
        val manager = getSystemService(NotificationManager::class.java)
        if (!manager.areNotificationsEnabled()) return

        createRetentionChannel(this)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            campaign?.let { putExtra(RetentionCampaign.EXTRA_CAMPAIGN, it.key) }
        }
        val notificationId = (campaign?.ordinal ?: -1) + 100
        val openApp = PendingIntent.getActivity(
            this, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title ?: getString(R.string.app_name))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        manager.notify(notificationId, notification)
    }
}
