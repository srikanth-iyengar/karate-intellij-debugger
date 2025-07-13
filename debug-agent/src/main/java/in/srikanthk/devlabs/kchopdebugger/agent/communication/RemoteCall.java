package in.srikanthk.devlabs.kchopdebugger.agent.communication;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Builder
@Getter
@Setter
public class RemoteCall implements Serializable {
    private String methodName;
    private List<Object> args;
    private String topicName;
}
