package com.lana.kitchen

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * استقبال إشعارات الطلبات الجديدة من Firebase.
 * والتطبيق مغلق: يعرض أندرويد الإشعار تلقائياً (قناة new_orders).
 * والتطبيق مفتوح: نعرضه نحن هنا، لأن أندرويد لا يعرضه تلقائياً.
 */
class PushService : FirebaseMessagingService() {
  override fun onNewToken(token: String) {
    val prefs = getSharedPreferences("lana", MODE_PRIVATE)
    prefs.edit().putString("fcm_token", token).apply()
    PushRegister.register(this, PushRegister.SITE, prefs.getString("token", null))
  }

  override fun onMessageReceived(message: RemoteMessage) {
    val title = message.notification?.title ?: message.data["title"] ?: "طلب جديد"
    val body = message.notification?.body ?: message.data["body"] ?: "وصل طلب جديد"
    PushRegister.show(this, title, body)
  }
}
