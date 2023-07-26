package es.virtualplanet.velocitycore.storage.redis;

import lombok.Getter;

@Getter
public class PlayerMessage {

    private final String executor;
    private final String message;

    public PlayerMessage(String executor, String message) {
        this.executor = executor;
        this.message = message;
    }
}
