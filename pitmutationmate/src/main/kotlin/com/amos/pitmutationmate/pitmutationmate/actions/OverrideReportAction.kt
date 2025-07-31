package com.amos.pitmutationmate.pitmutationmate.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.amos.pitmutationmate.pitmutationmate.services.ReportPathGeneratorService

class OverrideReportAction : AnAction("Override Report", "Copies specific files from the submodule's report directory to the root report directory", null) {
    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val reportService = project.getService(ReportPathGeneratorService::class.java)
        val submodulePath = "domain" // Replace with the actual submodule path
        reportService.overrideRootReportWithSubmodule(submodulePath)
    }
}