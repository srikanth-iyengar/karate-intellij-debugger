package `in`.srikanthk.devlabs.kchopdebugger.ui

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
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.xdebugger.ui.DebuggerColors
import com.intuit.karate.KarateException
import com.intuit.karate.core.Variable
import `in`.srikanthk.devlabs.kchopdebugger.service.DebuggerState
import `in`.srikanthk.devlabs.kchopdebugger.service.KarateExecutionService
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoRequestTopic
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoResponseTopic
import java.awt.BorderLayout
import java.util.Optional
import javax.swing.JPanel

class ChopDebuggerWindow(private val project: Project) : JPanel(BorderLayout()) {

    private val debugVarsPanel = DebugVariableTable(project)
    private val logViewPanel = LogViewPanel(project)
    private val breakpointsPanel = BreakpointEditorPanel(project)
    private val tabbedPane = JBTabbedPane()
    private val karatePropertiesPanel = PropertiesEditorPanel()
    private var state: DebuggerState = DebuggerState.Finished
    private val publisher: DebuggerInfoRequestTopic? = project.messageBus.syncPublisher(DebuggerInfoRequestTopic.TOPIC)

    private val resumeAction = object : AnAction("Resume", "Resume Execution", AllIcons.Actions.Resume) {
        override fun actionPerformed(e: AnActionEvent) {
            publisher?.resume()
        }
    }

    private val stepOverAction = object : AnAction("Step Over", "Step Over", AllIcons.Actions.TraceInto) {
        override fun actionPerformed(e: AnActionEvent) {
            publisher?.stepForward()
        }
    }

    init {
        // -- Setup IntelliJ-style vertical toolbar
        val actionGroup = DefaultActionGroup(resumeAction, stepOverAction)
        val actionToolbar = ActionManager.getInstance().createActionToolbar(
            "KarateDebuggerToolbar", actionGroup, false
        ).apply {
            setTargetComponent(this@ChopDebuggerWindow)
        }

        val toolbarPanel = NonOpaquePanel(BorderLayout()).apply {
            add(actionToolbar.component, BorderLayout.NORTH)
        }

        // -- Setup tabbed panel
        tabbedPane.apply {
            isOpaque = false
            addTab("Variables", debugVarsPanel)
            addTab("Logs", logViewPanel)
            addTab("Breakpoints", breakpointsPanel)
            addTab("Run Properties", karatePropertiesPanel)
        }

        val centerPanel = NonOpaquePanel(BorderLayout()).apply {
            border = null
            add(tabbedPane, BorderLayout.CENTER)
        }

        // -- Final layout
        add(toolbarPanel, BorderLayout.WEST)
        add(centerPanel, BorderLayout.CENTER)

        // -- Subscribe to debugger updates
        project.messageBus.connect().subscribe(DebuggerInfoResponseTopic.TOPIC, object : DebuggerInfoResponseTopic {
            override fun updateKarateVariables(vars: Map<String, Variable>) {}
            override fun updateState(state: DebuggerState) {
                WriteCommandAction.runWriteCommandAction(project) {
                    updateDebuggerState(state)
                }
            }

            override fun navigateTo(filepath: String, lineNumber: Int) {
                WriteCommandAction.runWriteCommandAction(project) {
                    focusTo(filepath, lineNumber)
                }
            }

            override fun evaluateExpressionResult(result: Optional<Variable>, error: Optional<KarateException>) {
            }

            override fun appendLog(log: String, isSuccess: Boolean) {}
        })
    }

    private fun updateDebuggerState(newState: DebuggerState) {
        state = newState

        if (state == DebuggerState.Finished) {
            KarateExecutionService.BREAKPOINTS.keys.forEach { cleanupMarkups(it) }
        }

        if(state == DebuggerState.Started) {
            ToolWindowManager.getInstance(project).getToolWindow("Karate Chop Debugger")?.show()
            this.tabbedPane.selectedIndex = 0
        }
    }

    private fun cleanupMarkups(filepath: String) {
        val file = LocalFileSystem.getInstance().findFileByPath(filepath)
        file?.let {
            val descriptor = OpenFileDescriptor(project, it, 0, 0)
            FileEditorManager.getInstance(project)
                .openTextEditor(descriptor, true)
                ?.markupModel
                ?.removeAllHighlighters()
        }
    }

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
}
