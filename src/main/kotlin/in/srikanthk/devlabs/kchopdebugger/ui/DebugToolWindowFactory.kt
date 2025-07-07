package `in`.srikanthk.devlabs.kchopdebugger.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class DebugToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val debuggerWindow = ChopDebuggerWindow(project)
        val content = ContentFactory.getInstance().createContent(debuggerWindow, "Debugger", false)
        toolWindow.contentManager.addContent(content)
    }
}

