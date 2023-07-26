package es.virtualplanet.velocitycore.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ServerListener {

    private final VelocityCorePlugin plugin;

    public ServerListener(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onKickedFromServer(KickedFromServerEvent event) {
        if (!plugin.getConfig().getAutoReconnect().isEnabled()) {
            return;
        }

        RegisteredServer server = event.getServer();

        if (server == null || server.getServerInfo().getName().equals("authlobby")) {
            return;
        }

        String serverName = server.getServerInfo().getName();

        if (!plugin.getConfig().getAutoReconnect().getAvailableServers().contains(serverName)) {
            return;
        }

        Component reason = event.getServerKickReason().orElse(null);

        if (reason == null || !reason.toString().contains("El servidor se está reiniciando...")) {
            return;
        }

        Player player = event.getPlayer();

        if (player ==  null || !player.hasPermission("queue.bypass." + serverName)) {
            return;
        }

        plugin.getUserManager().getAutoReconnectQueue().put(player.getUniqueId(), serverName.substring(0, 1).toUpperCase() + serverName.substring(1));
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        if (!plugin.getConfig().getAutoReconnect().isEnabled()) {
            return;
        }

        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        RegisteredServer server = event.getResult().getServer().orElse(null);

        if (server == null) {
            return;
        }

        String serverName = server.getServerInfo().getName();

        if (!plugin.getServerCache().getAvailableServers().containsKey(serverName)) {
            return;
        }

        if (!plugin.getServerCache().getAvailableServers().get(serverName)) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            player.sendMessage(Component.text("El servidor no se encuentra disponible en este momento.").color(NamedTextColor.RED));
            return;
        }

        if (!plugin.getConfig().getAutoReconnect().getAvailableServers().contains(serverName)) {
            return;
        }

        if (plugin.getUserManager().getAutoReconnectQueue().isEmpty() || plugin.getUserManager().getAutoReconnectQueue().containsKey(player.getUniqueId())) {
            return;
        }

        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        player.sendMessage(Component.text("El servidor se está terminando de iniciar.").color(NamedTextColor.RED));
    }
}
