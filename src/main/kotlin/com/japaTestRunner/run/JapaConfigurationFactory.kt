package com.japaTestRunner.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

/**
 * Factory for creating Japa run configurations
 */
class JapaConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    override fun getId(): String = "Japa"

    override fun createTemplateConfiguration(project: Project): RunConfiguration {
        return JapaRunConfiguration(project, this, "Japa Tests")
    }

    override fun getName(): String = "Japa"
}
