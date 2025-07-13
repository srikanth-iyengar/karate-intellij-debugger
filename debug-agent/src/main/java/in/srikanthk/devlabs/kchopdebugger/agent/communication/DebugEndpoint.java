package in.srikanthk.devlabs.kchopdebugger.agent.communication;

import in.srikanthk.devlabs.kchopdebugger.agent.DebugMessageBus;
import in.srikanthk.devlabs.kchopdebugger.agent.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public abstract class DebugEndpoint {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected Socket socket;
    private final List<Thread> handlerThreads = new ArrayList<>();

    protected void setup(Socket socket) throws IOException {
        this.socket = socket;
        registerHandlers();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> handlerThreads.forEach(Thread::interrupt)));
    }

    protected abstract Class<?> getRequestTopicInterface();
    protected abstract<U> Topic<U> getRequestTopic();
    protected abstract Class<?> getResponseTopicInterface();
    protected abstract<U> Topic<U> getResponseTopic();

    protected void registerHandlers() throws IOException {
        registerInputHandler();
        registerOutputProxy();
    }

    @SuppressWarnings("unchecked")
    protected void registerInputHandler() {
        Object publisher = DebugMessageBus.getInstance().publisher(getRequestTopic());
        Thread thread = new Thread(() -> {
            try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
                while (!Thread.currentThread().isInterrupted()) {
                    RemoteCall call = (RemoteCall) ois.readObject();
                    logger.info("Received request for {}", call);
                    Method method = publisher.getClass().getMethod(call.getMethodName(),
                            call.getArgs().stream().map(Object::getClass).toArray(Class[]::new));
                    method.invoke(publisher, call.getArgs().toArray());
                }
            } catch (Exception e) {
                logger.error("Error reading remote call", e);
            }
        }, getClass().getSimpleName() + "-InputHandler");
        thread.start();
        handlerThreads.add(thread);
    }

    @SuppressWarnings("unchecked")
    protected void registerOutputProxy() throws IOException {
        ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
        Object proxy = Proxy.newProxyInstance(
                getResponseTopicInterface().getClassLoader(),
                new Class[]{getResponseTopicInterface()},
                (proxyInstance, method, args) -> {
                    logger.info("Received request for {} {} {}", method, proxyInstance, args);
                    RemoteCall call = RemoteCall.builder()
                            .methodName(method.getName())
                            .args(args != null ? List.of(args) : List.of())
                            .build();
                    try {
                        oos.writeObject(call);
                        oos.flush();
                    } catch (IOException e) {
                        logger.error("Error writing remote call", e);
                        throw new RuntimeException("Failed to send remote call: " + method.getName(), e);
                    }
                    return null;
                }
        );
        DebugMessageBus.getInstance().subscribe(getResponseTopic(), proxy);
    }

    public void stop() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        handlerThreads.forEach(Thread::interrupt);
    }
}
