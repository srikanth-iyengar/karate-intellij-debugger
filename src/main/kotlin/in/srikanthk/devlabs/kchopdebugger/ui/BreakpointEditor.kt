package `in`.srikanthk.devlabs.kchopdebugger.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.components.JBScrollPane
import `in`.srikanthk.devlabs.kchopdebugger.service.KarateExecutionService
import `in`.srikanthk.devlabs.kchopdebugger.topic.BreakpointUpdatedTopic
import java.awt.BorderLayout
import java.io.File
import java.util.concurrent.ConcurrentSkipListSet
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class BreakpointEditorPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val root = CheckedTreeNode("Breakpoints")
    private val treeModel = DefaultTreeModel(root)
    private val tree = object : CheckboxTree(object : CheckboxTree.CheckboxTreeCellRenderer() {
        override fun customizeRenderer(
            tree: JTree,
            value: Any,
            selected: Boolean,
            expanded: Boolean,
            leaf: Boolean,
            row: Int,
            hasFocus: Boolean
        ) {
            val node = value as? CheckedTreeNode ?: return
            val userObject = node.userObject

            if (userObject is BreakpointEntry) {
                textRenderer.append(userObject.toString())
            } else if (userObject is String) {
                textRenderer.append(userObject)
            }
        }
    }, root) {
        override fun onNodeStateChanged(node: CheckedTreeNode) {
            val userObject = node.userObject as? BreakpointEntry ?: return
            if (userObject.type == EntryType.LINE) {
                handleLineToggle(userObject)
                project.messageBus.syncPublisher(BreakpointUpdatedTopic.TOPIC).updatedBreakpoint()
            }
        }
    }.apply {
        model = treeModel
        isRootVisible = false
        showsRootHandles = true
    }

    init {
        project.messageBus.connect().subscribe(BreakpointUpdatedTopic.TOPIC, object : BreakpointUpdatedTopic {
            override fun updatedBreakpoint() {
                buildTree()
            }
        })
    }


    init {
        buildTree()
        add(JBScrollPane(tree), BorderLayout.CENTER)
    }

    private fun buildTree() {
        root.removeAllChildren()

        val fileTree = mutableMapOf<String, MutableMap<String, MutableList<Int>>>()

        // Build map like: dir -> file -> lines
        KarateExecutionService.BREAKPOINTS.forEach { (filePath, lines) ->
            val file = File(filePath)
            val dir = file.parent ?: return@forEach
            val fileName = file.name

            val files = fileTree.getOrPut(dir) { mutableMapOf() }
            files[fileName] = lines.toMutableList()
        }

        for ((dir, files) in fileTree) {
            val dirNode = CheckedTreeNode(dir)
            for ((fileName, lines) in files) {
                val filePath = "$dir/$fileName"
                val fileNode = CheckedTreeNode(BreakpointEntry(filePath, EntryType.FILE))
                for (line in lines.sorted()) {
                    val lineEntry = BreakpointEntry(filePath, EntryType.LINE, line)
                    val lineNode = CheckedTreeNode(lineEntry)
                    lineNode.isChecked = true
                    fileNode.add(lineNode)
                }
                dirNode.add(fileNode)
            }
            root.add(dirNode)
        }

        treeModel.reload()
        tree.expandRow(0)
    }

    private fun handleLineToggle(entry: BreakpointEntry) {
        val breakpoints = KarateExecutionService.BREAKPOINTS
        if (entry.line != null) {
            val set = breakpoints.getOrPut(entry.path) { ConcurrentSkipListSet() }
            if (set.contains(entry.line)) {
                set.remove(entry.line)
                if (set.isEmpty()) breakpoints.remove(entry.path)
            } else {
                set.add(entry.line)
            }
        }
        expandAll(tree, TreePath(root))
    }

    private fun expandAll(tree: JTree, parent: TreePath) {
        val node = parent.lastPathComponent
        val model = tree.model
        val childCount = model.getChildCount(node)

        for (i in 0 until childCount) {
            val child = model.getChild(node, i)
            val path = parent.pathByAddingChild(child)
            expandAll(tree, path) // Recursive call
        }

        tree.expandPath(parent)
    }

    private data class BreakpointEntry(
        val path: String,
        val type: EntryType,
        val line: Int? = null
    ) {
        override fun toString(): String {
            return when (type) {
                EntryType.FILE -> File(path).name
                EntryType.LINE -> "Line $line"
                else -> path
            }
        }
    }

    private enum class EntryType {
        DIRECTORY, FILE, LINE
    }
}
