package es.virtualplanet.velocitycore.punish;

import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import litebans.api.Database;
import litebans.api.Entry;
import litebans.api.Events;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Getter
public class PunishManager {

    private final VelocityCorePlugin plugin;

    public PunishManager(VelocityCorePlugin plugin) {
        this.plugin = plugin;

        if (plugin.getDiscordManager().getGuild() == null) {
            plugin.getLogger().error("Error with the Discord connection, guild is null.");
            return;
        }

        this.listenPunishments();
    }

    private void listenPunishments() {
        Events.get().register(new Events.Listener() {
            @Override
            public void entryAdded(Entry entry) {
                Player victim = plugin.getServer().getPlayer(entry.getUuid()).orElse(null);

                String victimInfo = victim == null ? entry.getUuid() : victim.getUsername();
                String operatorInfo, expireTime;

                Player operator = plugin.getServer().getPlayer(entry.getExecutorUUID()).orElse(null);
                operatorInfo = operator == null ? "Consola" : operator.getUsername();

                DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());
                expireTime = entry.isPermanent() ? "Permanente" : format.format(Instant.ofEpochMilli(entry.getDateEnd()));

                EmbedBuilder builder = new EmbedBuilder()
                        .setTitle("SANCIÓN ID #" + entry.getId() + " | " + entry.getType().toUpperCase())
                        .setColor(0xff6868)
                        .setThumbnail("https://crafthead.net/helm/" + entry.getUuid() + "/64.png")
                        .addField("\u200b\n\uD83D\uDC6E OPERADOR", "• " + operatorInfo, true)
                        .addField("\u200b\n\uD83D\uDCA5 VICTIMA", "• " + victimInfo, true)
                        .addField("\u200b\n\uD83D\uDD58 DURACIÓN", "• " + expireTime, true)
                        .addField("\u200b\n\uD83D\uDCDD RAZÓN", entry.getReason() + "\n\u200b", true)
                        .setFooter("VirtualPlanetNT - " + format.format(Instant.now()), plugin.getDiscordManager().getGuild().getIconUrl());

                plugin.getDiscordManager().getChannelMap().get("ban-logs").sendMessageEmbeds(builder.build()).queue();
            }
        });
    }

    public boolean isBanned(UUID uniqueId, String address, String server) {
        return server == null ? Database.get().isPlayerBanned(uniqueId, address) : Database.get().isPlayerBanned(uniqueId, address, server);
    }
}
