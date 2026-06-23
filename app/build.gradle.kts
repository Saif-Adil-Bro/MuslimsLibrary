plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.kotlinx.serialization)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.muslimslibrary.abcde"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // Read SUPABASE_ANON_KEY from gradle properties / local.properties or system environment variables
    val tempAnonKey = (project.findProperty("SUPABASE_ANON_KEY") ?: System.getenv("SUPABASE_ANON_KEY") ?: "").toString()
    buildConfigField("String", "SUPABASE_ANON_KEY", "\"$tempAnonKey\"")

    // Read SUPABASE_URL from gradle properties / local.properties or system environment variables
    val tempUrl = (project.findProperty("SUPABASE_URL") ?: System.getenv("SUPABASE_URL") ?: "").toString()
    buildConfigField("String", "SUPABASE_URL", "\"$tempUrl\"")

    // Read Firebase configuration values
    val firebaseProjectId = (project.findProperty("FIREBASE_PROJECT_ID") ?: System.getenv("FIREBASE_PROJECT_ID") ?: "").toString()
    buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$firebaseProjectId\"")

    val firebaseApiKey = (project.findProperty("FIREBASE_API_KEY") ?: System.getenv("FIREBASE_API_KEY") ?: "").toString()
    buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseApiKey\"")

    val firebaseAppId = (project.findProperty("FIREBASE_APP_ID") ?: System.getenv("FIREBASE_APP_ID") ?: "").toString()
    buildConfigField("String", "FIREBASE_APP_ID", "\"$firebaseAppId\"")

    val firebaseMessagingSenderId = (project.findProperty("FIREBASE_MESSAGING_SENDER_ID") ?: System.getenv("FIREBASE_MESSAGING_SENDER_ID") ?: "").toString()
    buildConfigField("String", "FIREBASE_MESSAGING_SENDER_ID", "\"$firebaseMessagingSenderId\"")

    val firebaseStorageBucket = (project.findProperty("FIREBASE_STORAGE_BUCKET") ?: System.getenv("FIREBASE_STORAGE_BUCKET") ?: "").toString()
    buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"$firebaseStorageBucket\"")

    val googleWebClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") ?: System.getenv("GOOGLE_WEB_CLIENT_ID") ?: "").toString()
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("SUPABASE_ANON_KEY")
  ignoreList.add("SUPABASE_URL")
  ignoreList.add("FIREBASE_PROJECT_ID")
  ignoreList.add("FIREBASE_API_KEY")
  ignoreList.add("FIREBASE_APP_ID")
  ignoreList.add("FIREBASE_MESSAGING_SENDER_ID")
  ignoreList.add("FIREBASE_STORAGE_BUCKET")
  ignoreList.add("GOOGLE_WEB_CLIENT_ID")
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.auth)
  implementation(libs.firebase.messaging)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth)
  implementation(libs.play.services.auth)
  implementation(libs.googleid)
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  
  // Supabase Database & Storage SDK
  implementation(platform(libs.supabase.bom))
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.storage)
  implementation(libs.supabase.auth)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.ktor.client.android)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
  
  // Epub readers
  implementation("io.documentnode:epub4j-core:4.1") {
    exclude(group = "org.slf4j")
    exclude(group = "xmlpull")
  }
  implementation("org.jsoup:jsoup:1.17.2")
}