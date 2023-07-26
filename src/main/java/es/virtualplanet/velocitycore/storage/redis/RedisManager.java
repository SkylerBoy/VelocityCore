package es.virtualplanet.velocitycore.storage.redis;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.common.Utils;
import lombok.Getter;
import me.fixeddev.fixedredis.JedisBuilder;
import me.fixeddev.fixedredis.Redis;
import me.fixeddev.fixedredis.messenger.Channel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@Getter
public class RedisManager implements Messager {

    private final VelocityCorePlugin plugin;

    private Redis redis;
    private final ExecutorService executorService;

    public RedisManager(VelocityCorePlugin plugin, ExecutorService executorService) {
        this.plugin = plugin;
        this.executorService = executorService;

        plugin.getLogger().info("Iniciando Redis...");

        try {
            redis = Redis.builder(plugin)
                    .serverId("bungeecord")
                    .jedis(JedisBuilder.fromConfig(plugin.getConfig().getRedis()))
                    .build();

            setListeners();
            plugin.getLogger().info("Redis iniciado correctamente.");
        } catch (Exception exception) {
            plugin.getLogger().error("Un error ocurrió al inicializar la conexion a Redis: {}", exception.getMessage());
        }
    }

    public void stop() {
        try {
            redis.close();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void setListeners() {
        RedisListener listener = new RedisListenerImpl(this, executorService, plugin);

        listener.addListener("bungee:check-servers", (message) -> {
            String serverFrom = message.getServerFrom();

            if (serverFrom.equalsIgnoreCase("bungeecord")) {
                return;
            }

            plugin.getLogger().info("Recibido mensaje desde {}, contenido: {}", serverFrom, message.getMessage());
            Map<String, Boolean> availableServers = plugin.getServerCache().getAvailableServers();

            if (message.getMessage().equalsIgnoreCase("online")) {
                if (plugin.getConfig().getAutoReconnect().isEnabled()) {
                    for (Map.Entry<UUID, String> entry : plugin.getUserManager().getAutoReconnectQueue().entrySet()) {
                        Player player = plugin.getServer().getPlayer(entry.getKey()).orElse(null);

                        if (player == null || !player.isActive() || player.getCurrentServer().isEmpty()) {
                            continue;
                        }

                        ServerConnection server = player.getCurrentServer().orElse(null);

                        if (server == null) {
                            continue;
                        }

                        String serverName = server.getServerInfo().getName();

                        if (!plugin.getConfig().getServer().getHubs().contains(serverName)) {
                            continue;
                        }

                        player.createConnectionRequest(plugin.getServer().getServer(serverFrom).orElse(null)).connect().thenAccept(result ->
                                plugin.getLogger().info("El usuario {} ha sido enviado al servidor {}.", player.getUsername(), serverFrom));

                        player.sendMessage(Component.text().append(Component.text("¡Has sido teletransportado a ").color(TextColor.color(0xFFFFFF)))
                                .append(Component.text((serverFrom.substring(0, 1).toUpperCase() + serverFrom.substring(1))).color(TextColor.color(0x84FF8F)))
                                .append(Component.text(" automáticamente!").color(TextColor.color(0xFFFFFF))).build());
                    }

                    plugin.getUserManager().getAutoReconnectQueue().clear();
                }

                Utils.replaceValue(availableServers, message.getServerFrom(), true);
            } else {
                Utils.replaceValue(availableServers, message.getServerFrom(), false);
            }
        });

        listener.addListener("bangui:punish", (message) -> {
            Player player = plugin.getServer().getPlayer(message.getExecutor()).orElse(null);
            String[] split = message.getMessage().split(";");

            plugin.getServer().getCommandManager().executeAsync(player, split[0] + " " + split[1] + (split.length == 2 ? "" : (" " + split[2] + (split.length == 4 ? " " + split[3] : ""))));
        });
    }

    @Override
    public void sendMessage(String key, String message, String executor) {
        getChannel(key, PlayerMessage.class).sendMessage(new PlayerMessage(executor, message));
    }

    @Override
    public <T> Channel<T> getChannel(String name, Class<T> type) {
        return redis.messenger().getChannel("survival-" + name, type);
    }

    public JedisPool getPool() {
        return redis.rawConnection();
    }
}
