plugins { id("com.android.application") }
android {
 namespace = "com.ideia.cdrviewer"
 compileSdk = 35
 defaultConfig { applicationId = "com.ideia.cdrviewer"; minSdk = 26; targetSdk = 28; versionCode = 7; versionName = "0.7" }
 signingConfigs {
   create("releaseKey") {
     val storeFilePath=System.getenv("CDR_KEYSTORE_FILE")
     if(!storeFilePath.isNullOrBlank()) {
       storeFile=file(storeFilePath)
       storePassword=System.getenv("CDR_KEYSTORE_PASSWORD")
       keyAlias=System.getenv("CDR_KEY_ALIAS")
       keyPassword=System.getenv("CDR_KEY_PASSWORD")
     }
   }
 }
 buildTypes {
   getByName("release") {
     isMinifyEnabled=false
     if(!System.getenv("CDR_KEYSTORE_FILE").isNullOrBlank()) signingConfig=signingConfigs.getByName("releaseKey")
   }
 }
}
dependencies { implementation("androidx.core:core:1.15.0") }
