package com.qiaomu.prompter.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ScriptRepository(
    private val scriptDao: ScriptDao,
    scope: CoroutineScope
) {
    val scripts: StateFlow<List<Script>> =
        scriptDao.observeScripts()
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun getScript(id: String): Script? = scriptDao.getScript(id)

    suspend fun save(script: Script) {
        scriptDao.save(script.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun delete(script: Script) {
        scriptDao.delete(script)
    }

    suspend fun deleteById(id: String) {
        scriptDao.deleteById(id)
    }
}
