package es.virtualplanet.velocitycore.command.list;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.user.staff.StaffPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Arrays;

public class RegisterStaffCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;

    public RegisterStaffCommand(VelocityCorePlugin plugin) {
        this.plugin = plugin;
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
        if (invocation.arguments().length != 2) {
            source.sendMessage(Component.text("Usage: ").color(TextColor.color(0xFFFFFF)).append(Component.text("/registerstaff <player> <group>").color(TextColor.color(0xFCFF7B))));
            return;
        }

        String group = invocation.arguments()[1];
        String[] groups = {"helper", "mod", "smod", "developer", "admin", "owner"};

        // If group is not valid, send a message to the source and return.
        if (!Arrays.asList(groups).contains(group.toLowerCase())) {
            source.sendMessage(Component.text("Grupos válidos: " + Arrays.toString(groups)).color(TextColor.color(0xFF434B)));
            return;
        }

        Player target = plugin.getServer().getPlayer(invocation.arguments()[0]).orElse(null);

        // Target is not online, send a message to the source and return.
        if (target == null) {
            source.sendMessage(Component.text("El jugador no se encuentra conectado.").color(TextColor.color(0xFF434B)));
            return;
        }

        // If player is already registered, send a message to the source and return.
        if (plugin.getStaffManager().getStaffList().containsKey(target.getUniqueId())) {
            source.sendMessage(Component.text("¡Ey! El jugador ya forma parte del staff...").color(TextColor.color(0xFF434B)));
            return;
        }

        // Register the player as staff.
        StaffPlayer staffPlayer = new StaffPlayer(target.getUsername(), target.getUniqueId());
        plugin.getStaffManager().registerStaffPlayer(staffPlayer);

        // Set the corresponding group and send a message to the source.
        plugin.getUserManager().addGroup(target, group);
        source.sendMessage(Component.text(staffPlayer.getName() + " ha sido registrado correctamente.").color(TextColor.color(0x84FF8F)));
    }
}
