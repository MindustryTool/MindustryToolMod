// solim-reactive — Pure reactive primitives (Signal, Computed, Effect, Query, Mutation, ...)
// Extracted from solim-core so the reactive graph can be consumed independently of UI components.
// Shared java/repositories/dependencies are configured in root build.gradle.kts via subprojects.
plugins {
    `java-library`
}

val mindustryVersion: String by rootProject.extra

dependencies {
    // The import must not change unless i asked for it
    api(project(":solim-api"))
    implementation(project(":solim-runtime"))

    // Mindustry/Arc needed at compile time for Signals (mindustry events) and Arc types
    testImplementation("Anuken:Mindustry:$mindustryVersion")
}