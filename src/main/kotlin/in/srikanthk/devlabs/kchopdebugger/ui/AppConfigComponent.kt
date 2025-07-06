package `in`.srikanthk.devlabs.kchopdebugger.ui

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel


/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class AppConfigComponent {
    val panel: JPanel?
    private val mavenHomePath = JBTextField()

    init {
        this.panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Maven home path"), mavenHomePath, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    val preferredFocusedComponent: JComponent
        get() = mavenHomePath

    var mavenPath: String?
        get() = mavenHomePath.getText()
        set(newText) {
            mavenHomePath.setText(newText)
        }
}