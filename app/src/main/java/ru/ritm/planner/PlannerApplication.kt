package ru.ritm.planner

import android.app.Application
import ru.ritm.planner.data.LocalTaskRepository
import ru.ritm.planner.data.TaskDatabase
import ru.ritm.planner.data.TaskRepository
import ru.ritm.planner.data.AppearancePreferences
import ru.ritm.planner.reminders.ReminderScheduler

class PlannerApplication : Application() {
    val appearance by lazy { AppearancePreferences(this) }
    val database by lazy { TaskDatabase(this) }
    val reminders by lazy { ReminderScheduler(this) }
    val repository: TaskRepository by lazy { LocalTaskRepository(database, reminders) }
    override fun onCreate() {
        super.onCreate()
        reminders.createChannel()
    }
}
