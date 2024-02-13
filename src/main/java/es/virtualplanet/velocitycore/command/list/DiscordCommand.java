package es.virtualplanet.velocitycore.command.list;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.common.Utils;
import es.virtualplanet.velocitycore.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DiscordCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;

    public DiscordCommand(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Solo puedes ejecutar este comando como jugador.").color(NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("virtual.command.discord")) {
            player.sendMessage(Component.text("No tienes permisos para ejecutar este comando.").color(NamedTextColor.RED));
            return;
        }

        if (plugin.getDiscordManager().getCodeMap().containsValue(player.getUniqueId())) {
            player.sendMessage(Component.text("Ya tienes un código de verificación pendiente.").color(NamedTextColor.RED));
            return;
        }

        User user = plugin.getUserManager().getUser(player.getUniqueId());

        if (user.isVerified()) {
            player.sendMessage(Component.text("Ya estás verificado en el servidor de Discord.").color(NamedTextColor.RED));
            return;
        }

        String code = Utils.generateCode(8, true, true);

        plugin.getDiscordManager().getCodeMap().put(code, player.getUniqueId());
        player.sendMessage(Component.text("Tu código es: ").color(NamedTextColor.WHITE).append(Component.text(code).color(NamedTextColor.GREEN)));
    }
}
