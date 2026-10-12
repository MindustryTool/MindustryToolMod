// solim-core — Public components, layouts, and reactive types
// Shared java/repositories/dependencies are configured in root build.gradle.kts via subprojects.
plugins {
    `java-library`
}

val mindustryVersion: String by rootProject.extra

dependencies {
    // The import must not change unless i asked for it
    api(project(":solim-api"))
    implementation(project(":solim-runtime"))

    // Pure-Java media decoding for SolimVideo. jcodec-javase is intentionally NOT used:
    // it depends on java.awt.image.BufferedImage, which does not exist on Android.
    //
    // jcodec core has no transitive runtime dependencies and already ships JAAD shaded in
    // under net.sourceforge.jaad.aac.*, wrapped by org.jcodec.codecs.aac.AACDecoder.
    // A separate net.sourceforge.jaadec:jaad artifact is therefore unnecessary (and was
    // unresolvable — that coordinate only ever existed on the shut-down JCenter).
    // Audio path: AACUtils.getCodecPrivate -> AACDecoder.decodeFrame -> AudioBuffer -> WAV.
    implementation("org.jcodec:jcodec:0.2.5")

    // Mindustry/Arc needed at test runtime for Element instantiation in component tests
    testImplementation("Anuken:Mindustry:$mindustryVersion")
    testImplementation(project(":solim-test"))
    // Public facade for tests exercising factory behavior (test-scope only; main stays decoupled)
    // solim-core is not allowed to import solim 
}
