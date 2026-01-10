package com.metelci.ardunakon.util

/**
 * Parser for playstore-changelog.txt to extract release notes for a specific version.
 */
object ChangelogParser {

    /**
     * Parses playstore-changelog.txt and extracts release notes for the specified version.
     *
     * @param changelog Full playstore-changelog.txt content
     * @param currentVersion Version to extract notes for (e.g., "0.2.10-alpha")
     * @return List of feature/change bullet points, or fallback message if not found
     */
    fun parseLatestRelease(changelog: String, currentVersion: String): List<String> {
        val lines = changelog.lines()
        
        // Match "Version 0.2.20-alpha - January 10, 2026"
        val startHeaderPrefix = "Version $currentVersion"
        
        // Find the start of the current version section
        val startIndex = lines.indexOfFirst { line -> 
            line.trim().startsWith(startHeaderPrefix) 
        }
        
        if (startIndex == -1) {
            return listOf("Release notes for version $currentVersion not found in CHANGELOG")
        }

        // Find the end (next version header or "---" separator)
        val endIndex = lines
            .drop(startIndex + 1)
            .indexOfFirst { it.trim().startsWith("Version ") || it.trim() == "---" }
            .let { offset -> if (offset == -1) lines.size else startIndex + 1 + offset }

        // Extract the version section
        val versionSection = lines.subList(startIndex, endIndex)

        // Parse bullet points (starts with "• ")
        val features = mutableListOf<String>()

        for (line in versionSection) {
            val trimmed = line.trim()
            
            if (trimmed.startsWith("• ")) {
                // Extract the bullet point
                val feature = trimmed.substring(2).trim()

                // Skip empty lines and section headers (NEW FEATURES:, FIXES:, etc.)
                if (feature.isNotEmpty() && !feature.endsWith(":")) {
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
