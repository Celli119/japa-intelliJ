package com.japaTestRunner.execution

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.util.Key
import java.io.PrintStream

/**
 * Converts Japa test output to TeamCity service messages for IDE test runner
 */
class JapaOutputToTestEventsConverter(
    private val serviceMessageOutput: PrintStream,
    private val testFilePath: String,
    private val workingDirectory: String,
) : ProcessAdapter() {

    private val buffer = StringBuilder()
    private var currentSuite: String? = null
    private var currentGroup: String? = null
    private var currentFilePath: String = testFilePath // Track current file path from output
    private val startedTests = mutableSetOf<String>()

    // Patterns for parsing Japa output - capture file paths
    private val suitePattern = Regex("^\\s*(.+?)\\s+\\((tests/.+?\\.spec\\.ts)\\)\\s*$")
    private val testPassPattern = Regex("^\\s*✔\\s+(.+?)\\s+\\(([^)]+)\\)\\s*$")
    private val testFailPattern = Regex("^\\s*✖\\s+(.+?)\\s+\\(([^)]+)\\)\\s*$")
    private val groupPattern = Regex("^\\s*(.+?)\\s+/\\s+(.+?)\\s+\\((tests/.+?\\.spec\\.ts)\\)\\s*$")

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
        val text = event.text ?: return

        buffer.append(text)

        // Process complete lines
        var newlineIndex = buffer.indexOf("\n")
        while (newlineIndex >= 0) {
            val line = buffer.substring(0, newlineIndex).trim()
            buffer.delete(0, newlineIndex + 1)

            // Skip TeamCity service messages to avoid processing our own output
            // Also skip empty lines
            if (line.isNotEmpty() && !line.startsWith("##teamcity[")) {
                processLine(line)
            }

            newlineIndex = buffer.indexOf("\n")
        }
    }

    private fun processLine(line: String) {
        // Check for suite/group (e.g., "functional / Auth Controller - Routes (tests/functional/auth.spec.ts)")
        groupPattern.find(line)?.let { match ->
            val suiteName = match.groupValues[1].trim()
            val testGroup = match.groupValues[2].trim()
            val relativeFilePath = match.groupValues[3].trim()

            // Update current file path - convert to absolute path
            currentFilePath = if (relativeFilePath.isNotEmpty()) {
                "$workingDirectory/$relativeFilePath"
            } else {
                testFilePath
            }

            // Finish previous group if exists
            currentGroup?.let {
                sendTestSuiteFinished(it)
                currentGroup = null
            }

            // Start suite if new
            if (currentSuite != suiteName) {
                currentSuite?.let { sendTestSuiteFinished(it) }
                sendTestSuiteStarted(suiteName)
                currentSuite = suiteName
            }

            // Start test group
            sendTestSuiteStarted(testGroup)
            currentGroup = testGroup
            return
        }

        // Check for single suite (e.g., "Auth Controller (tests/functional/auth.spec.ts)")
        suitePattern.find(line)?.let { match ->
            val suiteName = match.groupValues[1].trim()
            val relativeFilePath = match.groupValues[2].trim()

            // Update current file path - convert to absolute path
            currentFilePath = if (relativeFilePath.isNotEmpty()) {
                "$workingDirectory/$relativeFilePath"
            } else {
                testFilePath
            }

            // Finish previous group if exists
            currentGroup?.let {
                sendTestSuiteFinished(it)
                currentGroup = null
            }

            if (currentSuite != suiteName) {
                currentSuite?.let { sendTestSuiteFinished(it) }
                sendTestSuiteStarted(suiteName)
                currentSuite = suiteName
            }
            return
        }

        // Check for passed test
        testPassPattern.find(line)?.let { match ->
            val testName = match.groupValues[1].trim()
            val duration = parseDuration(match.groupValues[2])

            if (!startedTests.contains(testName)) {
                sendTestStarted(testName)
                startedTests.add(testName)
            }
            sendTestFinished(testName, duration)
            return
        }

        // Check for failed test
        testFailPattern.find(line)?.let { match ->
            val testName = match.groupValues[1].trim()
            val duration = parseDuration(match.groupValues[2])

            if (!startedTests.contains(testName)) {
                sendTestStarted(testName)
                startedTests.add(testName)
            }
            sendTestFailed(testName, "Test failed", duration)
            return
        }
    }

    private fun parseDuration(durationStr: String): Long {
        val match = Regex("([\\d.]+)\\s*(ms|s)?").find(durationStr)
        return if (match != null) {
            val value = match.groupValues[1].toDoubleOrNull() ?: 0.0
            when (match.groupValues[2]) {
                "s" -> (value * 1000).toLong()
                else -> value.toLong()
            }
        } else {
            0
        }
    }

    private fun sendTestSuiteStarted(name: String) {
        val locationHint = "japa://$currentFilePath?suite=$name"
        val message = ServiceMessageBuilder.testSuiteStarted(name)
            .addAttribute("locationHint", locationHint)
            .toString()
        serviceMessageOutput.println(message)
    }

    private fun sendTestSuiteFinished(name: String) {
        val message = ServiceMessageBuilder.testSuiteFinished(name).toString()
        serviceMessageOutput.println(message)
    }

    private fun sendTestStarted(testName: String) {
        val groupPart = if (currentGroup != null) "group=$currentGroup&" else ""
        val locationHint = "japa://$currentFilePath?${groupPart}test=$testName"
        val message = ServiceMessageBuilder.testStarted(testName)
            .addAttribute("locationHint", locationHint)
            .toString()
        serviceMessageOutput.println(message)
    }

    private fun sendTestFinished(testName: String, duration: Long) {
        val message = ServiceMessageBuilder.testFinished(testName)
            .addAttribute("duration", duration.toString())
            .toString()
        serviceMessageOutput.println(message)
    }

    private fun sendTestFailed(testName: String, errorMessage: String, duration: Long) {
        val message = ServiceMessageBuilder.testFailed(testName)
            .addAttribute("message", errorMessage)
            .addAttribute("duration", duration.toString())
            .toString()
        serviceMessageOutput.println(message)
    }

    override fun processTerminated(event: ProcessEvent) {
        // Process any remaining buffer content
        if (buffer.isNotEmpty()) {
            processLine(buffer.toString().trim())
            buffer.clear()
        }

        // Close current group and suite in proper order
        currentGroup?.let { sendTestSuiteFinished(it) }
        currentSuite?.let { sendTestSuiteFinished(it) }
    }
}
