package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class StudyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Daily Study Reminder 📚"
        val message = intent?.getStringExtra(EXTRA_MESSAGE) 
            ?: "Your daily scheduled study plan is waiting! Review cards now to keep your streak active 🔥"

        StudyNotificationScheduler.sendImmediateNotification(
            context = context,
            title = title,
            message = message
        )
    }

    companion object {
        const val EXTRA_TITLE = "extra_reminder_title"
        const val EXTRA_MESSAGE = "extra_reminder_message"
    }
}
