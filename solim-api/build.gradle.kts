// solim-api — Base contracts and interfaces for Solim (Component, Disposable, Readable)
// Shared java/repositories/dependencies are configured in root build.gradle.kts via subprojects.
plugins {
    `java-library`
}

val mindustryVersion: String by rootProject.extra

dependencies {
    testImplementation("Anuken:Mindustry:$mindustryVersion")
}
