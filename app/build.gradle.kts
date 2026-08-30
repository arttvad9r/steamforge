import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.steamforge.game"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.steamforge.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "APPMETRICA_API_KEY", prop("steamforge.appmetricaApiKey", ""))
        buildConfigField("String", "PRIVACY_POLICY_URL", prop("steamforge.privacyPolicyUrl", ""))
    }

    buildTypes {
        debug {
            // AdsManager намеренно игнорирует production IDs в debug и всегда использует demo units.
            buildConfigField("String", "REWARDED_AD_UNIT_ID", "\"\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", "\"\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("String", "REWARDED_AD_UNIT_ID", prop("steamforge.rewardedAdUnitId", ""))
            buildConfigField("String", "INTERSTITIAL_AD_UNIT_ID", prop("steamforge.interstitialAdUnitId", ""))

            val storeFilePath = keystoreProps.getProperty("storeFile")
            if (storeFilePath != null) {
                signingConfig = signingConfigs.create("release") {
                    storeFile = rootProject.file(storeFilePath)
                    storePassword = keystoreProps.getProperty("storePassword")
                    keyAlias = keystoreProps.getProperty("keyAlias")
                    keyPassword = keystoreProps.getProperty("keyPassword")
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

fun prop(name: String, fallback: String): String =
    "\"${providers.gradleProperty(name).orElse(fallback).get()}\""

kotlin {
    jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.appmetrica.analytics)
  implementation(libs.yandex.mobileads)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  debugImplementation(libs.androidx.compose.ui.tooling)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
