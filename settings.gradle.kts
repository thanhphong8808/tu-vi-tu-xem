pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TuViTuXem"

// Giai đoạn 1–2: lịch + engine an sao + vận hạn (Kotlin/JVM thuần, test nhanh trên máy dev).
include(":core:calendar")
include(":core:engine")
// Giai đoạn 3: knowledge base (JSON) + RuleEngine.
include(":data:kb")
include(":core:interpret")

// Các giai đoạn sau (docs/PROMPT_TUVI_APP.md mục 3 và 15): :data:store, :feature:*, :ai.
// App Android hiện build bằng tools/apk/build_apk.sh (DECISIONS D-28).
