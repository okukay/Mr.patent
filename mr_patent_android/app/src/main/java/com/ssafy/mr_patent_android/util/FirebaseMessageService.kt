package com.ssafy.mr_patent_android.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ssafy.mr_patent_android.MainActivity
import com.ssafy.mr_patent_android.R


class FirebaseMessageService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {

        if (message.data.isNotEmpty()) {
            Log.d("", "onMessageReceived: ${message} ")
            sendNotification(message)
        } else {
            Log.i("수신에러: ", "data가 비어있습니다. 메시지를 수신하지 못했습니다.")
        }
    }

    private fun sendNotification(remoteMessage: RemoteMessage) {
        val uniId: Int = (System.currentTimeMillis() / 7).toInt()
        val notificationBuilder: NotificationCompat.Builder

        val type = remoteMessage.data["type"]
        val channelId = type ?: "default"
        val channelName = getChannelName(type ?: "default")
        createNotificationChannel(channelId, type ?: "default")


        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = createPendingIntent(type ?: "default", remoteMessage)


        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            val channelDescription = "New test Information"
            val channel =
                NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = channelDescription
            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300)
            notificationManager.createNotificationChannel(channel)
            notificationBuilder = NotificationCompat.Builder(this, channelId)
        } else {
            notificationBuilder = NotificationCompat.Builder(this)
        }

        notificationBuilder
            .setSmallIcon(R.mipmap.ic_patent_round)
            .setContentTitle(remoteMessage.notification?.title)
            .setContentText(remoteMessage.notification?.body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setContentIntent(pendingIntent)

        notificationManager.notify(uniId, notificationBuilder.build())
    }

    private fun createNotificationChannel(channelId: String, type: String) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(
            NotificationChannel(
                channelId,
                getChannelName(type),
                importance
            )
        )
    }


    private fun getChannelName(fcmType: String): String {
        return when (fcmType) {
            "CHAT" -> "채팅 알림"
            "SIMILARITY_TEST" -> "유사도 알림"
            else -> "기본 알림"
        }
    }

    private fun createPendingIntent(fcmType: String, remoteMessage: RemoteMessage): PendingIntent {
        val intent = when (fcmType) {
            "CHAT" -> Intent(this, MainActivity::class.java).apply {
                for (key in remoteMessage.data.keys) {
                    putExtra(key, remoteMessage.data.getValue(key))
                }
            }

            "SIMILARITY_TEST" -> Intent(this, MainActivity::class.java).apply {
                putExtra("type", "SIMILARITY_TEST")
            }

            else -> Intent(this, MainActivity::class.java).apply {

            }
        }

        return PendingIntent.getActivity(
            this,
            10101,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

}