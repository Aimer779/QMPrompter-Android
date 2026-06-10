package com.qiaomu.prompter

import android.app.Application
import com.qiaomu.prompter.data.AppDatabase
import com.qiaomu.prompter.data.ScriptRepository
import com.qiaomu.prompter.settings.AiProviderConfigStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class QMPrompterApp : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val scriptRepository: ScriptRepository by lazy {
        ScriptRepository(database.scriptDao(), applicationScope)
    }

    val aiProviderConfigStore: AiProviderConfigStore by lazy {
        AiProviderConfigStore(this)
    }
}
