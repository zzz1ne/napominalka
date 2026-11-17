package ru.ritm.planner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.ritm.planner.domain.Task

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PlannerApplication
    val tasks = app.repository.tasks
    private val loadingState = MutableStateFlow(true)
    val loading = loadingState.asStateFlow()
    private val errorState = MutableStateFlow<String?>(null)
    val error = errorState.asStateFlow()
    private val savingState = MutableStateFlow(false)
    val saving = savingState.asStateFlow()
    init { refresh() }
    fun clearError() { errorState.value = null }
    fun refresh() = viewModelScope.launch {
        try {
            app.repository.reload()
            withContext(Dispatchers.IO) { app.reminders.restore(app.database.all()) }
        } catch (_: Exception) { errorState.value = "Не удалось прочитать задачи. Попробуйте открыть приложение снова." }
        finally { loadingState.value = false }
    }
    fun save(task: Task, onSaved: () -> Unit) = viewModelScope.launch {
        if (savingState.value) return@launch
        savingState.value = true
        try { app.repository.save(task); onSaved() }
        catch (_: Exception) { errorState.value = "Не удалось сохранить задачу. Попробуйте ещё раз." }
        finally { savingState.value = false }
    }
    fun toggle(task: Task) = save(task.copy(completedAt = if (task.completed) null else System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis())) {}
    fun delete(task: Task, onDeleted: () -> Unit) = viewModelScope.launch {
        try { app.repository.delete(task.id); onDeleted() }
        catch (_: Exception) { errorState.value = "Не удалось удалить задачу. Попробуйте ещё раз." }
    }
}
