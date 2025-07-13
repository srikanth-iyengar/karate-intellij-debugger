package `in`.srikanthk.devlabs.kchopdebugger.topic

import com.intellij.util.messages.Topic

interface DebuggerInfoRequestTopic {
    companion object {
        val TOPIC = Topic.create("Karate Chop Debugger Topic", DebuggerInfoRequestTopic::class.java)
    }

    fun publishKarateVariables()
    fun stepForward()
    fun resume()
    fun evaluateExpression(expression: String)
    fun addBreakpoint(fileName: String, lineNumber: Int)
    fun removeBreakpoint(fileName: String, lineNumber: Int)
}