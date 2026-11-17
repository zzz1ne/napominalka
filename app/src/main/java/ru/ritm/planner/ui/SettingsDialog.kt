package ru.ritm.planner.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ritm.planner.BuildConfig
import ru.ritm.planner.PlannerApplication

private val appColors = listOf(0xFFBEF264, 0xFF78B8FF, 0xFFB49AFF, 0xFFFF85B0, 0xFFFFAD69, 0xFF63CDB5).map { it.toInt() }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsDialog(notificationsAllowed: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as PlannerApplication
    val appearance by app.appearance.appearance.collectAsStateWithLifecycle()
    var customColor by rememberSaveable { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Настройки") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Оформление", fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().toggleable(appearance.lightTheme, role = Role.Switch, onValueChange = app.appearance::setLightTheme)
                    .semantics { contentDescription = "Светлая тема" },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Светлая тема", modifier = Modifier.weight(1f), fontSize = 15.sp)
                    Switch(checked = appearance.lightTheme, onCheckedChange = null)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Цвет приложения", fontWeight = FontWeight.Medium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        appColors.forEach { color ->
                            val selected = appearance.accent == color
                            Box(Modifier.size(42.dp).clip(CircleShape)
                                .selectable(selected, role = Role.RadioButton, onClick = { app.appearance.setAccent(color) })
                                .semantics { contentDescription = "Цвет приложения #${String.format("%06X", color and 0xFFFFFF)}" },
                                contentAlignment = Alignment.Center) {
                                Box(Modifier.size(30.dp).clip(CircleShape).background(Color(color))
                                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center) {
                                    if (selected) Glyph("check", Modifier.size(18.dp), Color.Black)
                                }
                            }
                        }
                    }
                    OutlinedButton(onClick = { customColor = true }, modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.size(15.dp).clip(CircleShape).background(Color(appearance.accent)).border(1.dp, Line, CircleShape))
                        Spacer(Modifier.width(10.dp))
                        Text("Свой цвет приложения")
                    }
                }
                HorizontalDivider(color = Line)
                Text("Напоминания", fontWeight = FontWeight.Bold)
                Text(if (notificationsAllowed) "Уведомления разрешены." else "Разрешите уведомления, чтобы получать напоминания при закрытом приложении.", color = Muted)
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }) { Text("Настроить уведомления") }
                Text(if (app.reminders.exactAllowed()) "Точное время напоминаний разрешено." else "Android может задерживать напоминания. Для точного времени разрешите будильники и напоминания.", color = Muted, fontSize = 12.sp)
                if (Build.VERSION.SDK_INT >= 31 && !app.reminders.exactAllowed()) OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }) { Text("Разрешить точное время") }
                HorizontalDivider(color = Line)
                Text("Все задачи хранятся только на этом устройстве. Интернет и аккаунт не нужны.", color = Muted, fontSize = 12.sp)
                Text("Круг — доля времени от создания задачи до срока. При остатке 5% или меньше появится «!».", color = Muted, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("v${BuildConfig.VERSION_NAME}", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Готово") }
            }
        })
    if (customColor) ColorPicker(appearance.accent, { app.appearance.setAccent(it); customColor = false }, { customColor = false })
}
