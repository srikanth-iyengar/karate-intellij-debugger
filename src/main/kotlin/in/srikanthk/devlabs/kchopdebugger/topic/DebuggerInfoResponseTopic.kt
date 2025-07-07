package `in`.srikanthk.devlabs.kchopdebugger.topic

import com.intellij.util.messages.Topic
import com.intuit.karate.KarateException
import com.intuit.karate.core.Variable
import com.intuit.karate.template.KarateExpression
import `in`.srikanthk.devlabs.kchopdebugger.service.DebuggerState
import java.util.Optional

interface DebuggerInfoResponseTopic {
    companion object {
        val TOPIC = Topic.create("Karate Chop Debugger Response Topic", DebuggerInfoResponseTopic::class.java)
    }

    fun updateKarateVariables(vars: Map<String, Variable>)
    fun updateState(state: DebuggerState)
    fun navigateTo(filepath: String, lineNumber: Int)
    fun appendLog(log: String, isSuccess: Boolean)
    fun evaluateExpressionResult(result: Optional<Variable>, error: Optional<KarateException>)
}
