package in.srikanthk.devlabs.kchopdebugger.agent;

import lombok.Getter;

@Getter
public class Topic<T> {
    private final Class<T> listenerClazz;
    private final String topicName;

    private Topic(String topicName, Class<T> listenerClazz) {
        this.topicName = topicName;
        this.listenerClazz = listenerClazz;
    }

    public static<U> Topic<U> createTopic(String topicName, Class<U> listenerClazz) {
        return new Topic<>(topicName, listenerClazz);
    }
}
