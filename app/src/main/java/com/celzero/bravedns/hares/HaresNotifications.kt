package com.celzero.bravedns.hares
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.celzero.bravedns.R

object HaresNotifications {
    private const val CHANNEL_ID = "hares_security_alerts"
    fun show(context: Context, event: HaresThreatEvent) {
        if (event.level == HaresThreatEvent.Level.LOW) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "تنبيهات حارس الأمنية", NotificationManager.IMPORTANCE_HIGH))
        val title = if (event.level == HaresThreatEvent.Level.HIGH) "تحذير أمني مرتفع" else "تنبيه أمني"
        val body = event.reasons.joinToString(" • ").take(240)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_icon).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("\${event.url}\\n$body"))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        NotificationManagerCompat.from(context).notify((event.timestamp % Int.MAX_VALUE).toInt(), notification)
    }
}
