package es.virtualplanet.velocitycore.command.list;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.common.Utils;
import es.virtualplanet.velocitycore.user.staff.StaffPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class SetPassCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;

    public SetPassCommand(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Comando solo para jugadores.").color(TextColor.color(0xFF434B)));
            return;
        }

        if (!player.hasPermission("group.helper")) {
            return;
        }

        StaffPlayer staffPlayer = plugin.getStaffManager().getStaffPlayer(player.getUniqueId());

        if (staffPlayer.getPassword() == null) {
            if (invocation.arguments().length != 2) {
                player.sendMessage(Component.text()
                        .content("Uso correcto: ").color(TextColor.color(0xFFFFFF))
                        .append(Component.text("/setpass <contraseña> <contraseña>").color(TextColor.color(0xFCFF7B))));
                return;
            }

            if (!invocation.arguments()[0].equals(invocation.arguments()[1])) {
                player.sendMessage(Component.text("¡Vaya! Parece que las contraseñas no coinciden.").color(TextColor.color(0xFF434B)));
                return;
            }

            if (invocation.arguments()[0].length() < 3 || invocation.arguments()[0].length() > 12) {
                player.sendMessage(Component.text("La contraseña debe tener entre 3 y 12 caracteres.").color(TextColor.color(0xFF434B)));
                return;
            }

            staffPlayer.setPassword(Utils.sha256(invocation.arguments()[0]));
            player.sendMessage(Component.text("¡Tu contraseña fue actualizada con éxito!").color(TextColor.color(0x00FF00)));
            return;
        }

        if (invocation.arguments().length != 2) {
            player.sendMessage(Component.text()
                    .content("Uso correcto: ").color(TextColor.color(0xFFFFFF))
                    .append(Component.text("/setpass <contraseña> <contraseña>").color(TextColor.color(0xFCFF7B))));
            return;
        }

        if (!Utils.sha256(invocation.arguments()[0]).equals(staffPlayer.getPassword())) {
            player.sendMessage(Component.text("¡Vaya! Parece que la contraseña es incorrecta.").color(TextColor.color(0xFF434B)));
            return;
        }

        if (Utils.sha256(invocation.arguments()[1]).equals(staffPlayer.getPassword())) {
            player.sendMessage(Component.text("¡Hey! No puedes usar la misma contraseña.").color(TextColor.color(0xFF434B)));
            return;
        }

        staffPlayer.setPassword(Utils.sha256(invocation.arguments()[1]));
        player.sendMessage(Component.text("¡Tu contraseña fue actualizada con éxito!").color(TextColor.color(0x84FF8F)));
    }
}
