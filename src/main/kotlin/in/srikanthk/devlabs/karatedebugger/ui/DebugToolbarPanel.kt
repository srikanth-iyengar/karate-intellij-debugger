package `in`.srikanthk.devlabs.karatedebugger.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBTextField
import com.intellij.xdebugger.ui.DebuggerColors
import com.intuit.karate.core.Variable
import `in`.srikanthk.devlabs.karatedebugger.service.DebuggerState
import `in`.srikanthk.devlabs.karatedebugger.service.KarateExecutionService
import `in`.srikanthk.devlabs.karatedebugger.topic.DebuggerInfoRequestTopic
import `in`.srikanthk.devlabs.karatedebugger.topic.DebuggerInfoResponseTopic
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JPanel

class DebugToolbarPanel(val project: Project) : JPanel(BorderLayout()) {

    val publisher = project.messageBus.syncPublisher(DebuggerInfoRequestTopic.TOPIC)
    var state = DebuggerState.Finished

    init {

        project.messageBus.connect().subscribe(DebuggerInfoResponseTopic.TOPIC, object : DebuggerInfoResponseTopic {
            override fun updateKarateVariables(vars: Map<String, Variable>) {
            }

            override fun updateState(state: DebuggerState) {
                updateDebuggerState(state)
            }

            override fun navigateTo(filepath: String, lineNumber: Int) {
                WriteCommandAction.runWriteCommandAction(project) {
                    focusTo(filepath, lineNumber)
                }
            }

            override fun appendLog(log: String, isSuccess: Boolean) {
            }
        })
    }

    private fun updateDebuggerState(state: DebuggerState) {
        this.state = state

        this.resumeAction.apply {
            setEnabled(state == DebuggerState.Halted)
        }
        this.stepOverAction.apply {
            setEnabled(state == DebuggerState.Halted)
        }

        if (state == DebuggerState.Finished) {
            KarateExecutionService.BREAKPOINTS.keys.forEach { file -> cleanupMarkups(file) }
        }
    }

    private val resumeAction = object : AnAction("Resume", null, AllIcons.Actions.Resume) {
        override fun actionPerformed(e: AnActionEvent) {
            publisher.resume()
        }
    }

    private val stepOverAction = object : AnAction("Step Over", null, AllIcons.Debugger.ForceStepOver) {
        override fun actionPerformed(e: AnActionEvent) {
            publisher.stepForward()
        }
    }

    private val expressionField = JBTextField(30)

    init {
        val actionGroup = DefaultActionGroup().apply {
            add(resumeAction)
            add(stepOverAction)
        }

        val toolbar = ActionManager.getInstance().createActionToolbar("DebugToolbar", actionGroup, true)
        toolbar.targetComponent = this

        val evalPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(toolbar.component)
            add(expressionField)
        }

        add(evalPanel, BorderLayout.CENTER)
    }

    fun getExpression(): String = expressionField.text

    private fun focusTo(filePath: String, lineNumber: Int) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filePath)

        if (virtualFile != null) {
            val descriptor = OpenFileDescriptor(project, virtualFile, lineNumber - 1, 0)
            val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true) ?: return
            val markupModel = editor.markupModel

            // Get start and end offset of the line
            val startOffset = editor.document.getLineStartOffset(lineNumber - 1)
            val endOffset = editor.document.getLineEndOffset(lineNumber - 1)

            val attributes =
                EditorColorsManager.getInstance().globalScheme.getAttributes(DebuggerColors.EXECUTIONPOINT_ATTRIBUTES)

            markupModel.removeAllHighlighters()
            // Add highlighter to the line
            markupModel.addRangeHighlighter(
                startOffset,
                endOffset,
                HighlighterLayer.SELECTION - 1,
                attributes,
                HighlighterTargetArea.LINES_IN_RANGE
            )

            descriptor.navigate(true)
        }
    }

    private fun cleanupMarkups(filepath: String) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(filepath)

        if (virtualFile != null) {
            val descriptor = OpenFileDescriptor(project, virtualFile, 0, 0)
            val editor = FileEditorManager.getInstance(project).openTextEditor(descriptor, true) ?: return
            val markupModel = editor.markupModel
            markupModel.removeAllHighlighters()
        }
    }
}
