package `in`.srikanthk.devlabs.kchopdebugger.ui

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.xdebugger.ui.DebuggerColors
import com.intuit.karate.core.Variable
import `in`.srikanthk.devlabs.kchopdebugger.service.DebuggerState
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoResponseTopic
import java.awt.BorderLayout
import javax.swing.JPanel

class DebugToolbarPanel(val project: Project) : JPanel(BorderLayout()) {


    init {

        project.messageBus.connect().subscribe(DebuggerInfoResponseTopic.TOPIC, object : DebuggerInfoResponseTopic {
            override fun updateKarateVariables(vars: Map<String, Variable>) {
            }

            override fun updateState(state: DebuggerState) {
            }

            override fun navigateTo(filepath: String, lineNumber: Int) {
                WriteCommandAction.runWriteCommandAction(project) {
//                    focusTo(filepath, lineNumber)
                }
            }

            override fun appendLog(log: String, isSuccess: Boolean) {
            }
        })
    }


}
