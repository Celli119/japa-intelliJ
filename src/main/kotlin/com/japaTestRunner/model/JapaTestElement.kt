package com.japaTestRunner.model

import com.intellij.lang.javascript.psi.JSCallExpression
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement

/**
 * Represents a Japa test element (either a test or a test group)
 */
sealed class JapaTestElement {
    abstract val name: String
    abstract val file: VirtualFile
    abstract val psiElement: PsiElement
    abstract val lineNumber: Int

    data class Test(
        override val name: String,
        override val file: VirtualFile,
        override val psiElement: JSCallExpression,
        override val lineNumber: Int,
        val groupName: String? = null,
    ) : JapaTestElement()

    data class TestGroup(
        override val name: String,
        override val file: VirtualFile,
        override val psiElement: JSCallExpression,
        override val lineNumber: Int,
    ) : JapaTestElement()
}

/**
 * Location of a test for execution
 */
data class JapaTestLocation(
    val filePath: String,
    val testName: String? = null,
    val groupName: String? = null,
    val lineNumber: Int? = null,
) {
    fun toCliArgument(): String {
        return when {
            testName != null && groupName != null -> "$filePath:$groupName > $testName"
            testName != null -> "$filePath:$testName"
            groupName != null -> "$filePath:$groupName"
            else -> filePath
        }
    }
}
