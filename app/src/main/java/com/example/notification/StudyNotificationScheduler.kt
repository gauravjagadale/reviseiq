package com.example.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import java.util.Calendar

object StudyNotificationScheduler {

    const val CHANNEL_ID = "study_reminders_channel"
    private const val CHANNEL_NAME = "Daily Study Reminders"
    private const val CHANNEL_DESC = "Notifications reminding you to complete scheduled study sessions"
    private const val ALARM_REQUEST_CODE = 8080
    private const val NOTIFICATION_ID = 9090

    const val ACTION_POMODORO_FINISHED = "com.example.POMODORO_FINISHED"
    private const val POMODORO_ALARM_REQUEST_CODE = 8081
    private const val POMODORO_NOTIFICATION_ID = 9091

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Exact alarms need SCHEDULE_EXACT_ALARM, which the user can grant/revoke
     * from system settings. Always check before scheduling so a revoked
     * permission never crashes the app.
     */
    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Sends the user to the "Alarms & reminders" system screen when exact
     * alarms are unavailable. Returns false when they already can be scheduled.
     */
    fun requestExactAlarmPermission(context: Context): Boolean {
        if (canScheduleExact(context)) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                context.startActivity(
                    Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                // No settings screen available; fall back to inexact alarms.
            }
        }
        return true
    }

    fun scheduleDailyReminder(
        context: Context,
        hourOfDay: Int,
        minute: Int,
        title: String = "Daily Study Session Scheduled! 📚",
        message: String = "Your planned study plan for today is ready. Tap to start reviewing!"
    ) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            putExtra(StudyReminderReceiver.EXTRA_TITLE, title)
            putExtra(StudyReminderReceiver.EXTRA_MESSAGE, message)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canScheduleExact(context)) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Fallback for strict alarm permission settings
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StudyReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Re-arms the daily reminder from persisted settings. Used after the alarm
     * fires (so it repeats every day) and after BOOT_COMPLETED (so reminders
     * survive a reboot). No-op when reminders are disabled.
     */
    fun rescheduleReminderIfEnabled(context: Context) {
        val prefs = context.getSharedPreferences("reviseiq_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("key_reminders_enabled", true)) return
        scheduleDailyReminder(
            context = context,
            hourOfDay = prefs.getInt("key_reminder_hour", 20),
            minute = prefs.getInt("key_reminder_minute", 0)
        )
    }

    fun sendImmediateNotification(
        context: Context,
        title: String = "Study Reminder 📚",
        message: String = "Time for your scheduled study session! Keep your streak active."
    ) {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) return

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * One-shot exact alarm fired when a focus session ends while the app is
     * closed or in the background. Posts a "session complete" notification so
     * the user knows the sprint is done; the session itself is finalized when
     * the app is next opened (see ReviseViewModel.checkPomodoroCompletion).
     */
    fun schedulePomodoroCompletionAlarm(context: Context, endTimeMillis: Long) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_POMODORO_FINISHED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            POMODORO_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canScheduleExact(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Fallback for strict alarm permission settings
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pendingIntent)
        }
    }

    fun cancelPomodoroCompletionAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StudyReminderReceiver::class.java).apply {
            action = ACTION_POMODORO_FINISHED
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            POMODORO_ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun sendPomodoroCompleteNotification(context: Context) {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) return

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.example.R.drawable.ic_notification)
            .setContentTitle("🎉 Focus Session Complete!")
            .setContentText("Your focus session is done — great job! Tap to see your AI summary.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Your focus session is done — great job! Tap to see your Gemini AI summary and record your progress."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(POMODORO_NOTIFICATION_ID, notification)
    }
}
