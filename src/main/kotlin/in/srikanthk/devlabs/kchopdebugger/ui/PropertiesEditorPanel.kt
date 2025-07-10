package `in`.srikanthk.devlabs.kchopdebugger.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import `in`.srikanthk.devlabs.kchopdebugger.configuration.KaratePropertiesState
import java.awt.BorderLayout
import java.awt.datatransfer.DataFlavor
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.table.AbstractTableModel

data class PropertyEntry(var name: String, var value: String)

class PropertiesEditorPanel : JPanel(BorderLayout()) {

    private val tableModel = PropertyTableModel()
    private val table = JBTable(tableModel).apply {
        autoResizeMode = JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS
        rowHeight = 24
    }

    init {
        loadProperties()

        // this doesn't work
        val saveKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)
        table.inputMap.put(saveKeyStroke, "saveProperties")
        table.actionMap.put("saveProperties", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                saveProperties()
            }
        })

        val panel = ToolbarDecorator.createDecorator(table)
            .setAddAction {
                tableModel.addRow(PropertyEntry("", ""))
            }
            .setRemoveAction {
                val row = table.selectedRow
                if (row != -1) {
                    tableModel.removeRow(row)
                }
            }
            .addExtraAction(object : ToolbarDecorator.ElementActionButton("Paste", null, com.intellij.icons.AllIcons.Actions.MenuPaste) {
                override fun actionPerformed(p0: AnActionEvent) {
                    val clipboardText = CopyPasteManager.getInstance()
                        .getContents<String>(DataFlavor.stringFlavor) ?: return
                    clipboardText.lines().forEach { line ->
                        val parts = line.split("\t", limit = 2)
                        if (parts.size == 2) {
                            tableModel.addRow(PropertyEntry(parts[0].trim(), parts[1].trim()))
                        }
                    }
                }
            })
            .addExtraAction(object : ToolbarDecorator.ElementActionButton("Save", null, com.intellij.icons.AllIcons.Actions.MenuSaveall) {
                override fun actionPerformed(p0: AnActionEvent) {
                    saveProperties()
                }
            })
            .createPanel()

        add(JBScrollPane(panel), BorderLayout.CENTER)
    }

    private fun loadProperties() {
        val state = KaratePropertiesState.getInstance()?.state ?: return
        val entries = state.entries.map { PropertyEntry(it.key.toString(), it.value.toString()) }
        tableModel.setData(entries)
    }

    private fun saveProperties() {
        val state = KaratePropertiesState.getInstance()?.state ?: return
        state.clear()
        tableModel.entries.forEach {
            if (it.name.isNotBlank()) {
                state.setProperty(it.name.trim(), it.value.trim())
            }
        }
    }

    private class PropertyTableModel : AbstractTableModel() {
        val entries: MutableList<PropertyEntry> = mutableListOf()

        fun setData(data: List<PropertyEntry>) {
            entries.clear()
            entries.addAll(data)
            fireTableDataChanged()
        }

        fun addRow(entry: PropertyEntry) {
            entries.add(entry)
            fireTableRowsInserted(entries.size - 1, entries.size - 1)
        }

        fun removeRow(index: Int) {
            if (index in entries.indices) {
                entries.removeAt(index)
                fireTableRowsDeleted(index, index)
            }
        }

        override fun getRowCount(): Int = entries.size
        override fun getColumnCount(): Int = 2
        override fun getColumnName(col: Int): String = if (col == 0) "Name" else "Value"
        override fun getValueAt(row: Int, col: Int): Any =
            if (col == 0) entries[row].name else entries[row].value

        override fun isCellEditable(row: Int, col: Int): Boolean = true

        override fun setValueAt(aValue: Any?, row: Int, col: Int) {
            val value = StringUtil.notNullize(aValue?.toString())
            if (row in entries.indices) {
                if (col == 0) entries[row].name = value else entries[row].value = value
                fireTableCellUpdated(row, col)
            }
        }
    }
}
