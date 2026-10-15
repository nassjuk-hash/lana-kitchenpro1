plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
  namespace = "com.lana.kitchen"
  compileSdk = 34
  defaultConfig { applicationId = "com.lana.kitchen"; minSdk = 24; targetSdk = 34; versionCode = 6; versionName = "1.5" }
  buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("debug") } }
  compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
  kotlinOptions { jvmTarget = "17" }
}

dependencies {
  implementation("com.google.firebase:firebase-messaging:24.1.0")
}

// ملف إعداد Firebase اختياري: الإشعارات تُفعّل فقط عند وجوده داخل مجلد app
if (file("google-services.json").exists()) {
  apply(plugin = "com.google.gms.google-services")
}
