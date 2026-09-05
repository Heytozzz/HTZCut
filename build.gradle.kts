import java.util.Properties
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

// Loads mod version info from version.properties so it can be read
// from any subproject and from the version bump task below.
val versionProps = Properties().apply {
    load(rootProject.file("version.properties").inputStream())
}

val modVersion = "${versionProps["major"]}.${versionProps["minor"]}.${versionProps["patch"]}"

allprojects {
    group = "com.heytozzz.htzcut"
    version = modVersion

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

/**
 * Bumps the mod version according to semantic-ish rules:
 *   BIG    -> major++, minor=0, patch=0
 *   MID    -> minor++, patch=0
 *   PATCH  -> patch++
 *
 * Usage: ./gradlew bumpVersion -Ptype=BIG|MID|PATCH
 */
tasks.register("bumpVersion") {
    group = "htzcut"
    description = "Bumps version.properties according to -Ptype=BIG|MID|PATCH"

    doLast {
        val type = (project.findProperty("type") as String? ?: "PATCH").uppercase()

        var major = versionProps["major"].toString().toInt()
        var minor = versionProps["minor"].toString().toInt()
        var patch = versionProps["patch"].toString().toInt()

        when (type) {
            "BIG" -> { major++; minor = 0; patch = 0 }
            "MID" -> { minor++; patch = 0 }
            "PATCH", "MINIMAL" -> { patch++ }
            else -> throw GradleException("Unknown bump type '$type'. Use BIG, MID or PATCH.")
        }

        versionProps["major"] = major.toString()
        versionProps["minor"] = minor.toString()
        versionProps["patch"] = patch.toString()

        val file = rootProject.file("version.properties")
        file.outputStream().use { out ->
            versionProps.store(out, "HTZCut version - do not edit manually, use ./gradlew bumpVersion")
        }

        println("Version bumped ($type) -> $major.$minor.$patch")
    }
}
