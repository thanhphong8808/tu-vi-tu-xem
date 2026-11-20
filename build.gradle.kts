import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

// Cấu hình chung cho các module Kotlin/JVM thuần (:core:*).
// Đích JVM 17 để dùng lại nguyên vẹn trên Android (minSdk 26 có java.time).
subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        extensions.configure<KotlinJvmProjectExtension> {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_17)
                allWarningsAsErrors.set(false)
            }
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            maxHeapSize = "1g"
            jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8")
            testLogging {
                events("failed", "skipped")
                exceptionFormat = TestExceptionFormat.FULL
                showStandardStreams = false
            }
        }
        // Công cụ dev (dumpLunar, dumpCharts, dumpFull, stressCheck) in tiếng Việt: ép UTF-8 kể cả trên Windows.
        tasks.withType<JavaExec>().configureEach {
            jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8")
        }
    }
}
