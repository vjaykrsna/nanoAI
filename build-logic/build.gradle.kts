import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.vjaykrsna.nanoai.buildlogic"
version = "0.1.0-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    implementation(libs.findLibrary("android-gradle-plugin").get())
    implementation(libs.findLibrary("kotlin-gradle-plugin").get())
    implementation(libs.findLibrary("kotlin-compose-gradle-plugin").get())
    implementation(libs.findLibrary("kotlin-serialization-gradle-plugin").get())
    implementation(libs.findLibrary("ksp-gradle-plugin").get())
    implementation(libs.findLibrary("hilt-gradle-plugin").get())
    implementation(libs.findLibrary("androidx-room-gradle-plugin").get())
    implementation(libs.findLibrary("detekt-gradle-plugin").get())

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
    testImplementation(libs.findLibrary("junit-jupiter-api").get())
    testRuntimeOnly(libs.findLibrary("junit-jupiter-engine").get())
}

gradlePlugin {
    plugins {
        register("androidApplicationConvention") {
            id = "com.vjaykrsna.nanoai.android.application"
            implementationClass =
                "com.vjaykrsna.nanoai.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidFeatureConvention") {
            id = "com.vjaykrsna.nanoai.android.feature"
            implementationClass =
                "com.vjaykrsna.nanoai.buildlogic.AndroidFeatureConventionPlugin"
        }
        register("androidLibraryComposeConvention") {
            id = "com.vjaykrsna.nanoai.android.library.compose"
            implementationClass =
                "com.vjaykrsna.nanoai.buildlogic.AndroidLibraryComposeConventionPlugin"
        }
        register("androidTestingConvention") {
            id = "com.vjaykrsna.nanoai.android.testing"
            implementationClass =
                "com.vjaykrsna.nanoai.buildlogic.AndroidTestingConventionPlugin"
        }
        register("kotlinLibraryConvention") {
            id = "com.vjaykrsna.nanoai.kotlin.library"
            implementationClass =
                "com.vjaykrsna.nanoai.buildlogic.KotlinLibraryConventionPlugin"
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<ProcessResources>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
