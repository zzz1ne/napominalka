package ru.ritm.planner.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import ru.ritm.planner.domain.Task

class TaskDatabase(context: Context, name: String = "ritm.db") : SQLiteOpenHelper(context, name, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE tasks (
            id TEXT PRIMARY KEY, title TEXT NOT NULL, description TEXT NOT NULL,
            created_at INTEGER NOT NULL, deadline INTEGER NOT NULL, color INTEGER NOT NULL,
            reminder_at INTEGER, completed_at INTEGER, updated_at INTEGER NOT NULL,
            owner_id TEXT, notified_at INTEGER)""")
        db.execSQL("CREATE INDEX tasks_deadline ON tasks(deadline)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Add explicit, non-destructive migrations here when the schema changes.
        check(oldVersion == newVersion) { "Missing database migration" }
    }
    fun all(): List<Task> = readableDatabase.query("tasks", null, null, null, null, null, "deadline ASC")
        .use { cursor -> buildList { while (cursor.moveToNext()) add(cursor.task()) } }
    fun find(id: String): Task? = readableDatabase.query("tasks", null, "id = ?", arrayOf(id), null, null, null)
        .use { if (it.moveToFirst()) it.task() else null }
    fun save(task: Task) {
        val values = ContentValues().apply {
            put("id", task.id); put("title", task.title); put("description", task.description)
            put("created_at", task.createdAt); put("deadline", task.deadline); put("color", task.color)
            put("reminder_at", task.reminderAt); put("completed_at", task.completedAt)
            put("updated_at", task.updatedAt); put("owner_id", task.ownerId); put("notified_at", task.notifiedAt)
        }
        check(writableDatabase.insertWithOnConflict("tasks", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L)
    }
    fun delete(id: String) { writableDatabase.delete("tasks", "id = ?", arrayOf(id)) }
    fun markNotified(id: String, expectedReminderAt: Long, now: Long) {
        writableDatabase.update("tasks", ContentValues().apply { put("notified_at", now) },
            "id = ? AND reminder_at = ?", arrayOf(id, expectedReminderAt.toString()))
    }
    private fun Cursor.task() = Task(
        id = string("id"), title = string("title"), description = string("description"),
        createdAt = long("created_at"), deadline = long("deadline"), color = long("color").toInt(),
        reminderAt = nullableLong("reminder_at"), completedAt = nullableLong("completed_at"),
        updatedAt = long("updated_at"), ownerId = getString(getColumnIndexOrThrow("owner_id")),
        notifiedAt = nullableLong("notified_at"),
    )
    private fun Cursor.string(column: String) = getString(getColumnIndexOrThrow(column))
    private fun Cursor.long(column: String) = getLong(getColumnIndexOrThrow(column))
    private fun Cursor.nullableLong(column: String): Long? = getColumnIndexOrThrow(column).let { if (isNull(it)) null else getLong(it) }
}
