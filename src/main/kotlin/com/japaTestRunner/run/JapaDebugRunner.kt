package com.japaTestRunner.run

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.GenericProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.fileEditor.FileDocumentManager

/**
 * Program runner for debugging Japa tests
 *
 * Note: Full Node.js debugging support requires integration with
 * the JavaScript debugger plugin. This basic implementation registers
 * the debug executor but delegates to the standard runner.
 *
 * For production use, consider integrating with:
 * - com.jetbrains.nodejs.run.NodeJsDebugProcessStarter
 * - JavaScript debugger APIs
 */
class JapaDebugRunner : GenericProgramRunner<RunnerSettings>() {

    override fun getRunnerId(): String = "JapaDebugRunner"

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        // Enable debug executor for Japa configurations
        return executorId == DefaultDebugExecutor.EXECUTOR_ID && profile is JapaRunConfiguration
    }

    override fun doExecute(state: RunProfileState, environment: ExecutionEnvironment): RunContentDescriptor? {
        // Save all documents before debugging
        FileDocumentManager.getInstance().saveAllDocuments()

        // For now, just run normally - full debug support requires Node.js debugger integration
        val executionResult = state.execute(environment.executor, this) ?: return null

        val descriptor = RunContentDescriptor(
            executionResult.executionConsole,
            executionResult.processHandler,
            executionResult.executionConsole.component,
            environment.runProfile.name,
        )

        com.intellij.execution.ui.RunContentManager.getInstance(environment.project)
            .showRunContent(environment.executor, descriptor)

        return descriptor
    }

    // Note: Actual debugging requires additional Node.js debugger integration
    // Production implementation would need to:
    // 1. Start Node.js with --inspect flag
    // 2. Connect to debug port
    // 3. Set up breakpoint handling
    // 4. Integrate with XDebugProcess
}
