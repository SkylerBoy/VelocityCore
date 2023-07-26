package es.virtualplanet.velocitycore.discord;

import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.config.MainConfig;
import es.virtualplanet.velocitycore.listener.DiscordListener;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class DiscordManager extends ListenerAdapter {

    /* TODO
    - Agregar tiempo de expiración a los códigos de verificación
    - Agregar sistema de máximas cuentas verificadas.
    - Crear base de datos de discord con los datos de las sincronizaciones.
     */

    private final VelocityCorePlugin plugin;
    private final MainConfig.DiscordInfo discordInfo;

    private JDA jda = null;
    private Guild guild = null;

    private final Map<String, UUID> codeMap;
    private final Map<String, TextChannel> channelMap = new HashMap<>();

    public DiscordManager(VelocityCorePlugin plugin) {
        this.plugin = plugin;

        discordInfo = plugin.getConfig().getDiscord();
        codeMap = new HashMap<>();

        try {
            JDABuilder builder = JDABuilder.createDefault(discordInfo.getToken());

            builder.disableCache(CacheFlag.VOICE_STATE, CacheFlag.STICKER, CacheFlag.EMOJI, CacheFlag.ACTIVITY)
                    .addEventListeners(new DiscordListener(plugin))
                    .setBulkDeleteSplittingEnabled(false)
                    .setCompression(Compression.ZLIB);

            jda = builder.build();

            jda.upsertCommand("ping", "Comprueba el ping aproximado que tienes con el servidor.").queue();
            jda.upsertCommand("verify", "Vincula tu cuenta con el servidor de Minecraft.").addOption(OptionType.STRING, "code", "Código generado tras usar /verify en Minecraft.").queue();
            jda.upsertCommand("profile", "Comprueba las estadísticas de cualquier jugador.").addOption(OptionType.STRING, "player", "Nombre de usuario.").queue();

            jda.awaitReady(); // Wait for JDA to be ready.

            guild = jda.getGuildById("327836556918390785");

            if (guild != null) {
                channelMap.put("staff-logs", guild.getTextChannelById("1043203832882135163"));
                channelMap.put("ban-logs", guild.getTextChannelById("1050836665003946074"));
            }

            plugin.getLogger().info("JDA iniciado correctamente con {}.", jda.getSelfUser().getAsTag());
        } catch (Exception exception) {
            plugin.getLogger().error("Error al iniciar JDA. No hay conexión con Discord.");
        }
    }
}
