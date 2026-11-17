package ru.ritm.planner.domain

import org.junit.Assert.*
import org.junit.Test

class TaskTest {
    private val task = Task(title = "План", createdAt = 1000, deadline = 101000)
    @Test fun progressUsesWholeLifetimeAndClamps() {
        assertEquals(1f, task.remainingFraction(0), 0.0001f)
        assertEquals(.5f, task.remainingFraction(51000), 0.0001f)
        assertEquals(0f, task.remainingFraction(200000), 0.0001f)
    }
    @Test fun exclamationAppearsAtFivePercent() {
        assertFalse(task.urgent(95999))
        assertTrue(task.urgent(96000))
        assertTrue(task.urgent(101000))
        assertTrue(task.overdue(101000))
    }
    @Test fun completedTasksAreNeverUrgentOrOverdue() {
        val done = task.copy(completedAt = 5000)
        assertFalse(done.urgent(200000))
        assertFalse(done.overdue(200000))
        assertEquals(0f, done.remainingFraction(1000), 0f)
    }
    @Test fun zeroDurationDoesNotDivideByZero() {
        assertEquals(0f, task.copy(createdAt = 101000).remainingFraction(101000), 0f)
    }
    @Test fun countdownRoundsUpAndHandlesBoundary() {
        assertEquals(Countdown("1", "мин."), countdown(1001, 1000))
        assertEquals(Countdown("1", "ч."), countdown(3_601_000, 1000))
        assertEquals(Countdown("1", "дн."), countdown(86_401_000, 1000))
        assertEquals(Countdown("0", "время вышло"), countdown(1000, 1000))
    }
    @Test fun reminderDefaultCanBeInThePastForShortTasks() {
        val now = 1_000_000L
        val deadline = now + 3_600_000
        assertNotNull(validateTask("Задача", deadline, deadline - 86_400_000, now))
        assertNull(validateTask("Задача", deadline, deadline - 900_000, now))
        assertNull(validateTask("Задача", deadline, null, now))
    }
    @Test fun invalidInputsAreRejected() {
        assertNotNull(validateTask("   ", 5000, null, 1000))
        assertNotNull(validateTask("a".repeat(121), 5000, null, 1000))
        assertNotNull(validateTask("План", 1000, null, 1000))
        assertNotNull(validateTask("План", 5000, 5001, 1000))
        assertNull(validateTask("План", 5000, 5000, 1000))
    }
    @Test fun extendingDeadlineKeepsOriginalStart() {
        assertEquals(.75f, task.copy(deadline = 201000).remainingFraction(51000), .0001f)
    }
}
