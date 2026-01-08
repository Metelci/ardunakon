package com.metelci.ardunakon.util

/**
 * Parser for CHANGELOG.md to extract release notes for a specific version.
 */
object ChangelogParser {

    /**
     * Parses CHANGELOG.md and extracts release notes for the specified version.
     *
     * @param changelog Full CHANGELOG.md content
     * @param currentVersion Version to extract notes for (e.g., "0.2.10-alpha")
     * @return List of feature/change bullet points, or fallback message if not found
     */
    fun parseLatestRelease(changelog: String, currentVersion: String): List<String> {
        val lines = changelog.lines()
        
        // Match "## 0.2.20-alpha" or "## 0.2.20-alpha (Build 51)"
        // We look for a line that starts with "## currentVersion"
        val startHeaderPrefix = "## $currentVersion"
        
        // Find the start of the current version section
        val startIndex = lines.indexOfFirst { line -> 
            line.trim().startsWith(startHeaderPrefix) 
        }
        
        if (startIndex == -1) {
            return listOf("Release notes for version $currentVersion not found in CHANGELOG")
        }

        // Find the end (next version header starting with "## " or end of file)
        val endIndex = lines
            .drop(startIndex + 1)
            .indexOfFirst { it.trim().startsWith("## ") }
            .let { offset -> if (offset == -1) lines.size else startIndex + 1 + offset }

        // Extract the version section
        val versionSection = lines.subList(startIndex, endIndex)

        // Parse bullet points
        val features = mutableListOf<String>()

        for (line in versionSection) {
            val trimmed = line.trim()
            
            if (trimmed.startsWith("- ")) {
                // Extract the bullet point, clean up formatting
                val feature = trimmed.substring(2).trim()
                    .replace("**", "") // Remove bold markdown
                    .replace("`", "") // Remove code markdown

                // Skip overly long bullets or sub-bullets
                if (feature.isNotEmpty()) {
                    features.add(feature)
                }
            }
        }

        return if (features.isEmpty()) {
            listOf("No details found for this version.")
        } else {
            features.take(8) // Limit to 8 items for UI
        }
    }
}
