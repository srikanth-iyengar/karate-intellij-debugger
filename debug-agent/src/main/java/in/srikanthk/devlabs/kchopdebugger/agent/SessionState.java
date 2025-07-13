package in.srikanthk.devlabs.kchopdebugger.agent;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.TreeSet;

@Getter
@Setter
public class SessionState {
    @Getter
    private static final SessionState instance = new SessionState();

    private HashMap<String, TreeSet<Integer>> breakpoints = new HashMap<>();
    private String projectPath;
    private String featureClassPath;

    // singleton
    private SessionState() {

    }
}
