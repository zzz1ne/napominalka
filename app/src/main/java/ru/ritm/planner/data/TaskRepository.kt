package ru.ritm.planner.data

import kotlinx.coroutines.flow.Flow
import ru.ritm.planner.domain.Task

/** Local-first boundary: a future account/sync implementation can decorate this contract. */
interface TaskRepository {
    val tasks: Flow<List<Task>>
    suspend fun save(task: Task)
    suspend fun delete(id: String)
    suspend fun reload()
}
