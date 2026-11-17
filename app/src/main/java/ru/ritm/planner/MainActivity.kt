package ru.ritm.planner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ru.ritm.planner.ui.PlannerApp
import ru.ritm.planner.ui.RitmTheme

class MainActivity : ComponentActivity() {
    private var requestedTask by mutableStateOf<String?>(null)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateSystemBars((application as PlannerApplication).appearance.appearance.value.lightTheme)
        requestedTask = intent.getStringExtra("taskId")
        setContent {
            val appearance by (application as PlannerApplication).appearance.appearance.collectAsStateWithLifecycle()
            LaunchedEffect(appearance.lightTheme) { updateSystemBars(appearance.lightTheme) }
            RitmTheme(appearance) { PlannerApp(requestedTask, { requestedTask = null }) }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTask = intent.getStringExtra("taskId")
    }
    private fun updateSystemBars(light: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (light) SystemBarStyle.light(transparent, transparent) else SystemBarStyle.dark(transparent)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
