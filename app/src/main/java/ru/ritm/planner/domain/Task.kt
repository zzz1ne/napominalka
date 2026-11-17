package ru.ritm.planner.domain

import java.util.UUID
import kotlin.math.ceil

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val deadline: Long,
    val color: Int = 0xFFBEF264.toInt(),
    val reminderAt: Long? = null,
    val completedAt: Long? = null,
    val updatedAt: Long = System.currentTimeMillis(),
    val ownerId: String? = null,
    val notifiedAt: Long? = null,
) {
    val completed: Boolean get() = completedAt != null
    fun remainingFraction(now: Long): Float {
        if (completed) return 0f
        val duration = (deadline - createdAt).coerceAtLeast(1)
        return ((deadline - now).toDouble() / duration).coerceIn(0.0, 1.0).toFloat()
    }
    fun urgent(now: Long) = !completed && remainingFraction(now) <= 0.05f
    fun overdue(now: Long) = !completed && deadline <= now
}

data class Countdown(val value: String, val unit: String)

fun countdown(deadline: Long, now: Long): Countdown {
    val remaining = deadline - now
    return when {
        remaining <= 0 -> Countdown("0", "время вышло")
        remaining >= 86_400_000L -> Countdown(ceil(remaining / 86_400_000.0).toInt().toString(), "дн.")
        remaining >= 3_600_000L -> Countdown(ceil(remaining / 3_600_000.0).toInt().toString(), "ч.")
        else -> Countdown(ceil(remaining / 60_000.0).toInt().coerceAtLeast(1).toString(), "мин.")
    }
}

fun validateTask(title: String, deadline: Long, reminderAt: Long?, now: Long): String? = when {
    title.isBlank() -> "Добавьте название задачи"
    title.trim().length > 120 -> "Название — не больше 120 символов"
    deadline <= now -> "Дедлайн должен быть в будущем"
    reminderAt != null && reminderAt <= now -> "Время напоминания уже прошло. Выберите другое или отключите его."
    reminderAt != null && reminderAt > deadline -> "Напоминание должно быть не позже дедлайна"
    else -> null
}
