package com.qiaomu.prompter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.qiaomu.prompter.ai.OpenAICompatGenerator
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.settings.AiProviderConfigStore
import com.qiaomu.prompter.ui.ai.AIGenerationScreen
import com.qiaomu.prompter.ui.editor.ScriptEditorScreen
import com.qiaomu.prompter.ui.prompter.PrompterScreen
import com.qiaomu.prompter.ui.scriptlist.ScriptListScreen
import com.qiaomu.prompter.ui.settings.AppSettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as QMPrompterApp
        setContent {
            QMPrompterRoot(
                scriptRepository = app.scriptRepository,
                aiProviderConfigStore = app.aiProviderConfigStore
            )
        }
    }
}

@Composable
private fun QMPrompterRoot(
    scriptRepository: ScriptRepository,
    aiProviderConfigStore: AiProviderConfigStore
) {
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
                    ScriptListScreen(
                        scriptRepository = scriptRepository,
                        onOpenScript = { scriptId ->
                            navController.navigate("editor/$scriptId")
                        },
                        onOpenSettings = {
                            navController.navigate(ROUTE_SETTINGS)
                        },
                        onOpenAiGeneration = {
                            navController.navigate(ROUTE_AI_GENERATION)
                        }
                    )
                }
                composable(
                    route = ROUTE_EDITOR,
                    arguments = listOf(navArgument(ARG_SCRIPT_ID) {})
                ) { backStackEntry ->
                    val scriptId = backStackEntry.arguments?.getString(ARG_SCRIPT_ID)
                    if (scriptId != null) {
                        ScriptEditorScreen(
                            scriptId = scriptId,
                            scriptRepository = scriptRepository,
                            onBack = { navController.popBackStack() },
                            onStartPrompter = {
                                navController.navigate("prompter/$scriptId")
                            }
                        )
                    }
                }
                composable(
                    route = ROUTE_PROMPTER,
                    arguments = listOf(navArgument(ARG_SCRIPT_ID) {})
                ) { backStackEntry ->
                    val scriptId = backStackEntry.arguments?.getString(ARG_SCRIPT_ID)
                    if (scriptId != null) {
                        PrompterScreen(
                            scriptId = scriptId,
                            scriptRepository = scriptRepository,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(ROUTE_SETTINGS) {
                    AppSettingsScreen(
                        configStore = aiProviderConfigStore,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(ROUTE_AI_GENERATION) {
                    AIGenerationScreen(
                        scriptRepository = scriptRepository,
                        scriptGenerator = OpenAICompatGenerator(aiProviderConfigStore),
                        onBack = { navController.popBackStack() },
                        onGenerated = { scriptId ->
                            navController.navigate("editor/$scriptId") {
                                popUpTo(ROUTE_SCRIPTS)
                            }
                        }
                    )
                }
            }
        }
    }
}

private const val ROUTE_SCRIPTS = "scripts"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_AI_GENERATION = "ai-generation"
private const val ARG_SCRIPT_ID = "scriptId"
private const val ROUTE_EDITOR = "editor/{$ARG_SCRIPT_ID}"
private const val ROUTE_PROMPTER = "prompter/{$ARG_SCRIPT_ID}"
