// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023 Lennart Heimbs

package com.amos.pitmutationmate.pitmutationmate.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * A service that generates the path to the report directory.
 */
@Service(Service.Level.PROJECT)
class ReportPathGeneratorService(private val project: Project) {
    private var buildType: String? = null

    /**
     * Returns the path to the report base path.
     * @return the path to the report base path
     */
    private fun getBasePath(): Path {
        val projectBasePath = project.basePath ?: ""
        if (projectBasePath.isEmpty()) {
            log.warn("Project base path is empty, using current directory as base path")
        }
        return Path.of(projectBasePath)
    }

    /**
     * Returns the path to the report directory.
     * @return the path to the report directory
     */
    fun getReportPath(): Path {
        val projectBasePath = getBasePath()
        return Path.of("$projectBasePath/build/reports/pitest/pitmutationmate")
    }

    /**
     * Returns the path to the report archive.
     * @return the path to the report archive
     */
    fun getArchivePath(): Path {
        val projectBasePath = getBasePath()
        return Path.of("$projectBasePath/.history")
    }

    /**
     * Checks if either the given build type or the debug build type exists.
     * @return the path to the report directory with the build type or without
     */
    private fun checkForDebugBuiltType(path: Path): Path {
        if (!buildType.isNullOrEmpty() && Files.exists(Path.of("$path/$buildType"))) {
            return Path.of("$path/$buildType")
        } else if (Files.exists(Path.of("$path/debug"))) {
            return Path.of("$path/debug")
        }
        return path
    }

    /**
     * Returns the path to mutations.xml.
     * @return the path to the report file
     */
    fun getReportMutationsFile(path: Path = getReportPath()): Path {
        val fullPath = checkForDebugBuiltType(path)
        return Path.of("$fullPath/mutations.xml")
    }

    /**
     * Returns the path to coverage.xml.
     * @return the path to the report file
     */
    fun getReportCoverageFile(path: Path = getReportPath()): Path {
        val fullPath = checkForDebugBuiltType(path)
        return Path.of("$fullPath/coverageInformation.xml")
    }

    fun setBuildType(buildType: String?) {
        this.buildType = buildType
    }

    /**
     * Copies only specific files and directories from the submodule's report directory to the root report directory,
     * overwriting any existing files in the root report directory.
     * Only the following are copied: arcmutate-licences (dir), linecoverage.xml, mutations.xml, simplified.json, summary.md
     */
    fun overrideRootReportWithSubmodule(submodulePath: String) {
        val projectBasePath = project.basePath ?: return
        val submoduleReportDir = Path.of(projectBasePath, submodulePath, "build", "reports", "pitest", "debug")
        print(submoduleReportDir)
        val rootReportDir = Path.of(projectBasePath, "build", "reports", "pitest", "pitmutationmate", "debug")
        print(rootReportDir)
        if (!Files.exists(submoduleReportDir)) {
            log.warn("Submodule report directory does not exist: $submoduleReportDir")
            return
        }
        if (!Files.exists(rootReportDir)) {
            Files.createDirectories(rootReportDir)
        }

        val allowedNames = setOf(
            "arcmutate-licences",
            "linecoverage.xml",
            "mutations.xml",
            "simplified.json",
            "summary.md"
        )

        Files.list(submoduleReportDir).use { files ->
            files.forEach { file ->
                val name = file.fileName.toString()
                if (allowedNames.contains(name)) {
                    val target = rootReportDir.resolve(name)
                    if (Files.isDirectory(file)) {
                        // Recursively copy directory
                        copyDirectoryRecursively(file, target)
                        log.info("Copied directory $file to $target")
                    } else {
                        Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING)
                        log.info("Copied file $file to $target")
                    }
                }
            }
        }
    }

    /**
     * Recursively copies a directory from source to target.
     */
    private fun copyDirectoryRecursively(source: Path, target: Path) {
        if (!Files.exists(target)) {
            Files.createDirectories(target)
        }
        Files.walk(source).use { stream ->
            stream.forEach { src ->
                val dest = target.resolve(source.relativize(src))
                if (Files.isDirectory(src)) {
                    if (!Files.exists(dest)) {
                        Files.createDirectories(dest)
                    }
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    companion object {
        private val log: Logger = Logger.getInstance(ReportPathGeneratorService::class.java)
    }
}
