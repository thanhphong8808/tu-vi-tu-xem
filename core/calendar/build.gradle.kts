plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Công cụ dev: in âm lịch mọi ngày 1900–2100 để tools/crosscheck/calendar_crosscheck.js so với amlich.js.
// Dùng: ./gradlew :core:calendar:dumpLunar -q > /tmp/lunar.txt
tasks.register<JavaExec>("dumpLunar") {
    group = "verification"
    description = "In âm lịch 1900–2100 (tz 7 và 8) cho tools/crosscheck"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("vn.tuvi.core.calendar.tools.DumpLunarKt")
}
