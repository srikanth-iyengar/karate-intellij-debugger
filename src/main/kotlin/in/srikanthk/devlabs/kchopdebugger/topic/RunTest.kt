package `in`.srikanthk.devlabs.kchopdebugger.topic

import com.intellij.util.messages.Topic;

interface RunTest {
    companion object {
        val TOPIC: Topic<RunTest> = Topic.create("Karate Chop Debugger Runner", RunTest::class.java)
    }

    fun executeTest(file: String)
}