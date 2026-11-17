package ru.ritm.planner.reminders

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import ru.ritm.planner.domain.Task

class ReminderScheduler(private val context: Context) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)
    private val notifications get() = context.getSystemService(NotificationManager::class.java)
    fun createChannel() {
        notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Напоминания о задачах", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Сроки задач в приложении Напоминалка" })
    }
    fun notificationsAllowed(): Boolean = notifications.areNotificationsEnabled() &&
        notifications.getNotificationChannel(CHANNEL)?.importance != NotificationManager.IMPORTANCE_NONE &&
        (Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    fun exactAllowed() = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
    private fun pending(id: String, reminderAt: Long = 0) = PendingIntent.getBroadcast(context, 0,
        Intent(context, ReminderReceiver::class.java).setData(Uri.parse("ritm://reminder/$id"))
            .putExtra("id", id).putExtra("reminderAt", reminderAt),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun cancel(id: String) {
        alarms.cancel(pending(id))
        notifications.cancel(id, 1)
    }
    fun schedule(task: Task) {
        alarms.cancel(pending(task.id))
        if (task.completed) notifications.cancel(task.id, 1)
        val at = task.reminderAt ?: return
        if (task.completed || task.notifiedAt != null || !notificationsAllowed()) return
        // Recover a missed reminder after reboot while the task's deadline is still ahead.
        if (at < System.currentTimeMillis() && task.deadline <= System.currentTimeMillis()) return
        val trigger = at.coerceAtLeast(System.currentTimeMillis() + 1000)
        val intent = pending(task.id, at)
        try {
            if (exactAllowed()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        } catch (_: SecurityException) {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, intent)
        }
    }
    fun restore(tasks: List<Task>) = tasks.forEach(::schedule)
    companion object { const val CHANNEL = "task_reminders" }
}
