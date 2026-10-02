package com.tomaesseblock.overlay

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/**
 * Observa o estado da chamada (tocando / atendida / encerrada) para fechar o aviso na hora certa.
 * Requer READ_PHONE_STATE; sem a permissão, [start] retorna false. Deve ser usado na thread principal.
 */
class CallStateWatcher(private val context: Context, private val onState: (Int) -> Unit) {

    private val telephony = context.getSystemService(TelephonyManager::class.java)
    private var modernCallback: Any? = null
    private var legacyListener: PhoneStateListener? = null

    fun start(): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                modernCallback = Api31.register(context, telephony, onState)
            } else {
                legacyListener = LegacyListener(onState).also {
                    @Suppress("DEPRECATION")
                    telephony.listen(it, PhoneStateListener.LISTEN_CALL_STATE)
                }
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun stop() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            modernCallback?.let { Api31.unregister(telephony, it) }
        }
        legacyListener?.let {
            @Suppress("DEPRECATION")
            telephony.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        modernCallback = null
        legacyListener = null
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    private class LegacyListener(private val onState: (Int) -> Unit) : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) = onState(state)
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private object Api31 {
        fun register(context: Context, telephony: TelephonyManager, onState: (Int) -> Unit): Any {
            val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) = onState(state)
            }
            telephony.registerTelephonyCallback(context.mainExecutor, callback)
            return callback
        }

        fun unregister(telephony: TelephonyManager, callback: Any) {
            telephony.unregisterTelephonyCallback(callback as TelephonyCallback)
        }
    }
}
