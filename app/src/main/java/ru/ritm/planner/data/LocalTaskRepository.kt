package ru.ritm.planner.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.ritm.planner.domain.Task
import ru.ritm.planner.reminders.ReminderScheduler

class LocalTaskRepository(private val db: TaskDatabase, private val reminders: ReminderScheduler) : TaskRepository {
    private val state = MutableStateFlow<List<Task>>(emptyList())
    private val mutex = Mutex()
    override val tasks = state.asStateFlow()
    override suspend fun reload() = withContext(Dispatchers.IO) { mutex.withLock { state.value = db.all() } }
    override suspend fun save(task: Task) = withContext(Dispatchers.IO) { mutex.withLock {
        val old = db.find(task.id)
        val saved = if (old?.reminderAt == task.reminderAt && old?.completed == task.completed)
            task.copy(notifiedAt = old?.notifiedAt) else task.copy(notifiedAt = null)
        db.save(saved)
        reminders.cancel(saved.id)
        reminders.schedule(saved)
        state.value = db.all()
    } }
    override suspend fun delete(id: String) = withContext(Dispatchers.IO) { mutex.withLock {
        db.delete(id)
        reminders.cancel(id)
        state.value = db.all()
    } }
}
