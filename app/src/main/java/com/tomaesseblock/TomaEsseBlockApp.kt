package com.tomaesseblock

import android.app.Application
import com.tomaesseblock.data.AppDatabase
import com.tomaesseblock.data.CallRepository
import com.tomaesseblock.data.ContactsLookup
import com.tomaesseblock.data.SettingsRepository
import com.tomaesseblock.service.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Injeção de dependências manual — simples e suficiente para o tamanho do app. */
class AppContainer(app: Application) {
    val database: AppDatabase by lazy { AppDatabase.build(app) }
    val repository: CallRepository by lazy { CallRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(app) }
    val contacts: ContactsLookup by lazy { ContactsLookup(app) }

    /**
     * Escopo que vive enquanto o processo do app viver. O serviço de triagem é desligado pelo
     * sistema logo depois de responder à chamada, então o que vem depois (gravar no histórico,
     * notificar) precisa rodar aqui, e não num escopo cancelado junto com o serviço.
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

class TomaEsseBlockApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifications.createChannels(this)
    }
}
