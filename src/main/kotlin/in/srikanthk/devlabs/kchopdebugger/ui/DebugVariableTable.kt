package `in`.srikanthk.devlabs.kchopdebugger.ui

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.table.JBTable
import com.intuit.karate.KarateException
import com.intuit.karate.core.Variable
import `in`.srikanthk.devlabs.kchopdebugger.agent.DebuggerState
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoRequestTopic
import `in`.srikanthk.devlabs.kchopdebugger.topic.DebuggerInfoResponseTopic
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

class DebugVariableTable(project: Project) : JPanel(BorderLayout()) {
    private val tableModel = DefaultTableModel(arrayOf("Variable", "Type", "Value"), 0);
    private val table = JBTable(tableModel).apply {
        autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
    }
    private val objectMapper = ObjectMapper()
    private val expressionField = JBTextField().apply {
        emptyText.text = "Evaluate Karate JS expression"

    }
    private val inputMap = expressionField.getInputMap(JComponent.WHEN_FOCUSED)
    private val actionMap = expressionField.actionMap
    private val publisher = project.messageBus.syncPublisher(DebuggerInfoRequestTopic.TOPIC)
    private var jsonResultString = ""

    private val resultField = JBTextField().apply {
        isEditable = false
        emptyText.text = "Evaluation result will appear here"
        isEnabled = false
    }

    init {
        val evalPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

            expressionField.emptyText.text = "Evaluate Karate JS expression"
            resultField.emptyText.text = "Result will appear here"
            resultField.isEditable = false
            resultField.border = BorderFactory.createEmptyBorder(3, 3, 3, 3)
            resultField.background = UIManager.getColor("EditorPane.background")
            resultField.foreground = UIManager.getColor("Label.foreground")

            add(expressionField)
            add(Box.createVerticalStrut(4))
            add(resultField)
        }
        add(evalPanel, BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "evaluateExpression")
        actionMap.put("evaluateExpression", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                val expr = expressionField.text
                if (expr.isNotEmpty()) {
                    publisher.evaluateExpression(expr)
                }
            }
        })

        // initiate subscription to the variables
        val messageBus = project.messageBus.connect()

        messageBus.subscribe(DebuggerInfoResponseTopic.TOPIC, object : DebuggerInfoResponseTopic {
            override fun updateKarateVariables(vars: HashMap<String, Map<String, Object>>) {
                WriteCommandAction.runWriteCommandAction(project) {
                    val shouldResize = tableModel.rowCount == 0
                    tableModel.setNumRows(0)
                    vars.entries.forEach {
                        tableModel.addRow(arrayOf(it.key, it.value.get("type"), it.value.get("value")))
                    }

                    if (shouldResize) {
                        table.doLayout()
                    }
                }
            }

            override fun updateState(state: DebuggerState) {
                WriteCommandAction.runWriteCommandAction(project) {
                    if (state == DebuggerState.Finished) {
                        tableModel.setNumRows(0)
                    }
                }
            }

            override fun evaluateExpressionResult(result: String, error: String) {
                if (error.isNotEmpty()) {
                    resultField.text = "[Error] error"
                    resultField.toolTipText = null
                    resultField.cursor = Cursor.getDefaultCursor()
                    return;
                }

                if (result.isNotEmpty()) {
                    val karateVar = result
                    val value = karateVar

                    if (value == null) {
                        resultField.text = "NULL"
                        resultField.toolTipText = null
                        resultField.cursor = Cursor.getDefaultCursor()
                        return
                    }

                    jsonResultString = value
                    resultField.text = if (value.length > 100) value.take(100) + "..." else value
                    resultField.toolTipText = "Click to expand"
                    resultField.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

                } else {
                    resultField.text = "[null]"
                }
            }

            override fun appendLog(log: String, isSuccess: Boolean) {
            }

            override fun navigateTo(filepath: String, lineNumber: Int) {
            }
        })

        resultField.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                showJsonPopup(project, jsonResultString)
            }
        })

    }

    private fun showJsonPopup(project: Project, json: String) {
        val editorFactory = com.intellij.openapi.editor.EditorFactory.getInstance()
        val document = editorFactory.createDocument(json)
        val editor = editorFactory.createViewer(document, project)

        val popup = com.intellij.openapi.ui.popup.JBPopupFactory.getInstance()
            .createComponentPopupBuilder(editor.component, null).setTitle("Evaluated JSON").setResizable(true)
            .setMovable(true).setRequestFocus(true).setFocusable(true)
            .setCancelOnWindowDeactivation(true)
            .setCancelOnClickOutside(true)
            .setDimensionServiceKey(project, "KarateChopDebugger.JsonPopup", false).createPopup()

        popup.showInFocusCenter()
    }

}