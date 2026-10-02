package com.tomaesseblock.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tomaesseblock.MainActivity
import com.tomaesseblock.R
import com.tomaesseblock.domain.BlockReason
import com.tomaesseblock.domain.CallDecision
import com.tomaesseblock.domain.PhoneNumbers

object Notifications {
    private const val CHANNEL_BLOCKED = "blocked_calls"
    private const val CHANNEL_CALLER_ID = "caller_id"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_BLOCKED, "Chamadas bloqueadas", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Aviso quando uma chamada é bloqueada"
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_CALLER_ID, "Identificador de chamadas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerta de possível spam enquanto o telefone toca"
            },
        )
    }

    fun showBlocked(context: Context, number: String, decision: CallDecision.Block) {
        val notification = NotificationCompat.Builder(context, CHANNEL_BLOCKED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Chamada bloqueada: ${PhoneNumbers.format(number)}")
            .setContentText(decision.label)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .apply {
                // "Desbloquear" só desfaz bloqueios por lista/denúncia; os demais são ajustes globais.
                if (decision.reason == BlockReason.BLOCK_LIST || decision.reason == BlockReason.SPAM_REPORTED) {
                    addAction(0, "Desbloquear", actionIntent(context, NotificationActionReceiver.ACTION_UNBLOCK, number))
                }
            }
            .build()
        notify(context, number.hashCode(), notification)
    }

    fun showCallerId(context: Context, number: String, identification: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_CALLER_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠ $identification")
            .setContentText(PhoneNumbers.format(number))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(openAppIntent(context))
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .addAction(0, "Bloquear", actionIntent(context, NotificationActionReceiver.ACTION_BLOCK, number))
            .build()
        notify(context, number.hashCode(), notification)
    }

    fun cancel(context: Context, number: String) {
        NotificationManagerCompat.from(context).cancel(number.hashCode())
    }

    @SuppressLint("MissingPermission") // verificado logo abaixo
    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun openAppIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun actionIntent(context: Context, action: String, number: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (action + number).hashCode(),
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action)
                .putExtra(NotificationActionReceiver.EXTRA_NUMBER, number),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
