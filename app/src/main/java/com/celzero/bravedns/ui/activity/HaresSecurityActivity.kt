package com.celzero.bravedns.ui.activity

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.celzero.bravedns.R
import com.celzero.bravedns.hares.HaresNotifications
import com.celzero.bravedns.hares.HaresThreatEvent
import com.celzero.bravedns.hares.HaresThreatStore
import com.celzero.bravedns.hares.HaresUrlScanner
import com.celzero.bravedns.service.PersistentState
import com.celzero.bravedns.ui.BaseActivity
import com.celzero.bravedns.util.Themes
import org.koin.android.ext.android.inject

class HaresSecurityActivity : BaseActivity(R.layout.activity_hares_security) {
    private val persistentState by inject<PersistentState>()
    private lateinit var linkInput: EditText
    private lateinit var resultText: TextView
    private lateinit var historyText: TextView
    private lateinit var statsText: TextView
    private lateinit var store: HaresThreatStore

    private fun Context.isDarkThemeOn(): Boolean =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    override fun onCreate(savedInstanceState: Bundle?) {
        theme.applyStyle(Themes.getCurrentTheme(isDarkThemeOn(), persistentState.theme), true)
        super.onCreate(savedInstanceState)
        store = HaresThreatStore(this)
        linkInput = findViewById(R.id.hares_link_input)
        resultText = findViewById(R.id.hares_scan_result)
        historyText = findViewById(R.id.hares_incident_history)
        statsText = findViewById(R.id.hares_incident_stats)
        findViewById<Button>(R.id.hares_scan_button).setOnClickListener { scanCurrentLink() }
        findViewById<Button>(R.id.hares_paste_button).setOnClickListener { pasteLink() }
        findViewById<Button>(R.id.hares_clear_history_button).setOnClickListener { clearHistory() }
        renderHistory()
        requestNotificationPermissionIfNeeded()
        handleIncomingShare(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingShare(intent)
    }

    private fun handleIncomingShare(source: Intent?) {
        if (source?.action != Intent.ACTION_SEND) return
        val shared = source.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.trim()
        if (!shared.isNullOrBlank()) {
            linkInput.setText(shared)
            linkInput.setSelection(linkInput.text.length)
            scanCurrentLink()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 7301)
    }

    private fun scanCurrentLink() {
        val raw = linkInput.text.toString().trim()
        if (raw.isBlank()) {
            Toast.makeText(this, "أدخل رابطاً أولاً.", Toast.LENGTH_SHORT).show()
            return
        }
        val event = HaresUrlScanner.scan(raw)
        store.add(event)
        resultText.setTextColor(when (event.level) {
            HaresThreatEvent.Level.HIGH -> 0xFFFF7777.toInt()
            HaresThreatEvent.Level.MEDIUM -> 0xFFFFC45C.toInt()
            HaresThreatEvent.Level.LOW -> 0xFF65D6A4.toInt()
        })
        resultText.text = buildString {
            append("مستوى الخطورة: ${event.level}\n\n")
            append(event.reasons.joinToString("\n• ", prefix = "• "))
            append("\n\nهذا تحليل محلي أولي، وليس شهادة سلامة.")
        }
        HaresNotifications.show(this, event)
        renderHistory()
    }

    private fun pasteLink() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val shared = if (clipboard.hasPrimaryClip()) {
            clipboard.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()
        } else null
        if (shared.isNullOrBlank()) {
            Toast.makeText(this, "لا يوجد رابط منسوخ.", Toast.LENGTH_SHORT).show()
        } else {
            linkInput.setText(shared.trim())
            linkInput.setSelection(linkInput.text.length)
        }
    }

    private fun clearHistory() {
        store.clear()
        resultText.text = "تم مسح سجل الفحوصات."
        renderHistory()
    }

    private fun renderHistory() {
        val events = store.all()
        val high = events.count { it.level == HaresThreatEvent.Level.HIGH }
        val medium = events.count { it.level == HaresThreatEvent.Level.MEDIUM }
        statsText.text = "الفحوصات: ${events.size}   •   مرتفع: $high   •   متوسط: $medium"
        historyText.text = events.take(15).joinToString("\n\n") { event ->
            val time = java.text.DateFormat.getDateTimeInstance(
                java.text.DateFormat.SHORT, java.text.DateFormat.SHORT
            ).format(java.util.Date(event.timestamp))
            "$time\n${event.level} — ${event.url}\n${event.reasons.joinToString("، ")}"
        }.ifBlank { "لا توجد حوادث مسجلة حتى الآن." }
    }
}
