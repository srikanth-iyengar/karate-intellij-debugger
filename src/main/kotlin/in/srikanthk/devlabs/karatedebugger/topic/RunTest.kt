package `in`.srikanthk.devlabs.karatedebugger.topic

import com.intellij.util.messages.Topic;

interface RunTest {
    companion object {
        val TOPIC: Topic<RunTest> = Topic.create("Karate Debugger Runner", RunTest::class.java)
    }

    fun executeTest(file: String)
}