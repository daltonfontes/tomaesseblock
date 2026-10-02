package com.tomaesseblock.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.tomaesseblock.R
import com.tomaesseblock.TomaEsseBlockApp
import com.tomaesseblock.domain.AlertLevel
import com.tomaesseblock.domain.PhoneNumbers
import com.tomaesseblock.domain.RuleType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Cartão "quem está ligando" desenhado por cima da tela de chamada (estilo Whoscall).
 *
 * Usa uma janela TYPE_APPLICATION_OVERLAY, que exige a permissão "Exibir sobre outros apps".
 * Some quando a chamada é atendida/encerrada, quando o usuário fecha, ou após um tempo limite.
 * Pode ser arrastado para cima/baixo.
 */
object CallerIdOverlay {

    private const val TIMEOUT_WITH_CALL_STATE_MS = 90_000L
    private const val TIMEOUT_WITHOUT_CALL_STATE_MS = 30_000L
    private const val TIMEOUT_PREVIEW_MS = 10_000L

    private val main = Handler(Looper.getMainLooper())
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val hideRunnable = Runnable { hideNow() }

    private var current: View? = null
    private var windowManager: WindowManager? = null
    private var watcher: CallStateWatcher? = null

    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * Mostra o aviso. Retorna false se não há permissão para desenhar sobre outros apps.
     * [preview] mostra um exemplo (sem acompanhar chamada e sem botão de bloquear).
     */
    fun show(
        context: Context,
        number: String,
        identification: String,
        level: AlertLevel,
        preview: Boolean = false,
    ): Boolean {
        val app = context.applicationContext
        if (!canDrawOverlays(app)) return false
        main.post { showOnMain(app, number, identification, level, preview) }
        return true
    }

    fun hide() {
        main.post { hideNow() }
    }

    @SuppressLint("ClickableViewAccessibility", "InflateParams")
    private fun showOnMain(
        app: Context,
        number: String,
        identification: String,
        level: AlertLevel,
        preview: Boolean,
    ) {
        hideNow()
        val wm = app.getSystemService(WindowManager::class.java)
        val themed = ContextThemeWrapper(app, android.R.style.Theme_DeviceDefault_Light)
        val view = LayoutInflater.from(themed).inflate(R.layout.overlay_caller_id, null)

        val (badge, color) = style(level)
        view.findViewById<View>(R.id.overlay_header).background.mutate().setTint(color)
        view.findViewById<TextView>(R.id.overlay_badge).text = if (preview) "$badge (exemplo)" else badge
        view.findViewById<TextView>(R.id.overlay_title).text = identification
        view.findViewById<TextView>(R.id.overlay_number).text = PhoneNumbers.format(number)
        view.findViewById<ImageButton>(R.id.overlay_close).setOnClickListener { hideNow() }

        val block = view.findViewById<Button>(R.id.overlay_block)
        if (preview || number.isEmpty()) {
            block.visibility = View.GONE
        } else {
            block.setOnClickListener {
                val repository = (app as TomaEsseBlockApp).container.repository
                ioScope.launch { repository.addRule(number, RuleType.EXACT, identification) }
                Toast.makeText(app, "Número bloqueado. As próximas chamadas serão recusadas.", Toast.LENGTH_LONG).show()
                hideNow()
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            // Abaixo do nome na tela de chamada e acima dos botões de atender/recusar.
            y = (app.resources.displayMetrics.heightPixels * 0.22f).toInt()
        }

        // Arrastar verticalmente.
        var startY = 0
        var touchStartRawY = 0f
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = params.y
                    touchStartRawY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.y = (startY + (event.rawY - touchStartRawY)).toInt().coerceAtLeast(0)
                    runCatching { wm.updateViewLayout(v, params) }
                    true
                }
                else -> false
            }
        }

        try {
            wm.addView(view, params)
        } catch (_: RuntimeException) {
            // Permissão revogada ou janela inválida: simplesmente não mostra.
            return
        }
        current = view
        windowManager = wm

        val timeout = when {
            preview -> TIMEOUT_PREVIEW_MS
            watchCall(app) -> TIMEOUT_WITH_CALL_STATE_MS
            else -> TIMEOUT_WITHOUT_CALL_STATE_MS
        }
        main.postDelayed(hideRunnable, timeout)
    }

    /** Fecha o aviso quando a chamada é atendida, ou encerrada depois de ter tocado. */
    private fun watchCall(app: Context): Boolean {
        var sawRinging = false
        val w = CallStateWatcher(app) { state ->
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> sawRinging = true
                TelephonyManager.CALL_STATE_OFFHOOK -> hideNow()
                TelephonyManager.CALL_STATE_IDLE -> if (sawRinging) hideNow()
            }
        }
        if (!w.start()) return false
        watcher = w
        return true
    }

    private fun hideNow() {
        main.removeCallbacks(hideRunnable)
        watcher?.stop()
        watcher = null
        current?.let { view -> runCatching { windowManager?.removeView(view) } }
        current = null
        windowManager = null
    }

    private fun style(level: AlertLevel): Pair<String, Int> = when (level) {
        AlertLevel.DANGER -> "POSSÍVEL SPAM" to Color.parseColor("#D32F2F")
        AlertLevel.WARNING -> "SUSPEITO" to Color.parseColor("#EF6C00")
        AlertLevel.INFO, AlertLevel.NONE -> "IDENTIFICADO" to Color.parseColor("#1565C0")
    }
}
