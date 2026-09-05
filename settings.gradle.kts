pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
    }
}

rootProject.name = "HTZCut"

include(":htzcut-core")
include(":htzcut-audio-libs")
include(":htzcut-neoforge-1.21.1")
