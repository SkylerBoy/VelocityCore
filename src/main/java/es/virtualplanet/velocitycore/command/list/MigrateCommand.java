package es.virtualplanet.velocitycore.command.list;

import com.nickuc.login.api.nLoginAPI;
import com.nickuc.login.api.types.AccountData;
import com.nickuc.login.api.types.Identity;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Optional;
import java.util.UUID;

public class MigrateCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;

    public MigrateCommand(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            return;
        }

        if (invocation.arguments().length != 1) {
            player.sendMessage(Component.text("Uso correcto: /migrate <contraseña>", TextColor.color(0xFF0000)));
            return;
        };

        Identity identity = Identity.ofKnownName(player.getUsername());
        AccountData accountData = plugin.getNLogin().getAccount(identity).orElse(null);

        if (accountData == null) {
            player.sendMessage(Component.text("Ha ocurrido un error, contacta con un administrador.", TextColor.color(0xFF0000)));
            return;
        }

        // If the player is not authenticated, the migration is not available.
        if (!plugin.getNLogin().isAuthenticated(identity)) {
            return;
        }

        // If the password is not correct, player can't migrate the account.
        if (!plugin.getNLogin().comparePassword(accountData, invocation.arguments()[0])) {
            player.sendMessage(Component.text("La contraseña introducida no es correcta.", TextColor.color(0xFF0000)));
            return;
        }

        Optional<UUID>
                uniqueId = accountData.getUniqueId(),
                mojangId = accountData.getMojangId();

        // If the uniqueId/mojangId is empty or the uniqueId is the same as the mojangId, the migration is not available.
        if (uniqueId.isEmpty() || mojangId.isEmpty() || (uniqueId.get().equals(mojangId.get()))) {
            player.sendMessage(Component.text("La migración no está disponible para tu cuenta.", TextColor.color(0xFF0000)));
            return;
        }

        plugin.getServer().getCommandManager().executeAsync(plugin.getServer().getConsoleCommandSource(), "nlogin changeuuid " + player.getUsername() + " " + mojangId.get());
    }
}
