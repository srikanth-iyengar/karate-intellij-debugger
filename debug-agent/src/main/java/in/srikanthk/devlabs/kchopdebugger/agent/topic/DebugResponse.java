package in.srikanthk.devlabs.kchopdebugger.agent.topic;

import in.srikanthk.devlabs.kchopdebugger.agent.DebuggerState;
import in.srikanthk.devlabs.kchopdebugger.agent.Topic;

import java.util.HashMap;

public interface DebugResponse {
    Topic<DebugResponse> TOPIC = Topic.createTopic("DebugResponseTopic", DebugResponse.class);

    void updateKarateVariable(HashMap<String, String> vars);

    void updateState(DebuggerState state);
    void navigateTo(String filePath, Integer lineNumber);
    void appendLog(String log, Boolean isSuccess);
    void evaluationResult(String result, String error);
}
