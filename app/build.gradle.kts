import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.androidx.room)
  jacoco
}

fun createQuotedStringBuildConfigField(
  defaultConfig: com.android.build.api.dsl.DefaultConfig,
  name: String,
  propertyName: String,
  defaultValue: String,
) {
  val propertyValue = (project.findProperty(propertyName) as? String)?.trim()
  val finalValue = propertyValue.takeIf { !it.isNullOrBlank() } ?: defaultValue
  val quotedValue = "\"${finalValue.replace("\"", "\\\"")}\""
  defaultConfig.buildConfigField("String", name, quotedValue)
}

android {
  namespace = "com.vjaykrsna.nanoai"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.vjaykrsna.nanoai"
    minSdk = 31
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables { useSupportLibrary = true }

    createQuotedStringBuildConfigField(this, "HF_OAUTH_CLIENT_ID", "nanoai.hf.oauth.clientId", "")
    createQuotedStringBuildConfigField(
      this,
      "HF_OAUTH_SCOPE",
      "nanoai.hf.oauth.scope",
      "all offline_access",
    )
    createQuotedStringBuildConfigField(
      this,
      "HF_OAUTH_REDIRECT_URI",
      "nanoai.hf.oauth.redirectUri",
      "nanoai://auth/huggingface",
    )
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug {
      enableAndroidTestCoverage = true
      enableUnitTestCoverage = true
    }
    create("baselineProfile") {
      initWith(buildTypes.getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }
    create("benchmark") {
      initWith(buildTypes.getByName("release"))
      signingConfig = signingConfigs.getByName("debug")
      matchingFallbacks += listOf("release")
      isDebuggable = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlin {
    compilerOptions {
      jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
      val composeMetricsDir = project.layout.buildDirectory.dir("compose/metrics")
      val composeReportsDir = project.layout.buildDirectory.dir("compose/reports")
      freeCompilerArgs.addAll(
        listOf(
          "-opt-in=kotlin.RequiresOptIn",
          "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
          "-opt-in=kotlinx.coroutines.FlowPreview",
          "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
          "-Xenable-incremental-compilation", // Enable incremental compilation
          "-Xuse-fast-jar-file-system", // Faster JAR file access
          "-Xannotation-default-target=param-property", // Apply annotations to both parameter and
          // backing field
          "-P",
          "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${composeMetricsDir.get().asFile.absolutePath}",
          "-P",
          "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${composeReportsDir.get().asFile.absolutePath}",
        )
      )
    }
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  composeOptions { kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get() }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "/META-INF/LICENSE.md"
      excludes += "/META-INF/LICENSE-notice.md"
    }
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      isReturnDefaultValues = true
    }
    managedDevices {
      val pixel6 = allDevices.create("pixel6Api34", ManagedVirtualDevice::class.java)
      pixel6.device = "Pixel 6"
      pixel6.apiLevel = 34
      pixel6.systemImageSource = "aosp-atd"
      pixel6.testedAbi = "x86_64"
      groups.create("ci") { targetDevices.add(pixel6) }
    }
  }

  sourceSets {
    getByName("test") {
      java.srcDir("src/test/contract")
      java.srcDir("src/test/java")
      resources.srcDir("$rootDir/config")
    }
  }
}

room { schemaDirectory("$projectDir/schemas") }

dependencies {
  // Core Android
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.material.iconsExtended)
  implementation(libs.androidx.compose.material3.windowSizeClass)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.window)
  debugImplementation(libs.androidx.compose.ui.test)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // MediaPipe
  implementation(libs.mediapipe.tasks.genai)
  implementation(libs.mediapipe.tasks.vision)

  // Leap
  implementation(libs.leap.sdk)

  // Networking
  implementation(libs.retrofit)
  implementation(libs.retrofit.kotlin.serialization)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)

  // Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)

  // Date and time utilities
  implementation(libs.kotlinx.datetime)

  // WorkManager
  implementation(libs.androidx.work.runtime.ktx)

  // Security
  implementation(libs.androidx.security.crypto)

  // DataStore
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.datastore)
  implementation(libs.androidx.datastore.core)
  implementation(libs.androidx.datastore.preferences.core)

  // Hilt
  implementation(libs.hilt.android)
  implementation(libs.hilt.navigation.compose)
  implementation(libs.hilt.work)
  ksp(libs.hilt.compiler)
  ksp(libs.androidx.hilt.compiler)
  kspTest(libs.hilt.compiler)

  // Image Loading
  implementation(libs.coil.compose)

  // ProfileInstaller for baseline profiles
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.metrics.performance)

  // Unit Testing
  testImplementation(kotlin("test-junit5"))
  testImplementation(kotlin("reflect"))
  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testImplementation(libs.mockk)
  testImplementation(libs.turbine)
  testImplementation(libs.robolectric)
  testImplementation(libs.truth)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.androidx.room.testing)
  testImplementation(libs.networknt.json.schema.validator)
  testImplementation(libs.mockwebserver)
  testImplementation(libs.androidx.work.testing)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.test.core)
  testImplementation(libs.androidx.navigation.testing)
  testImplementation(libs.hilt.android.testing)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testRuntimeOnly(libs.junit.platform.launcher)
  testRuntimeOnly(libs.junit.vintage.engine)

  // Instrumentation Testing
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.uiautomator)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.runtime)
  androidTestImplementation(libs.androidx.compose.ui)
  androidTestImplementation(libs.androidx.compose.ui.test)
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(kotlin("test-junit5"))
  androidTestImplementation(libs.junit.jupiter.api)
  androidTestImplementation(libs.junit.jupiter.params)
  androidTestImplementation(libs.junit.vintage.engine)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.truth)
  androidTestImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(libs.androidx.room.testing)
  androidTestImplementation(libs.androidx.work.testing)
  androidTestImplementation(libs.mockwebserver)
  androidTestImplementation(libs.androidx.navigation.testing)
  kspAndroidTest(libs.hilt.compiler)
}

apply(from = "coverage.gradle.kts")

androidComponents {
  beforeVariants(selector().all()) { variant ->
    if (variant.buildType in listOf("benchmark", "baselineProfile")) {
      variant.enable = false
    }
  }
}

// TODO: Re-enable lint when newer versions fix the FIR symbol resolution bug
afterEvaluate { tasks.findByName("lintVitalAnalyzeRelease")?.enabled = false }
