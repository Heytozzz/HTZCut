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
    // "implementation" only puts htzcut-core on the classpath for
    // compiling/running from Gradle - it does NOT bundle its classes into
    // the distributable jar. "jarJar" is NeoForge's officially supported
    // mechanism for embedding a subproject's classes inside the final mod
    // jar (Jar-in-Jar), which is what actually ships to players and fixes
    // the NoClassDefFoundError at runtime.
    implementation(project(":htzcut-core"))
    jarJar(project(":htzcut-core"))

    // htzcut-core depends on snakeyaml, but jarJar-ing a subproject only
    // embeds that subproject's OWN compiled classes, not its external
    // dependencies. snakeyaml has to be embedded here too, explicitly,
    // using NeoForge's version-range syntax for external Jar-in-Jar deps.
    jarJar(implementation("org.yaml:snakeyaml") {
        version {
            strictly("[2.0,3.0)")
            prefer("2.2")
        }
    })

    // Client-side dialogue audio playback (ClientDialogueHandler) uses
    // javax.sound.sampled to play the downloaded .ogg. The JDK has no
    // built-in Ogg Vorbis decoder, so vorbisspi registers one as a
    // javax.sound.sampled SPI provider. tritonus-share is vorbisspi's
    // own runtime dependency; jorbis/jogg are the actual Ogg Vorbis
    // decoding libraries underneath it. All embedded explicitly since,
    // as with snakeyaml above, jarJar doesn't reliably resolve a
    // dependency's own transitive closure for us.
    jarJar(implementation("com.googlecode.soundlibs:vorbisspi") {
        version {
            strictly("[1.0,2.0)")
            prefer("1.0.3.3")
        }
    })
    jarJar(implementation("com.googlecode.soundlibs:tritonus-share") {
        version {
            strictly("[0.3,0.4)")
            prefer("0.3.7.4")
        }
    })
    jarJar(implementation("org.jcraft:jorbis") {
        version {
            strictly("[0.0,1.0)")
            prefer("0.0.17")
        }
    })
    jarJar(implementation("org.jcraft:jogg") {
        version {
            strictly("[0.0,1.0)")
            prefer("0.0.7")
        }
    })

    // Soft depends - compileOnly, presence is detected at runtime.
    // Real coordinates/repositories to be pinned once we wire up the
    // permission and audio delivery implementations.
    // compileOnly("net.luckperms:api:5.4")
    // compileOnly("de.maxhenkel.voicechat:voicechat-api:2.x.x")
}
