package es.virtualplanet.velocitycore.command.list;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Arrays;

public class DeleteStaffCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;
    private final String[] exemptUsers;

    public DeleteStaffCommand(VelocityCorePlugin plugin) {
        this.plugin = plugin;
        this.exemptUsers = new String[] {"Skyler_Boy", "ElLocoMen"};
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        // If source don't have the permission, send a message to the source and return.
        if (!source.hasPermission("group.admin")) {
            source.sendMessage(Component.text("No tienes permisos para ejecutar este comando.").color(TextColor.color(0xFF434B)));
            return;
        }

        // If argument length is not 2, send a message to the source and return.
        if (invocation.arguments().length != 1) {
            source.sendMessage(Component.text("Usage: ").color(TextColor.color(0xFFFFFF)).append(Component.text("/registerstaff <player>").color(TextColor.color(0xFCFF7B))));
            return;
        }

        String targetName = invocation.arguments()[0];

        if (Arrays.asList(exemptUsers).contains(targetName)) {
            source.sendMessage(Component.text("Parece que " + targetName + " está inmunizado.").color(TextColor.color(0xFF434B)));
            return;
        }

        Player target = plugin.getServer().getPlayer(invocation.arguments()[0]).orElse(null);

        // Target is not online, send a message to the source and return.
        if (target == null) {
            plugin.getStaffManager().unregisterStaffPlayer(targetName);

            source.sendMessage(Component.text(targetName + " está desconectado. Se borraron sus datos.").color(TextColor.color(0xFF434B)));
            source.sendMessage(Component.text("Recuerda eliminar el rango de manera manual.").color(TextColor.color(0xFF434B)));
            return;
        }

        String group = plugin.getUserManager().getGroup(target);
        String[] groups = {"helper", "mod", "smod", "developer", "admin", "owner"};

        // If group is not valid, send a message to the source and return.
        if (!Arrays.asList(groups).contains(group.toLowerCase()) || !plugin.getStaffManager().getStaffList().containsKey(target.getUniqueId())) {
            source.sendMessage(Component.text(target.getUsername() + " no es un miembro del equipo.").color(TextColor.color(0xFF434B)));
            return;
        }

        // Unregister the player as staff.
        plugin.getStaffManager().unregisterStaffPlayer(target.getUniqueId());

        // Set the corresponding group and send a message to the source.
        plugin.getUserManager().removeGroup(target, group);

        source.sendMessage(Component.text(targetName + " ha sido borrado correctamente.").color(TextColor.color(0x84FF8F)));
        target.disconnect(Component.text("Has sido expulsado del equipo de VP."));
    }
}
