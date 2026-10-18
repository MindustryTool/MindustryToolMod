// solim-api — Base contracts and interfaces for Solim (Component, Disposable, Readable)
// Also hosts the ambient ownership stack (OwnershipContext, SolimAssert), because a
// Component installs itself as owner from its own module.
// Shared java/repositories/dependencies are configured in root build.gradle.kts via subprojects.
plugins {
    `java-library`
}

val mindustryVersion: String by rootProject.extra

dependencies {
    testImplementation("Anuken:Mindustry:$mindustryVersion")
}
