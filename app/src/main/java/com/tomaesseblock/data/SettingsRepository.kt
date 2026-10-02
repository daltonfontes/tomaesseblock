package com.tomaesseblock.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tomaesseblock.domain.BlockSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("blocking_enabled")
        val HIDDEN = booleanPreferencesKey("block_hidden")
        val NOT_IN_CONTACTS = booleanPreferencesKey("block_not_in_contacts")
        val TELEMARKETING = booleanPreferencesKey("block_0303")
        val SPAM = booleanPreferencesKey("block_reported_spam")
        val THRESHOLD = intPreferencesKey("spam_threshold")
        val CALLER_ID = booleanPreferencesKey("show_caller_id")
        val OVERLAY = booleanPreferencesKey("show_overlay")
        val NOTIFY = booleanPreferencesKey("notify_blocked")
    }

    val settings: Flow<BlockSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun current(): BlockSettings = settings.first()

    suspend fun update(transform: (BlockSettings) -> BlockSettings) {
        context.dataStore.edit { prefs ->
            val s = transform(prefs.toSettings())
            prefs[Keys.ENABLED] = s.blockingEnabled
            prefs[Keys.HIDDEN] = s.blockHidden
            prefs[Keys.NOT_IN_CONTACTS] = s.blockNotInContacts
            prefs[Keys.TELEMARKETING] = s.blockTelemarketing0303
            prefs[Keys.SPAM] = s.blockReportedSpam
            prefs[Keys.THRESHOLD] = s.spamThreshold
            prefs[Keys.CALLER_ID] = s.showCallerId
            prefs[Keys.OVERLAY] = s.showOverlay
            prefs[Keys.NOTIFY] = s.notifyBlocked
        }
    }

    private fun Preferences.toSettings(): BlockSettings {
        val d = BlockSettings()
        return BlockSettings(
            blockingEnabled = this[Keys.ENABLED] ?: d.blockingEnabled,
            blockHidden = this[Keys.HIDDEN] ?: d.blockHidden,
            blockNotInContacts = this[Keys.NOT_IN_CONTACTS] ?: d.blockNotInContacts,
            blockTelemarketing0303 = this[Keys.TELEMARKETING] ?: d.blockTelemarketing0303,
            blockReportedSpam = this[Keys.SPAM] ?: d.blockReportedSpam,
            spamThreshold = this[Keys.THRESHOLD] ?: d.spamThreshold,
            showCallerId = this[Keys.CALLER_ID] ?: d.showCallerId,
            showOverlay = this[Keys.OVERLAY] ?: d.showOverlay,
            notifyBlocked = this[Keys.NOTIFY] ?: d.notifyBlocked,
        )
    }
}
