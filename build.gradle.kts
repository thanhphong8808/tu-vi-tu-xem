import org.gradle.api.tasks.testing.TestDescriptor
import org.gradle.api.tasks.testing.TestListener
import org.gradle.api.tasks.testing.TestResult
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
            // In một dòng tổng kết cho mỗi module (đọc được trong nhật ký GitHub Actions).
            val modulePath = project.path
            addTestListener(object : TestListener {
                override fun beforeSuite(suite: TestDescriptor) {}
                override fun beforeTest(testDescriptor: TestDescriptor) {}
                override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
                override fun afterSuite(suite: TestDescriptor, result: TestResult) {
                    if (suite.parent == null) {
                        println("TONG KET TEST $modulePath: ${result.testCount} test, ${result.successfulTestCount} dat, " +
                            "${result.failedTestCount} loi, ${result.skippedTestCount} bo qua")
                    }
                }
            })
        }
        // Công cụ dev (dumpLunar, dumpCharts, dumpFull, stressCheck) in tiếng Việt: ép UTF-8 kể cả trên Windows.
        tasks.withType<JavaExec>().configureEach {
            jvmArgs("-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8")
        }
    }
}
