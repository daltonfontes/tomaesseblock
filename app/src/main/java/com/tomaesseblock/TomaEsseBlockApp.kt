package com.tomaesseblock

import android.app.Application
import com.tomaesseblock.data.AppDatabase
import com.tomaesseblock.data.CallRepository
import com.tomaesseblock.data.ContactsLookup
import com.tomaesseblock.data.SettingsRepository
import com.tomaesseblock.service.Notifications

/** Injeção de dependências manual — simples e suficiente para o tamanho do app. */
class AppContainer(app: Application) {
    val database: AppDatabase by lazy { AppDatabase.build(app) }
    val repository: CallRepository by lazy { CallRepository(database) }
    val settings: SettingsRepository by lazy { SettingsRepository(app) }
    val contacts: ContactsLookup by lazy { ContactsLookup(app) }
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
