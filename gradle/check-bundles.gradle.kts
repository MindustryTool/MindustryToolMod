import java.util.regex.Pattern

tasks.register("checkBundles") {
    group = "verification"
    description = "Verifies localization bundles against bundle.properties for missing, obsolete, duplicate, and untranslated keys."

    val bundlesDir = file("${rootProject.projectDir}/assets/bundles")
    val baseFile = File(bundlesDir, "bundle.properties")
    val allowlistFile = File(bundlesDir, "untranslated-allowlist.txt")
    val localeFiles = fileTree(bundlesDir) { include("bundle_*.properties") }
    val reportFile = layout.buildDirectory.file("reports/bundles/checkBundles.md")

    inputs.file(baseFile)
    if (allowlistFile.exists()) {
        inputs.file(allowlistFile)
    }
    inputs.files(localeFiles)
    outputs.file(reportFile)

    doLast {
        if (!baseFile.exists()) {
            throw GradleException("Base bundle file not found at: ${baseFile.absolutePath}")
        }

        val placeholderPattern = Pattern.compile("\\{(\\d+)\\}")
        val urlPattern = Pattern.compile("^https?://.*$")
        val numericPunctPattern = Pattern.compile("^[0-9\\s.,:%/\\-_()#\\[\\]+*!?@^<>=|\\\\~`'\"]+$")
        val formatTokenPattern = Pattern.compile("^(\\[#[0-9a-fA-F]{6,8}\\])?\\{[0-9]+\\}[a-zA-Z%]*$")

        // English stop-words that indicate natural language prose/sentences.
        // A special term / acronym / proper noun should NEVER contain these words.
        val englishStopWords = setOf(
            "the", "be", "to", "of", "and", "a", "in", "that", "have", "i", "it", "for", "not", "on", "with",
            "he", "as", "you", "do", "at", "this", "but", "his", "by", "from", "they", "we", "say", "her", "she",
            "or", "an", "will", "my", "one", "all", "would", "there", "their", "what", "so", "up", "out", "if",
            "about", "who", "get", "which", "go", "me", "when", "make", "can", "like", "time", "no", "just", "him",
            "know", "take", "people", "into", "year", "your", "good", "some", "could", "them", "see", "other",
            "than", "then", "now", "look", "only", "come", "its", "over", "think", "also", "back", "after", "use",
            "two", "how", "our", "work", "first", "well", "way", "even", "new", "want", "because", "any", "these",
            "give", "day", "most", "us", "please", "failed", "failure", "success", "cannot", "select", "click",
            "press", "drag", "toggle", "enable", "disable", "error", "warning", "display", "message", "window",
            "dialog", "button", "settings", "feature", "requires", "current", "latest", "version", "available"
        )

        // Load allowlist
        val allowlist = mutableSetOf<String>()
        if (allowlistFile.exists()) {
            allowlistFile.forEachLine(Charsets.UTF_8) { line ->
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && !trimmed.startsWith("!")) {
                    // Extract key (ignoring any trailing whitespace/comments)
                    val key = trimmed.split(Regex("\\s+"))[0].trim()
                    if (key.isNotEmpty()) {
                        allowlist.add(key)
                    }
                }
            }
        }

        data class ParsedBundle(
            val file: File,
            val keys: List<String>,
            val entries: Map<String, String>,
            val lineNumbers: Map<String, List<Int>>
        )

        // Helper to parse bundle file with duplicate detection
        fun parseBundle(f: File): ParsedBundle {
            val keys = mutableListOf<String>()
            val entries = mutableMapOf<String, String>()
            val lineNumbers = mutableMapOf<String, MutableList<Int>>()
            var lineNum = 0

            f.forEachLine(Charsets.UTF_8) { rawLine ->
                lineNum++
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("!")) {
                    return@forEachLine
                }
                val eqIdx = line.indexOf('=')
                if (eqIdx == -1) {
                    return@forEachLine
                }
                val key = line.substring(0, eqIdx).trim()
                val value = line.substring(eqIdx + 1).trim()

                if (!lineNumbers.containsKey(key)) {
                    lineNumbers[key] = mutableListOf()
                    keys.add(key)
                }
                lineNumbers[key]!!.add(lineNum)
                entries[key] = value
            }
            return ParsedBundle(f, keys, entries, lineNumbers)
        }

        fun extractPlaceholders(value: String): List<Int> {
            val matcher = placeholderPattern.matcher(value)
            val result = mutableListOf<Int>()
            while (matcher.find()) {
                result.add(matcher.group(1).toInt())
            }
            result.sort()
            return result
        }

        fun isUniversalToken(value: String): Boolean {
            if (value.isEmpty()) return true
            if (urlPattern.matcher(value).matches()) return true
            if (formatTokenPattern.matcher(value).matches()) return true
            if (numericPunctPattern.matcher(value).matches()) return true
            return false
        }

        val errors = mutableListOf<String>()
        data class ReportRow(
            val file: String,
            val total: Int,
            val missing: Int,
            val obsolete: Int,
            val untranslated: Int,
            val status: String
        )
        val tableRows = mutableListOf<ReportRow>()

        // 1. Parse base bundle
        val baseData = parseBundle(baseFile)
        val baseKeySet = baseData.entries.keys

        // Check duplicates in base
        baseData.lineNumbers.forEach { (key, lines) ->
            if (lines.size > 1) {
                errors.add("Base bundle '${baseFile.name}' has duplicate key '$key' at lines $lines")
            }
        }

        // 2. Anti-Laziness Guard: Validate entries in allowlist
        allowlist.forEach { key ->
            if (!baseData.entries.containsKey(key)) {
                errors.add("[ALLOWLIST] Key '$key' in ${allowlistFile.name} does not exist in ${baseFile.name}")
                return@forEach
            }
            val englishText = baseData.entries[key]!!
            val words = englishText.lowercase().split(Regex("[^a-z0-9]+"))
            for (w in words) {
                if (englishStopWords.contains(w)) {
                    errors.add("[ANTI-LAZINESS] Key '$key' in ${allowlistFile.name} cannot be ignored: English text '$englishText' contains English word '$w'. It must be translated into the target language.")
                    break
                }
            }
        }

        tableRows.add(
            ReportRow(
                file = baseFile.name,
                total = baseKeySet.size,
                missing = 0,
                obsolete = 0,
                untranslated = 0,
                status = if (errors.isEmpty()) "✅ Valid Base" else "❌ Error"
            )
        )

        // 3. Check each locale bundle
        val targetFiles = localeFiles.files.sortedBy { it.name }
        targetFiles.forEach { locFile ->
            val locData = parseBundle(locFile)
            val locKeySet = locData.entries.keys

            // Check duplicates in locale
            locData.lineNumbers.forEach { (key, lines) ->
                if (lines.size > 1) {
                    errors.add("${locFile.name}: Duplicate key '$key' at lines $lines")
                }
            }

            // Check missing keys (in base but not in locale)
            val missingKeys = baseKeySet - locKeySet
            if (missingKeys.isNotEmpty()) {
                errors.add("${locFile.name}: Missing ${missingKeys.size} keys from base bundle: ${missingKeys.take(10).joinToString(", ")}${if (missingKeys.size > 10) "..." else ""}")
            }

            // Check obsolete / extra keys (in locale but not in base)
            val obsoleteKeys = locKeySet - baseKeySet
            if (obsoleteKeys.isNotEmpty()) {
                errors.add("${locFile.name}: Contains ${obsoleteKeys.size} obsolete keys not found in base bundle: ${obsoleteKeys.joinToString(", ")}")
            }

            // Check placeholder syntax consistency & untranslated keys
            var untranslatedCount = 0
            val untranslatedKeys = mutableListOf<String>()

            locData.entries.forEach { (key, locVal) ->
                if (baseData.entries.containsKey(key)) {
                    val baseVal = baseData.entries[key]!!
                    val basePh = extractPlaceholders(baseVal)
                    val locPh = extractPlaceholders(locVal)

                    if (basePh != locPh) {
                        errors.add("${locFile.name}: Key '$key' placeholder mismatch: base=$basePh != translation=$locPh")
                    }

                    // Untranslated check: identical to English base
                    if (locVal == baseVal) {
                        if (!isUniversalToken(locVal) && !allowlist.contains(key)) {
                            untranslatedCount++
                            untranslatedKeys.add(key)
                        }
                    }
                }
            }

            if (untranslatedCount > 0) {
                errors.add("${locFile.name}: $untranslatedCount untranslated key(s) identical to English: ${untranslatedKeys.take(10).joinToString(", ")}${if (untranslatedKeys.size > 10) "..." else ""}. AI translation must not lazily copy English. Either translate them or, if they are legitimate special terms, add them to untranslated-allowlist.txt.")
            }

            val hasFileErrors = errors.any { it.startsWith(locFile.name) }
            tableRows.add(
                ReportRow(
                    file = locFile.name,
                    total = locKeySet.size,
                    missing = missingKeys.size,
                    obsolete = obsoleteKeys.size,
                    untranslated = untranslatedCount,
                    status = if (hasFileErrors) "❌ Failed" else "✅ Pass"
                )
            )
        }

        // 4. Build markdown report
        val sb = StringBuilder()
        sb.append("### 🌐 Localization Bundle Verification Report\n\n")
        sb.append("| Bundle File | Total Keys | Missing | Obsolete | Untranslated | Status |\n")
        sb.append("| :--- | :---: | :---: | :---: | :---: | :---: |\n")
        tableRows.forEach { r ->
            sb.append("| `${r.file}` | ${r.total} | ${r.missing} | ${r.obsolete} | ${r.untranslated} | ${r.status} |\n")
        }
        sb.append("\n")

        if (errors.isNotEmpty()) {
            sb.append("#### ❌ Errors (${errors.size})\n")
            errors.forEach { sb.append("- $it\n") }
            sb.append("\n")
        }

        // Print to console
        logger.lifecycle("================================================================================")
        logger.lifecycle("BUNDLE VERIFICATION SUMMARY")
        logger.lifecycle("================================================================================")
        tableRows.forEach { r ->
            logger.lifecycle(
                String.format(
                    "  %-25s | Total: %4d | Missing: %3d | Obsolete: %3d | Untranslated: %3d | %s",
                    r.file, r.total, r.missing, r.obsolete, r.untranslated, r.status
                )
            )
        }
        logger.lifecycle("================================================================================")

        // Save report file
        val outReport = reportFile.get().asFile
        outReport.parentFile.mkdirs()
        outReport.writeText(sb.toString(), Charsets.UTF_8)

        // GitHub Step Summary if running in GitHub Actions
        val ghSummaryPath = System.getenv("GITHUB_STEP_SUMMARY")
        if (!ghSummaryPath.isNullOrBlank()) {
            try {
                File(ghSummaryPath).appendText(sb.toString(), Charsets.UTF_8)
            } catch (e: Exception) {
                logger.warn("Could not write to GITHUB_STEP_SUMMARY: ${e.message}")
            }
        }

        // Fail build if errors exist
        if (errors.isNotEmpty()) {
            logger.error("Bundle verification failed with ${errors.size} error(s):")
            errors.forEach { logger.error("  [ERROR] $it") }
            throw GradleException("Localization bundle verification failed with ${errors.size} error(s). See summary above.")
        }
    }
}
