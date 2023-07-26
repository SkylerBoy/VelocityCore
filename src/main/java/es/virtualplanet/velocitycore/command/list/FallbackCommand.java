package es.virtualplanet.velocitycore.command.list;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class FallbackCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;

    public FallbackCommand(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            return;
        }

        if (player.getCurrentServer().isPresent() && player.getCurrentServer().get().getServerInfo().getName().equals("authlobby")) {
            player.sendMessage(Component.text("No puedes usar este comando en el servidor de autenticación.").color(TextColor.color(0xFF434B)));
            return;
        }

        if (plugin.getServer().getServer("lobby-1").isEmpty()) {
            player.sendMessage(Component.text("No hay servidores disponibles.").color(TextColor.color(0xFF434B)));
            return;
        }

        player.createConnectionRequest(plugin.getServer().getServer("lobby-1").get()).fireAndForget();
    }
}
