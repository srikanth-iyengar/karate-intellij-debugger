package `in`.srikanthk.devlabs.karatedebugger.ui

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import `in`.srikanthk.devlabs.karatedebugger.service.KarateExecutionService


class KarateLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (shouldHighlight(element)) {
            return LineMarkerInfo(
                element,
                element.textRange,
                AllIcons.Debugger.Db_set_breakpoint,
                null,
                null,
                GutterIconRenderer.Alignment.RIGHT,
            )
        }

        return null
    }

    private fun shouldHighlight(element: PsiElement?): Boolean {

        val document = PsiDocumentManager.getInstance(element?.project!!).getDocument(element.containingFile)
        if (document == null) {
            return false
        }

        val lineNumber = document.getLineNumber(element.textOffset) + 1;
        val filepath = element.containingFile.virtualFile.path;
        val karateExecutionService = element.project.getService(KarateExecutionService::class.java)
        return karateExecutionService.isBreakpointPlaced(filepath, lineNumber) && (element.elementType.toString() == "STEP_KEYWORD");
    }
}