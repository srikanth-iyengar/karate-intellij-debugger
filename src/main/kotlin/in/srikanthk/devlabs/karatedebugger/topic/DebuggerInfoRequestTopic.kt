package `in`.srikanthk.devlabs.karatedebugger.topic

import com.intellij.util.messages.Topic

interface DebuggerInfoRequestTopic {
    companion object {
        val TOPIC = Topic.create("Karate Debugger Topic", DebuggerInfoRequestTopic::class.java)
    }

    fun publishKarateVariables()
    fun stepForward()
    fun resume()

}