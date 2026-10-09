import java.net.URI

// Root project — multi-module coordinator
// Shared configuration for :mod and :solim is defined via subprojects
// Main mod artifact is built via :mod, UI library via :solim

// Mindustry version to depend on.
// Valid values:
// - latest: depend on the latest release of mindustry
// - be: depend on the very latest commit of mindustry
// - v<number>: depend on a specific commit
val mindustryVersion = "v160.7"
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val sdkRoot = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")

extra["mindustryVersion"] = mindustryVersion
extra["isWindows"] = isWindows
extra["sdkRoot"] = sdkRoot

subprojects {
    apply(plugin = "java")
    apply(plugin = "eclipse")
    apply(plugin = "checkstyle")

    configure<CheckstyleExtension> {
        toolVersion = "10.12.5"
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
        isIgnoreFailures = false
        isShowViolations = true
    }

    version = "1.0"

    repositories {
        mavenCentral()

        // Downloads the dependencies JAR file from Mindustry releases; does not use any real repository. Surprisingly, this is the most reliable option.
        ivy {
            url = URI("https://github.com/")
            patternLayout { artifact("/[organisation]/[module]/releases/download/[revision]/dependencies.jar") }
            metadataSources { artifact() }
        }

        // If the version is set to 'latest', downloads the latest Mindustry *release* as a dependency
        ivy {
            url = URI("https://github.com/")
            patternLayout { artifact("/[organisation]/[module]/releases/[revision]/download/dependencies.jar") }
            metadataSources { artifact() }
        }

        // For depending on the absolute newest commit for Mindustry
        ivy {
            url = URI("https://github.com/")
            patternLayout { artifact("/[organisation]/[module]/releases/download/master/[revision].jar") }
            metadataSources { artifact() }
        }
    }

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(8)
        options.encoding = "UTF-8"
    }

    configure<SourceSetContainer> {
        named("main") {
            java {
                setSrcDirs(listOf("src"))
                exclude("**/test/**")
            }
        }
        named("test") {
            java {
                setSrcDirs(listOf("src/test/java"))
            }
        }
    }

    dependencies {
        add("compileOnly", if (mindustryVersion == "be") "Anuken:MindustryBuilds:latest" else "Anuken:Mindustry:$mindustryVersion")
        add("compileOnly", "org.projectlombok:lombok:1.18.36")

        add("implementation", "com.fasterxml.jackson.core:jackson-databind:2.16.2")
        add("implementation", "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.2")

        add("annotationProcessor", "org.projectlombok:lombok:1.18.36")

        add("testImplementation", "org.junit.jupiter:junit-jupiter:5.10.2")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher:1.10.2")
    }

    afterEvaluate {
        tasks.withType<JavaCompile>().configureEach {
            options.annotationProcessorPath = configurations.getByName("annotationProcessor")
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}

// Convenience delegates so `gradlew jar` / `gradlew deploy` still work from root
tasks.register("jar") {
    dependsOn(":mod:jar")
}

tasks.register("jarAndroid") {
    dependsOn(":mod:jarAndroid")
}

tasks.register("deploy") {
    dependsOn(":mod:deploy")
}

apply(from = "gradle/check-bundles.gradle.kts")

tasks.register("checkNoFullyQualifiedNames") {
    group = "verification"
    description = "Fails on fully qualified class names in code. Use imports instead."
    doLast {
        fun stripStringsAndComments(src: String): String {
            val out = StringBuilder(src.length)
            var i = 0
            var state = 0
            while (i < src.length) {
                when (state) {
                    0 -> {
                        if (src.startsWith("//", i)) { state = 1; out.append("  "); i += 2 }
                        else if (src.startsWith("/*", i)) { state = 2; out.append("  "); i += 2 }
                        else if (src.startsWith("\"\"\"", i)) { state = 5; out.append("   "); i += 3 }
                        else if (src[i] == '"') { state = 3; out.append(' '); i++ }
                        else if (src[i] == '\'') { state = 4; out.append(' '); i++ }
                        else { out.append(src[i]); i++ }
                    }
                    1 -> {
                        if (src[i] == '\n') { state = 0; out.append('\n') } else { out.append(' ') }
                        i++
                    }
                    2 -> {
                        if (src.startsWith("*/", i)) { state = 0; out.append("  "); i += 2 }
                        else { out.append(if (src[i] == '\n') '\n' else ' '); i++ }
                    }
                    3 -> {
                        if (src[i] == '\\' && i + 1 < src.length) { out.append("  "); i += 2 }
                        else if (src[i] == '"') { state = 0; out.append(' '); i++ }
                        else { out.append(if (src[i] == '\n') '\n' else ' '); i++ }
                    }
                    4 -> {
                        if (src[i] == '\\' && i + 1 < src.length) { out.append("  "); i += 2 }
                        else if (src[i] == '\'') { state = 0; out.append(' '); i++ }
                        else { out.append(if (src[i] == '\n') '\n' else ' '); i++ }
                    }
                    else -> {
                        if (src.startsWith("\"\"\"", i)) { state = 0; out.append("   "); i += 3 }
                        else { out.append(if (src[i] == '\n') '\n' else ' '); i++ }
                    }
                }
            }
            return out.toString()
        }
        val fqn = Regex("(?<!\\w)(java|javax|jakarta|org|com|net|io|mindustry|mindustrytool|arc|common|plugin|server|gateway)(\\.[a-z0-9_]+)+\\.([A-Z][\\w$]*)")
        val importDecl = Regex("(?m)^\\s*import\\s+(?:static\\s+)?([\\w.]+);")
        val typeDecl = Regex("(?m)^\\s*(?:public\\s+|protected\\s+|private\\s+|static\\s+|final\\s+|abstract\\s+)*(?:class|interface|enum|record|@interface)\\s+([A-Za-z0-9_]+)")
        val failures = mutableListOf<String>()
        val javaFiles = projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .filter { f ->
                val rel = f.relativeTo(projectDir).path.replace('\\', '/')
                !rel.contains("/build/") && !rel.startsWith("build/") &&
                    !rel.contains("/bin/") && !rel.startsWith("bin/") &&
                    !rel.contains("/.gradle/") && !rel.contains("/out/")
            }.toList()
        for (file in javaFiles) {
            val rel = file.relativeTo(projectDir).path.replace('\\', '/')
            val original = file.readText()
            val imports = importDecl.findAll(original).map { it.groupValues[1] }.toSet()
            val code = stripStringsAndComments(original)
            val declaredTypes = typeDecl.findAll(code).map { it.groupValues[1] }.toSet()
            val lines = code.lines()
            for (lineIndex in lines.indices) {
                val line = lines[lineIndex]
                val trimmed = line.trim()
                if (trimmed.startsWith("package ") || trimmed.startsWith("import ")) continue
                for (m in fqn.findAll(line)) {
                    val full = m.value
                    val simple = m.groupValues[3]
                    val pkg = full.substringBeforeLast('.')
                    val importedSameSimple = imports.any { it.substringAfterLast('.') == simple }
                    val hasConflict = importedSameSimple || declaredTypes.contains(simple)
                    val importedSameFqn = imports.contains(full)
                    if (hasConflict && !importedSameFqn) continue
                    if (pkg == "java.lang") {
                        failures.add("$rel:${lineIndex + 1}: $full (use simple name, java.lang needs no import)")
                    } else {
                        failures.add("$rel:${lineIndex + 1}: $full (add import and use $simple)")
                    }
                }
            }
        }
        if (failures.isNotEmpty()) {
            throw GradleException(
                "Fully qualified class names found (${failures.size}). Use imports instead:\n" +
                    failures.joinToString("\n")
            )
        }
    }
}

tasks.register("check") {
    group = "verification"
    description = "Runs all subproject checks and localization bundle verification."
    dependsOn(subprojects.map { it.tasks.matching { t -> t.name == "check" } })
    dependsOn("checkBundles")
}

tasks.named("check") {
    dependsOn("checkNoFullyQualifiedNames")
}
