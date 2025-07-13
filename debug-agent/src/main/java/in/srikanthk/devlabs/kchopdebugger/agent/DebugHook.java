package in.srikanthk.devlabs.kchopdebugger.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intuit.karate.KarateException;
import com.intuit.karate.RuntimeHook;
import com.intuit.karate.Suite;
import com.intuit.karate.core.ScenarioRuntime;
import com.intuit.karate.core.Step;
import com.intuit.karate.core.StepResult;
import com.intuit.karate.core.Variable;
import in.srikanthk.devlabs.kchopdebugger.agent.topic.DebugRequest;
import in.srikanthk.devlabs.kchopdebugger.agent.topic.DebugResponse;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang3.StringUtils.removeStart;

public class DebugHook implements RuntimeHook {
    private final HashMap<String, TreeSet<Integer>> breakpoints = SessionState.getInstance().getBreakpoints();
    private final AtomicBoolean stopOnNextStep = new AtomicBoolean(false);
    private final DebugResponse responsePublisher = DebugMessageBus.getInstance().publisher(DebugResponse.TOPIC);
    private final DebugMessageBus messageBus = DebugMessageBus.getInstance();
    private final SessionState sessionState = SessionState.getInstance();

    private static final String JAVA_BASE_PATH = "src/test/java";
    private static final String CLASSPATH_COLON = "classpath:";

    @Override
    public void beforeSuite(Suite suite) {
        // publish state to debugger started
        responsePublisher.updateState(DebuggerState.Started);
    }

    @Override
    public boolean beforeStep(Step step, ScenarioRuntime sr) {
        var startLine = step.getLine();
        var endLine = step.getLine();
        var filePath = String.format(
                "%s/%s/%s",
                sessionState.getProjectPath(),
                JAVA_BASE_PATH,
                removeStart(step.getFeature().getResource().getPrefixedPath(), CLASSPATH_COLON)
        );
        var lineSet = breakpoints.computeIfAbsent(filePath, (k) -> new TreeSet<>());

        var shouldHalt =
                lineSet.contains(startLine) || (lineSet.ceiling(startLine) != null && lineSet.ceiling(startLine) <= endLine && lineSet.floor(
                        endLine
                ) != null && lineSet.floor(endLine) >= startLine);

        if (shouldHalt || stopOnNextStep.get()) {
            responsePublisher.navigateTo(filePath, startLine);
            responsePublisher.updateState(DebuggerState.Halted);
            publishKarateVariablesSerializabel(sr.engine.vars);
            stopOnNextStep.set(false);
            CountDownLatch latch = new CountDownLatch(1);

            var listener = new DebugRequest() {
                @Override
                public void publishKarateVariables() {
                    publishKarateVariablesSerializabel(sr.engine.vars);
                }

                @Override
                public void stepOver() {
                    stopOnNextStep.set(true);
                    latch.countDown();
                }

                @Override
                public void resume() {
                    latch.countDown();
                }

                @Override
                public void evaluateExpression(String expression) {
                    try {
                        var response = sr.engine.evalJs(expression);
                        responsePublisher.evaluationResult(response.getAsString(), "");
                    } catch (KarateException exception) {
                        responsePublisher.evaluationResult("", exception.getMessage());
                    } finally {
                        publishKarateVariablesSerializabel(sr.engine.vars);
                        sr.engine.setFailedReason(null);
                    }
                }

                @Override
                public void addBreakpoint(String fileName, Integer lineNumber) {
                    breakpoints.computeIfAbsent(fileName, k -> new TreeSet<>()).add(lineNumber);
                }

                @Override
                public void removeBreakpoint(String fileName, Integer lineNumber) {
                    breakpoints.computeIfAbsent(fileName, k -> new TreeSet<>()).remove(lineNumber);
                }
            };
            messageBus.subscribe(DebugRequest.TOPIC, listener);

            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            responsePublisher.updateState(DebuggerState.Running);
            messageBus.unsubscribe(DebugRequest.TOPIC, listener);
        }

        return RuntimeHook.super.beforeStep(step, sr);
    }

    @Override
    public void afterStep(StepResult result, ScenarioRuntime sr) {
        if (result.getStepLog() != null && !result.getStepLog().isEmpty()) {
            responsePublisher.appendLog(result.getStepLog(), result.isFailed());
        }
    }

    public void publishKarateVariablesSerializabel(Map<String, Variable> vars) {
        HashMap<String, String> mp = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        for (Map.Entry<String, Variable> entry : vars.entrySet()) {
            Map value = Map.of("type", entry.getValue().type, "value", entry.getValue().getAsString());
            try {
                mp.put(entry.getKey(), mapper.writeValueAsString(value));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        responsePublisher.updateKarateVariable(mp);
    }

    @Override
    public void afterSuite(Suite suite) {
        String reportPath = String.format("%s/karate-summary.html", new File(suite.reportDir).toPath().toUri());
        responsePublisher.appendLog(String.format("Karate report: %s", reportPath), true);
        responsePublisher.updateState(DebuggerState.Finished);
    }
}
