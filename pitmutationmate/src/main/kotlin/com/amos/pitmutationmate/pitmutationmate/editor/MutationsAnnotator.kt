// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023 Lennart Heimbs

package com.amos.pitmutationmate.pitmutationmate.editor

import com.amos.pitmutationmate.pitmutationmate.reporting.XMLParser
import com.amos.pitmutationmate.pitmutationmate.services.MutationResultService
import com.amos.pitmutationmate.pitmutationmate.utils.PitestSeverity
import com.amos.pitmutationmate.pitmutationmate.visualization.HighlightGutterRenderer
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.ui.UIUtil

class MutationsAnnotator :
    ExternalAnnotator<List<XMLParser.MutationResult>, Map<Int, List<XMLParser.MutationResult>>>() {

    object Util {
        private fun isWhitespace(document: Document, offset: Int, endLineOffset: Int): Boolean {
            return offset < endLineOffset && Character.isWhitespace(document.charsSequence[offset])
        }

        fun getContentOffset(document: Document, lineNr: Int): TextRange {
            val lineStartOffset = document.getLineStartOffset(lineNr)
            val lineEndOffset = document.getLineEndOffset(lineNr)
            var contentStartOffset = lineStartOffset
            // Find the first non-whitespace character to exclude the leading indentation
            while (isWhitespace(document, contentStartOffset, lineEndOffset)) {
                contentStartOffset++
            }
            return TextRange(contentStartOffset, lineEndOffset)
        }

        fun getMessage(mutationResults: List<XMLParser.MutationResult>, isTooltip: Boolean): String {
            // Add icon, link mutation result, and improve formatting
            val separator = if (isTooltip) "<br/>" else ", "
            return mutationResults.mapIndexed { index, elem ->
                val iconHtml = if (isTooltip) {
                    // Use Unicode icons for tooltip, or you could use <img> with a resource path if available
                    when (elem.status) {
                        "KILLED" -> "&#x2714;" // check mark
                        "SURVIVED" -> "&#x26A0;" // warning
                        "TIMED_OUT" -> "&#x23F1;" // clock
                        else -> "&#x25CF;" // bullet
                    }
                } else {
                    // For inline, just use a short symbol
                    when (elem.status) {
                        "KILLED" -> "✔"
                        "SURVIVED" -> "⚠"
                        "TIMED_OUT" -> "⏱"
                        else -> "•"
                    }
                }

                val description = elem.description
                val status = elem.status

                // If possible, link to the killing test (if present and in tooltip mode)
                val killingTestLink = if (isTooltip && elem.killingTest.isNotBlank()) {
                    // Not a real hyperlink, but format as code for now
                    "<br/><b>Killing Test:</b> <code>${elem.killingTest}</code>"
                } else {
                    ""
                }

                // Format: [icon] 1. description → status [killing test]
                if (isTooltip) {
                    """$iconHtml <b>${index + 1}.</b> ${description} <b>→</b> <span style="color:gray;">$status</span>$killingTestLink"""
                } else {
                    "$iconHtml ${index + 1}. $description → $status"
                }
            }.joinToString(separator)
        }

        fun formatTooltipMessage(message: String): String {
            val font = UIUtil.getLabelFont()
            return """
                <html>
                     <body style="font-family: '$font'; font-size: 12px;">
                        <h3>PiTest:</h3>
                        <p>$message</p>
                    </body>
                </html>
            """.trimIndent()
        }
    }

    companion object {
        private val log = Logger.getInstance(MutationsAnnotator::class.java)
    }

    override fun collectInformation(file: PsiFile): List<XMLParser.MutationResult>? {
        if (!PluginState.isAnnotatorEnabled) {
            return null
        }
        log.debug("collectInformation")
        val resultGenerator = file.project.service<MutationResultService>()
        return resultGenerator.getMutationResult()?.mutationResults?.filter { it.sourceFile == file.name }
    }

    override fun doAnnotate(
        annotationResult: List<XMLParser.MutationResult>
    ): Map<Int, List<XMLParser.MutationResult>> {
        log.debug("doAnnotate")

        // map fileResults by lineNumber into a map
        return annotationResult.groupBy { it.lineNumber }
    }

    override fun apply(
        file: PsiFile,
        annotationResult: Map<Int, List<XMLParser.MutationResult>>,
        holder: AnnotationHolder
    ) {
        log.debug("apply")
        val document: Document = PsiDocumentManager.getInstance(file.project).getDocument(file) ?: return

        for (r in annotationResult) {
            val lineIndex = r.key - 1
            if (lineIndex < 0 || lineIndex >= document.lineCount) {
                log.warn("Line number ${r.key} is out of bounds")
                continue
            }
            val lineRange = Util.getContentOffset(document, lineIndex)
            val severity = PitestSeverity.fromMutationResults(r.value)
            holder.newAnnotation(
                severity.highlightSeverity(),
                Util.getMessage(r.value, false)
            ).range(lineRange)
                .tooltip(Util.formatTooltipMessage(Util.getMessage(r.value, true)))
                .highlightType(severity.highlightType())
                .gutterIconRenderer(HighlightGutterRenderer(severity.gutterIcon()))
                .create()
        }
    }
}