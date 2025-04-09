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


private const val TAG = "FirebaseMessageService"
class FirebaseMessageService : FirebaseMessagingService() {
    // 새로운 토큰이 생성될 때 마다 해당 콜백이 호출된다.
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "From: " + message!!.from)

        if(message.data.isNotEmpty()){
            Log.d(TAG, "onMessageReceived: ${message} ")
            Log.d(TAG, "onMessageReceived: ${message.data} ")
            Log.d(TAG, "onMessageReceived: ${message.notification}")
            val body = message.notification?.body
            val title = message.notification?.title
            val roomId = message.data["roomId"]
            val userId= message.data["userId"]
            val userName = message.data["userName"]
            val userImage = message.data["userImage"]
            val type = message.data["type"]
            Log.i("바디: ", body.toString())
            Log.i("타이틀: ", title.toString())
            Log.i("roomId: ", roomId.toString())
            Log.i("userId: ", userId.toString())
            Log.i("userName: ", userName.toString())
            Log.i("userImage: ", userImage.toString())
            Log.i("타입: ", type.toString())
            sendNotification(message)
        }
        else {
            Log.i("수신에러: ", "data가 비어있습니다. 메시지를 수신하지 못했습니다.")
            Log.i("data값: ", message.data.toString())
        }
    }
    private fun sendNotification(remoteMessage: RemoteMessage) {
        val uniId: Int = (System.currentTimeMillis() / 7).toInt()
        val notificationBuilder: NotificationCompat.Builder

        val type = remoteMessage.data["type"]
        val channelId = type ?: "default" // CHAT
        val channelName = getChannelName(type ?: "default") // 채팅 알림
        createNotificationChannel(channelId, type ?: "default")


        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP) // Activity Stack 을 경로만 남긴다. A-B-C-D-B => A-B
        val pendingIntent = createPendingIntent(type ?: "default", remoteMessage)


        // 알림 소리
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            val channelDescription = "New test Information"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = channelDescription
            //각종 채널에 대한 설정
            channel.enableLights(true)
            channel.lightColor = Color.RED
            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(100, 200, 300)
            notificationManager.createNotificationChannel(channel)
            //channel이 등록된 builder
            notificationBuilder = NotificationCompat.Builder(this, channelId)
        } else {
            notificationBuilder = NotificationCompat.Builder(this)
        }

        notificationBuilder
            .setSmallIcon(R.mipmap.ic_patent_round) // 아이콘 설정
            .setContentTitle(remoteMessage.notification?.title) // 제목
            .setContentText(remoteMessage.notification?.body) // 메시지 내용
            .setAutoCancel(true)
            .setSound(soundUri) // 알림 소리
            .setContentIntent(pendingIntent) // 알림 실행 시 Intent

        // 알림 생성
        notificationManager.notify(uniId, notificationBuilder.build())
    }
    private fun createNotificationChannel(channelId: String, type: String) {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(NotificationChannel(channelId, getChannelName(type), importance))
    }



    private fun getChannelName(fcmType: String): String {
        return when (fcmType) {
            "CHAT" -> "채팅 알림"
            "SIMILARITY_TEST" -> "유사도 알림"
            else -> "기본 알림"
        }
    }

    private fun createPendingIntent(fcmType: String, remoteMessage: RemoteMessage): PendingIntent {
        Log.d(TAG, "createPendingIntent: $fcmType")
        val intent = when (fcmType) {
            "CHAT" -> Intent(this, MainActivity::class.java).apply {
                for(key in remoteMessage.data.keys){
                    putExtra(key, remoteMessage.data.getValue(key))
                }
            }
            "SIMILARITY_TEST" -> Intent(this, MainActivity::class.java).apply {
                putExtra("type", "SIMILARITY_TEST")
            }
            else -> Intent(this, MainActivity::class.java).apply {

            }
        }

        return PendingIntent.getActivity(this, 10101, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

}