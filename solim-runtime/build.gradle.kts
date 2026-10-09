// solim-runtime — Internal engine mechanics (AttachmentStack, OwnershipContext, ReactiveContext)
// Shared java/repositories/dependencies are configured in root build.gradle.kts via subprojects.
plugins {
    `java-library`
}

val mindustryVersion: String by rootProject.extra

dependencies {
    // The import must not change unless i asked for it
    api(project(":solim-api"))
    testImplementation("Anuken:Mindustry:$mindustryVersion")
}
