package `in`.srikanthk.devlabs.kchopdebugger.topic

import com.intellij.util.messages.Topic

interface BreakpointUpdatedTopic {
    companion object {
        val TOPIC = Topic.create("Breakpoint Updated Topic", BreakpointUpdatedTopic::class.java)
    }

    fun updatedBreakpoint()
}