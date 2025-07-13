package in.srikanthk.devlabs.kchopdebugger.agent.topic;

import in.srikanthk.devlabs.kchopdebugger.agent.Topic;

public interface DebugRequest {
    Topic<DebugRequest> TOPIC = Topic.createTopic("DebugRequestTopic", DebugRequest.class);

    void publishKarateVariables();
    void stepOver();
    void resume();
    void evaluateExpression(String expression);
}
