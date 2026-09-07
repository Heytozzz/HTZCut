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

repositories {
    // SimpleVoiceChat publishes to their own repo, not Maven Central.
    maven { url = uri("https://maven.maxhenkel.de/repository/public") }
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

    // NOTE: client-side dialogue audio playback (ClientDialogueHandler)
    // decodes Ogg Vorbis via javax.sound.sampled, backed by vorbisspi.
    // Two earlier approaches were tried and rejected here:
    //   1. jarJar-ing vorbisspi/jorbis/tritonus-share directly as external
    //      Maven coordinates crashed the game with a
    //      java.lang.module.ResolutionException - Java derives a jar's
    //      automatic module name from its FILENAME, and these old
    //      javazoom/jcraft libraries are commonly bundled by many
    //      unrelated mods (observed colliding with Iris) under names
    //      that resolve to the same module regardless of Maven
    //      coordinates used.
    //   2. Minecraft's own internal com.mojang.blaze3d.audio.OggAudioStream
    //      isn't public API and doesn't exist under that name/package in
    //      1.21.1 the way it did in earlier versions - too unstable to
    //      depend on.
    // The htzcut-audio-libs subproject bundles the same libraries but
    // RELOCATES their packages via the Shadow plugin, so the resulting
    // merged jar shares no classes (and no module name) with any other
    // mod's copy of the same libraries - permanently avoiding this
    // collision regardless of what else is installed. It's also reused
    // SERVER-SIDE now, to decode dialogue .ogg files to PCM before
    // sending them through Simple Voice Chat (see audio/AudioDecoder).
    implementation(project(":htzcut-audio-libs"))
    jarJar(project(":htzcut-audio-libs"))

    // Soft depend - compileOnly since it's provided at runtime by the
    // actual Simple Voice Chat mod jar when installed; our own
    // SimpleVoiceChatSupport checks for its presence defensively at
    // runtime, so the interfaces being merely "on the classpath" here
    // (without an actual implementation behind them) is harmless when
    // the mod is absent - nothing calls into them unless it's confirmed
    // present. NOT jarJar'd: this must never be embedded, since it needs
    // to bind against whatever real SVC version the server actually has
    // installed, not a copy we shipped ourselves.
    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.6.21")

    // Soft depend - compileOnly, presence is detected at runtime.
    // Real coordinates/repositories to be pinned once we wire up the
    // permission implementation.
    // compileOnly("net.luckperms:api:5.4")
}
