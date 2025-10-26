package com.japaTestRunner.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element

/**
 * Run configuration for Japa tests
 */
class JapaRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String,
) : LocatableConfigurationBase<JapaRunProfileState>(project, factory, name) {

    var testFilePath: String = ""
    var testName: String? = null
    var groupName: String? = null
    var additionalArgs: String = ""
    var workingDirectory: String = ""
    var envVars: Map<String, String> = emptyMap()

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return JapaRunConfigurationEditor()
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return JapaRunProfileState(environment, this)
    }

    override fun checkConfiguration() {
        if (workingDirectory.isEmpty()) {
            workingDirectory = project.basePath ?: ""
        }
    }

    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
        super.readExternal(element)
        testFilePath = element.getAttributeValue("testFilePath") ?: ""
        testName = element.getAttributeValue("testName")
        groupName = element.getAttributeValue("groupName")
        additionalArgs = element.getAttributeValue("additionalArgs") ?: ""
        workingDirectory = element.getAttributeValue("workingDirectory") ?: ""

        // Read environment variables
        val envElement = element.getChild("envs")
        if (envElement != null) {
            envVars = envElement.children.associate {
                it.getAttributeValue("name") to it.getAttributeValue("value")
            }
        }
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        element.setAttribute("testFilePath", testFilePath)
        testName?.let { element.setAttribute("testName", it) }
        groupName?.let { element.setAttribute("groupName", it) }
        element.setAttribute("additionalArgs", additionalArgs)
        element.setAttribute("workingDirectory", workingDirectory)

        // Write environment variables
        if (envVars.isNotEmpty()) {
            val envsElement = Element("envs")
            envVars.forEach { (name, value) ->
                val envElement = Element("env")
                envElement.setAttribute("name", name)
                envElement.setAttribute("value", value)
                envsElement.addContent(envElement)
            }
            element.addContent(envsElement)
        }
    }

    fun getTestScope(): TestScope {
        return when {
            testName != null -> TestScope.SingleTest(testFilePath, testName!!, groupName)
            groupName != null -> TestScope.TestGroup(testFilePath, groupName!!)
            testFilePath.isNotEmpty() -> TestScope.TestFile(testFilePath)
            else -> TestScope.AllTests
        }
    }

    sealed class TestScope {
        object AllTests : TestScope()
        data class TestFile(val filePath: String) : TestScope()
        data class TestGroup(val filePath: String, val groupName: String) : TestScope()
        data class SingleTest(val filePath: String, val testName: String, val groupName: String?) : TestScope()
    }
}
