package ru.ritm.planner.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import ru.ritm.planner.PlannerApplication
import ru.ritm.planner.PlannerViewModel
import ru.ritm.planner.domain.Task
import ru.ritm.planner.domain.countdown
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Russian = Locale.forLanguageTag("ru")
fun dateText(millis: Long, pattern: String = "d MMM, HH:mm"): String = DateTimeFormatter.ofPattern(pattern, Russian)
    .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerApp(requestedTask: String?, consumeRequest: () -> Unit, vm: PlannerViewModel = viewModel()) {
    val tasks by vm.tasks.collectAsStateWithLifecycle(emptyList())
    val loading by vm.loading.collectAsStateWithLifecycle()
    val saving by vm.saving.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val app = context.applicationContext as PlannerApplication
    var query by rememberSaveable { mutableStateOf("") }
    var completedTab by rememberSaveable { mutableStateOf(false) }
    var editorId by rememberSaveable { mutableStateOf<String?>(null) }
    var editorOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var permissionRefresh by remember { mutableIntStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionRefresh++; vm.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { permissionRefresh++; now = System.currentTimeMillis(); vm.refresh() }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(15_000) } }
    LaunchedEffect(requestedTask, loading, tasks) {
        now = System.currentTimeMillis()
        if (requestedTask != null && !loading) {
            if (tasks.any { it.id == requestedTask }) { editorId = requestedTask; editorOpen = true }
            consumeRequest()
        }
    }
    val notificationsAllowed = remember(permissionRefresh) { app.reminders.notificationsAllowed() }
    val active = tasks.filterNot { it.completed }
    val visible = tasks.filter { it.completed == completedTab &&
        (it.title.contains(query, true) || it.description.contains(query, true)) }
    val editing = tasks.find { it.id == editorId }
    fun newTask() { editorId = null; editorOpen = true }

    Scaffold(containerColor = Background,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = ::newTask, containerColor = Accent, contentColor = OnAccent,
                shape = RoundedCornerShape(22.dp), modifier = Modifier.padding(bottom = 8.dp),
                icon = { Glyph("plus", Modifier.size(24.dp), OnAccent) },
                text = { Text("Создать", fontWeight = FontWeight.Bold, fontSize = 16.sp) })
        }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Glyph("leaf", Modifier.size(22.dp), Accent)
                            Text("Напоминалка", fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text(dateText(now, "d MMMM").uppercase(Russian), color = Muted, fontSize = 11.sp, letterSpacing = 1.3.sp)
                        Spacer(Modifier.width(12.dp))
                        IconButton(onClick = { settingsOpen = true }) { Glyph("settings", Modifier.size(23.dp), label = "Настройки") }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Ваши планы", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-1.2).sp,
                        modifier = Modifier.padding(top = 3.dp, bottom = 16.dp))
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Найти задачу", color = Muted, fontSize = 14.sp) }, singleLine = true,
                        leadingIcon = { Glyph("search", Modifier.size(21.dp)) }, shape = RoundedCornerShape(18.dp),
                        trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Glyph("close", Modifier.size(20.dp), label = "Очистить поиск") } },
                        colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Line, focusedBorderColor = Accent),
                        keyboardOptions = KeyboardOptions.Default)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = !completedTab, onClick = { completedTab = false }, label = { Text("В работе  ${active.size}") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = OnAccent))
                        FilterChip(selected = completedTab, onClick = { completedTab = true }, label = { Text("Готово  ${tasks.count { it.completed }}") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = OnAccent))
                    }
                }
                if (!notificationsAllowed && active.any { it.reminderAt != null }) item {
                    Surface(color = Card, shape = RoundedCornerShape(16.dp), onClick = { settingsOpen = true }) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Glyph("bell", Modifier.size(22.dp), Accent)
                            Text("Уведомления выключены. Нажмите, чтобы настроить.", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                if (loading) item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Accent) } }
                else if (visible.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val canCreate = !completedTab && query.isBlank()
                        Box(Modifier.size(64.dp).clip(CircleShape).background(Card)
                            .then(if (canCreate) Modifier.clickable(onClick = ::newTask).semantics { contentDescription = "Создать первую задачу" } else Modifier),
                            contentAlignment = Alignment.Center) {
                            Glyph(if (query.isNotBlank()) "search" else if (completedTab) "check" else "plus", Modifier.size(32.dp), Accent)
                        }
                        when {
                            query.isNotBlank() -> {
                                Text("Ничего не найдено", fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 20.dp))
                                Text("Попробуйте другое название или описание.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 9.dp))
                            }
                            completedTab -> Text("Выполненные задачи появятся здесь!", color = Muted, fontSize = 16.sp,
                                modifier = Modifier.padding(top = 20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            else -> Text("Добавьте свою первую задачу", fontSize = 19.sp, fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 20.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                }
                else {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (completedTab) "ВЫПОЛНЕННЫЕ" else "БЛИЖАЙШИЕ ДЕДЛАЙНЫ", fontSize = 10.sp, color = Muted, letterSpacing = 1.6.sp)
                            Text("${visible.size}", fontSize = 11.sp, color = Muted)
                        }
                    }
                    items(visible, key = { it.id }) { task ->
                        TaskCard(task, now, { editorId = task.id; editorOpen = true }, { vm.toggle(task) },
                            onDelete = { pendingDeleteId = task.id })
                    }
                }
            }
        }
    }
    if (editorOpen) TaskEditor(editing, saving,
        onDismiss = { editorOpen = false },
        onSave = { task -> vm.save(task) {
            editorOpen = false
            if (task.reminderAt != null && Build.VERSION.SDK_INT >= 33 && !app.reminders.notificationsAllowed()) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } }, onDelete = { task -> vm.delete(task) { editorOpen = false } })
    if (settingsOpen) SettingsDialog(notificationsAllowed, onDismiss = { settingsOpen = false })
    val deleting = tasks.find { it.id == pendingDeleteId }
    if (deleting != null) AlertDialog(onDismissRequest = { pendingDeleteId = null },
        title = { Text("Удалить задачу?") },
        text = { Text("«${deleting.title}» и её напоминание будут удалены с устройства.") },
        confirmButton = { TextButton(onClick = { pendingDeleteId = null; vm.delete(deleting) {} }) { Text("Удалить", color = Danger) } },
        dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Отмена") } })
    if (error != null) AlertDialog(onDismissRequest = vm::clearError, title = { Text("Не получилось") }, text = { Text(error!!) },
        confirmButton = { TextButton(onClick = vm::clearError) { Text("Понятно") } })
}

@Composable
fun TaskCard(task: Task, now: Long, onEdit: () -> Unit, onToggle: () -> Unit, onDelete: () -> Unit) {
    val accent = readableTone(Color(task.color), Card)
    var menuOpen by rememberSaveable(task.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    Box(Modifier.fillMaxWidth()) {
    Surface(color = Card, shape = shape, modifier = Modifier.clip(shape).combinedClickable(
        onClick = onEdit,
        onLongClick = if (task.completed) ({ menuOpen = true }) else null,
        onLongClickLabel = if (task.completed) "Действия с выполненной задачей" else null,
    )) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 20.dp, bottom = 20.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            CountdownRing(task, now)
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(task.title, color = if (task.completed) Muted else accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (task.description.isNotBlank()) Text(task.description, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
                Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Glyph("calendar", Modifier.size(13.dp), if (task.overdue(now)) Danger else Muted)
                    Text(dateText(task.deadline), fontSize = 11.sp, color = if (task.overdue(now)) Danger else Muted)
                    if (task.reminderAt != null && !task.completed) { Spacer(Modifier.width(3.dp)); Glyph("bell", Modifier.size(13.dp), Muted, "Напоминание включено") }
                }
                if (task.overdue(now)) Text("Срок прошёл", color = Danger, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                else if (!task.completed) {
                    val left = countdown(task.deadline, now)
                    Text("Осталось ${left.value} ${left.unit}", color = Muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
            IconButton(onClick = onToggle, modifier = Modifier.semantics { contentDescription = if (task.completed) "Вернуть в работу: ${task.title}" else "Выполнить: ${task.title}" }) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(if (task.completed) Accent else Line), contentAlignment = Alignment.Center) {
                    Glyph("check", Modifier.size(15.dp), if (task.completed) OnAccent else Muted)
                }
            }
        }
    }
    DropdownMenu(expanded = task.completed && menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(text = { Text("Удалить", color = Danger) }, onClick = { menuOpen = false; onDelete() })
        DropdownMenuItem(text = { Text("Восстановить") }, onClick = { menuOpen = false; onToggle() })
    }
    }
}

@Composable
fun CountdownRing(task: Task, now: Long) {
    val fraction = task.remainingFraction(now)
    val accent = readableTone(Color(task.color), Card)
    val track = Line
    val remaining = countdown(task.deadline, now)
    Box(Modifier.size(74.dp).semantics(mergeDescendants = true) {
        contentDescription = when { task.completed -> "Задача выполнена"; task.overdue(now) -> "Срок прошёл"; else -> "Осталось ${remaining.value} ${remaining.unit}" }
        progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
    }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(4.dp)) {
            drawCircle(track, style = Stroke(5.dp.toPx()))
            if (!task.completed) drawArc(accent, -90f, 360 * fraction, false, style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
        }
        if (task.completed) Glyph("check", Modifier.size(28.dp), Accent)
        else if (task.urgent(now)) Text("!", color = if (task.overdue(now)) Danger else accent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        else Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(remaining.value, fontSize = 23.sp, fontWeight = FontWeight.SemiBold, color = accent, lineHeight = 25.sp)
            Text(remaining.unit, color = Muted, fontSize = 10.sp)
        }
    }
}
