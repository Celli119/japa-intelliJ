package com.japaTestRunner.run

import com.intellij.execution.Location
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.japaTestRunner.detection.JapaTestDetector
import java.net.URLDecoder

/**
 * Parse Japa location hint parameters
 */
fun parseJapaLocationHint(locationHint: String): JapaTestLocator.LocationInfo? {
    if (!locationHint.startsWith("${JapaTestLocator.PROTOCOL}://")) return null

    val path = locationHint.removePrefix("${JapaTestLocator.PROTOCOL}://")
    val parts = path.split("?")
    if (parts.isEmpty()) return null

    val filePath = parts[0]
    val params = if (parts.size > 1) {
        parts[1].split("&").associate {
            val kv = it.split("=")
            if (kv.size == 2) {
                URLDecoder.decode(kv[0], "UTF-8") to URLDecoder.decode(kv[1], "UTF-8")
            } else {
                "" to ""
            }
        }
    } else {
        emptyMap()
    }

    return JapaTestLocator.LocationInfo(
        filePath = filePath,
        suiteName = params["suite"],
        groupName = params["group"],
        testName = params["test"],
    )
}

/**
 * Locates tests in the IDE from test output
 * Parses location hints in format: japa://path/to/test.ts?suite=SuiteName&test=TestName
 */
class JapaTestLocator : SMTestLocator {

    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope,
    ): List<Location<*>> {
        if (protocol != PROTOCOL) {
            return emptyList()
        }

        // Parse the location hint
        val locationInfo = parseJapaLocationHint("$PROTOCOL://$path") ?: return emptyList()

        // For suite-level runs (functional, unit, etc.), there might be no file path
        // In this case, we still need to provide a location for the context menu to work
        // We'll use the project's base directory as a fallback PsiElement
        if (locationInfo.filePath.isEmpty()) {
            // For suite-level tests, use the project base directory
            val basePath = project.basePath ?: return emptyList()
            val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return emptyList()
            val psiDir = PsiManager.getInstance(project).findDirectory(baseDir) ?: return emptyList()
            return listOf(JapaTestLocation(project, psiDir, locationInfo))
        }

        val virtualFile = LocalFileSystem.getInstance().findFileByPath(locationInfo.filePath) ?: return emptyList()
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) as? JSFile ?: return emptyList()

        // Try to find the specific test/suite in the file
        val testElement = findTestElement(psiFile, locationInfo)

        return if (testElement != null) {
            // Create our custom location with metadata for run configuration
            listOf(JapaTestLocation(project, testElement, locationInfo))
        } else {
            // Fallback to file location
            listOf(JapaTestLocation(project, psiFile, locationInfo))
        }
    }

    private fun findTestElement(psiFile: JSFile, locationInfo: LocationInfo): JSCallExpression? {
        val allCalls = PsiTreeUtil.findChildrenOfType(psiFile, JSCallExpression::class.java)

        // Look for the specific test
        if (locationInfo.testName != null) {
            return allCalls.firstOrNull { call ->
                JapaTestDetector.isJapaTestCall(call) &&
                    JapaTestDetector.getTestName(call) == locationInfo.testName
            }
        }

        // Look for the suite/group
        if (locationInfo.suiteName != null) {
            return allCalls.firstOrNull { call ->
                JapaTestDetector.isTestGroup(call) &&
                    JapaTestDetector.getTestName(call) == locationInfo.suiteName
            }
        }

        return null
    }

    companion object {
        const val PROTOCOL = "japa"
    }

    data class LocationInfo(
        val filePath: String,
        val suiteName: String?,
        val groupName: String?,
        val testName: String?,
    )
}
