package com.example.homework_kotlin

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Timer
import java.util.TimerTask


const val tickRate = 5// seconds

@SuppressLint("DefaultLocale")
fun backgroundNotificationBuilder(context: Context, tick:Int):NotificationCompat.Builder {
    val title = "Roll dice?"
    val content = String.format("You closed the app %d seconds ago\nWhy not open it again?", tick)
    val openIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    val pendingIntent: PendingIntent =
        PendingIntent.getActivity(context, 1, openIntent, PendingIntent.FLAG_IMMUTABLE)

    val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
        .setSmallIcon(R.drawable.chat)
        .setContentTitle(title)
        .setContentText(content)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)// tap to remove notification
        .setSilent(true)
        .setTimeoutAfter(tickRate*1500.toLong())
    return builder
}

@SuppressLint("MissingPermission")
fun backgroundNotification(context: Context, tick:Int):()->Unit {
    val id = 1
    val builder = backgroundNotificationBuilder(context, tick)
    return {
        checkPermission(context, "android.permission.POST_NOTIFICATIONS") {
            with(NotificationManagerCompat.from(context)) {
                notify(id, builder.build())
            }
        }
    }
}

class BackgroundTimerTask(context: Context):TimerTask() {
    private var tick:Int = 0
    val lambda:()->Unit = {
        tick += tickRate
        backgroundNotification(context, tick)()
    }
    override fun run() {
        lambda()
    }
}


class BackgroundService:Service() {
    private var timer = Timer()
    override fun onStartCommand(intent:Intent, flags: Int, startId: Int): Int {
        timer.cancel()// stop old
        timer = Timer()
        val task = BackgroundTimerTask(this)
        val rate = tickRate*1000.toLong()
        timer.schedule(task, rate, rate)
        return START_STICKY
    }

    override fun onDestroy() {
        timer.cancel()// stop old
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}