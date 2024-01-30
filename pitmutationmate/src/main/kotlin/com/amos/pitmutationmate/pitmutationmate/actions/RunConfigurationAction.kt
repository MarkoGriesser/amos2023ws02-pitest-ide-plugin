// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023 Brianne Oberson <brianne.oberson@gmail.com>

package com.amos.pitmutationmate.pitmutationmate.actions

import com.amos.pitmutationmate.pitmutationmate.configuration.RunConfiguration
import com.amos.pitmutationmate.pitmutationmate.configuration.RunConfigurationType
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

abstract class RunConfigurationAction : AnAction() {
    fun updateAndExecuteRunConfig(classFQN: String?, project: Project) {
        val executor = ExecutorRegistry.getInstance().getExecutorById("Run")

        val runManager = RunManager.getInstance(project)

        var runConfig = runManager.findConfigurationByName("Default")
        if (runConfig == null) {
            runConfig = runManager.createConfiguration("Default", RunConfigurationType::class.java)
            (runConfig.configuration as RunConfiguration).isDefault = true
        }
        runConfig.configuration.let {
            val rc = it as RunConfiguration
            if (classFQN != null) {
                rc.classFQN = classFQN
            }
        }
        runManager.addConfiguration(runConfig)

        ProgramRunnerUtil.executeConfiguration(runConfig, executor!!)
    }
}
