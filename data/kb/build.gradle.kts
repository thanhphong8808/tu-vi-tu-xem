// Knowledge base dạng JSON (src/main/resources/kb). Sinh từ tools/kb/build_kb.py — DECISIONS D-34.
plugins {
    `java-library`
}

// Cùng đích JVM 17 với các module :core:* (nếu không, Gradle coi module này chỉ chạy được trên JVM đang build).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
