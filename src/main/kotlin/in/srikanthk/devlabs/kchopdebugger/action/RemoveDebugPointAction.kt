package `in`.srikanthk.devlabs.kchopdebugger.action

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileDocumentManager
import `in`.srikanthk.devlabs.kchopdebugger.service.KarateExecutionService


open class RemoveDebugPointAction : AnAction() {
    override fun actionPerformed(action: AnActionEvent) {
        val editor = action.getData(CommonDataKeys.EDITOR)
        val psiFile = action.getData(CommonDataKeys.PSI_FILE)
        val karateExecutionService = action.project?.getService(KarateExecutionService::class.java)

        if (editor == null || psiFile == null) {
            return
        }

        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)


        // Get current line number (0-based)
        val lineNumber = editor.caretModel.logicalPosition.line + 1

        karateExecutionService?.removeBreakpoint(virtualFile?.path!!, lineNumber);
        DaemonCodeAnalyzer.getInstance(action.project).restart(psiFile)
    }
}