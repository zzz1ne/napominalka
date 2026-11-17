package ru.ritm.planner.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.ritm.planner.domain.Task
import ru.ritm.planner.domain.validateTask
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZoneId

private val palette = listOf(0xFFBEF264, 0xFFA7C7FF, 0xFFC6AAFF, 0xFFFFB7D3, 0xFFFFB98A, 0xFFF5D76E, 0xFF78DFCB)
    .map { it.toInt() }
private val reminderOptions = listOf("За день" to 86_400_000L, "За час" to 3_600_000L, "За 15 минут" to 900_000L, "В срок" to 0L, "Своё время" to -1L)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditor(task: Task?, saving: Boolean, onDismiss: () -> Unit, onSave: (Task) -> Unit, onDelete: (Task) -> Unit) {
    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title ?: "") }
    var description by rememberSaveable(task?.id) { mutableStateOf(task?.description ?: "") }
    var deadline by rememberSaveable(task?.id) { mutableLongStateOf(task?.deadline ?: ZonedDateTime.now().plusDays(2).withHour(18).withMinute(0).withSecond(0).withNano(0).toInstant().toEpochMilli()) }
    var color by rememberSaveable(task?.id) { mutableIntStateOf(task?.color ?: palette.first()) }
    var reminders by rememberSaveable(task?.id) { mutableStateOf(task == null || task.reminderAt != null) }
    var offset by rememberSaveable(task?.id) { mutableLongStateOf(task?.reminderAt?.let { at -> (task.deadline - at).takeIf { duration -> reminderOptions.any { it.second == duration } } ?: -1L } ?: 86_400_000L) }
    var customReminder by rememberSaveable(task?.id) { mutableLongStateOf(task?.reminderAt ?: (deadline - 3_600_000)) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var colorPicker by rememberSaveable { mutableStateOf(false) }
    var deleteConfirm by rememberSaveable { mutableStateOf(false) }
    var discardConfirm by rememberSaveable { mutableStateOf(false) }
    val reminderAt = if (!reminders) null else if (offset == -1L) customReminder else deadline - offset
    val dirty = if (task == null) title.isNotBlank() || description.isNotBlank() else
        title != task.title || description != task.description || deadline != task.deadline || color != task.color || reminderAt != task.reminderAt
    fun dismiss() { if (!saving) { if (dirty) discardConfirm = true else onDismiss() } }

    Dialog(onDismissRequest = ::dismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        RitmTheme {
            Surface(Modifier.fillMaxSize(), color = Background) {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = ::dismiss) { Glyph("close", Modifier.size(24.dp), label = "Закрыть редактор") }
                        Text(if (task == null) "Новая задача" else "Ваша задача", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        Box(Modifier.padding(end = 12.dp).size(12.dp).clip(CircleShape).background(Color(color)))
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column {
                            Text("Дайте планам форму.", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.8).sp)
                        }
                        OutlinedTextField(title, { if (it.length <= 120) title = it; error = null }, label = { Text("Название") },
                            placeholder = { Text("Что нужно сделать?") }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp), singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                        OutlinedTextField(description, { if (it.length <= 5000) description = it }, label = { Text("Описание") },
                            placeholder = { Text("Детали, которые не хочется забыть") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 6,
                            shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionLabel("КОГДА ВЫПОЛНИТЬ")
                            DateTimeFields(deadline, { deadline = it; error = null }, "Дедлайн")
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionLabel("ЦВЕТ ЗАДАЧИ")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                palette.forEach { choice ->
                                    Box(Modifier.size(44.dp).clip(CircleShape).clickable { color = choice }
                                        .semantics { contentDescription = "Цвет #${String.format("%06X", choice and 0xFFFFFF)}" }, contentAlignment = Alignment.Center) {
                                        Box(Modifier.size(32.dp).clip(CircleShape).background(Color(choice))
                                            .then(if (color == choice) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                                            if (color == choice) Glyph("check", Modifier.size(19.dp), Color.Black)
                                        }
                                    }
                                }
                                Box(Modifier.size(44.dp).clip(CircleShape).clickable { colorPicker = true }
                                    .semantics { contentDescription = "Выбрать любой цвет" }, contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(32.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))), contentAlignment = Alignment.Center) {
                                        Glyph("plus", Modifier.size(19.dp), Color.White)
                                    }
                                }
                            }
                            if (color !in palette) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(16.dp).clip(CircleShape).background(Color(color)))
                                Text("Ваш цвет · #${String.format("%06X", color and 0xFFFFFF)}", color = Muted, fontSize = 12.sp)
                            }
                        }
                        Surface(color = Card, shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Glyph("bell", Modifier.size(23.dp), Accent)
                                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                        Text("Напомнить", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                        Text("Чтобы важное не потерялось", color = Muted, fontSize = 11.sp)
                                    }
                                    Switch(checked = reminders, onCheckedChange = { reminders = it; error = null }, modifier = Modifier.semantics { contentDescription = "Напоминание" })
                                }
                                if (reminders) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        reminderOptions.forEach { (label, value) ->
                                            FilterChip(selected = offset == value, onClick = { offset = value; error = null }, label = { Text(label, fontSize = 12.sp) },
                                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Accent, selectedLabelColor = OnAccent))
                                        }
                                    }
                                    if (offset == -1L) DateTimeFields(customReminder, { customReminder = it; error = null }, "Напоминание")
                                    Text("Напомним ${dateText(reminderAt!!)}", fontSize = 12.sp, color = Muted)
                                    if (reminderAt <= System.currentTimeMillis()) Text("Это время уже прошло. Выберите другой вариант.", color = Danger, fontSize = 12.sp)
                                }
                            }
                        }
                        if (task != null) TextButton(onClick = { deleteConfirm = true }, colors = ButtonDefaults.textButtonColors(contentColor = Danger)) { Text("Удалить задачу") }
                        if (error != null) Text(error!!, color = Danger, fontSize = 13.sp, modifier = Modifier.semantics { liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite })
                    }
                    Box(Modifier.fillMaxWidth().background(Background).padding(horizontal = 24.dp, vertical = 12.dp)) {
                        Button(onClick = {
                            val now = System.currentTimeMillis()
                            error = validateTask(title, deadline, reminderAt, now)
                            if (error == null) onSave((task ?: Task(title = title.trim(), deadline = deadline)).copy(
                                title = title.trim(), description = description.trim(), deadline = deadline,
                                color = color, reminderAt = reminderAt, updatedAt = now))
                        }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp)) {
                            if (saving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = OnAccent)
                            else { Text(if (task == null) "Создать задачу" else "Сохранить изменения", fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(10.dp)); Glyph("arrow", Modifier.size(21.dp), OnAccent) }
                        }
                    }
                }
            }
            if (colorPicker) ColorPicker(color, { color = it; colorPicker = false }, { colorPicker = false })
            if (deleteConfirm && task != null) AlertDialog(onDismissRequest = { deleteConfirm = false }, title = { Text("Удалить задачу?") },
                text = { Text("«${task.title}» и её напоминание будут удалены с устройства.") },
                confirmButton = { TextButton(onClick = { deleteConfirm = false; onDelete(task) }) { Text("Удалить", color = Danger) } },
                dismissButton = { TextButton(onClick = { deleteConfirm = false }) { Text("Оставить") } })
            if (discardConfirm) AlertDialog(onDismissRequest = { discardConfirm = false }, title = { Text("Закрыть без сохранения?") },
                text = { Text("Изменения в задаче не сохранятся.") },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
                dismissButton = { TextButton(onClick = { discardConfirm = false }) { Text("Продолжить") } })
        }
    }
}

@Composable
private fun SectionLabel(text: String) { Text(text, color = Muted, fontSize = 10.sp, letterSpacing = 1.8.sp) }

@Composable
private fun DateTimeFields(value: Long, onChange: (Long) -> Unit, prefix: String) {
    val context = LocalContext.current
    val pickerTheme = if (LocalAppearance.current.lightTheme) android.R.style.Theme_Material_Light_Dialog_Alert else android.R.style.Theme_Material_Dialog_Alert
    val time = Instant.ofEpochMilli(value).atZone(ZoneId.systemDefault())
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(onClick = {
            DatePickerDialog(context, pickerTheme, { _, year, month, day ->
                // Construct by date to avoid month-end clamping while moving between years/months.
                onChange(java.time.LocalDate.of(year, month + 1, day).atTime(time.toLocalTime()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
            }, time.year, time.monthValue - 1, time.dayOfMonth).show()
        }, color = Card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line), modifier = Modifier.weight(1.4f)
            .semantics { contentDescription = "$prefix: дата" }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Glyph("calendar", Modifier.size(19.dp), Accent); Text(dateText(value, "d MMM yyyy"), fontSize = 13.sp)
            }
        }
        Surface(onClick = {
            TimePickerDialog(context, pickerTheme, { _, hour, minute -> onChange(time.withHour(hour).withMinute(minute).withSecond(0).withNano(0).toInstant().toEpochMilli()) }, time.hour, time.minute, true).show()
        }, color = Card, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line), modifier = Modifier.weight(1f)
            .semantics { contentDescription = "$prefix: время" }) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Glyph("clock", Modifier.size(19.dp), Accent); Text(dateText(value, "HH:mm"), fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun ColorPicker(initial: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var hue by rememberSaveable { mutableFloatStateOf(hsv[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(hsv[1]) }
    var brightness by rememberSaveable { mutableFloatStateOf(hsv[2]) }
    val selected = Color.hsv(hue, saturation, brightness)
    var hex by rememberSaveable { mutableStateOf(String.format("%06X", initial and 0xFFFFFF)) }
    fun updateHex() { hex = String.format("%06X", Color.hsv(hue, saturation, brightness).toArgb() and 0xFFFFFF) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Ваш цвет") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp)).background(selected).border(1.dp, Line, RoundedCornerShape(14.dp)))
            Text("Оттенок", color = Muted, fontSize = 12.sp)
            Slider(hue, { hue = it; updateHex() }, valueRange = 0f..359.99f, modifier = Modifier.semantics { contentDescription = "Оттенок" })
            Text("Насыщенность", color = Muted, fontSize = 12.sp)
            Slider(saturation, { saturation = it; updateHex() }, modifier = Modifier.semantics { contentDescription = "Насыщенность" })
            Text("Яркость", color = Muted, fontSize = 12.sp)
            Slider(brightness, { brightness = it; updateHex() }, modifier = Modifier.semantics { contentDescription = "Яркость" })
            OutlinedTextField(hex, { input ->
                hex = input.removePrefix("#").uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                if (hex.length == 6) {
                    val parsed = android.graphics.Color.parseColor("#$hex")
                    android.graphics.Color.colorToHSV(parsed, hsv)
                    hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]
                }
            }, label = { Text("HEX · 6 символов") }, prefix = { Text("#") }, singleLine = true, isError = hex.length != 6)
        } }, confirmButton = { TextButton(onClick = { onSelect(selected.toArgb()) }, enabled = hex.length == 6) { Text("Выбрать") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } })
}
