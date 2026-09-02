import java.util.Base64
import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class GenerateLauncherIconTask : DefaultTask() {
    @get:InputFiles
    abstract val parts: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val encoded = parts.files
            .sortedBy { it.name }
            .joinToString(separator = "") { it.readText().trim() }
        val decoded = Base64.getDecoder().decode(encoded)
        check(decoded.size == 44_702) { "Unexpected launcher icon size: ${decoded.size}" }
        check(decoded.copyOfRange(0, 4).contentEquals("RIFF".toByteArray())) { "Launcher icon is not RIFF" }
        check(decoded.copyOfRange(8, 12).contentEquals("WEBP".toByteArray())) { "Launcher icon is not WebP" }

        val target = outputDirectory.get()
            .dir("drawable-nodpi")
            .file("ic_launcher_art.webp")
            .asFile
        target.parentFile.mkdirs()
        target.writeBytes(decoded)
    }
}

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

val generateLauncherIcon = tasks.register<GenerateLauncherIconTask>("generateLauncherIcon") {
    parts.from(fileTree("src/main/icon-assets") {
        include("steamforge-launcher-*.b64")
    })
    outputDirectory.set(layout.buildDirectory.dir("generated/steamforgeLauncher/res"))
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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

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

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(
            generateLauncherIcon,
            GenerateLauncherIconTask::outputDirectory,
        )
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
  androidTestImplementation(libs.androidx.test.uiautomator)

  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
}
