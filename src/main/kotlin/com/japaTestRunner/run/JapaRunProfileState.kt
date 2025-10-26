package com.japaTestRunner.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.japaTestRunner.config.AdonisConfigParser
import java.io.File

/**
 * State for running Japa tests
 */
class JapaRunProfileState(
    private val environment: ExecutionEnvironment,
    private val configuration: JapaRunConfiguration,
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        val commandLine = createCommandLine()
        val processHandler = com.intellij.execution.process.KillableColoredProcessHandler(commandLine)
        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }

    override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult {
        val processHandler = startProcess()
        val consoleProperties = JapaConsoleProperties(configuration, executor)

        // Create a custom output stream that writes complete lines
        val buffer = StringBuilder()
        val serviceMessageStream = java.io.PrintStream(
            object : java.io.OutputStream() {
                override fun write(b: Int) {
                    val char = b.toChar()
                    if (char == '\n') {
                        // Write the complete line at once
                        val line = buffer.toString()
                        buffer.clear()
                        processHandler.notifyTextAvailable(
                            line + "\n",
                            com.intellij.execution.process.ProcessOutputTypes.STDOUT,
                        )
                    } else {
                        buffer.append(char)
                    }
                }
            },
        )

        // Add output parser BEFORE attaching console
        // Pass the test file path and working directory for location hints
        val testFile = when (val scope = configuration.getTestScope()) {
            is JapaRunConfiguration.TestScope.TestFile -> scope.filePath
            is JapaRunConfiguration.TestScope.TestGroup -> scope.filePath
            is JapaRunConfiguration.TestScope.SingleTest -> scope.filePath
            else -> ""
        }
        val workingDir = configuration.workingDirectory.ifEmpty {
            configuration.project.basePath ?: ""
        }
        val outputParser = com.japaTestRunner.execution.JapaOutputToTestEventsConverter(
            serviceMessageStream,
            testFile,
            workingDir,
        )
        processHandler.addProcessListener(outputParser)

        // Create and attach console
        val console = SMTestRunnerConnectionUtil.createConsole("Japa", consoleProperties)
        console.attachToProcess(processHandler)

        return DefaultExecutionResult(console, processHandler)
    }

    private fun createCommandLine(): GeneralCommandLine {
        val project = configuration.project
        val workingDir = configuration.workingDirectory.ifEmpty {
            project.basePath ?: throw IllegalStateException("Project path not found")
        }

        // Get Node.js interpreter
        val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
        val nodePath = when (interpreter) {
            is NodeJsLocalInterpreter -> interpreter.interpreterSystemDependentPath
            else -> "node"
        }

        val commandLine = GeneralCommandLine()
        commandLine.exePath = nodePath
        commandLine.workDirectory = File(workingDir)

        // Build the command arguments
        val args = buildTestCommand()
        commandLine.addParameters(args)

        // Add environment variables
        commandLine.environment.putAll(configuration.envVars)

        return commandLine
    }

    private fun buildTestCommand(): List<String> {
        val args = mutableListOf<String>()

        // Detect the test runner command by checking package.json and common files
        val workingDir = configuration.workingDirectory.ifEmpty { configuration.project.basePath ?: "" }
        val aceFile = File(workingDir, "ace")
        val testFile = File(workingDir, "bin/test.js")
        val packageJsonFile = File(workingDir, "package.json")

        // Try to read test script from package.json
        var testScript: String? = null
        if (packageJsonFile.exists()) {
            try {
                val packageJson = packageJsonFile.readText()
                val testScriptMatch = Regex(""""test"\s*:\s*"([^"]+)"""").find(packageJson)
                testScript = testScriptMatch?.groupValues?.get(1)
            } catch (e: Exception) {
                // Ignore errors reading package.json
            }
        }

        // Determine which command to use
        when {
            // If package.json has "node ace test", use that
            testScript?.contains("ace test") == true -> {
                args.add("ace")
                args.add("test")
            }
            // If ace file exists, use it
            aceFile.exists() -> {
                args.add("ace")
                args.add("test")
            }
            // If bin/test.js exists, use it
            testFile.exists() -> {
                args.add("bin/test.js")
            }
            // If package.json has any test script, try to parse it
            testScript != null -> {
                // Remove "node " prefix if present
                val script = testScript.removePrefix("node ").trim()
                args.addAll(script.split(" "))
            }
            else -> {
                // Last resort fallback
                args.add("ace")
                args.add("test")
            }
        }

        // Add test scope filters
        when (val scope = configuration.getTestScope()) {
            is JapaRunConfiguration.TestScope.TestFile -> {
                args.add("--files")
                args.add(scope.filePath)
            }
            is JapaRunConfiguration.TestScope.TestGroup -> {
                // Check if this is a top-level suite (functional, unit, etc.)
                // These are defined in adonisrc.ts and should be passed as positional arguments
                if (scope.filePath.isEmpty() && isJapaGroupName(scope.groupName)) {
                    // Add suite name as positional argument (e.g., "functional")
                    args.add(scope.groupName)
                } else {
                    // Regular test group within a file
                    args.add("--files")
                    args.add(scope.filePath)
                    args.add("--groups")
                    args.add(scope.groupName)
                }
            }
            is JapaRunConfiguration.TestScope.SingleTest -> {
                args.add("--files")
                args.add(scope.filePath)
                args.add("--tests")
                args.add(scope.testName)
                scope.groupName?.let {
                    args.add("--groups")
                    args.add(it)
                }
            }
            else -> {
                // Run all tests
            }
        }

        // Add additional arguments
        if (configuration.additionalArgs.isNotEmpty()) {
            args.addAll(configuration.additionalArgs.split(" "))
        }

        return args
    }

    private fun isJapaGroupName(name: String): Boolean {
        // Get suite names dynamically from adonisrc configuration
        val suiteNames = AdonisConfigParser.getSuiteNames(environment.project)
        return name in suiteNames
    }
}

/**
 * Console properties for Japa test runs
 */
class JapaConsoleProperties(
    config: JapaRunConfiguration,
    executor: Executor,
) : SMTRunnerConsoleProperties(config, "Japa", executor) {

    init {
        isIdBasedTestTree = false
        isPrintTestingStartedTime = false
    }

    override fun getTestLocator() = JapaTestLocator()
}
