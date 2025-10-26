package com.japaTestRunner.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.japaTestRunner.icons.JapaIcons
import javax.swing.Icon

/**
 * Configuration type for Japa test runs
 */
class JapaRunConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = "Japa Tests"

    override fun getConfigurationTypeDescription(): String = "Run Japa tests"

    override fun getIcon(): Icon = JapaIcons.Japa

    override fun getId(): String = ID

    override fun getConfigurationFactories(): Array<ConfigurationFactory> {
        return arrayOf(JapaConfigurationFactory(this))
    }

    companion object {
        const val ID = "JapaRunConfiguration"
    }
}

/**
 * Utility function to get the JapaRunConfigurationType instance
 */
fun getJapaConfigurationType(): JapaRunConfigurationType {
    return com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType(
        JapaRunConfigurationType::class.java,
    )
}
