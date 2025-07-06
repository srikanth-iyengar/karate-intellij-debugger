package `in`.srikanthk.devlabs.kchopdebugger.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.util.NlsContexts
import `in`.srikanthk.devlabs.kchopdebugger.configuration.KarateDebuggerConfiguration
import `in`.srikanthk.devlabs.kchopdebugger.configuration.MavenState
import org.jetbrains.annotations.Nullable
import java.util.*
import javax.swing.JComponent


/**
 * Provides controller functionality for application settings.
 */
internal class AppSettingsConfigurable : Configurable {
    private var debuggerConfigurationComponent: AppConfigComponent? = null

    @Nullable
    override fun createComponent(): JComponent? {
        debuggerConfigurationComponent = AppConfigComponent()
        return debuggerConfigurationComponent?.panel
    }

    override fun isModified(): Boolean {
        val state: MavenState? =
            Objects.requireNonNull(KarateDebuggerConfiguration.getInstance()?.state)
        return debuggerConfigurationComponent?.mavenPath != state?.mavenPath
    }

    override fun apply() {
        val state: MavenState? =
            Objects.requireNonNull(KarateDebuggerConfiguration.getInstance()?.state)
        state?.setMavenPath(debuggerConfigurationComponent?.mavenPath)
    }

    override fun reset() {
        val state: MavenState? =
            Objects.requireNonNull(KarateDebuggerConfiguration.getInstance()?.state)
        state?.setMavenPath(state.mavenPath)
        debuggerConfigurationComponent?.mavenPath = state?.mavenPath;
    }

    override fun disposeUIResources() {
        debuggerConfigurationComponent = null
    }

    override fun getDisplayName(): @NlsContexts.ConfigurableName String? {
        return "Karate Chop Debugger Preference"
    }
}