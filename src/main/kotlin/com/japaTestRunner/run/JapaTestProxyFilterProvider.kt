package com.japaTestRunner.run

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager

/**
 * Provides proper context for running tests from the test tree
 */
object JapaTestProxyFilterProvider {

    /**
     * Create a Location from a test proxy that can be used by run configuration producers
     */
    fun createLocation(project: Project, testProxy: SMTestProxy): Location<PsiElement>? {
        val locationUrl = testProxy.locationUrl ?: return null

        // Parse the location hint
        val locationInfo = parseJapaLocationHint(locationUrl) ?: return null

        // For suite-level tests without a file path, use the project's base directory
        if (locationInfo.filePath.isEmpty()) {
            val basePath = project.basePath ?: return null
            val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return null
            val psiDir = PsiManager.getInstance(project).findDirectory(baseDir) ?: return null
            return JapaTestLocation(project, psiDir, locationInfo)
        }

        val virtualFile = LocalFileSystem.getInstance().findFileByPath(locationInfo.filePath) ?: return null
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null

        // Create a custom location that includes test metadata
        return JapaTestLocation(project, psiFile, locationInfo)
    }
}

/**
 * Custom location that stores Japa test metadata
 */
class JapaTestLocation(
    project: Project,
    psiElement: PsiElement,
    val locationInfo: JapaTestLocator.LocationInfo,
) : PsiLocation<PsiElement>(project, psiElement) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JapaTestLocation) return false
        return locationInfo == other.locationInfo
    }

    override fun hashCode(): Int {
        return locationInfo.hashCode()
    }
}
