package in.srikanthk.devlabs.kchopdebugger.agent.communication;

import in.srikanthk.devlabs.kchopdebugger.agent.DebugMessageBus;
import in.srikanthk.devlabs.kchopdebugger.agent.topic.DebugRequest;
import in.srikanthk.devlabs.kchopdebugger.agent.topic.DebugResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Remote;
import java.util.List;
import java.util.stream.Collectors;

public class DebugServer {
    private static final DebugServer INSTANCE = new DebugServer();
    private static final Logger logger = LoggerFactory.getLogger(DebugServer.class);
    @Getter
    private Integer port;
    Socket socket;
    private Thread subscriber;

    DebugServer() {
    }

    public static DebugServer getInstance() {
        return INSTANCE;
    }

    public DebugServer start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(0);
        this.port = serverSocket.getLocalPort();
        Thread serverThread = new Thread(() -> {
            try {
                this.socket = serverSocket.accept();
                startSubscriber();
                registerForwarder();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        serverThread.start();
        return this;
    }

    public void startSubscriber() {
        var publisher = DebugMessageBus.getInstance().publisher(DebugResponse.TOPIC);
        Thread subscriberThread = new Thread(() -> {
            try(ObjectInputStream stream = new ObjectInputStream(socket.getInputStream())) {
                while(!Thread.currentThread().isInterrupted()) {
                    RemoteCall call = (RemoteCall) stream.readObject();
                    Method[] methods = DebugResponse.class.getMethods();
                    for(Method method: methods){
                        if(method.getName().startsWith(call.getMethodName())){
                            logger.info("method: {}", method);
                            method.invoke(publisher, call.getArgs().toArray(new Object[0]));
                            break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException | InvocationTargetException |
                     IllegalAccessException e) {
                logger.error(e.getMessage(), e);
            }
        });
        subscriberThread.start();

        this.subscriber = subscriberThread;
    }

    public void registerForwarder() throws IOException {
        var requestForwarder = new DebugRequest() {
            ObjectOutputStream stream = new ObjectOutputStream(socket.getOutputStream());

            @Override
            public void publishKarateVariables() {
                RemoteCall call = RemoteCall.builder().args(List.of()).methodName("publishKarateVariables").build();
                try {
                    stream.writeObject(call);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void stepOver() {
                RemoteCall call = RemoteCall.builder().args(List.of()).methodName("stepOver").build();
                try {
                    stream.writeObject(call);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void resume() {
                RemoteCall call = RemoteCall.builder().args(List.of()).methodName("resume").build();
                try {
                    stream.writeObject(call);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void evaluateExpression(String expression) {
                RemoteCall call = RemoteCall.builder().args(List.of(expression)).methodName("evaluateExpression").build();
                try {
                    stream.writeObject(call);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void addBreakpoint(String fileName, Integer lineNumber) {
                RemoteCall call = RemoteCall.builder().args(List.of(fileName, lineNumber)).methodName("addBreakpoint").build();
                try {
                    stream.writeObject(call);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void removeBreakpoint(String fileName, Integer lineNumber) {
                RemoteCall call = RemoteCall.builder().args(List.of(fileName, lineNumber)).methodName("removeBreakpoint").build();
                try {
                    stream.writeObject(call);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        DebugMessageBus.getInstance().subscribe(DebugRequest.TOPIC, requestForwarder);
    }

    public void stop() {
        this.subscriber.interrupt();
    }
}
