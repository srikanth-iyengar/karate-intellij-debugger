package `in`.srikanthk.devlabs.karatedebugger.service

import com.intellij.openapi.project.Project
import com.intuit.karate.RuntimeHook
import com.intuit.karate.Suite
import com.intuit.karate.core.ScenarioRuntime
import com.intuit.karate.core.Step
import com.intuit.karate.core.StepResult
import `in`.srikanthk.devlabs.karatedebugger.topic.DebuggerInfoRequestTopic
import `in`.srikanthk.devlabs.karatedebugger.topic.DebuggerInfoResponseTopic
import java.io.File
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CountDownLatch

class DebugHook(private val breakpoints: MutableMap<String, ConcurrentSkipListSet<Int>>, val project: Project) :
    RuntimeHook {

    val publisher: DebuggerInfoResponseTopic? = project.messageBus.syncPublisher(DebuggerInfoResponseTopic.TOPIC);

    @Volatile
    private var stopOnNextStep = false;
    override fun beforeSuite(suite: Suite?) {
        val publisher = project.messageBus.syncPublisher(DebuggerInfoResponseTopic.TOPIC);
        publisher.updateState(DebuggerState.Started)
        super.beforeSuite(suite)
    }

    override fun beforeStep(step: Step?, sr: ScenarioRuntime?): Boolean {
        val startLine = step?.line ?: -1
        val endLine = step?.endLine ?: -1
        val filepath = "${project.basePath}${Constants.JAVA_BASE_PATH}/${
            step?.feature?.resource.toString().removePrefix("classpath:")
        }"

        val lineSet = breakpoints.computeIfAbsent(filepath) { ConcurrentSkipListSet() }

        val shouldHalt =
            lineSet.contains(startLine) || (lineSet.ceiling(startLine) != null && lineSet.ceiling(startLine) <= endLine && lineSet.floor(
                endLine
            ) != null && lineSet.floor(endLine) >= startLine)
        if (shouldHalt || stopOnNextStep) {
            this.stopOnNextStep = false
            val latch = CountDownLatch(1);
            publisher?.updateKarateVariables(sr?.engine!!.vars)
            publisher?.updateState(DebuggerState.Halted)
            publisher?.navigateTo(filepath, startLine)
            project.messageBus.connect().subscribe(DebuggerInfoRequestTopic.TOPIC, object : DebuggerInfoRequestTopic {
                override fun publishKarateVariables() {
                    publisher?.updateKarateVariables(sr?.engine!!.vars)
                }


                override fun stepForward() {
                    stopOnNextStep = true;
                    latch.countDown()
                }

                override fun resume() {
                    latch.countDown()
                }
            })

            latch.await()
        }

        return super.beforeStep(step, sr)
    }

    override fun afterStep(result: StepResult?, sr: ScenarioRuntime?) {
        if (result?.stepLog != null && result.stepLog != "") {
            publisher?.appendLog(result.stepLog, result.isFailed)
        }
        super.afterStep(result, sr)
    }

    override fun afterSuite(suite: Suite?) {
        publisher?.updateState(DebuggerState.Finished)
        publisher?.appendLog("Report dir: ${File(suite?.reportDir).toPath().toUri()}/karate-summary.html", true)
    }
}