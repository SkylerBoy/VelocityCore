package es.virtualplanet.velocitycore.storage.redis;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RedisMessage {

    private String message, executor, serverFrom, key;

    public RedisMessage(String key, String message, String from, String executor) {
        this.key = key;
        this.message = message;
        this.serverFrom = from;
        this.executor = executor;
    }
}
