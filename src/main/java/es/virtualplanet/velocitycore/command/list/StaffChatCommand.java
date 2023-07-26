package es.virtualplanet.velocitycore.command.list;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.user.staff.StaffPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Map;
import java.util.UUID;

public class StaffChatCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;

    public StaffChatCommand(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Comando solo para jugadores.").color(TextColor.color(0xFF434B)));
            return;
        }

        if (!plugin.getStaffManager().getStaffList().containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("No tienes permiso para hacer esto.").color(TextColor.color(0xFF434B)));
            return;
        }

        if (invocation.arguments().length != 0) {
            String serverName = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "Unknown";
            StringBuilder message = new StringBuilder();

            for (String arg : invocation.arguments()) {
                message.append(arg).append(" ");
            }

            Component component = Component.text("SC").color(NamedTextColor.RED)
                    .append(Component.text(" || ").color(NamedTextColor.GRAY))
                    .append(Component.text("(").color(NamedTextColor.GRAY))
                    .append(Component.text(serverName).color(NamedTextColor.YELLOW))
                    .append(Component.text(") ").color(NamedTextColor.GRAY))
                    .append(Component.text(player.getUsername()).color(NamedTextColor.GRAY))
                    .append(Component.text(": ").color(NamedTextColor.GRAY))
                    .append(Component.text(message.toString()).color(NamedTextColor.WHITE));

            for (Map.Entry<UUID, StaffPlayer> entry : plugin.getStaffManager().getStaffList().entrySet()) {
                Player target = plugin.getServer().getPlayer(entry.getKey()).orElse(null);

                if (target == null || !target.hasPermission("group.helper")) {
                    continue;
                }

                target.sendMessage(component);
            }
            return;
        }

        StaffPlayer staffPlayer = plugin.getStaffManager().getStaffPlayer(player.getUniqueId());

        if (staffPlayer.isStaffChatEnabled()) {
            staffPlayer.setStaffChatEnabled(false);
            player.sendMessage(Component.text("StaffChat: ").color(TextColor.color(0xFFFFFF)).append(Component.text("Desactivado").color(TextColor.color(0xFF434B))));
        } else {
            staffPlayer.setStaffChatEnabled(true);
            player.sendMessage(Component.text("StaffChat: ").color(TextColor.color(0xFFFFFF)).append(Component.text("Activado").color(TextColor.color(0x84FF8F))));
        }
    }
}
