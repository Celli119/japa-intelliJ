package com.japaTestRunner.detection

import com.intellij.json.JsonFileType
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.lang.javascript.psi.JSFile
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.JSReferenceExpression
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

/**
 * Detects Japa tests in the project
 */
object JapaTestDetector {

    /**
     * Check if the project has Japa installed by looking at package.json
     */
    fun isJapaProject(project: Project): Boolean {
        val packageJsonFiles = FileTypeIndex.getFiles(
            JsonFileType.INSTANCE,
            GlobalSearchScope.projectScope(project),
        )

        for (file in packageJsonFiles) {
            if (file.name == "package.json") {
                val psiFile = PsiManager.getInstance(project).findFile(file) as? JsonFile ?: continue
                val rootObject = psiFile.topLevelValue as? JsonObject ?: continue

                // Check dependencies and devDependencies
                val deps = rootObject.findProperty("dependencies")?.value as? JsonObject
                val devDeps = rootObject.findProperty("devDependencies")?.value as? JsonObject

                if (hasJapaDependency(deps) || hasJapaDependency(devDeps)) {
                    return true
                }
            }
        }
        return false
    }

    private fun hasJapaDependency(depsObject: JsonObject?): Boolean {
        if (depsObject == null) return false
        return depsObject.propertyList.any { it.name.startsWith("@japa/") }
    }

    /**
     * Check if a file contains Japa tests
     */
    fun isJapaTestFile(psiFile: PsiFile): Boolean {
        if (psiFile !is JSFile) return false

        // Look for test() or test.group() calls
        val hasTestCalls = PsiTreeUtil.findChildrenOfType(psiFile, JSCallExpression::class.java)
            .any { isJapaTestCall(it) }

        return hasTestCalls
    }

    /**
     * Check if a call expression is a Japa test call
     */
    fun isJapaTestCall(callExpression: JSCallExpression): Boolean {
        val methodExpression = callExpression.methodExpression ?: return false

        return when {
            // test('name', ...)
            methodExpression is JSReferenceExpression && methodExpression.referenceName == "test" -> true
            // test.group('name', ...)
            methodExpression is JSReferenceExpression &&
                methodExpression.qualifier?.let { it is JSReferenceExpression && it.referenceName == "test" } == true &&
                methodExpression.referenceName == "group" -> true
            else -> false
        }
    }

    /**
     * Get the test name from a test call
     */
    fun getTestName(callExpression: JSCallExpression): String? {
        val arguments = callExpression.arguments
        if (arguments.isEmpty()) return null

        val firstArg = arguments[0]
        return when (firstArg) {
            is JSLiteralExpression -> firstArg.stringValue
            else -> null
        }
    }

    /**
     * Check if this is a test group
     */
    fun isTestGroup(callExpression: JSCallExpression): Boolean {
        val methodExpression = callExpression.methodExpression as? JSReferenceExpression ?: return false
        return methodExpression.referenceName == "group" &&
            methodExpression.qualifier?.let { it is JSReferenceExpression && it.referenceName == "test" } == true
    }
}
