package in.srikanthk.devlabs.kchopdebugger.agent;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;

public class DebugMessageBus {
    private final static Logger logger = LoggerFactory.getLogger(DebugMessageBus.class);

    @Getter
    private static final DebugMessageBus instance = new DebugMessageBus();
    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1,
            1,
            30,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );
    private final Map<Topic<?>, Set<Object>> subscribers = new ConcurrentHashMap<>();

    private DebugMessageBus() {
    }

    public <T> void subscribe(Topic<T> topic, T listener) {
        subscribers.computeIfAbsent(topic, k ->
                Collections.newSetFromMap(new ConcurrentHashMap<>())
        ).add(listener);
    }

    public <T> void unsubscribe(Topic<?> topic, T listener) {
        subscribers.computeIfAbsent(topic, k ->
                Collections.newSetFromMap(new ConcurrentHashMap<>())
        ).remove(listener);
    }

    @SuppressWarnings("unchecked")
    public <T> T publisher(Topic<T> topic) {
        return (T) Proxy.newProxyInstance(
                topic.getListenerClazz().getClassLoader(),
                new Class[]{topic.getListenerClazz()},
                (proxy, method, args) -> {
                    Set<Object> listeners = subscribers.getOrDefault(topic, Collections.emptySet());
                    for (Object listener : listeners) {
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                return method.invoke(listener, args);
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                logger.error(e.getMessage(), e);
                            }
                            return null;
                        }, executor);
                    }
                    return null;
                }
        );
    }

    public <T> T publisherByTopicName(String topicName) {
        Set<Topic<?>> topics = this.subscribers.keySet();
        return (T) topics.stream()
                .filter(topic -> topic.getTopicName().equals(topicName))
                .findFirst().get();
    }

    public void clearAll() {
        this.subscribers.clear();
    }
}
