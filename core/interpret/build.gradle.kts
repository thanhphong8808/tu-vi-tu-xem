plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":core:engine"))
    implementation(project(":data:kb"))
    implementation(libs.kotlinx.serialization.json)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Tìm lại lá số mẫu cho mọi luật (sau khi thêm/sửa luật). Ghi đè src/test/resources/rule-fixtures.json.
// Dùng: ./gradlew :core:interpret:findFixtures
tasks.register<JavaExec>("findFixtures") {
    group = "verification"
    description = "Tìm lá số kích hoạt và lá số suýt khớp cho từng luật"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("vn.tuvi.core.interpret.tools.FindFixturesKt")
    args(project.file("src/test/resources/rule-fixtures.json").absolutePath)
    maxHeapSize = "2g"
}
