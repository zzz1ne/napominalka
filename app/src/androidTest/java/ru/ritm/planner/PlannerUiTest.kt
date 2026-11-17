package ru.ritm.planner

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.Before
import org.junit.After
import ru.ritm.planner.data.Appearance
import ru.ritm.planner.data.AppearancePreferences
import ru.ritm.planner.domain.Task
import java.io.File
import java.io.FileOutputStream

class PlannerUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun taskCard(name: String) = hasText(name) and !hasSetTextAction()

    @Before fun removePreviousTestDrafts() {
        val app = compose.activity.application as PlannerApplication
        app.appearance.setLightTheme(false)
        app.appearance.setAccent(Appearance.DEFAULT_ACCENT)
        runBlocking { app.database.all().filter { it.title.startsWith("Тест UI ") }.forEach { app.repository.delete(it.id) } }
    }

    @After fun resetAppearance() {
        val app = compose.activity.application as PlannerApplication
        app.appearance.setLightTheme(false)
        app.appearance.setAccent(Appearance.DEFAULT_ACCENT)
    }

    private fun screenshot(name: String): android.graphics.Bitmap {
        compose.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        // UiAutomation captures the display compositor, which can trail the Compose test clock.
        Thread.sleep(500)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val app = compose.activity.application as PlannerApplication
        FileOutputStream(File(app.getExternalFilesDir("screenshots"), name)).use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    @Test fun emptyPlusOpensEditorAndCopyIsUpdated() {
        compose.waitUntil(10000) { compose.onAllNodesWithText("Добавьте свою первую задачу").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Напоминалка").assertExists()
        compose.onNodeWithText("Освободите мысли.").assertDoesNotExist()
        compose.onNodeWithText("Добавить задачу").assertDoesNotExist()
        screenshot("empty.png")
        compose.onNodeWithContentDescription("Создать первую задачу").performClick()
        compose.onNodeWithText("Новая задача").assertExists()
        compose.onNodeWithText("Один шаг ближе к тому, что важно.").assertDoesNotExist()
        screenshot("editor.png")
    }

    @Test fun lightThemeAndCustomAccentPersistAndApplyToEditor() {
        val app = compose.activity.application as PlannerApplication
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithText("v${BuildConfig.VERSION_NAME}").assertExists()
        compose.onNodeWithContentDescription("Светлая тема").performClick().assertIsOn()
        compose.onNodeWithContentDescription("Цвет приложения #78B8FF").performClick().assertIsSelected()
        compose.onNodeWithText("Свой цвет приложения").performScrollTo().performClick()
        compose.onNodeWithText("HEX · 6 символов").performTextClearance()
        compose.onNodeWithText("HEX · 6 символов").performTextInput("6C5CE7")
        compose.onNodeWithText("Выбрать", substring = false).performClick()
        screenshot("settings-light.png")
        compose.onNodeWithText("Готово", substring = false).performClick()
        val persisted = AppearancePreferences(app).appearance.value
        assertTrue(persisted.lightTheme)
        assertEquals(0xFF6C5CE7.toInt(), persisted.accent)
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithContentDescription("Создать первую задачу").fetchSemanticsNodes().isNotEmpty() }
        val main = screenshot("empty-light.png")
        assertEquals(android.graphics.Color.WHITE, main.getPixel(2, main.height / 2))
        compose.onNodeWithContentDescription("Создать первую задачу").performClick()
        compose.onNodeWithText("Новая задача").assertExists()
        val editor = screenshot("editor-light.png")
        assertEquals(android.graphics.Color.WHITE, editor.getPixel(2, editor.height / 2))
    }

    @Test fun createReopenSearchCompleteAndDelete() {
        val name = "Тест UI ${System.currentTimeMillis()}"
        compose.onNodeWithText("Создать", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Название").performTextInput(name)
        compose.onNodeWithText("Описание").performTextInput("Проверка локальной задачи")
        compose.onNodeWithContentDescription("Выбрать любой цвет").performScrollTo().performClick()
        compose.onNodeWithText("HEX · 6 символов").performTextClearance()
        compose.onNodeWithText("HEX · 6 символов").performTextInput("55AAFF")
        compose.onNodeWithText("Выбрать", substring = false).performClick()
        compose.onNodeWithContentDescription("Напоминание").performScrollTo().performClick()
        compose.onNodeWithText("Создать задачу").performClick()
        compose.waitUntil(10000) { compose.onAllNodes(taskCard(name)).fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodes(taskCard(name)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Найти задачу").performTextInput(name)
        compose.onNode(taskCard(name)).assertIsDisplayed()
        assertEquals(0xFF55AAFF.toInt(), (compose.activity.application as PlannerApplication).database.all().single { it.title == name }.color)
        compose.onNodeWithContentDescription("Выполнить: $name").performClick()
        compose.onNodeWithText("Готово", substring = true).performClick()
        compose.waitUntil(10000) { compose.onAllNodes(taskCard(name)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(taskCard(name)).performClick()
        compose.onNodeWithText("Удалить задачу").performScrollTo().performClick()
        compose.onNodeWithText("Удалить", substring = false).performClick()
        compose.waitUntil(10000) { compose.onAllNodes(taskCard(name)).fetchSemanticsNodes().isEmpty() }
        val app = compose.activity.application as PlannerApplication
        assertNull(app.database.all().find { it.title == name })
    }

    @Test fun blankTitleIsRejected() {
        compose.onNodeWithText("Создать", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Создать задачу").performClick()
        compose.onNodeWithText("Добавьте название задачи").assertExists()
    }

    @Test fun completedLongPressRestoresOnlySelectedTask() {
        val app = compose.activity.application as PlannerApplication
        val now = System.currentTimeMillis()
        val selected = Task(title = "Тест UI восстановить $now", deadline = now + 86400000, completedAt = now)
        val other = Task(title = "Тест UI оставить $now", deadline = now + 172800000, completedAt = now)
        try {
            runBlocking { app.repository.save(selected); app.repository.save(other) }
            compose.onNodeWithText("Готово", substring = true).performClick()
            compose.waitUntil(10000) { compose.onAllNodes(taskCard(selected.title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(taskCard(selected.title)).performTouchInput { longClick() }
            compose.onNodeWithText("Удалить", substring = false).assertIsDisplayed()
            compose.onNodeWithText("Восстановить").assertIsDisplayed()
            screenshot("completed-menu.png")
            compose.onNodeWithText("Восстановить").performClick()
            compose.waitUntil(10000) { app.database.find(selected.id)?.completed == false }
            compose.onNode(taskCard(selected.title)).assertDoesNotExist()
            assertTrue(app.database.find(other.id)!!.completed)
            compose.onNodeWithText("В работе", substring = true).performClick()
            compose.onNode(taskCard(selected.title)).assertIsDisplayed()
            compose.waitForIdle()
            compose.activityRule.scenario.recreate()
            compose.waitUntil(10000) { compose.onAllNodes(taskCard(selected.title)).fetchSemanticsNodes().isNotEmpty() }
            assertFalse(app.database.find(selected.id)!!.completed)
        } finally { runBlocking { app.repository.delete(selected.id); app.repository.delete(other.id) } }
    }

    @Test fun completedLongPressDeletesOnlySelectedTaskAndCanBeCancelled() {
        val app = compose.activity.application as PlannerApplication
        val now = System.currentTimeMillis()
        val selected = Task(title = "Тест UI удалить $now", deadline = now + 86400000, completedAt = now)
        val other = Task(title = "Тест UI сохранить $now", deadline = now + 172800000, completedAt = now)
        try {
            runBlocking { app.repository.save(selected); app.repository.save(other) }
            compose.onNodeWithText("Готово", substring = true).performClick()
            compose.waitUntil(10000) { compose.onAllNodes(taskCard(selected.title)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNode(taskCard(selected.title)).performTouchInput { longClick() }
            compose.onNodeWithText("Удалить", substring = false).performClick()
            compose.onNodeWithText("Удалить задачу?").assertIsDisplayed()
            compose.onNodeWithText("Отмена").performClick()
            assertNotNull(app.database.find(selected.id))
            compose.onNode(taskCard(selected.title)).performTouchInput { longClick() }
            compose.onNodeWithText("Удалить", substring = false).performClick()
            compose.onNodeWithText("Удалить задачу?").assertIsDisplayed()
            compose.onNodeWithText("Удалить", substring = false).performClick()
            compose.waitUntil(10000) { app.database.find(selected.id) == null }
            compose.onNode(taskCard(selected.title)).assertDoesNotExist()
            assertTrue(app.database.find(other.id)!!.completed)
            compose.onNode(taskCard(other.title)).assertIsDisplayed()
        } finally { runBlocking { app.repository.delete(selected.id); app.repository.delete(other.id) } }
    }

    @Test fun urgencyAndDesignPreview() {
        val app = compose.activity.application as PlannerApplication
        val now = System.currentTimeMillis()
        val samples = listOf(
            Task(title = "Отправить проект", description = "Последние штрихи — и готово", createdAt = now - 96 * 60000, deadline = now + 4 * 60000, color = 0xFFFFB98A.toInt()),
            Task(title = "Спланировать выходные", description = "Маршрут, билеты и немного спонтанности", createdAt = now - 3 * 3600000, deadline = now + 5 * 3600000, color = 0xFFBEF264.toInt()),
            Task(title = "Прочитать 20 страниц", description = "Время для себя и хорошей книги", createdAt = now - 86400000, deadline = now + 3 * 86400000, color = 0xFFA7C7FF.toInt()),
        )
        try {
            runBlocking { samples.forEach { app.repository.save(it) } }
            compose.waitUntil(10000) { compose.onAllNodesWithText(samples.first().title).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("!").assertExists()
            compose.waitForIdle()
            screenshot("planner.png")
        } finally { runBlocking { samples.forEach { app.repository.delete(it.id) } } }
    }
}
