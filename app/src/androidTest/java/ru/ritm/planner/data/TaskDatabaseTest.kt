package ru.ritm.planner.data

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import ru.ritm.planner.domain.Task
import ru.ritm.planner.reminders.ReminderScheduler

class TaskDatabaseTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: TaskDatabase
    @Before fun create() { context.deleteDatabase("test-ritm.db"); database = TaskDatabase(context, "test-ritm.db") }
    @After fun close() { database.close(); context.deleteDatabase("test-ritm.db") }

    @Test fun taskPersistsAcrossDatabaseReopening() {
        val task = Task(title = "Проверить локальное хранение", description = "Без интернета", deadline = 900000, color = 0xFF123456.toInt(), reminderAt = 800000)
        database.save(task)
        database.close()
        database = TaskDatabase(context, "test-ritm.db")
        assertEquals(task, database.find(task.id))
        database.save(task.copy(completedAt = 600000))
        assertTrue(database.find(task.id)!!.completed)
        database.delete(task.id)
        assertNull(database.find(task.id))
    }
    @Test fun changingReminderResetsDeliveryAndCompletionCancelsIt() = runBlocking {
        val scheduler = ReminderScheduler(context)
        scheduler.createChannel()
        val repo = LocalTaskRepository(database, scheduler)
        val now = System.currentTimeMillis()
        val task = Task(title = "Напоминание", deadline = now + 1000000, reminderAt = now + 500000)
        repo.save(task)
        database.markNotified(task.id, task.reminderAt!!, now)
        repo.save(task.copy(title = "Новое название"))
        assertEquals(now, database.find(task.id)!!.notifiedAt)
        repo.save(task.copy(reminderAt = now + 600000))
        assertNull(database.find(task.id)!!.notifiedAt)
        repo.save(task.copy(completedAt = now))
        assertTrue(repo.tasks.first().single().completed)
        repo.delete(task.id)
        assertTrue(repo.tasks.first().isEmpty())
    }
    @Test fun staleDeliveryCannotMarkRescheduledTask() {
        val task = Task(title = "Перенос", deadline = 9000, reminderAt = 8000)
        database.save(task)
        database.markNotified(task.id, 7000, 1000)
        assertNull(database.find(task.id)!!.notifiedAt)
    }
}
