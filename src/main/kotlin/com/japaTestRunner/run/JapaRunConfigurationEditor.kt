package com.japaTestRunner.run

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Editor for Japa run configuration settings
 */
class JapaRunConfigurationEditor : SettingsEditor<JapaRunConfiguration>() {

    private val testFileField = TextFieldWithBrowseButton()
    private val testNameField = JBTextField()
    private val groupNameField = JBTextField()
    private val additionalArgsField = JBTextField()
    private val workingDirField = TextFieldWithBrowseButton()

    init {
        testFileField.addBrowseFolderListener(
            "Select Test File",
            "Choose a test file to run",
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor(),
        )

        workingDirField.addBrowseFolderListener(
            "Select Working Directory",
            "Choose the working directory",
            null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
        )
    }

    override fun createEditor(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Test file:"), testFileField, 1, false)
            .addLabeledComponent(JBLabel("Test name (optional):"), testNameField, 1, false)
            .addLabeledComponent(JBLabel("Group name (optional):"), groupNameField, 1, false)
            .addLabeledComponent(JBLabel("Additional arguments:"), additionalArgsField, 1, false)
            .addLabeledComponent(JBLabel("Working directory:"), workingDirField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    override fun resetEditorFrom(configuration: JapaRunConfiguration) {
        testFileField.text = configuration.testFilePath
        testNameField.text = configuration.testName ?: ""
        groupNameField.text = configuration.groupName ?: ""
        additionalArgsField.text = configuration.additionalArgs
        workingDirField.text = configuration.workingDirectory
    }

    override fun applyEditorTo(configuration: JapaRunConfiguration) {
        configuration.testFilePath = testFileField.text
        configuration.testName = testNameField.text.takeIf { it.isNotEmpty() }
        configuration.groupName = groupNameField.text.takeIf { it.isNotEmpty() }
        configuration.additionalArgs = additionalArgsField.text
        configuration.workingDirectory = workingDirField.text
    }
}
