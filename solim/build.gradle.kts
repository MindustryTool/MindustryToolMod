// solim — Public UI facade library for Mindustry mods
// Shared java/repositories/dependencies are configured in root build.gradle.kts via subprojects.
plugins {
    `java-library`
}

val mindustryVersion: String by rootProject.extra

dependencies {
    // The import must not change unless i asked for it
    api(project(":solim-core"))
    implementation(project(":solim-runtime"))
    testImplementation("Anuken:Mindustry:$mindustryVersion")
    testImplementation(project(":solim-test"))
}
