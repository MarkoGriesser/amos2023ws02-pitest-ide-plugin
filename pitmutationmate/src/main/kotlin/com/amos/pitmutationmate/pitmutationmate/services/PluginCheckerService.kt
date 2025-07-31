// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023

package com.amos.pitmutationmate.pitmutationmate.services

import com.amos.pitmutationmate.pitmutationmate.plugincheck.PluginCheckData
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.idea.IdeaProject
import org.jetbrains.kotlin.idea.configuration.isMavenized
import java.io.File

import org.gradle.api.GradleException

@Service(Service.Level.PROJECT)
class PluginCheckerService(private val project: Project) {
    private val logger = Logger.getInstance(this::class.java)

    private var isCompanionPluginAvailable = false
    private var pluginCheckData: PluginCheckData? = null
    private var buildFiles = mutableListOf<File>()
    private var testDirectories = mutableListOf<File>()
    private var sourceDirectories = mutableListOf<File>()

    private val TEST_FILE_NAME = "COMPANION_IS_PRESENT"

    /**
     * Check if the companion plugin is available and if the pitest plugin is applied
     *
     * If the project is mavenized no checks are performed, all plugins are assumed to be available.
     * If the project is gradle, the companion plugin is checked first based on the existence of a task.
     * If the companion plugin is available, the pitest plugins task performs the plugin checks
     * and returns them as a JSON string to stdout.
     */
    fun checkPlugins() {
        if (project.isMavenized) {
            isCompanionPluginAvailable = true
            pluginCheckData = PluginCheckData(
                pitestPluginApplied = true,
                androidPitestPluginApplied = true,
                androidPluginApplied = true,
                androidBuildTypes = listOf()
            )
        } else {
            checkTestFilePresence()
        }
    }

    /**
     * Gets build environment information
     *
     * If the project is mavenized, no actual checks are performed.
     * It is assumed that the build file is a pom.xml and the test directories are src/test/{java/kotlin}.
     *
     * If the project is gradle, the build files and test directories are collected from the project.
     */
    fun getBuildEnvironment() {
        if (project.isMavenized) {
            getBuildEnvironmentMaven()
        } else {
            getBuildEnvironmentGradle()
        }
    }

    fun crashBuild() {
        if (!isCompanionPluginAvailable) {
            throw GradleException("Override plugin is missing!")
        }
    }

    /**
     * Get the error message for the plugin checks
     *
     * @param withHeader If true, the error message will be prefixed with a title
     * @return The error message or null if no errors were found
     *          The message is formatted in HTML and designed to be placed in a <body> tag
     */
    fun getErrorMessage(withHeader: Boolean = true): String? {
        var errorMessage = ""

        if (!isCompanionPluginAvailable) {
            if (errorMessage.isNotEmpty()) errorMessage += "\n"
            errorMessage += ERROR_MESSAGE_COMPANION_PLUGIN_MISSING.format(
                getBuildFileName(),
                getCompanionPluginString()
            )
        }

        if (errorMessage.isNotEmpty()) {
            if (withHeader) {
                return "$ERROR_MESSAGE_TITLE\n\n$errorMessage"
            }
            return errorMessage
        }
        return null
    }

    /**
     * Get the test directories of the project
     *
     * The test directories are collected from the project and its subprojects.
     * On Android they are always empty since the information is not available.
     *
     * @return A list of source directories
     */
    fun getTestDirectories(): List<File> {
        return testDirectories
    }

    /**
     * Get the used android build types for the project
     *
     * The build types are only available for Android projects.
     * @return A list of build types, empty if the project is not an Android project
     */
    fun getBuildTypes(): List<String> {
        return pluginCheckData?.androidBuildTypes ?: emptyList()
    }

    private fun checkTestFilePresence() {
        val projectDir = File(project.basePath ?: "")
        val testFile = File(projectDir, TEST_FILE_NAME)
        isCompanionPluginAvailable = testFile.exists()
    }

    /**
     * Get the build files and test directories for maven projects
     *
     * The build file is assumed to be a pom.xml.
     * No test/source directories are assumed.
     */
    private fun getBuildEnvironmentMaven() {
        val projectDir = File(project.basePath ?: "")
        val pomFile = File(projectDir, "pom.xml")
        buildFiles.add(pomFile)
    }

    /**
     * Get the build files and test directories for gradle projects
     *
     * The build files and test/source directories are collected from the project and its subprojects.
     */
    private fun getBuildEnvironmentGradle() {
        val projectDir = File(project.basePath ?: "")

        GradleConnector.newConnector().forProjectDirectory(projectDir).connect().use { connection ->
            getBuildFiles(connection)
        }
    }

    /**
     * Get the build files and test directories for gradle projects by checking the
     * IdeaProject model for the build files and source/test directories.
     *
     * On Android the source/test directories are always empty since the IdeaProject is not available.
     */
    private fun getBuildFiles(connection: ProjectConnection) {
        try {
            val ideaProject = connection.getModel(IdeaProject::class.java)
            ideaProject.modules.forEach { module ->
                buildFiles.add(module.gradleProject.buildScript.sourceFile)
                module.contentRoots.forEach { contentRoot ->
                    sourceDirectories.addAll(contentRoot.sourceDirectories.map { it.directory })
                    testDirectories.addAll(contentRoot.testDirectories.map { it.directory })
                }
            }
        } catch (e: Exception) {
            logger.warn("Could not get build environment information from gradle", e)
        }
    }

    /**
     * Get the name of the build file
     *
     * @return The name of the build file or "Unknown build file" if no build file was found
     */
    private fun getBuildFileName(): String {
        return buildFiles.firstOrNull()?.name ?: "Unknown build file"
    }

    /**
     * Get the gradle plugin inclusion string for the companion plugin based on the build file
     */
    private fun getCompanionPluginString(): String {
        val buildFile = getBuildFileName()
        return if (buildFile.endsWith(KOTLIN_BUILD_FILE)) {
            KOTLIN_OVERRIDE_PLUGIN
        } else if (buildFile.endsWith(GROOVY_BUILD_FILE)) {
            GROOVY_OVERRIDE_PLUGIN
        } else {
            ""
        }
    }

    /**
     * Get the pitest plugin name based on the build file and the android plugin
     *
     * @return A list containing the build file, the pitest plugin name and the plugin URL
     */
    private fun getPitestPluginName(): List<String> {
        val isAndroid = pluginCheckData?.androidPluginApplied ?: false
        val buildFile = getBuildFileName()

        return if (isAndroid) {
            when (buildFile) {
                KOTLIN_BUILD_FILE -> listOf(buildFile, KOTLIN_PITEST_ANDROID_PLUGIN, PITEST_ANDROID_URL)
                GROOVY_BUILD_FILE -> listOf(buildFile, GROOVY_PITEST_ANDROID_PLUGIN, PITEST_ANDROID_URL)
                else -> listOf(buildFile, PITEST_ANDROID_PLUGIN, PITEST_ANDROID_URL)
            }
        } else {
            when (buildFile) {
                KOTLIN_BUILD_FILE -> listOf(buildFile, KOTLIN_PITEST_PLUGIN, PITEST_URL)
                GROOVY_BUILD_FILE -> listOf(buildFile, GROOVY_PITEST_PLUGIN, PITEST_URL)
                else -> listOf(buildFile, PITEST_PLUGIN, PITEST_URL)
            }
        }
    }

    companion object {
        private const val KOTLIN_BUILD_FILE = "build.gradle.kts"
        private const val GROOVY_BUILD_FILE = "build.gradle"

        private const val PITEST_ANDROID_PLUGIN = "pl.droidsonroids.pitest"
        private const val PITEST_PLUGIN = "info.solidsoft.pitest"
        private const val PITEST_ANDROID_URL = "https://plugins.gradle.org/plugin/pl.droidsonroids.pitest"
        private const val PITEST_URL = "https://plugins.gradle.org/plugin/info.solidsoft.pitest"

        private const val KOTLIN_PITEST_PLUGIN = "id(\"$PITEST_PLUGIN\") version \"x.y.z\""
        private const val KOTLIN_PITEST_ANDROID_PLUGIN = "id(\"$PITEST_ANDROID_PLUGIN\") version \"x.y.z\""
        private const val KOTLIN_OVERRIDE_PLUGIN =
            "id(\"io.github.amos-pitmutationmate.pitmutationmate.override\") version \"1.3\""

        private const val GROOVY_PITEST_PLUGIN = "id '$PITEST_PLUGIN' version 'x.y.z'"
        private const val GROOVY_PITEST_ANDROID_PLUGIN = "id '$PITEST_ANDROID_PLUGIN' version 'x.y.z'"
        private const val GROOVY_OVERRIDE_PLUGIN =
            "id \"io.github.amos-pitmutationmate.pitmutationmate.override\" version \"1.3\""

        const val ERROR_MESSAGE_TITLE = "Plugins for PITMutationPlugin are missing"
        private val ERROR_MESSAGE_PITEST_PLUGIN_MISSING = """<b>The pitest gradle Plugin is missing.</b>
            <p>Please add a Gradle Pitest Plugin line to your %s file:</p>
            <em>%s</em>
            <p>See more here: %s</p><!--anchors done seem to work in the tool window we have created-->
            <p>And see the pitest docs for missing configurations of pitest</p>
        """.trimIndent()
        private val ERROR_MESSAGE_COMPANION_PLUGIN_MISSING = """<b>The Override Companion Plugin is missing.</b>
            <p>Please add the following line to your %s file:</p>
            <em>%s</em>
        """.trimIndent()
    }
}
