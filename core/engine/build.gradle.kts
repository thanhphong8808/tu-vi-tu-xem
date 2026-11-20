plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Can, Chi, LunarDate, BirthRegion nằm ở :core:calendar và được xuất lại cho mọi module dùng engine.
    api(project(":core:calendar"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Công cụ dev: in lá số dạng JSON để tools/crosscheck/crosscheck.py so với lasotuvi.
// Dùng: ./gradlew :core:engine:dumpCharts -q --args="3000 20261120 lasotuvi" > /tmp/charts.jsonl
tasks.register<JavaExec>("dumpCharts") {
    group = "verification"
    description = "In lá số (JSON Lines) cho tools/crosscheck"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("vn.tuvi.core.engine.tools.DumpChartsKt")
}

// Công cụ dev: in lá số + vận hạn đầy đủ (JSON Lines) cho tools/crosscheck/spec_reference.py.
// Dùng: ./gradlew :core:engine:dumpFull -q --args="20000 7" > /tmp/full.jsonl
tasks.register<JavaExec>("dumpFull") {
    group = "verification"
    description = "In lá số và vận hạn đầy đủ cho bản tham chiếu độc lập"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("vn.tuvi.core.engine.tools.DumpFullKt")
}

// Kiểm vét cạn mọi ngày giờ sinh 1900–2100 (vài phút). Dùng: ./gradlew :core:engine:stressCheck
tasks.register<JavaExec>("stressCheck") {
    group = "verification"
    description = "Vét cạn mọi ngày dương × 13 canh × 2 giới và mọi ngày âm 1900–2100 qua bộ bất biến"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("vn.tuvi.core.engine.tools.StressCheckKt")
    maxHeapSize = "2g"
}
