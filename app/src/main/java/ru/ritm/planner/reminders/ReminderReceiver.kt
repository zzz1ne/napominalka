package ru.ritm.planner.reminders

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ru.ritm.planner.MainActivity
import ru.ritm.planner.PlannerApplication
import ru.ritm.planner.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.Executors

internal val reminderExecutor = Executors.newSingleThreadExecutor()

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        val expected = intent.getLongExtra("reminderAt", -1)
        val pending = goAsync()
        reminderExecutor.execute {
            try {
                val app = context.applicationContext as PlannerApplication
                val task = app.database.find(id) ?: return@execute
                if (task.completed || task.notifiedAt != null || task.reminderAt != expected ||
                    expected > System.currentTimeMillis() || !app.reminders.notificationsAllowed()) return@execute
                val open = PendingIntent.getActivity(context, 0,
                    Intent(context, MainActivity::class.java)
                        .setData(android.net.Uri.parse("ritm://task/$id"))
                        .putExtra("taskId", id)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                val date = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("ru"))
                    .format(Instant.ofEpochMilli(task.deadline).atZone(ZoneId.systemDefault()))
                val text = "Срок: $date" + if (task.description.isNotBlank()) "\n${task.description}" else ""
                val notification = Notification.Builder(context, ReminderScheduler.CHANNEL)
                    .setSmallIcon(R.drawable.ic_notification).setContentTitle(task.title)
                    .setContentText("Срок: $date").setStyle(Notification.BigTextStyle().bigText(text))
                    .setColor(task.color).setContentIntent(open).setAutoCancel(true)
                    .setCategory(Notification.CATEGORY_REMINDER).build()
                context.getSystemService(NotificationManager::class.java).notify(id, 1, notification)
                app.database.markNotified(id, expected, System.currentTimeMillis())
            } finally { pending.finish() }
        }
    }
}

class RestoreRemindersReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> Unit
            else -> return
        }
        val pending = goAsync()
        reminderExecutor.execute {
            try {
                val app = context.applicationContext as PlannerApplication
                app.reminders.restore(app.database.all())
            } finally { pending.finish() }
        }
    }
}
