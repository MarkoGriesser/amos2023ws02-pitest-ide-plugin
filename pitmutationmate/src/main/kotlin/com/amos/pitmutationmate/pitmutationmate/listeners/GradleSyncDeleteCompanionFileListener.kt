package com.amos.pitmutationmate.pitmutationmate.listeners

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener
import com.intellij.util.messages.MessageBusConnection
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import java.io.File

import com.intellij.openapi.externalSystem.model.task.*
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import org.jetbrains.plugins.gradle.util.GradleConstants

/**
 * Listens for Gradle sync events and deletes the COMPANION_IS_PRESENT marker file
 * if the override plugin is no longer present in the build file.
 */
class GradleSyncDeleteCompanionFileListener : StartupActivity {

    override fun runActivity(project: Project) {
        val connection: MessageBusConnection = project.messageBus.connect()
        connection.subscribe(
            GradleSettingsListener.TOPIC,
            object : GradleSettingsListener {
                override fun onProjectsLoaded(linkedProjects: MutableCollection<GradleProjectSettings>) {
                    checkAndDeleteCompanionFile(project)
                }

                override fun onProjectsLinked(settings: MutableCollection<GradleProjectSettings>) {
                    checkAndDeleteCompanionFile(project)
                }

                override fun onProjectsUnlinked(linkedProjectPaths: Set<String>) {
                    checkAndDeleteCompanionFile(project)
                }
            }
        )

        // 🔹 External System Task Listener – listens to all Gradle syncs
        val notificationManager = project.getService(ExternalSystemProgressNotificationManager::class.java)

        notificationManager.addNotificationListener(object : ExternalSystemTaskNotificationListener {
            @Deprecated("Superseded by onStart(id, workingDir)")
            override fun onStart(id: ExternalSystemTaskId) {
                // no-op
            }

            override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
                if (
                    id.projectSystemId == GradleConstants.SYSTEM_ID &&
                    id.type == ExternalSystemTaskType.RESOLVE_PROJECT
                ) {
                    // This is triggered before Gradle sync
                    checkAndDeleteCompanionFile(project)
                }
            }

            // necessary because interface is abstract
            override fun onEnd(id: ExternalSystemTaskId) {}
            override fun onSuccess(id: ExternalSystemTaskId) {}
            override fun onFailure(id: ExternalSystemTaskId, e: Exception) {}
            override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {}
            override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {}
            override fun onCancel(id: ExternalSystemTaskId) {}
            override fun beforeCancel(p0: ExternalSystemTaskId) {}
        }, project)
    }

    private fun checkAndDeleteCompanionFile(project: Project) {
        val logger = Logger.getInstance(GradleSyncDeleteCompanionFileListener::class.java)
        val projectDir = project.basePath ?: return
        val buildGradleKts = File(projectDir, "build.gradle.kts")
        val markerFile = File(projectDir, "COMPANION_IS_PRESENT")

        val pluginIdKts = "io.github.amos-pitmutationmate.pitmutationmate.override"

        val pluginPresent = when {
            buildGradleKts.exists() -> buildGradleKts.readText().contains(pluginIdKts)
            else -> false
        }

        if (!pluginPresent && markerFile.exists()) {
            val deleted = markerFile.delete()
            if (deleted) {
                logger.info("Deleted marker file: ${markerFile.absolutePath} because override plugin is not present.")
            } else {
                logger.warn("Failed to delete marker file: ${markerFile.absolutePath}")
            }
        }
    }
}