// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023

package com.amos.pitmutationmate.pitmutationmate.services

import com.amos.pitmutationmate.pitmutationmate.utils.Utils
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.psi.KtClass
import java.io.File

@Service(Service.Level.PROJECT)
class TestEnvCheckerService(private val project: Project) {

    fun isTestFile(file: File): Boolean {
        val testDirs = project.service<PluginCheckerService>().getTestDirectories()
        val isTest: Boolean

        if (testDirs.isEmpty()) {
            val psiFile = Utils.getPsiFileFromPath(project, file.path)
            val psiClasses = (psiFile as PsiClassOwner).classes
            isTest = psiClasses.any { isPsiTestClass(it) }
        } else {
            isTest = testDirs.any { testDir -> file.path.startsWith(testDir.path) }
        }

        return isTest
    }

    fun isPsiTestClass(psiClass: PsiClass): Boolean {
        if (project.service<PluginCheckerService>().getTestDirectories().isNotEmpty()) {
            val file = getFileFromPsiFile(psiClass.containingFile)
            if (file != null) {
                return isTestFile(file)
            }
        }
        val fqn = psiClass.qualifiedName.toString()
        return fqn.endsWith("Test")
    }

    fun isKtTestClass(ktClass: KtClass): Boolean {
        val testDirs = project.service<PluginCheckerService>().getTestDirectories()
        val isTest = if (testDirs.isNotEmpty()) {
            val file = getFileFromPsiFile(ktClass.containingFile)
            file?.let { isTestFile(it) } ?: false
        } else {
            val fqn = ktClass.fqName.toString()
            fqn.endsWith("Test")
        }
        return isTest
    }

    private fun getFileFromPsiFile(psiFile: PsiFile): File? {
        val file: VirtualFile? = psiFile.virtualFile
        if (file != null && file.isInLocalFileSystem && file.canonicalPath != null) {
            return File(file.canonicalPath!!)
        }
        return null
    }
}
