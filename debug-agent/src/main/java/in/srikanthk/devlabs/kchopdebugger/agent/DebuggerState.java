package in.srikanthk.devlabs.kchopdebugger.agent;

import java.io.Serializable;

public enum DebuggerState implements Serializable {
    Halted,
    Running,
    Finished,
    Started
}
