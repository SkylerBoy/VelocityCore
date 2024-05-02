package es.virtualplanet.velocitycore.command.list;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.common.Utils;
import es.virtualplanet.velocitycore.user.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DiscordCommand implements SimpleCommand {

    private final VelocityCorePlugin plugin;
    private final Component PREFIX, ERROR_PREFIX;

    public DiscordCommand(VelocityCorePlugin plugin) {
        this.plugin = plugin;

        this.PREFIX = plugin.getDiscordManager().getPREFIX();
        this.ERROR_PREFIX = Component.text("ERROR").color(NamedTextColor.RED).decorate(TextDecoration.BOLD).append(Component.text(" || ")).color(NamedTextColor.DARK_GRAY);
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player player)) {
            invocation.source().sendMessage(Component.text("Solo puedes ejecutar este comando como jugador.").color(NamedTextColor.RED));
            return;
        }

        if (!player.hasPermission("virtual.command.discord")) {
            player.sendMessage(ERROR_PREFIX.append(Component.text("No tienes permisos para ejecutar este comando.").color(NamedTextColor.WHITE)));
            return;
        }

        User user = plugin.getUserManager().getUser(player.getUniqueId());

        if (user.isVerified()) {
            player.sendMessage(ERROR_PREFIX.append(Component.text("Ya has vinculado tu cuenta.")).color(NamedTextColor.WHITE));
            return;
        }

        if (plugin.getDiscordManager().getCodeMap().containsValue(player.getUniqueId())) {
            String code = plugin.getDiscordManager().getCodeMap().entrySet().stream().filter(entry -> entry.getValue().equals(player.getUniqueId())).findFirst().get().getKey();
            player.sendMessage(PREFIX.append(Component.text("Tu código de verificación es: ")).color(NamedTextColor.WHITE).append(Component.text(code).color(NamedTextColor.GREEN).clickEvent(ClickEvent.copyToClipboard(code))));
            return;
        }

        String code = Utils.generateCode(6, true, true);
        plugin.getDiscordManager().getCodeMap().put(code, player.getUniqueId());

        player.sendMessage(PREFIX.append(Component.text("Tu código de verificación es: ").color(NamedTextColor.WHITE).append(Component.text(code).color(NamedTextColor.GREEN).clickEvent(ClickEvent.copyToClipboard(code)))));
        scheduleCodeRemoval(code);
    }

    public void scheduleCodeRemoval(String code) {
        plugin.getServer().getScheduler().buildTask(this, () -> {
            Map<String, UUID> codeMap = plugin.getDiscordManager().getCodeMap();
            Player player = plugin.getServer().getPlayer(codeMap.get(code)).orElse(null);

            if (codeMap.containsKey(code)) {
                codeMap.remove(code);

                if (player != null)
                    player.sendMessage(ERROR_PREFIX.append(Component.text("Tu código de verificación ha expirado.")).color(NamedTextColor.WHITE));
            }
        }).delay(30, TimeUnit.SECONDS).schedule();
    }
}
