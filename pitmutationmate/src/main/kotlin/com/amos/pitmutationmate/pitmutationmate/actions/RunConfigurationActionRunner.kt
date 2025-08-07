// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023 Brianne Oberson <brianne.oberson@gmail.com>

package com.amos.pitmutationmate.pitmutationmate.actions

import com.amos.pitmutationmate.pitmutationmate.configuration.RunConfiguration
import com.amos.pitmutationmate.pitmutationmate.configuration.RunConfigurationType
import com.amos.pitmutationmate.pitmutationmate.services.ReportPathGeneratorService
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.psi.PsiElement

object RunConfigurationActionRunner {
    private const val DEFAULT_RUN_CONFIG_NAME = "PITest"

    private fun getGradleSubmodulePath(project: Project, psiElement: PsiElement?): Pair<String, String> {
        if (psiElement != null) {
            val vFile: VirtualFile? = psiElement.containingFile?.virtualFile
            if (vFile != null) {
                val module: Module? = ProjectFileIndex.getInstance(project).getModuleForFile(vFile)
                if (module != null) {
                    // Use the first content root as the module's directory
                    val contentRoots = com.intellij.openapi.roots.ModuleRootManager.getInstance(module).contentRoots
                    if (contentRoots.isNotEmpty()) {
                        val projectBasePath = project.basePath ?: return ":" to "root"
                        val contentRoot = contentRoots[0]
                        val contentRootPath = contentRoot.path

                        // Only use the first directory after the project root as the gradle submodule
                        val relative = contentRootPath.removePrefix(projectBasePath).trimStart('/', '\\')
                        if (relative.isNotEmpty()) {
                            // Split the relative path and take only the first directory as the submodule
                            val parts = relative.split("[/\\\\]".toRegex()).filter { it.isNotEmpty() }
                            if (parts.isNotEmpty()) {
                                val gradlePath = ":" + parts[0]
                                val gradleName = parts[0]
                                return gradlePath to gradleName
                            }
                        }
                    }
                    // Fallback: Use module name
                    return ":${module.name}" to module.name
                }
            }
        }
        // Fallback to root
        return ":" to "root"
    }

    fun updateAndExecuteRunConfig(classFQN: String?, project: Project, psiElement: PsiElement? = null) {
        val executor = ExecutorRegistry.getInstance().getExecutorById("Run")
        val runManager = RunManager.getInstance(project)

        val (gradleSubmodulePath, gradleModuleDisplayName) = getGradleSubmodulePath(project, psiElement)

        val runConfigName = DEFAULT_RUN_CONFIG_NAME
        var runConfig = runManager.findConfigurationByName(runConfigName)
        if (runConfig == null) {
            runConfig = runManager.createConfiguration(runConfigName, RunConfigurationType::class.java)
            (runConfig.configuration as RunConfiguration).isDefault = true
        }
        runConfig.configuration.let { config ->
            val rc = config as RunConfiguration
            if (classFQN != null) {
                // Adds a star at the end of each ClassFQN so every inner class is included in the pitest task
                rc.classFQN = classFQN.split(",").joinToString(separator = ",") { classIt -> "$classIt*" }
            }

            val module = ModuleManager.getInstance(project)
                .modules
                .find { it.name == gradleModuleDisplayName }

            // Get buildVariant from the run configuration
            val buildVariant = rc.buildType
            val pitestTaskName = if (buildVariant.isNullOrBlank()) {
                "pitestDebug"
            } else {
                // Capitalize the first letter of the buildVariant for Gradle task naming
                val capitalized = buildVariant.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                "pitest${capitalized}"
            }
            rc.taskName = "$gradleSubmodulePath${if (!gradleSubmodulePath.endsWith(":")) ":" else ""}$pitestTaskName"
        }
        runManager.addConfiguration(runConfig)
        runManager.selectedConfiguration = runConfig

        ProgramRunnerUtil.executeConfiguration(runConfig, executor!!)
    }

    // Delete the run configuration if the override plugin is not loaded
    fun deleteRunConfiguration(project: Project) {
        val runManager = RunManager.getInstance(project)
        val configurations = runManager.allSettings.filter { it.name.contains(DEFAULT_RUN_CONFIG_NAME) }
        configurations.forEach { runManager.removeConfiguration(it) }
    }
}
