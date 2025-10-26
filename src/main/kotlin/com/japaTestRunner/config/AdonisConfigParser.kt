package com.japaTestRunner.config

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import java.io.File

/**
 * Parses adonisrc.ts/adonisrc.json to extract Japa suite names
 */
object AdonisConfigParser {

    /**
     * Get the list of Japa suite names defined in adonisrc
     * Returns common defaults if file is not found or cannot be parsed
     */
    fun getSuiteNames(project: Project): Set<String> {
        val basePath = project.basePath ?: return getDefaultSuiteNames()

        // Try adonisrc.json first (easier to parse)
        val jsonFile = File(basePath, "adonisrc.json")
        if (jsonFile.exists()) {
            val suites = parseSuitesFromJson(project, jsonFile.absolutePath)
            if (suites.isNotEmpty()) return suites
        }

        // Try adonisrc.ts (TypeScript config)
        val tsFile = File(basePath, "adonisrc.ts")
        if (tsFile.exists()) {
            val suites = parseSuitesFromTs(tsFile)
            if (suites.isNotEmpty()) return suites
        }

        // Fallback to common defaults
        return getDefaultSuiteNames()
    }

    /**
     * Parse suite names from adonisrc.json
     */
    private fun parseSuitesFromJson(project: Project, filePath: String): Set<String> {
        try {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath) ?: return emptySet()
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? JsonFile ?: return emptySet()

            val rootObject = psiFile.topLevelValue as? JsonObject ?: return emptySet()
            val testsObject =
                rootObject.findProperty("tests")?.value as? JsonObject
                    ?: return emptySet()
            val suitesArray =
                testsObject.findProperty("suites")?.value as? com.intellij.json.psi.JsonArray
                    ?: return emptySet()

            return suitesArray.valueList
                .mapNotNull { it as? JsonObject }
                .mapNotNull { it.findProperty("name")?.value as? JsonStringLiteral }
                .map { it.value }
                .toSet()
        } catch (e: Exception) {
            return emptySet()
        }
    }

    /**
     * Parse suite names from adonisrc.ts using simple regex
     * This is a basic parser - for production, consider using a proper TS parser
     */
    private fun parseSuitesFromTs(file: File): Set<String> {
        try {
            val content = file.readText()
            val suites = mutableSetOf<String>()

            // Match pattern: name: 'suiteName' or name: "suiteName"
            val namePattern = Regex("""name\s*:\s*['"]([^'"]+)['"]""")

            // Look for the suites array section
            val suitesMatch = Regex("""suites\s*:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL)
                .find(content)

            if (suitesMatch != null) {
                val suitesContent = suitesMatch.groupValues[1]
                namePattern.findAll(suitesContent).forEach { match ->
                    suites.add(match.groupValues[1])
                }
            }

            return suites
        } catch (e: Exception) {
            return emptySet()
        }
    }

    /**
     * Default suite names commonly used in AdonisJS/Japa projects
     */
    private fun getDefaultSuiteNames(): Set<String> {
        return setOf("functional", "unit", "integration", "e2e")
    }
}
