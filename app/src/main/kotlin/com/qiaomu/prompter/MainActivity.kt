package com.qiaomu.prompter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.poc.SpeechRecognizerPocActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as QMPrompterApp
        setContent {
            QMPrompterRoot(app.scriptRepository)
        }
    }
}

@Composable
private fun QMPrompterRoot(scriptRepository: ScriptRepository) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = ROUTE_SCRIPTS
            ) {
                composable(ROUTE_SCRIPTS) {
                    ScriptShellScreen(scriptRepository)
                }
            }
        }
    }
}

@Composable
private fun ScriptShellScreen(scriptRepository: ScriptRepository) {
    val scripts by scriptRepository.scripts.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "QMPrompter",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = if (scripts.isEmpty()) {
                    "No scripts ready"
                } else {
                    "${scripts.size} script ready"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    context.startActivity(
                        Intent(context, SpeechRecognizerPocActivity::class.java)
                    )
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Speech PoC")
            }
        }
    }
}

private const val ROUTE_SCRIPTS = "scripts"
