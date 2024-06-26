// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023-2024

package com.amos.pitmutationmate.pitmutationmate.execution

import com.amos.pitmutationmate.pitmutationmate.editor.PluginState
import com.amos.pitmutationmate.pitmutationmate.services.MutationResultService
import com.amos.pitmutationmate.pitmutationmate.services.RunArchiveService
import com.amos.pitmutationmate.pitmutationmate.services.UdpMessagingServer
import com.amos.pitmutationmate.pitmutationmate.ui.ToolWindowFactory
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope

class ExecutionDoneProcessListener(val project: Project, private val classFqdns: String?) : ProcessAdapter() {
    private val log: Logger = Logger.getInstance(ExecutionDoneProcessListener::class.java)

    override fun processTerminated(event: ProcessEvent) {
        log.debug("BasePitestExecutor: executeTask: processTerminated: event: $event")
        project.service<UdpMessagingServer>().stopServer() // Stop the UDP server
        if (classFqdns.isNullOrEmpty()) {
            return
        }

        // save and get latest pitest results
        project.service<MutationResultService>().updateLastMutationResult()
        val resultData = project.service<MutationResultService>().getMutationResult()
        // update tool window with latest result data
        ToolWindowFactory.Util.updateReport(project, resultData)
        ToolWindowFactory.Util.updateTree(project)

        // restart code highlighting upon new pitest results
        ApplicationManager.getApplication().runReadAction {
            restartCodeHighlighting()
        }
        // archive the pitest run
        project.service<RunArchiveService>().archiveRun()
        // newest run is also part of the history tab
        project.service<MutationResultService>().updateHistoricMutationResults()
        ToolWindowFactory.Util.updateHistory(project)
    }

    private fun restartCodeHighlighting() {
        PluginState.isAnnotatorEnabled = true
        val analyser = DaemonCodeAnalyzer.getInstance(project)
        val psiFiles = classFqdns!!.split(",").map { getPsiFileFromFQN(project, it) }
        psiFiles.forEach { file ->
            if (file != null) {
                analyser.restart(file)
            }
        }
    }

    private fun getPsiFileFromFQN(project: Project, fqdn: String): PsiFile? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val psiClass = javaPsiFacade.findClass(fqdn, GlobalSearchScope.allScope(project))
        val psiFile = psiClass?.containingFile
        if (psiFile == null) {
            log.debug("Could not find PsiFile for classFQN $classFqdns")
        }
        return psiFile
    }
}
