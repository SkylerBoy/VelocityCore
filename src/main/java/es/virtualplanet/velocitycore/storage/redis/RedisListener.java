package es.virtualplanet.velocitycore.storage.redis;

import es.virtualplanet.velocitycore.storage.Callback;

public interface RedisListener {
    void addListener(String key, Callback<RedisMessage> callback);
}
