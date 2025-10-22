import com.android.build.api.dsl.ManagedVirtualDevice
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

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

fun com.android.build.api.dsl.DefaultConfig.createQuotedStringBuildConfigField(
  name: String,
  propertyName: String,
  defaultValue: String,
) {
  val propertyValue = (project.findProperty(propertyName) as? String)?.trim()
  val finalValue = propertyValue.takeIf { !it.isNullOrBlank() } ?: defaultValue
  val quotedValue = "\"${finalValue.replace("\"", "\\\"")}\""
  buildConfigField("String", name, quotedValue)
}

// Common exclusion patterns for Jacoco reports to avoid repetition.
val jacocoExclusionPatterns =
  listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*")

// Helper function to generate class directories for Jacoco, reducing repetition.
fun jacocoClassDirectories(variant: String) =
  files(
    fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/$variant").get()) {
      exclude(jacocoExclusionPatterns)
    },
    fileTree(layout.buildDirectory.dir("intermediates/javac/$variant/classes").get()) {
      exclude(jacocoExclusionPatterns)
    },
  )

val isCiEnvironment = System.getenv("CI")?.equals("true", ignoreCase = true) == true
val usePhysicalDeviceProperty =
  (project.findProperty("nanoai.usePhysicalDevice") as? String)?.toBoolean() ?: false
val skipInstrumentation =
  (project.findProperty("nanoai.skipInstrumentation") as? String)?.toBoolean() ?: false
val useManagedDeviceForInstrumentation =
  !skipInstrumentation && (isCiEnvironment || !usePhysicalDeviceProperty)
// Pixel 6 API 34 managed virtual device ships an x86_64-only system image starting in
// Android 14. Lock the ABI so CI and local runs share the same emulator bits.
val managedDeviceName = "pixel6Api34"
val managedDeviceTaskName = "${managedDeviceName}DebugAndroidTest"

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

    createQuotedStringBuildConfigField("HF_OAUTH_CLIENT_ID", "nanoai.hf.oauth.clientId", "")
    createQuotedStringBuildConfigField(
      "HF_OAUTH_SCOPE",
      "nanoai.hf.oauth.scope",
      "all offline_access",
    )
    createQuotedStringBuildConfigField(
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
          "-Xannotation-default-target=param-property",
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
      val pixel6 = allDevices.create(managedDeviceName, ManagedVirtualDevice::class.java)
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
      resources.srcDir("$rootDir/config")
    }
  }
}

room { schemaDirectory("$projectDir/schemas") }

jacoco {
  toolVersion = "0.8.12"
  reportsDirectory.set(layout.buildDirectory.dir("reports/jacoco"))
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  extensions.configure(JacocoTaskExtension::class.java) {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }
}

configurations.configureEach {
  exclude(group = "org.mockito", module = "mockito-core")
  exclude(group = "org.mockito", module = "mockito-android")
}

// Collect common coverage inputs for JVM + instrumentation runs.
val coverageExecutionData =
  files(
    layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"),
    layout.buildDirectory.file(
      "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    ),
    fileTree(layout.buildDirectory.dir("outputs/code_coverage").get().asFile) {
      include("**/*.ec")
    },
    fileTree(layout.buildDirectory.dir("outputs/managed_device_code_coverage").get().asFile) {
      include("**/*.ec")
    },
  )

val coverageClassDirectories = jacocoClassDirectories("debug")
val coverageSourceDirectories = files("src/main/java", "src/main/kotlin")

tasks.register<JacocoReport>("jacocoFullReport") {
  group = "verification"
  description = "Generates a merged coverage report for unit and instrumentation tests."

  dependsOn(tasks.named("testDebugUnitTest"))
  if (!skipInstrumentation) {
    dependsOn(tasks.named("connectedDebugAndroidTest"))
  }

  classDirectories.setFrom(coverageClassDirectories)
  additionalClassDirs.setFrom(coverageClassDirectories)
  sourceDirectories.setFrom(coverageSourceDirectories)
  executionData.setFrom(coverageExecutionData)

  reports {
    xml.required.set(true)
    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/full/jacocoFullReport.xml"))
    html.required.set(true)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/full/html"))
    csv.required.set(false)
  }
}

tasks.register("ciManagedDeviceDebugAndroidTest") {
  group = "verification"
  description = "Runs instrumentation tests on the CI managed Pixel 6 API 34 virtual device."

  dependsOn(tasks.named(managedDeviceTaskName))
  onlyIf { !skipInstrumentation }
  doFirst { logger.lifecycle("Executing managed-device instrumentation on $managedDeviceName") }
}

if (useManagedDeviceForInstrumentation) {
  tasks
    .matching { it.name == "connectedDebugAndroidTest" }
    .configureEach {
      dependsOn(managedDeviceTaskName)
      // Skip the device-provider task when using the managed virtual device to avoid requiring
      // a physical emulator in headless environments.
      onlyIf { false }
    }
}

tasks.register<JacocoReport>("jacocoUnitReport") {
  group = "verification"
  description = "Generates a coverage report for unit tests only."

  dependsOn(tasks.named("testDebugUnitTest"))

  val coverageClassDirectoriesUnit = jacocoClassDirectories("debugUnitTest")
  classDirectories.setFrom(coverageClassDirectoriesUnit)
  additionalClassDirs.setFrom(coverageClassDirectoriesUnit)
  sourceDirectories.setFrom(coverageSourceDirectories)
  executionData.setFrom(
    layout.buildDirectory.file(
      "outputs/unit_test_code_coverage/debugUnitTest/testDebugUnitTest.exec"
    )
  )

  reports {
    xml.required.set(true)
    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/unit/jacocoUnitReport.xml"))
    html.required.set(true)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/unit/html"))
    csv.required.set(false)
  }
}

val coverageReportXml = layout.buildDirectory.file("reports/jacoco/full/jacocoFullReport.xml")
val layerMapFile = rootProject.layout.projectDirectory.file("config/coverage/layer-map.json")
val coverageGateMarkdown = layout.buildDirectory.file("coverage/thresholds.md")
val coverageGateJson = layout.buildDirectory.file("coverage/thresholds.json")

tasks.register<JavaExec>("verifyCoverageThresholds") {
  group = "verification"
  description = "Verifies merged coverage meets minimum layer thresholds."

  dependsOn(tasks.named("compileDebugKotlin"))
  dependsOn(tasks.named("jacocoFullReport"))

  inputs.file(coverageReportXml)
  inputs.file(layerMapFile)
  outputs.file(coverageGateMarkdown)
  outputs.file(coverageGateJson)

  mainClass.set("com.vjaykrsna.nanoai.coverage.tasks.VerifyCoverageThresholdsTask")

  classpath(
    files(
      layout.buildDirectory.dir("tmp/kotlin-classes/debug"),
      layout.buildDirectory.dir("intermediates/javac/debug/classes"),
    ),
    configurations.getByName("debugRuntimeClasspath"),
    android.bootClasspath,
  )

  doFirst {
    coverageGateMarkdown.get().asFile.parentFile.mkdirs()
    coverageGateJson.get().asFile.parentFile.mkdirs()
  }

  args(
    "--report-xml",
    coverageReportXml.get().asFile.absolutePath,
    "--layer-map",
    layerMapFile.asFile.absolutePath,
    "--markdown",
    coverageGateMarkdown.get().asFile.absolutePath,
    "--build-id",
    "jacocoFullReport",
    "--json",
    coverageGateJson.get().asFile.absolutePath,
  )
}

tasks.named("check") {
  dependsOn(tasks.named("jacocoFullReport"))
  dependsOn(tasks.named("verifyCoverageThresholds"))
}

tasks.register<Exec>("coverageMergeArtifacts") {
  group = "verification"
  description = "Runs helper script to trigger merged coverage generation."

  commandLine(
    "bash",
    "${rootDir}/scripts/coverage/merge-coverage.sh",
    layout.buildDirectory.dir("reports/jacoco/full").get().asFile.absolutePath,
  )
}

val coverageSummaryMarkdown = layout.buildDirectory.file("coverage/summary.md")
val coverageSummaryJson = layout.buildDirectory.file("coverage/summary.json")
val legacyCoverageMarkdown = layout.buildDirectory.file("reports/jacoco/full/summary.md")

tasks.register<Exec>("coverageMarkdownSummary") {
  group = "verification"
  description = "Generates markdown coverage summary from merged JaCoCo XML report."

  dependsOn(tasks.named("jacocoFullReport"))

  val xmlReport = layout.buildDirectory.file("reports/jacoco/full/jacocoFullReport.xml")
  val layerMap = rootProject.layout.projectDirectory.file("config/coverage/layer-map.json")

  inputs.file(xmlReport)
  inputs.file(layerMap)
  outputs.file(coverageSummaryMarkdown)
  outputs.file(coverageSummaryJson)
  outputs.file(legacyCoverageMarkdown)

  doFirst { coverageSummaryMarkdown.get().asFile.parentFile.mkdirs() }

  commandLine(
    "python3",
    "${rootDir}/scripts/coverage/generate-summary.py",
    xmlReport.get().asFile.absolutePath,
    coverageSummaryMarkdown.get().asFile.absolutePath,
    "--json-output",
    coverageSummaryJson.get().asFile.absolutePath,
    "--layer-map",
    layerMap.asFile.absolutePath,
  )

  doLast {
    legacyCoverageMarkdown.get().asFile.parentFile.mkdirs()
    coverageSummaryMarkdown
      .get()
      .asFile
      .copyTo(target = legacyCoverageMarkdown.get().asFile, overwrite = true)
  }
}

androidComponents {
  beforeVariants(selector().all()) { variant ->
    if (variant.buildType in listOf("benchmark", "baselineProfile")) {
      variant.enable = false
    }
  }
}

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
  implementation(libs.androidx.compose.runtime.tracing)
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

  // Instrumentation Testing
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.uiautomator)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.compose.ui.test)
  androidTestImplementation(libs.mockk.android)
  androidTestImplementation(libs.truth)
  androidTestImplementation(libs.kotlinx.coroutines.test)
  androidTestImplementation(libs.androidx.room.testing)
  androidTestImplementation(libs.androidx.work.testing)
  androidTestImplementation(libs.mockwebserver)
  androidTestImplementation(libs.androidx.navigation.testing)
  kspAndroidTest(libs.hilt.compiler)
}
