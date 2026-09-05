// This subproject exists solely to bundle vorbisspi (+ its own runtime
// deps tritonus-share, jorbis) into a SINGLE jar with their packages
// RELOCATED under our own namespace, using the Shadow plugin.
//
// Why: embedding these old javazoom/jcraft libraries directly via
// jarJar (as external Maven coordinates) crashed the game with
// java.lang.module.ResolutionException, because Java derives a jar's
// automatic module name from its FILENAME, not its Maven coordinates -
// and these specific libraries are commonly bundled by many unrelated
// mods (observed colliding with Iris) under jar names that all resolve
// to the same module name ("jorbis", "tritonus-share", ...).
//
// Relocating their packages here means the resulting merged jar no
// longer contains any classes under org.jcraft.*, org.tritonus.*, or
// javazoom.* at all - just our own com.heytozzz.htzcut.audiolibs.shaded.*
// - so it can never collide with another mod's copy of the same
// libraries again, regardless of what that mod does.
//
// It's then embedded from htzcut-neoforge-1.21.1 the same way
// htzcut-core is: jarJar(project(":htzcut-audio-libs")). NeoForge's own
// docs note that subproject embeds are automatically prefixed with the
// group id specifically to avoid this class of collision, which gives
// us a second layer of protection on top of the relocation itself.
plugins {
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    implementation("com.googlecode.soundlibs:vorbisspi:1.0.3.3")
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")
    implementation("com.googlecode.soundlibs:jorbis:0.0.17.4")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles() // keeps javax.sound.sampled SPI registration working after relocation
    relocate("org.tritonus", "com.heytozzz.htzcut.audiolibs.shaded.tritonus")
    relocate("javazoom", "com.heytozzz.htzcut.audiolibs.shaded.javazoom")
    relocate("com.jcraft", "com.heytozzz.htzcut.audiolibs.shaded.jcraft")
}

// Make the relocated shadowJar output the artifact other subprojects get
// when they declare project(":htzcut-audio-libs") - not the plain,
// non-relocated "thin" jar the default 'jar' task would otherwise produce.
tasks.jar {
    enabled = false
}

configurations {
    runtimeElements {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.shadowJar)
    }
    apiElements {
        outgoing.artifacts.clear()
        outgoing.artifact(tasks.shadowJar)
    }
}
