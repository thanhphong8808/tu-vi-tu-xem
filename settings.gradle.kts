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

// Giai đoạn 1: lịch + engine an sao (Kotlin/JVM thuần, test nhanh trên máy dev).
include(":core:calendar")
include(":core:engine")

// Các giai đoạn sau sẽ thêm (xem docs/PROMPT_TUVI_APP.md mục 3 và 15):
// :core:interpret, :data:kb, :data:store, :feature:*, :ai, :app
