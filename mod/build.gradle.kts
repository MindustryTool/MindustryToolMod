import java.nio.charset.StandardCharsets

// mod-specific configuration — shared java/repositories/dependencies are in root build.gradle.kts

val mindustryVersion: String by rootProject.extra
val isWindows: Boolean by rootProject.extra
val sdkRoot: String? by rootProject.extra

dependencies {
    // Mod only allowed to import solim, solim-mcp and solim-test
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    implementation(project(":solim"))
    implementation(project(":solim-mcp"))
    testImplementation("Anuken:Mindustry:$mindustryVersion")
    testImplementation(project(":solim-test"))
}

val secretsOutputDir = layout.buildDirectory.dir("generated/resources/secrets").get().asFile

val generateDevXSecrets by tasks.registering {
    val envFile = File(rootProject.projectDir, ".env")
    if (envFile.exists()) {
        inputs.file(envFile)
    }
    outputs.dir(secretsOutputDir)

    doLast {
        var apiKey = ""
        val devxEnv = System.getenv("DEVX_API_KEY")
        val apiEnv = System.getenv("API_KEY")
        if (!devxEnv.isNullOrBlank()) {
            apiKey = devxEnv.trim()
        } else if (!apiEnv.isNullOrBlank()) {
            apiKey = apiEnv.trim()
        } else if (envFile.exists()) {
            envFile.forEachLine { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("API_KEY=")) {
                    apiKey = trimmed.substring("API_KEY=".length).trim()
                } else if (trimmed.startsWith("DEVX_API_KEY=")) {
                    apiKey = trimmed.substring("DEVX_API_KEY=".length).trim()
                }
            }
        }

        if (apiKey.startsWith("\"") && apiKey.endsWith("\"") && apiKey.length > 1) {
            apiKey = apiKey.substring(1, apiKey.length - 1).trim()
        } else if (apiKey.startsWith("'") && apiKey.endsWith("'") && apiKey.length > 1) {
            apiKey = apiKey.substring(1, apiKey.length - 1).trim()
        }

        val targetFile = File(secretsOutputDir, "devx.secret")
        targetFile.parentFile.mkdirs()

        if (apiKey.isEmpty()) {
            if (targetFile.exists()) {
                targetFile.delete()
            }
        } else {
            val keyBytes = apiKey.toByteArray(StandardCharsets.UTF_8)
            val mask = byteArrayOf(0x5A.toByte(), 0xA5.toByte(), 0x3C.toByte(), 0xC3.toByte(), 0x69.toByte(), 0x96.toByte(), 0x0F.toByte(), 0xF0.toByte())
            val encoded = ByteArray(keyBytes.size)
            for (i in keyBytes.indices) {
                encoded[i] = (keyBytes[i].toInt() xor mask[i % mask.size].toInt()).toByte()
            }
            targetFile.writeBytes(encoded)
        }
    }
}

sourceSets {
    named("main") {
        resources {
            srcDir(secretsOutputDir)
        }
    }
}
tasks.named("processResources") {
    dependsOn(generateDevXSecrets)
}

val jarAndroid by tasks.registering {
    dependsOn("jar")
    val projectName = rootProject.name

    doLast {
        if (sdkRoot.isNullOrEmpty() || !File(sdkRoot!!).exists()) {
            logger.warn("No valid Android SDK found (ANDROID_HOME not set). Skipping Android dexing.")
            return@doLast
        }

        val platformRoot = File("$sdkRoot/platforms/").listFiles()
            ?.sortedByDescending { it.name }
            ?.find { File(it, "android.jar").exists() }

        if (platformRoot == null) {
            logger.warn("No android.jar found in $sdkRoot/platforms. Skipping Android dexing.")
            return@doLast
        }

        // collect dependencies needed for desugaring
        val dependencies = (configurations["compileClasspath"].files + configurations["runtimeClasspath"].files + listOf(File(platformRoot, "android.jar")))
            .joinToString(" ") { "--classpath ${it.path}" }

        val d8Name = if (isWindows) "d8.bat" else "d8"
        // Try to locate d8 inside Android SDK build-tools (so PATH setup is optional), fallback to PATH lookup
        val buildToolsDir = File(sdkRoot!!, "build-tools")
        var d8File: File? = null
        if (buildToolsDir.exists()) {
            val candidates = buildToolsDir.listFiles()
                ?.filter { it.isDirectory }
                ?.sortedByDescending { it.name }
            d8File = candidates?.find { File(it, d8Name).exists() }?.let { File(it, d8Name) }
        }
        val d8 = if (d8File?.exists() == true) d8File.absolutePath else d8Name

        val libsDir = layout.buildDirectory.dir("libs").get().asFile
        libsDir.mkdirs()

        val cmd = "$d8 $dependencies --min-api 14 --output ${projectName}Android.jar ${projectName}Desktop.jar"
        val proc = ProcessBuilder(cmd.split(" "))
            .directory(libsDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
        val exitCode = proc.waitFor()
        if (exitCode != 0) throw GradleException("d8 failed with exit code $exitCode")
    }
}

tasks.named<Jar>("jar") {
    dependsOn(":solim-api:jar", ":solim-runtime:jar", ":solim-core:jar", ":solim:jar", ":solim-mcp:jar")
    dependsOn(configurations["runtimeClasspath"])
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveFileName.set("${rootProject.name}Desktop.jar")

    from({
        configurations["runtimeClasspath"].files.map { if (it.isDirectory) it else zipTree(it) }
    })

    from(rootProject.projectDir) {
        include("mod.hjson")
        include("icon.png")
    }

    from(File(rootProject.projectDir, "assets")) {
        include("**")
    }

    exclude("META-INF/versions/**")
    exclude("**/module-info.class")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    doLast {
        val rootLibsDir = File(rootProject.projectDir, "build/libs")
        rootLibsDir.mkdirs()
        copy {
            from(archiveFile)
            into(rootLibsDir)
        }
    }
}

val deploy by tasks.registering(Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    val projectName = rootProject.name
    dependsOn(jarAndroid)
    dependsOn("jar")
    archiveFileName.set("$projectName.jar")

    val libsDir = layout.buildDirectory.dir("libs").get().asFile
    from({
        val desktopJar = File(libsDir, "${projectName}Desktop.jar")
        val androidJar = File(libsDir, "${projectName}Android.jar")
        if (androidJar.exists()) {
            listOf(zipTree(desktopJar), zipTree(androidJar))
        } else {
            listOf(zipTree(desktopJar))
        }
    })

    doLast {
        val androidJar = File(libsDir, "${projectName}Android.jar")
        if (androidJar.exists()) {
            androidJar.delete()
        }
        val rootLibsDir = File(rootProject.projectDir, "build/libs")
        rootLibsDir.mkdirs()
        copy {
            from(archiveFile)
            into(rootLibsDir)
        }
    }
}
