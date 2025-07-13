package `in`.srikanthk.devlabs.kchopdebugger.ui

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import `in`.srikanthk.devlabs.kchopdebugger.agent.DebuggerState
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoResponseTopic
import java.awt.BorderLayout
import javax.swing.JPanel

class LogViewPanel(val project: Project) : JPanel(BorderLayout()) {
    private val consoleViewPanel = ConsoleViewImpl(project, true)
    private val messageBus = project.messageBus.connect()

    init {
        add(consoleViewPanel.component, BorderLayout.CENTER)
        messageBus.subscribe(DebuggerInfoResponseTopic.TOPIC, object : DebuggerInfoResponseTopic {
            override fun evaluateExpressionResult(result: String, error: String) {
            }

            override fun appendLog(log: String, isSuccess: Boolean) {
                WriteCommandAction.runWriteCommandAction(project) {
                    addLog(log, isSuccess)
                }
            }

            override fun navigateTo(filepath: String, lineNumber: Int) {
            }

            override fun updateKarateVariables(vars: HashMap<String, Map<String, Object>>) {
            }

            override fun updateState(state: DebuggerState) {
                if(state == DebuggerState.Started) {
                    consoleViewPanel.clear()
                }
            }
        })
    }

    fun addLog(message: String, isError: Boolean) {
        val contentType = if (isError) {
            com.intellij.execution.ui.ConsoleViewContentType.ERROR_OUTPUT
        } else {
            com.intellij.execution.ui.ConsoleViewContentType.LOG_INFO_OUTPUT
        }


        consoleViewPanel.print(message + "\n", contentType)

    }
}
