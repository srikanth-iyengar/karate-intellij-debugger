package `in`.srikanthk.devlabs.kchopdebugger.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.util.Properties


@State(
    name = "com.intellij.openapi.components.AppSettings",
    storages = [Storage("KarateChopRunProperties.xml")]
)
class KaratePropertiesState: PersistentStateComponent<Properties> {
    private val state = Properties()

    override fun getState(): Properties? {
        return this.state
    }

    override fun loadState(state: Properties) {
        this.state.clear()
        this.state.putAll(state)
    }

    companion object {
        fun getInstance(): KaratePropertiesState? {
            return ApplicationManager.getApplication()
                .getService(KaratePropertiesState::class.java)
        }
    }

}
