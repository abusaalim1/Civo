package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FocuslyRepository
import com.example.data.model.FocusSessionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Keeps a focus timer session alive even when the app UI is swiped away or the
 * process is killed. The session clock is end-time based (stored in SharedPreferences),
 * so it survives process death. If the process is killed and restarted after the
 * end time already passed, the session is completed properly (recorded + prefs
 * cleared + completion notification) instead of silently vanishing.
 *
 * Runs as a foreground service with a persistent low-priority notification showing
 * the remaining time, following the same specialUse FGS pattern as FocusShieldService.
 */
class FocusTimerService : Service() {

    companion object {
        private const val TAG = "FocusTimerService"
        const val ACTION_START_TIMER = "com.example.action.START_FOCUS_TIMER"
        const val ACTION_STOP_TIMER = "com.example.action.STOP_FOCUS_TIMER"
        const val EXTRA_END_TIME_MS = "extra_end_time_ms"
        const val EXTRA_TARGET_SECONDS = "extra_target_seconds"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_MODE = "extra_timer_mode"

        private const val NOTIFICATION_ID = 7100
        private const val CHANNEL_ID = "focus_timer_channel"

        private const val PREFS_LOCAL = "focusly_local_data"
        private const val PREFS_TIMER_STATE = "focusly_timer_state"
        private const val KEY_MANUAL_RUNNING = "is_manual_timer_running"
        private const val KEY_MANUAL_END = "manual_timer_end_time_ms"
        private const val KEY_TIMER_RUNNING = "is_timer_running"
        private const val KEY_REMAINING = "remaining_seconds"
        private const val KEY_TASK_TITLE = "current_task_title"
        private const val KEY_TIMER_MODE = "timer_mode"
        private const val KEY_TARGET_SECONDS = "target_seconds"
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP_TIMER) {
            clearTimerPrefs()
            stopForegroundSafely()
            stopSelf()
            return START_NOT_STICKY
        }

        val now = System.currentTimeMillis()
        var endTimeMs = intent?.getLongExtra(EXTRA_END_TIME_MS, 0L) ?: 0L
        var targetSeconds = intent?.getIntExtra(EXTRA_TARGET_SECONDS, 25 * 60) ?: (25 * 60)
        var taskTitle = intent?.getStringExtra(EXTRA_TASK_TITLE) ?: "Focus Session"
        var mode = intent?.getStringExtra(EXTRA_MODE) ?: "Deep Work"

        // Process restart (intent == null): recover from persisted prefs
        if (intent == null || action == null) {
            val prefs = getSharedPreferences(PREFS_LOCAL, Context.MODE_PRIVATE)
            val tPrefs = getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE)
            val running = prefs.getBoolean(KEY_MANUAL_RUNNING, false)
            endTimeMs = prefs.getLong(KEY_MANUAL_END, 0L)
            if (!running || endTimeMs <= 0L) {
                Log.d(TAG, "[Restart] No persisted timer found; stopping")
                stopSelf()
                return START_NOT_STICKY
            }
            targetSeconds = tPrefs.getInt(KEY_TARGET_SECONDS, targetSeconds).let { saved -> if (saved > 0) saved else targetSeconds }
            taskTitle = tPrefs.getString(KEY_TASK_TITLE, taskTitle) ?: taskTitle
            mode = tPrefs.getString(KEY_TIMER_MODE, mode) ?: mode
            Log.i(TAG, "[Restart] Recovering timer, end=$endTimeMs target=$targetSeconds")
        }

        if (endTimeMs <= now) {
            // End time already passed while the process was dead: complete the session properly.
            serviceScope.launch {
                completeMissedSession(targetSeconds, taskTitle, mode, endTimeMs)
            }
            return START_NOT_STICKY
        }

        persistTimerPrefs(endTimeMs, targetSeconds, taskTitle, mode)
        val remainingSec = ((endTimeMs - now) / 1000L).toInt()
        safeStartForeground(buildTimerNotification(remainingSec, taskTitle))
        startTicker(endTimeMs, targetSeconds, taskTitle, mode)
        return START_STICKY
    }

    private fun startTicker(endTimeMs: Long, targetSeconds: Int, taskTitle: String, mode: String) {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                val remainingSec = ((endTimeMs - System.currentTimeMillis()) / 1000L).toInt()
                if (remainingSec <= 0) {
                    // Timer finished while UI is dead: complete the session properly.
                    completeMissedSession(targetSeconds, taskTitle, mode, endTimeMs)
                    break
                }
                updateTimerNotification(remainingSec, taskTitle)
                persistRemaining(remainingSec)
                delay(1000L)
            }
        }
    }

    /**
     * Completes a session whose end time passed without the UI alive: records the
     * session row (like a normal finish), clears timer prefs, stops the service,
     * and posts a completion notification so the user knows it counted.
     */
    private suspend fun completeMissedSession(targetSeconds: Int, taskTitle: String, mode: String, endTimeMs: Long) {
        try {
            val cal = Calendar.getInstance()
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val mappedDayOfWeek = if (dayOfWeek == 1) 7 else dayOfWeek - 1
            val session = FocusSessionEntity(
                taskTitle = taskTitle,
                durationSeconds = targetSeconds.coerceAtLeast(60),
                targetDurationSeconds = targetSeconds.coerceAtLeast(60),
                mode = mode,
                completedAt = endTimeMs,
                endTime = endTimeMs,
                startTime = endTimeMs - (targetSeconds.coerceAtLeast(60) * 1000L),
                dayOfWeek = mappedDayOfWeek,
                hourOfDay = cal.get(Calendar.HOUR_OF_DAY),
                focusPointsEarned = maxOf(1, targetSeconds / 60)
            )
            FocuslyRepository.getInstance(applicationContext).recordSession(session)
            Log.i(TAG, "Missed session recorded: '$taskTitle' (${targetSeconds}s)")
        } catch (e: Exception) {
            Log.w(TAG, "Could not record missed session: ${e.message}")
        }
        clearTimerPrefs()
        stopForegroundSafely()
        postCompletionNotification(taskTitle)
        stopSelf()
    }

    private fun persistTimerPrefs(endTimeMs: Long, targetSeconds: Int, taskTitle: String, mode: String) {
        try {
            getSharedPreferences(PREFS_LOCAL, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_MANUAL_RUNNING, true)
                .putLong(KEY_MANUAL_END, endTimeMs)
                .apply()
            getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_TIMER_RUNNING, true)
                .putInt(KEY_REMAINING, ((endTimeMs - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0))
                .putInt(KEY_TARGET_SECONDS, targetSeconds)
                .putString(KEY_TASK_TITLE, taskTitle)
                .putString(KEY_TIMER_MODE, mode)
                .apply()
        } catch (_: Exception) {}
    }

    private fun persistRemaining(remainingSec: Int) {
        try {
            getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE).edit()
                .putInt(KEY_REMAINING, remainingSec)
                .apply()
        } catch (_: Exception) {}
    }

    private fun clearTimerPrefs() {
        try {
            getSharedPreferences(PREFS_LOCAL, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_MANUAL_RUNNING, false)
                .putLong(KEY_MANUAL_END, 0L)
                .apply()
            getSharedPreferences(PREFS_TIMER_STATE, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_TIMER_RUNNING, false)
                .apply()
        } catch (_: Exception) {}
    }

    private fun buildTimerNotification(remainingSec: Int, taskTitle: String): Notification {
        val minutes = remainingSec / 60
        val seconds = remainingSec % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(this, NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_small)
            .setContentTitle("⏱ Focus Session — $taskTitle")
            .setContentText("$timeText remaining — stay focused!")
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    private fun updateTimerNotification(remainingSec: Int, taskTitle: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, buildTimerNotification(remainingSec, taskTitle))
        } catch (_: Exception) {}
    }

    private fun postCompletionNotification(taskTitle: String) {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pi = PendingIntent.getActivity(this, NOTIFICATION_ID + 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notif = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_small)
                .setContentTitle("✅ Focus session complete")
                .setContentText("\"$taskTitle\" finished while you were away — great discipline!")
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            manager.notify(NOTIFICATION_ID + 1, notif)
        } catch (_: Exception) {}
    }

    private fun safeStartForeground(notification: Notification) {
        try {
            createNotificationChannel()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "safeStartForeground failed: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (_: Throwable) {}
        }
    }

    private fun stopForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows remaining time of the active focus session"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
