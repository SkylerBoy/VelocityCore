package es.virtualplanet.velocitycore.storage.redis;

import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.storage.Callback;
import lombok.Getter;
import me.fixeddev.fixedredis.messenger.Messenger;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;

@Getter
public class RedisListenerImpl implements RedisListener {

    private final HashMap<String, Callback<RedisMessage>> callbacks = new HashMap<>();
    private final RedisManager redisService;

    private final VelocityCorePlugin plugin;
    private final ExecutorService executorService;

    private final Messenger messenger;

    public RedisListenerImpl(RedisManager redisService, ExecutorService executorService, VelocityCorePlugin plugin) {
        this.plugin = plugin;
        this.redisService = redisService;
        this.executorService = executorService;
        this.messenger = redisService.getRedis().messenger();
    }

    @Override
    public void addListener(String channel, Callback<RedisMessage> callback) {
        messenger.getChannel("survival-" + channel, PlayerMessage.class).addListener((channelInstance, sender, playerMessage) ->
                callback.call(new RedisMessage(channelInstance.name(), playerMessage.getMessage(), sender, playerMessage.getExecutor())));
    }
}
