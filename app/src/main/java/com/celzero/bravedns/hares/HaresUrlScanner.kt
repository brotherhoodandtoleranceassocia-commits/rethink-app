package com.celzero.bravedns.hares
import android.net.Uri
import java.net.IDN
import java.util.Locale

/** Offline URL heuristics; a LOW result does not prove that a URL is safe. */
object HaresUrlScanner {
    private val shortDomains = setOf("bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly", "cutt.ly", "rb.gy")
    private val suspiciousTerms = listOf("verify", "wallet", "bank", "login", "account", "password", "recover", "security-check", "bonus", "payment", "confirm", "unlock")

    fun scan(raw: String): HaresThreatEvent {
        val reasons = linkedSetOf<String>()
        val value = raw.trim()
        if (value.isEmpty()) return HaresThreatEvent(value, HaresThreatEvent.Level.LOW, listOf("الرابط فارغ"))
        if (value.any { it.isWhitespace() || it.isISOControl() }) reasons += "الرابط يحتوي على مسافات أو محارف تحكم غير صالحة"
        val candidate = if ("://" in value) value else "https://$value"
        val uri = runCatching { Uri.parse(candidate) }.getOrNull()
        if (uri == null) reasons += "تعذر تحليل عنوان الرابط"
        val scheme = uri?.scheme?.lowercase(Locale.ROOT).orEmpty()
        if (scheme !in setOf("http", "https")) reasons += "نوع البروتوكول غير مدعوم أو غير آمن"
        else if (scheme == "http") reasons += "الرابط يستخدم HTTP غير المشفر"
        val authority = candidate.substringAfter("://", "").substringBefore('/').substringBefore('?').substringBefore('#')
        if ('@' in authority || uri?.userInfo != null) reasons += "وجود بيانات مستخدم داخل عنوان الرابط قد يخفي النطاق الحقيقي"
        val rawHost = uri?.host.orEmpty().trimEnd('.')
        if (rawHost.isBlank()) reasons += "اسم النطاق مفقود أو غير صالح"
        val host = runCatching { IDN.toASCII(rawHost, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT) }.getOrNull().orEmpty()
        if (rawHost.isNotBlank() && host.isBlank()) reasons += "اسم النطاق يحتوي على محارف غير صالحة"
        if (host.split('.').any { it.startsWith("xn--") }) reasons += "اسم النطاق يستخدم ترميز Punycode وقد يخفي أحرفاً متشابهة"
        if (host.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) {
            val valid = host.split('.').all { it.toIntOrNull()?.let { n -> n in 0..255 } == true }
            reasons += if (valid) "الرابط يعتمد على عنوان IP بدلاً من اسم نطاق معروف" else "عنوان IPv4 غير صالح"
        }
        if (host.count { it == '-' } >= 3) reasons += "عدد كبير من الشرطات في اسم النطاق"
        if (host.length > 45) reasons += "اسم النطاق طويل بشكل غير معتاد"
        if (host.split('.').size >= 5) reasons += "عدد كبير من النطاقات الفرعية"
        if ((uri?.port ?: -1) !in setOf(-1, 80, 443)) reasons += "منفذ اتصال غير شائع"
        if (host in shortDomains) reasons += "رابط مختصر يخفي الوجهة النهائية ويحتاج إلى تحقق إضافي"
        if (suspiciousTerms.any { value.lowercase(Locale.ROOT).contains(it) }) reasons += "العنوان يتضمن كلمات شائعة في رسائل التصيد؛ الكلمة وحدها لا تثبت الاحتيال"
        if (Regex("""(?i)%3a%2f%2f|%40""").containsMatchIn(value) || value.indexOf(92.toChar()) >= 0) reasons += "العنوان يحتوي على ترميز أو شرطات عكسية قد تُستخدم للتمويه"
        val level = when {
            reasons.any { it.contains("بيانات مستخدم") || it.contains("غير مدعوم") || it.contains("غير صالح") || it.contains("مسافات أو محارف") } -> HaresThreatEvent.Level.HIGH
            reasons.isNotEmpty() -> HaresThreatEvent.Level.MEDIUM
            else -> HaresThreatEvent.Level.LOW
        }
        return HaresThreatEvent(value, level, reasons.toList().ifEmpty {
            listOf("لم تُكتشف مؤشرات مريبة في الفحوصات المحلية المحدودة؛ هذا لا يثبت أن الرابط آمن")
        })
    }
}
