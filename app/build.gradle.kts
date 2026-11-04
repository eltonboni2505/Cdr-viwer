plugins { id("com.android.application") }
android {
 namespace = "com.ideia.cdrviewer"
 compileSdk = 35
 defaultConfig { applicationId = "com.ideia.cdrviewer"; minSdk = 26; targetSdk = 28; versionCode = 1; versionName = "0.1" }
}
dependencies { implementation("androidx.core:core:1.15.0") }
