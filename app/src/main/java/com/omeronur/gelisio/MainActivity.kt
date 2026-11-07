package com.omeronur.gelisio

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.omeronur.gelisio.notify.Notifier
import com.omeronur.gelisio.ui.GelisioApp
import com.omeronur.gelisio.ui.Nav
import com.omeronur.gelisio.ui.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        Notifier.ensureChannels(this)
        Notifier.reschedule(applicationContext)
        route(intent)
        setContent { GelisioApp() }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        route(intent)
    }

    /** Bildirime dokununca ilgili ekran açılır. */
    private fun route(intent: Intent?) {
        when (intent?.getStringExtra("open")) {
            "plan" -> { Nav.stack.clear(); Nav.tab.intValue = 1; Nav.push(Screen.PlanWizard) }
            "deep" -> { Nav.stack.clear(); Nav.tab.intValue = 2; Nav.push(Screen.DeepSession(intent.getStringExtra("extra")?.ifBlank { null })) }
            "week" -> { Nav.stack.clear(); Nav.tab.intValue = 1 }
            "today" -> { Nav.stack.clear(); Nav.tab.intValue = 0 }
        }
        intent?.removeExtra("open")
    }
}
