plugins {
    id("net.neoforged.moddev") version "2.0.141"
}

val minecraft_version: String by project
val neo_version: String by project
val mod_id: String by project

base {
    archivesName.set("htzcut-neoforge-${minecraft_version}")
}

neoForge {
    version = neo_version

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }

    mods {
        create(mod_id) {
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
