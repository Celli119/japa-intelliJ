package com.japaTestRunner.editor

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.psi.PsiElement
import com.japaTestRunner.detection.JapaTestDetector

/**
 * Provides gutter icons for running Japa tests
 */
class JapaTestLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        // Only show markers on the main "test" identifier, not on "group"
        if (!isMainTestIdentifier(element)) {
            return null
        }

        // For test('...'), structure is: element -> JSReferenceExpression -> JSCallExpression
        // For test.group('...'), structure is: element -> JSReferenceExpression -> JSReferenceExpression -> JSCallExpression
        val callExpression = (element.parent?.parent as? JSCallExpression)
            ?: (element.parent?.parent?.parent as? JSCallExpression)
            ?: return null

        if (!JapaTestDetector.isJapaTestCall(callExpression)) {
            return null
        }

        val testName = JapaTestDetector.getTestName(callExpression) ?: return null
        val isGroup = JapaTestDetector.isTestGroup(callExpression)

        val tooltipText = if (isGroup) {
            "Run test group '$testName'"
        } else {
            "Run test '$testName'"
        }

        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            { tooltipText },
            *ExecutorAction.getActions(0),
        )
    }

    private fun isMainTestIdentifier(element: PsiElement): Boolean {
        val text = element.text
        // Only match on "test", not on "group"
        if (text != "test") {
            return false
        }

        // For test.group(), only show marker on the leftmost "test"
        // Check if this is a reference expression
        val parent = element.parent as? com.intellij.lang.javascript.psi.JSReferenceExpression ?: return false

        // If this is test.group(), the parent will have a qualifier (test) and a reference name (group)
        // We only want to show marker when we're on the base "test", not when it's qualified
        // So we check: if parent has a parent that is also a JSReferenceExpression, skip it
        val grandParent = parent.parent
        if (grandParent is com.intellij.lang.javascript.psi.JSReferenceExpression) {
            // This means we're in the middle of a chain like "test.group"
            // Only show if we're the qualifier (leftmost), not the method name
            return grandParent.qualifier == parent
        }

        return true
    }
}
