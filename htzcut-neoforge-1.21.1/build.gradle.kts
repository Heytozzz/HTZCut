plugins {
    id("net.neoforged.moddev") version "2.0.51"
}

val minecraftVersion: String by project
val neoforgeVersion: String by project
val modId: String by project

base {
    archivesName.set("htzcut-neoforge-${minecraftVersion}")
}

neoForge {
    version = neoforgeVersion

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":htzcut-core"))

    // Soft depends - compileOnly, presence is detected at runtime.
    // Real coordinates/repositories to be pinned once we wire up the
    // permission and audio delivery implementations.
    // compileOnly("net.luckperms:api:5.4")
    // compileOnly("de.maxhenkel.voicechat:voicechat-api:2.x.x")
}
