package com.lana.kitchen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** الإشعارات: قناة تنبيه صوتي + تسجيل الجهاز في الخادم لاستقبال إشعارات الطلبات الجديدة */
object PushRegister {
  const val SITE = "https://lana-snacks.menutek.pro"
  const val CHANNEL_ID = "new_orders"

  fun createChannel(context: Context) {
    if (Build.VERSION.SDK_INT < 26) return
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(CHANNEL_ID, "الطلبات الجديدة", NotificationManager.IMPORTANCE_HIGH)
    channel.description = "تنبيه صوتي عالٍ عند وصول طلب جديد"
    channel.enableVibration(true)
    channel.vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 600)
    val attrs = AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_NOTIFICATION)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
    channel.setSound(Settings.System.DEFAULT_NOTIFICATION_URI, attrs)
    nm.createNotificationChannel(channel)
  }

  /**
   * يأخذ رمز FCM للجهاز ويسجّله في الخادم مربوطاً برمز الطابعة.
   * الاتصال بالخادم يتم في خيط خلفي (أندرويد يمنع الاتصال بالإنترنت من الخيط الرئيسي).
   * onResult يستقبل رسالة عربية بالنتيجة لعرضها للمستخدم.
   */
  fun register(context: Context, baseUrl: String, printerToken: String?, onResult: ((Boolean, String) -> Unit)? = null) {
    if (printerToken.isNullOrBlank()) { onResult?.invoke(false, "🔕 أدخل رمز الطابعة لتفعيل الإشعارات"); return }
    try {
      FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (!task.isSuccessful || task.result.isNullOrBlank()) {
          onResult?.invoke(false, "🔕 تعذر الحصول على رمز الإشعارات — تأكد من خدمات Google والإنترنت")
          return@addOnCompleteListener
        }
        val fcm = task.result
        Thread {
          try {
            val c = URL("$baseUrl/api/public/push/register").openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.doOutput = true
            c.connectTimeout = 10000; c.readTimeout = 15000
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use {
              it.write(
                JSONObject()
                  .put("token", printerToken)
                  .put("fcm_token", fcm)
                  .put("device_name", "${Build.MANUFACTURER} ${Build.MODEL}")
                  .toString().toByteArray(),
              )
            }
            val code = c.responseCode
            try { (if (code < 400) c.inputStream else c.errorStream)?.close() } catch (_: Exception) {}
            c.disconnect()
            when (code) {
              in 200..299 -> onResult?.invoke(true, "🔔 الإشعارات مفعّلة")
              401 -> onResult?.invoke(false, "🔕 رمز الطابعة غير صحيح — الإشعارات غير مفعّلة")
              else -> onResult?.invoke(false, "🔕 فشل تفعيل الإشعارات (رمز $code)")
            }
          } catch (e: Exception) {
            onResult?.invoke(false, "🔕 فشل تفعيل الإشعارات — تحقق من الإنترنت")
          }
        }.start()
      }
    } catch (_: Exception) {
      onResult?.invoke(false, "🔕 خدمة الإشعارات غير متوفرة على هذا الجهاز")
    }
  }

  /** عرض إشعار يدوياً (عندما يكون التطبيق مفتوحاً لا يعرضه أندرويد تلقائياً) */
  fun show(context: Context, title: String, body: String) {
    createChannel(context)
    if (Build.VERSION.SDK_INT >= 33 &&
      context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
      android.content.pm.PackageManager.PERMISSION_GRANTED
    ) return
    val intent = android.content.Intent(context, MainActivity::class.java)
      .addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
    val pi = android.app.PendingIntent.getActivity(
      context, 0, intent,
      android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
    )
    val builder = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(context, CHANNEL_ID)
      else @Suppress("DEPRECATION") android.app.Notification.Builder(context)
    @Suppress("DEPRECATION")
    builder.setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(body)
      .setStyle(android.app.Notification.BigTextStyle().bigText(body))
      .setAutoCancel(true)
      .setContentIntent(pi)
      .setPriority(android.app.Notification.PRIORITY_HIGH)
      .setDefaults(android.app.Notification.DEFAULT_ALL)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
  }
}
