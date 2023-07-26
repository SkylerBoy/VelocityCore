package es.virtualplanet.velocitycore.scheduler;

import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.*;

public class GeneralTask implements Runnable {

    private final VelocityCorePlugin plugin;

    private final Map<Integer, TextColor> colorList = new HashMap<>();
    private int number = 0;

    public GeneralTask(VelocityCorePlugin plugin) {
        this.plugin = plugin;

        int colorIndex = 0;

        List<TextColor> list = Arrays.asList(
                NamedTextColor.YELLOW,
                NamedTextColor.GREEN,
                NamedTextColor.LIGHT_PURPLE,
                NamedTextColor.GOLD,
                NamedTextColor.AQUA);

        for (TextColor chatColor : list) {
            colorList.put(colorIndex, chatColor);
            colorIndex++;
        }
    }

    @Override
    public void run() {
        if (plugin.getServer().getAllPlayers().isEmpty() || !plugin.getConfig().getAutoReconnect().isEnabled()) {
            return;
        }

        if (colorList.get(number) == null) {
            number = 0;
        }

        TextColor chatColor = colorList.get(number);

        for (UUID uniqueId : plugin.getUserManager().getAutoReconnectQueue().keySet()) {
            Player player = plugin.getServer().getPlayer(uniqueId).orElse(null);

            if (player == null || !player.isActive()) {
                continue;
            }

            String serverName = plugin.getUserManager().getAutoReconnectQueue().get(player.getUniqueId());
            player.sendActionBar(Component.text().append(Component.text("Serás teletransportado a ").color(NamedTextColor.WHITE)).append(Component.text(serverName).color(chatColor)).append(Component.text(" en breves...").color(NamedTextColor.WHITE)));
        }

        number++;
    }
}
