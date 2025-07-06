package `in`.srikanthk.devlabs.karatedebugger.ui

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intuit.karate.core.Variable
import `in`.srikanthk.devlabs.karatedebugger.service.DebuggerState
import `in`.srikanthk.devlabs.karatedebugger.topic.DebuggerInfoResponseTopic
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.table.DefaultTableModel

class DebugVariableTable(project: Project) : JPanel(BorderLayout()) {
    private val tableModel = DefaultTableModel(arrayOf("Variable", "Type", "Value"), 0);
    private val table = JBTable(tableModel)
    private val toolbar = DebugToolbarPanel(project)
    private val objectMapper = ObjectMapper()


    init {
        add(toolbar, BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)

        // initiate subscription to the variables
        val messageBus = project.messageBus.connect()

        messageBus.subscribe(DebuggerInfoResponseTopic.TOPIC, object : DebuggerInfoResponseTopic {
            override fun updateKarateVariables(vars: Map<String, Variable>) {
                tableModel.setNumRows(0)
                vars.entries.forEach {
                    val value =
                        if (it.value.type == Variable.Type.MAP) objectMapper.writeValueAsString(it.value.getValue()) else it.value.getValue<Object>()
                            .toString()
                    tableModel.addRow(arrayOf(it.key, it.value.type, value))
                }
            }

            override fun updateState(state: DebuggerState) {
                if (state == DebuggerState.Finished) {
                    tableModel.setNumRows(0)
                }
            }

            override fun navigateTo(filepath: String, lineNumber: Int) {
            }

            override fun appendLog(log: String) {
            }
        })

    }
}