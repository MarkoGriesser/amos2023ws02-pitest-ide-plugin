// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023
package com.amos.pitmutationmate.pitmutationmate

import com.amos.pitmutationmate.pitmutationmate.visualization.BarGraph
import com.amos.pitmutationmate.pitmutationmate.visualization.CustomProgressBar
import com.amos.pitmutationmate.pitmutationmate.visualization.LineGraph
import com.amos.pitmutationmate.pitmutationmate.visualization.treetable.JTreeTable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory


internal class MutationTestToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val progressBar = ContentFactory.getInstance().createContent(CustomProgressBar(), "Progressbar", false)
        val table = ContentFactory.getInstance().createContent(JTreeTable(), "Mutationtest Coverage", false)
        val lineChart = ContentFactory.getInstance().createContent(LineGraph(), "Line Chart", false)
        val barChart = ContentFactory.getInstance().createContent(BarGraph(), "Bar Chart", false)

        toolWindow.contentManager.addContent(progressBar)
        toolWindow.contentManager.addContent(table)
        toolWindow.contentManager.addContent(lineChart)
        toolWindow.contentManager.addContent(barChart)

    }
}
