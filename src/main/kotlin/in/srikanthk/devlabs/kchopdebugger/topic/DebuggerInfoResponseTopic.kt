package `in`.srikanthk.devlabs.kchopdebugger.topic

import com.intellij.util.messages.Topic
import com.intuit.karate.core.Variable
import `in`.srikanthk.devlabs.kchopdebugger.service.DebuggerState

interface DebuggerInfoResponseTopic {
    companion object {
        val TOPIC = Topic.create("Karate Chop Debugger Response Topic", DebuggerInfoResponseTopic::class.java)
    }

    fun updateKarateVariables(vars: Map<String, Variable>)

    fun updateState(state: DebuggerState)

    fun navigateTo(filepath: String, lineNumber: Int)

    fun appendLog(log: String, isSuccess: Boolean)
}
