package com.amos.pitmutationmate.pitmutationmate.listeners

import com.intellij.openapi.project.Project
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.io.File
import kotlin.io.path.createTempDirectory

class GradleSyncDeleteCompanionFileListenerTest {

    private lateinit var project: Project
    private lateinit var listener: GradleSyncDeleteCompanionFileListener

    @BeforeEach
    fun setUp() {
        project = Mockito.mock(Project::class.java)
        listener = GradleSyncDeleteCompanionFileListener()
    }

    @Test
    fun `should delete COMPANION_IS_PRESENT marker file when override plugin is not present in build_gradle_kts`() {
        // Setup temp project directory using createTempDirectory (Path API)
        val tempDirPath = createTempDirectory("pitmutationmate-test")
        val tempDir = tempDirPath.toFile()
        val buildGradleKts = File(tempDir, "build.gradle.kts")
        val markerFile = File(tempDir, "COMPANION_IS_PRESENT")

        // Write build.gradle.kts without the override plugin
        buildGradleKts.writeText(
            """
            plugins {
                kotlin("jvm") version "1.8.0"
            }
            """.trimIndent()
        )
        // Create the marker file
        markerFile.writeText("present")
        Assertions.assertTrue(markerFile.exists())

        // Mock Project to return tempDir as basePath
        Mockito.`when`(project.basePath).thenReturn(tempDir.absolutePath)

        // Use reflection to invoke the private method
        val method = GradleSyncDeleteCompanionFileListener::class.java
            .getDeclaredMethod("checkAndDeleteCompanionFile", Project::class.java)
        method.isAccessible = true
        method.invoke(listener, project)

        // Assert marker file is deleted
        Assertions.assertFalse(markerFile.exists())

        // Cleanup
        buildGradleKts.delete()
        markerFile.delete()
        tempDir.deleteRecursively()
    }

    @Test
    fun `should NOT delete COMPANION_IS_PRESENT marker file when override plugin IS present in build_gradle_kts`() {
        // Setup temp project directory using createTempDirectory (Path API)
        val tempDirPath = createTempDirectory("pitmutationmate-test")
        val tempDir = tempDirPath.toFile()
        val buildGradleKts = File(tempDir, "build.gradle.kts")
        val markerFile = File(tempDir, "COMPANION_IS_PRESENT")

        // Write build.gradle.kts WITH the override plugin
        buildGradleKts.writeText(
            """
            plugins {
                kotlin("jvm") version "1.8.0"
                id("io.github.amos-pitmutationmate.pitmutationmate.override")
            }
            """.trimIndent()
        )
        // Create the marker file
        markerFile.writeText("present")
        Assertions.assertTrue(markerFile.exists())

        // Mock Project to return tempDir as basePath
        Mockito.`when`(project.basePath).thenReturn(tempDir.absolutePath)

        // Use reflection to invoke the private method
        val method = GradleSyncDeleteCompanionFileListener::class.java
            .getDeclaredMethod("checkAndDeleteCompanionFile", Project::class.java)
        method.isAccessible = true
        method.invoke(listener, project)

        // Assert marker file is NOT deleted
        Assertions.assertTrue(markerFile.exists())

        // Cleanup
        buildGradleKts.delete()
        markerFile.delete()
        tempDir.deleteRecursively()
    }

    @Test
    fun `should do nothing if marker file does not exist`() {
        val tempDirPath = createTempDirectory("pitmutationmate-test")
        val tempDir = tempDirPath.toFile()
        val buildGradleKts = File(tempDir, "build.gradle.kts")
        buildGradleKts.writeText(
            """
        plugins {
            kotlin("jvm") version "1.8.0"
        }
        """.trimIndent()
        )
        // No marker file created
        Mockito.`when`(project.basePath).thenReturn(tempDir.absolutePath)
        val method = GradleSyncDeleteCompanionFileListener::class.java
            .getDeclaredMethod("checkAndDeleteCompanionFile", Project::class.java)
        method.isAccessible = true
        method.invoke(listener, project)
        // Assert marker file still does not exist
        Assertions.assertFalse(File(tempDir, "COMPANION_IS_PRESENT").exists())
        buildGradleKts.delete()
        tempDir.deleteRecursively()
    }

    @Test
    fun `should be idempotent when called multiple times`() {
        val tempDirPath = createTempDirectory("pitmutationmate-test")
        val tempDir = tempDirPath.toFile()
        val buildGradleKts = File(tempDir, "build.gradle.kts")
        val markerFile = File(tempDir, "COMPANION_IS_PRESENT")

        // Write build.gradle.kts without the override plugin
        buildGradleKts.writeText(
            """
        plugins {
            kotlin("jvm") version "1.8.0"
        }
        """.trimIndent()
        )
        // Create the marker file
        markerFile.writeText("present")
        Assertions.assertTrue(markerFile.exists())

        // Mock Project to return tempDir as basePath
        Mockito.`when`(project.basePath).thenReturn(tempDir.absolutePath)

        // Use reflection to invoke the private method multiple times
        val method = GradleSyncDeleteCompanionFileListener::class.java
            .getDeclaredMethod("checkAndDeleteCompanionFile", Project::class.java)
        method.isAccessible = true

        // First call: should delete the marker file
        method.invoke(listener, project)
        Assertions.assertFalse(markerFile.exists())

        // Second call: should do nothing, but not throw
        method.invoke(listener, project)
        Assertions.assertFalse(markerFile.exists())

        // Cleanup
        buildGradleKts.delete()
        markerFile.delete()
        tempDir.deleteRecursively()
    }

    @Test
    fun `should delete COMPANION_IS_PRESENT marker file when override plugin is only present in a comment`() {
        val tempDirPath = createTempDirectory("pitmutationmate-test")
        val tempDir = tempDirPath.toFile()
        val buildGradleKts = File(tempDir, "build.gradle.kts")
        val markerFile = File(tempDir, "COMPANION_IS_PRESENT")

        // Write build.gradle.kts with the plugin id only in a comment
        buildGradleKts.writeText(
            """
            plugins {
                kotlin("jvm") version "1.8.0"
                // id("io.github.amos-pitmutationmate.pitmutationmate.override")
            }
            // id("io.github.amos-pitmutationmate.pitmutationmate.override") in a comment
            /*
                id("io.github.amos-pitmutationmate.pitmutationmate.override")
            */
            """.trimIndent()
        )
        // Create the marker file
        markerFile.writeText("present")
        Assertions.assertTrue(markerFile.exists())

        // Mock Project to return tempDir as basePath
        Mockito.`when`(project.basePath).thenReturn(tempDir.absolutePath)

        // Use reflection to invoke the private method
        val method = GradleSyncDeleteCompanionFileListener::class.java
            .getDeclaredMethod("checkAndDeleteCompanionFile", Project::class.java)
        method.isAccessible = true
        method.invoke(listener, project)

        // Assert marker file is deleted
        Assertions.assertFalse(markerFile.exists())

        // Cleanup
        buildGradleKts.delete()
        markerFile.delete()
        tempDir.deleteRecursively()
    }
}