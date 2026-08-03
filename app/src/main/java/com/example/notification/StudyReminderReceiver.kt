package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_BOOT_COMPLETED) {
            // Device rebooted: restore the daily alarm from persisted settings.
            // Never post a notification on boot.
            StudyNotificationScheduler.rescheduleReminderIfEnabled(context)
            return
        }

        if (intent?.action == StudyNotificationScheduler.ACTION_POMODORO_FINISHED) {
            StudyNotificationScheduler.sendPomodoroCompleteNotification(context)
            return
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Daily Study Reminder 📚"
        val message = intent?.getStringExtra(EXTRA_MESSAGE)
            ?: "Your daily scheduled study plan is waiting! Review cards now to keep your streak active 🔥"

        StudyNotificationScheduler.sendImmediateNotification(
            context = context,
            title = title,
            message = message
        )

        // One-shot alarms do NOT repeat: re-arm tomorrow's reminder right here.
        StudyNotificationScheduler.rescheduleReminderIfEnabled(context)
    }

    companion object {
        const val EXTRA_TITLE = "extra_reminder_title"
        const val EXTRA_MESSAGE = "extra_reminder_message"

        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
    }
}
