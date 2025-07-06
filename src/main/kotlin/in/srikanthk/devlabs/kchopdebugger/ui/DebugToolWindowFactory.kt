package `in`.srikanthk.devlabs.kchopdebugger.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory

class DebugToolWindowFactory: ToolWindowFactory {
    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow
    ) {
        val debuggerWindow = ChopDebuggerWindow(project)

        val content = ContentFactory.getInstance().createContent(debuggerWindow, "", false)
        toolWindow.contentManager.addContent(content)
    }

}


class ChopDebuggerWindow(project: Project): JBTabbedPane() {
    private val debugVarsPanel = DebugVariableTable(project)
    private val logViewPanel = LogViewPanel(project)

    init {
        add("Variables", debugVarsPanel)
        add("Logs", logViewPanel)
    }
}