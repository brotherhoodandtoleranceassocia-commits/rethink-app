package com.celzero.bravedns.hares
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class HaresThreatStore(context: Context) {
    private val prefs = context.getSharedPreferences("hares_threats", Context.MODE_PRIVATE)
    @Synchronized fun add(event: HaresThreatEvent) {
        val items = JSONArray(prefs.getString("events", "[]"))
        val updated = JSONArray()
        updated.put(JSONObject().apply {
            put("url", event.url); put("level", event.level.name)
            put("reasons", JSONArray(event.reasons)); put("timestamp", event.timestamp)
        })
        for (i in 0 until minOf(items.length(), 99)) updated.put(items.getJSONObject(i))
        prefs.edit().putString("events", updated.toString()).apply()
    }
    fun clear() { prefs.edit().remove("events").apply() }
    fun all(): List<HaresThreatEvent> {
        val items = JSONArray(prefs.getString("events", "[]"))
        return (0 until items.length()).mapNotNull { i ->
            runCatching {
                val obj = items.getJSONObject(i); val reasons = obj.getJSONArray("reasons")
                HaresThreatEvent(obj.optString("url"), HaresThreatEvent.Level.valueOf(obj.optString("level", "LOW")),
                    (0 until reasons.length()).map { reasons.getString(it) }, obj.optLong("timestamp"))
            }.getOrNull()
        }
    }
}
