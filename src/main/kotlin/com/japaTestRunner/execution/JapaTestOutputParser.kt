package com.japaTestRunner.execution

import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import java.util.regex.Pattern

/**
 * Parses Japa test output and converts it to IDE test events
 */
class JapaTestOutputParser {

    private val testStartPattern = Pattern.compile("^\\s*RUNNING:\\s+(.+)$")
    private val testPassPattern = Pattern.compile("^\\s*✔\\s+(.+?)\\s+\\((\\d+)\\s*ms\\)$")
    private val testFailPattern = Pattern.compile("^\\s*✖\\s+(.+?)\\s+\\((\\d+)\\s*ms\\)$")
    private val groupStartPattern = Pattern.compile("^\\s*(.+)$")
    private val errorLinePattern = Pattern.compile("^\\s+Error:\\s+(.+)$")

    private val currentGroup = StringBuilder()
    private var inErrorBlock = false

    fun parseLine(line: String): List<ServiceMessageBuilder>? {
        val messages = mutableListOf<ServiceMessageBuilder>()

        // Test started
        testStartPattern.matcher(line).let { matcher ->
            if (matcher.matches()) {
                val testName = matcher.group(1).trim()
                messages.add(
                    ServiceMessageBuilder.testStarted(testName),
                )
                return messages
            }
        }

        // Test passed
        testPassPattern.matcher(line).let { matcher ->
            if (matcher.matches()) {
                val testName = matcher.group(1).trim()
                val duration = matcher.group(2).toLongOrNull() ?: 0
                messages.add(
                    ServiceMessageBuilder.testFinished(testName)
                        .addAttribute("duration", duration.toString()),
                )
                return messages
            }
        }

        // Test failed
        testFailPattern.matcher(line).let { matcher ->
            if (matcher.matches()) {
                val testName = matcher.group(1).trim()
                val duration = matcher.group(2).toLongOrNull() ?: 0
                messages.add(
                    ServiceMessageBuilder.testFailed(testName)
                        .addAttribute("message", "Test failed")
                        .addAttribute("duration", duration.toString()),
                )
                return messages
            }
        }

        // Error details
        errorLinePattern.matcher(line).let { matcher ->
            if (matcher.matches()) {
                inErrorBlock = true
                return null
            }
        }

        return null
    }

    fun parseGroupStart(groupName: String): ServiceMessageBuilder {
        return ServiceMessageBuilder.testSuiteStarted(groupName)
    }

    fun parseGroupEnd(groupName: String): ServiceMessageBuilder {
        return ServiceMessageBuilder.testSuiteFinished(groupName)
    }
}

/**
 * Test result data
 */
data class JapaTestResult(
    val name: String,
    val status: TestStatus,
    val duration: Long = 0,
    val errorMessage: String? = null,
    val stackTrace: String? = null,
)

enum class TestStatus {
    RUNNING,
    PASSED,
    FAILED,
    SKIPPED,
}
