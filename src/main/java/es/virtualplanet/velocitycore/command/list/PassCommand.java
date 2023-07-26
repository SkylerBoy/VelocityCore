package es.virtualplanet.velocitycore.command.list;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.common.Utils;
import es.virtualplanet.velocitycore.listener.event.staff.StaffAuthenticateEvent;
import es.virtualplanet.velocitycore.user.staff.StaffPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

public class PassCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;

    public PassCommand(VelocityCorePlugin plugin) {
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
            return;
        }

        if (invocation.arguments().length != 1) {
            player.sendMessage(Component.text()
                    .content("Uso correcto: ").color(TextColor.color(0xFFFFFF))
                    .append(Component.text("/pass <contraseña>").color(TextColor.color(0xFCFF7B))));
            return;
        }

        StaffPlayer staffPlayer = plugin.getStaffManager().getStaffPlayer(player.getUniqueId());

        if (staffPlayer.isLogged()) {
            player.sendMessage(Component.text("¡Ya has iniciado sesión correctamente!").color(TextColor.color(0xFF434B)));
            return;
        }

        if (!staffPlayer.getPassword().equals(Utils.sha256(invocation.arguments()[0]))) {
            player.sendMessage(Component.text("¡Vaya! Parece que la contraseña es incorrecta.").color(TextColor.color(0xFF434B)));
            return;
        }

        staffPlayer.setLogged(true);

        player.sendMessage(Component.text("¡Iniciaste sesión correctamente!").color(TextColor.color(0x84FF8F)));
        plugin.getServer().getEventManager().fire(new StaffAuthenticateEvent(staffPlayer));
    }
}
