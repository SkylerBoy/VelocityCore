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

        StaffPlayer staffPlayer = plugin.getStaffManager().getStaffPlayer(player.getUniqueId());

        if (staffPlayer == null) {
            player.sendMessage(Component.text("Ha ocurrido un error, contacta con un administrador.").color(TextColor.color(0xFF434B)));
            return;
        }

        if (invocation.arguments().length != 2) {
            player.sendMessage(Component.text()
                    .content("Uso correcto: ").color(TextColor.color(0xFFFFFF))
                    .append(Component.text("/setpass <old> <new>").color(TextColor.color(0xFCFF7B))));
            return;
        }

        String newPassword = Utils.sha256(invocation.arguments()[1]);

        if (!Utils.sha256(invocation.arguments()[0]).equals(staffPlayer.getPassword())) {
            player.sendMessage(Component.text("¡Vaya! Parece que la contraseña es incorrecta.").color(TextColor.color(0xFF434B)));
            return;
        }

        if (newPassword.equals(staffPlayer.getPassword())) {
            player.sendMessage(Component.text("¡Hey! No puedes usar la misma contraseña.").color(TextColor.color(0xFF434B)));
            return;
        }

        staffPlayer.setPassword(newPassword);
        player.sendMessage(Component.text("¡Tu contraseña fue actualizada con éxito!").color(TextColor.color(0x84FF8F)));
    }
}
