package `in`.srikanthk.devlabs.kchopdebugger.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage


@State(
    name = "com.intellij.openapi.components.AppSettings",
    storages = [Storage("KarateChopDebuggerSettings.xml")]
)
class KarateDebuggerConfiguration : PersistentStateComponent<MavenState> {
    private val state = MavenState()

    override fun getState(): MavenState? {
        return this.state
    }

    override fun loadState(state: MavenState) {
        this.state.setMavenPath(state.mavenPath)
    }


    companion object {
        fun getInstance(): KarateDebuggerConfiguration? {
            return ApplicationManager.getApplication()
                .getService(KarateDebuggerConfiguration::class.java)
        }
    }

}


class MavenState {
    private var mavenHome = ""

    val mavenPath get() = mavenHome
    fun setMavenPath(path: String?) {
        if (path != null) {
            this.mavenHome = path
        }
    }
}