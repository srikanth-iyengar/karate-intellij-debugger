package `in`.srikanthk.devlabs.kchopdebugger.topic

import com.intellij.util.messages.Topic
import `in`.srikanthk.devlabs.kchopdebugger.agent.DebuggerState
import java.util.*

interface DebuggerInfoResponseTopic {
    companion object {
        val TOPIC = Topic.create("Karate Chop Debugger Response Topic", DebuggerInfoResponseTopic::class.java)
    }

    fun updateKarateVariables(vars: HashMap<String, Map<String, Object>>)
    fun updateState(state: DebuggerState)
    fun navigateTo(filepath: String, lineNumber: Int)
    fun appendLog(log: String, isSuccess: Boolean)
    fun evaluateExpressionResult(result: String, error: String)
}
