package com.howlab.howlstagram

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessageService :FirebaseMessagingService(){
    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)

        var i = Intent(this, MainActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        var pendingIntent = PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_ONE_SHOT)
        var cId = "fcm_default_channel"

        var builder = NotificationCompat.Builder(this,cId)
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            builder.setSmallIcon(R.mipmap.ic_launcher)
            builder.setContentTitle(p0.notification?.title)
            builder.setContentText(p0.notification?.body)
            builder.setContentIntent(pendingIntent)

        var manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            var channel = NotificationChannel(cId,"Default channel",NotificationManager.IMPORTANCE_HIGH)
            manager.createNotificationChannel(channel)
        }


        manager.notify(0,builder.build())


    }

}