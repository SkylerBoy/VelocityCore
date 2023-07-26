package es.virtualplanet.velocitycore.storage.redis;

import me.fixeddev.fixedredis.messenger.Channel;

public interface Messager {

    void sendMessage(String key, String message, String executor);
    <T> Channel<T> getChannel(String name, Class<T> type);
}
