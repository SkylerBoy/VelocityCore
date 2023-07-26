package es.virtualplanet.velocitycore.command.list;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import net.kyori.adventure.text.Component;

public class StreamCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;

    public StreamCommand(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Only players can use this command!"));
            return;
        }

        if (invocation.arguments().length == 0) {
            player.sendMessage(Component.text("Usage: /stream <url>"));
            return;
        }

        String url = invocation.arguments()[0];
        player.sendMessage(Component.text("Your stream URL has been set to: " + url));
    }
}
