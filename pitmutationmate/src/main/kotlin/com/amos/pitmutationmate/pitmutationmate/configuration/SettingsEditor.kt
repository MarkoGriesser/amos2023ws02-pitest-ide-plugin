// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023 Lennart Heimbs <lennart@heimbs.me>

package com.amos.pitmutationmate.pitmutationmate.configuration

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.TextFieldWithHistory
import com.intellij.util.ui.FormBuilder
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel

class SettingsEditor : SettingsEditor<RunConfiguration>() {
    private val myPanel: JPanel
    private val gradleTaskField: TextFieldWithHistory = TextFieldWithHistory()
    private val gradleExecutableField: TextFieldWithBrowseButton = TextFieldWithBrowseButton()
    private val overwriteCheckbox: JCheckBox = JCheckBox("No Overwrite")

    init {
        gradleTaskField.text = "pitest"
        gradleExecutableField.addBrowseFolderListener(
            "Select Gradle Script",
            null,
            null,
            FileChooserDescriptorFactory.createSingleFileDescriptor()
        )
        myPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent("Overwrite behavior", overwriteCheckbox)
            .addLabeledComponent("Gradle task", gradleTaskField)
            .addLabeledComponent("Gradle script", gradleExecutableField)
            .panel
    }

    override fun resetEditorFrom(runConfiguration: RunConfiguration) {
        overwriteCheckbox.isSelected = runConfiguration.overwriteScope
        gradleTaskField.text = runConfiguration.taskName
        runConfiguration.gradleExecutable.also { gradleExecutableField.text = it ?: "" }
    }

    override fun applyEditorTo(runConfiguration: RunConfiguration) {
        runConfiguration.overwriteScope = overwriteCheckbox.isSelected
        runConfiguration.taskName = gradleTaskField.text
        runConfiguration.gradleExecutable = gradleExecutableField.text
    }

    override fun createEditor(): JComponent {
        return myPanel
    }
}
