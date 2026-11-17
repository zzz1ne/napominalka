package ru.ritm.planner

import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import ru.ritm.planner.domain.Task

class ReminderDeliveryTest {
    @Test fun realAlarmDeliversNotificationAndMarksItDelivered() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").close()
        instrumentation.uiAutomation.executeShellCommand("appops set ${context.packageName} SCHEDULE_EXACT_ALARM allow").close()
        val app = context.applicationContext as PlannerApplication
        val now = System.currentTimeMillis()
        val task = Task(title = "Проверка системного напоминания", deadline = now + 600000, reminderAt = now + 4000)
        val notifications = context.getSystemService(NotificationManager::class.java)
        try {
            app.database.save(task)
            app.reminders.schedule(task)
            val until = System.currentTimeMillis() + 25000
            while (System.currentTimeMillis() < until &&
                (notifications.activeNotifications.none { it.tag == task.id } || app.database.find(task.id)?.notifiedAt == null)) Thread.sleep(250)
            assertTrue("System notification must be delivered", notifications.activeNotifications.any { it.tag == task.id })
            assertNotNull(app.database.find(task.id)!!.notifiedAt)
            app.reminders.restore(listOf(app.database.find(task.id)!!))
            assertTrue("Restoring alarms must preserve delivered notifications", notifications.activeNotifications.any { it.tag == task.id })
            app.reminders.cancel(task.id)
            val cancelUntil = System.currentTimeMillis() + 5000
            while (System.currentTimeMillis() < cancelUntil && notifications.activeNotifications.any { it.tag == task.id }) Thread.sleep(100)
            assertFalse(notifications.activeNotifications.any { it.tag == task.id })
        } finally {
            app.reminders.cancel(task.id)
            app.database.delete(task.id)
        }
    }
}
