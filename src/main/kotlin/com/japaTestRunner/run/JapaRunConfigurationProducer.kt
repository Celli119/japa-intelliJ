package com.japaTestRunner.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.japaTestRunner.config.AdonisConfigParser
import com.japaTestRunner.detection.JapaTestDetector

/**
 * Produces run configurations from context (e.g., right-click on test)
 */
class JapaRunConfigurationProducer : LazyRunConfigurationProducer<JapaRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return getJapaConfigurationType().configurationFactories[0]
    }

    override fun isConfigurationFromContext(
        configuration: JapaRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val location = context.location ?: return false

        // Check if this is from test tree (custom location with metadata)
        if (location is JapaTestLocation) {
            val info = location.locationInfo
            return when {
                // Individual test
                info.testName != null -> {
                    configuration.testFilePath == info.filePath &&
                        configuration.testName == info.testName &&
                        configuration.groupName == info.groupName
                }
                // Suite or group
                info.suiteName != null -> {
                    if (info.filePath.isEmpty() && isJapaGroupName(info.suiteName)) {
                        // Top-level suite (functional, unit, etc.)
                        configuration.testFilePath.isEmpty() &&
                            configuration.groupName == info.suiteName &&
                            configuration.testName == null
                    } else {
                        // File-based group
                        configuration.testFilePath == info.filePath &&
                            configuration.groupName == info.suiteName &&
                            configuration.testName == null
                    }
                }
                // File-level
                info.filePath.isNotEmpty() -> {
                    configuration.testFilePath == info.filePath &&
                        configuration.testName == null &&
                        configuration.groupName == null
                }
                else -> false
            }
        }

        // Otherwise, check source code context
        val element = location.psiElement
        val testCall = findTestCall(element) ?: return false
        val testFile = element.containingFile.virtualFile ?: return false

        return when {
            JapaTestDetector.isTestGroup(testCall) -> {
                val groupName = JapaTestDetector.getTestName(testCall)
                configuration.testFilePath == testFile.path &&
                    configuration.groupName == groupName &&
                    configuration.testName == null
            }
            JapaTestDetector.isJapaTestCall(testCall) -> {
                val testName = JapaTestDetector.getTestName(testCall)
                configuration.testFilePath == testFile.path &&
                    configuration.testName == testName
            }
            else -> false
        }
    }

    override fun setupConfigurationFromContext(
        configuration: JapaRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        this.context = context // Store context for isJapaGroupName
        val location = context.location ?: return false

        // Check if this is from a test tree (has our custom location)
        if (location is JapaTestLocation) {
            return setupFromTestTree(configuration, location, sourceElement)
        }

        // Otherwise, it's from source code
        val element = location.psiElement
        val testCall = findTestCall(element) ?: return false
        val testFile = element.containingFile.virtualFile ?: return false

        if (!JapaTestDetector.isJapaTestCall(testCall)) {
            return false
        }

        configuration.testFilePath = testFile.path
        configuration.workingDirectory = context.project.basePath ?: ""

        when {
            JapaTestDetector.isTestGroup(testCall) -> {
                val groupName = JapaTestDetector.getTestName(testCall) ?: return false
                configuration.groupName = groupName
                configuration.name = "Test Group: $groupName"
            }
            else -> {
                val testName = JapaTestDetector.getTestName(testCall) ?: return false
                configuration.testName = testName
                configuration.name = "Test: $testName"

                // Try to find if this test is inside a group
                val parentGroup = findParentGroup(testCall)
                if (parentGroup != null) {
                    configuration.groupName = JapaTestDetector.getTestName(parentGroup)
                }
            }
        }

        sourceElement.set(testCall)
        return true
    }

    private fun setupFromTestTree(
        configuration: JapaRunConfiguration,
        location: JapaTestLocation,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val info = location.locationInfo
        configuration.workingDirectory = location.project.basePath ?: ""

        when {
            // Individual test
            info.testName != null -> {
                configuration.testFilePath = info.filePath
                configuration.testName = info.testName
                configuration.groupName = info.groupName
                configuration.name = "Test: ${info.testName}"
            }
            // Suite name that looks like a Japa suite (e.g., "functional", "unit")
            // These are defined in adonisrc.ts and should be passed as positional arguments
            info.suiteName != null && isJapaGroupName(info.suiteName) -> {
                configuration.testFilePath = "" // No file filter
                configuration.groupName = info.suiteName
                configuration.testName = null
                configuration.name = "Suite: ${info.suiteName}"
            }
            // Test group/suite with a specific file (like "Auth Controller - Routes")
            info.suiteName != null && info.filePath.isNotEmpty() -> {
                configuration.testFilePath = info.filePath
                configuration.groupName = info.suiteName
                configuration.testName = null
                configuration.name = "Test Suite: ${info.suiteName}"
            }
            // Test group without a file path (from suite-level run)
            // This happens when clicking on a group like "AdminController" from a "functional" suite run
            info.suiteName != null && info.filePath.isEmpty() -> {
                configuration.testFilePath = "" // No file filter
                configuration.groupName = info.suiteName
                configuration.testName = null
                configuration.name = "Test Group: ${info.suiteName}"
            }
            // Run specific file
            info.filePath.isNotEmpty() -> {
                configuration.testFilePath = info.filePath
                configuration.testName = null
                configuration.groupName = null
                configuration.name = "Tests in ${info.filePath.substringAfterLast("/")}"
            }
            // Fallback: run all tests
            else -> {
                configuration.testFilePath = ""
                configuration.testName = null
                configuration.groupName = null
                configuration.name = "All Tests"
            }
        }

        sourceElement.set(location.psiElement)
        return true
    }

    private fun isJapaGroupName(name: String): Boolean {
        // Get suite names dynamically from adonisrc configuration
        // This caches the result per project to avoid repeated file I/O
        val project = context?.project ?: return false
        val suiteNames = AdonisConfigParser.getSuiteNames(project)
        return name in suiteNames
    }

    // Store context for isJapaGroupName to access project
    private var context: ConfigurationContext? = null

    private fun findTestCall(element: PsiElement): JSCallExpression? {
        // Try to find the nearest test call expression
        var current: PsiElement? = element
        while (current != null) {
            if (current is JSCallExpression && JapaTestDetector.isJapaTestCall(current)) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun findParentGroup(testCall: JSCallExpression): JSCallExpression? {
        var parent = PsiTreeUtil.getParentOfType(testCall, JSCallExpression::class.java)
        while (parent != null) {
            if (JapaTestDetector.isTestGroup(parent)) {
                return parent
            }
            parent = PsiTreeUtil.getParentOfType(parent, JSCallExpression::class.java)
        }
        return null
    }
}
