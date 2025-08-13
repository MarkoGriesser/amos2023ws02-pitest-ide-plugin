package com.amos.pitmutationmate.pitmutationmate.listeners

import com.intellij.openapi.project.Project
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.startup.ProjectActivity
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

/**
 * Listens for Gradle sync events and deletes the marker file
 * if the override plugin is not present in the root build.gradle.kts.
 */
class GradleSyncDeleteCompanionFileListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        val notificationManager = project.getService(ExternalSystemProgressNotificationManager::class.java)
        notificationManager.addNotificationListener(object : ExternalSystemTaskNotificationListener {
            override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
                if (
                    id.projectSystemId == GradleConstants.SYSTEM_ID &&
                    id.type == ExternalSystemTaskType.RESOLVE_PROJECT
                ) {
                    checkAndDeleteCompanionFile(project)
                }
            }

            // Other methods are no-ops
            override fun onStart(id: ExternalSystemTaskId) {}
            override fun onEnd(id: ExternalSystemTaskId) {}
            override fun onSuccess(id: ExternalSystemTaskId) {}
            override fun onFailure(id: ExternalSystemTaskId, e: Exception) {}
            override fun onStatusChange(event: com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent) {}
            override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {}
            override fun onCancel(id: ExternalSystemTaskId) {}
            override fun beforeCancel(id: ExternalSystemTaskId) {}
        }, project)
    }

    private fun checkAndDeleteCompanionFile(project: Project) {
        val projectDir = project.basePath ?: return
        val buildGradleKts = File(projectDir, "build.gradle.kts")
        val markerFile = File(projectDir, ".pitmutationmate_companion_present")
        val pluginId = "io.github.amos-pitmutationmate.pitmutationmate.override"

        if (buildGradleKts.exists()) {
            val buildFileText = buildGradleKts.readText()

            // Remove block comments (/* ... */) and line comments (// ...)
            val noBlockComments = buildFileText.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            val noComments = noBlockComments.replace(Regex("//.*?$", setOf(RegexOption.MULTILINE)), "")

            // Now search for the pluginId in the uncommented code
            val pluginPresent = Regex("""\b$pluginId\b""").containsMatchIn(noComments)

            if (!pluginPresent && markerFile.exists()) {
                markerFile.delete()
            }
        }
    }
}