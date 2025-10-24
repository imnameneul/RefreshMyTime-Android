// 파일 경로: app/src/main/java/com/example/playtimemanager/timer/TimerService.kt

package com.example.playtimemanager.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.playtimemanager.R
import com.example.playtimemanager.MainActivity

class TimerService : Service() {

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTI_ID = 1001
        const val EXTRA_START_ELAPSED = "startElapsed"
        const val EXTRA_ACCUMULATED = "accumulatedMs"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID, "Timer", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startElapsed = intent?.getLongExtra(EXTRA_START_ELAPSED, SystemClock.elapsedRealtime())
            ?: SystemClock.elapsedRealtime()
        val accumulatedMs = intent?.getLongExtra(EXTRA_ACCUMULATED, 0L) ?: 0L

        val nowWall = System.currentTimeMillis()
        val elapsedSinceStart = SystemClock.elapsedRealtime() - startElapsed
        val baseWhen = nowWall - (accumulatedMs + elapsedSinceStart)

        val contentIntent = android.app.PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            (android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE)
        )

        val noti = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // 프로젝트의 타이머 아이콘으로 교체
            .setContentTitle("타이머 실행 중")
            .setContentText("터치하면 앱으로 이동")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setUsesChronometer(true)  // 알림의 시계를 초단위로 자동 증가
            .setWhen(baseWhen)         // 누적+경과 반영 시작점
            .build()

        startForeground(NOTI_ID, noti)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
