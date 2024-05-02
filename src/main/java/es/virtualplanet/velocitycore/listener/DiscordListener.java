package es.virtualplanet.velocitycore.listener;

import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.user.User;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {

    private final VelocityCorePlugin plugin;
    private final Component PREFIX;

    public DiscordListener(VelocityCorePlugin plugin) {
        this.plugin = plugin;
        this.PREFIX = plugin.getDiscordManager().getPREFIX();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        switch (event.getName().toLowerCase()) {
            case "ping" -> {
                long time = System.currentTimeMillis();
                event.reply("Pong!").setEphemeral(true).flatMap(v -> event.getHook().editOriginalFormat("Pong: %d ms", System.currentTimeMillis() - time)).queue();
            }

            case "verify" -> {
                String code = Objects.requireNonNull(event.getOption("code")).getAsString();

                if (!plugin.getDiscordManager().getCodeMap().containsKey(code)) {
                    event.reply("<a:no:737240171677876224> El código introducido no existe o ha expirado.").setEphemeral(true).queue();
                    break;
                }

                Player player = plugin.getServer().getPlayer(plugin.getDiscordManager().getCodeMap().get(code)).orElse(null);

                if (player == null) {
                    event.reply("<a:no:737240171677876224> Por favor, mantente conectado al servidor.").setEphemeral(true).queue();
                    break;
                }

                if (event.getGuild() == null) {
                    event.reply("<a:no:737240171677876224> No se ha podido verificar tu cuenta. (Error 7.0.1)").setEphemeral(true).queue();
                    break;
                }

                net.luckperms.api.model.user.User lpUser = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());

                if (lpUser == null) {
                    event.reply("<a:no:737240171677876224> No se ha podido verificar tu cuenta. (Error 7.0.2)").setEphemeral(true).queue();
                    break;
                }

                User user = plugin.getUserManager().getUser(player.getUniqueId());
                boolean donator = false;

                long discordId = event.getUser().getIdLong();

                // Update mc user info.
                user.setDiscordId(discordId);
                plugin.getUserManager().updateDiscordId(user, discordId);

                // Update discord user roles.
                for (String roleMap : plugin.getDiscordManager().getDiscordInfo().getVerify().getMapping()) {
                    String rankName = roleMap.split(":")[0];

                    if (!lpUser.getPrimaryGroup().equalsIgnoreCase(rankName)) {
                        continue;
                    }

                    long roleId = Long.parseLong(roleMap.split(":")[1]);

                    if (Objects.requireNonNull(event.getGuild()).getRoleById(roleId) == null) {
                        event.reply("<a:no:737240171677876224> No se ha podido verificar tu cuenta. (Error 7.0.3)").setEphemeral(true).queue();
                        break;
                    }

                    event.getGuild().addRoleToMember(event.getUser(), Objects.requireNonNull(event.getGuild().getRoleById(roleId))).queue();
                    donator = true;
                }

                if (donator) {
                    long donorRoleId = Long.parseLong(plugin.getDiscordManager().getDiscordInfo().getVerify().getDonorRole());
                    event.getGuild().addRoleToMember(event.getUser(), Objects.requireNonNull(event.getGuild().getRoleById(donorRoleId))).queue();
                }

                long verifiedRoleId = Long.parseLong(plugin.getDiscordManager().getDiscordInfo().getVerify().getVerifiedRole());
                event.getGuild().addRoleToMember(event.getUser(), Objects.requireNonNull(event.getGuild().getRoleById(verifiedRoleId))).queue();

                event.reply("<a:si:737240158172217344> Has vinculado la cuenta '" + player.getUsername() + "' con Discord.").setEphemeral(true).queue();
                player.sendMessage(PREFIX.append(Component.text("Has vinculado tu cuenta con Discord correctamente.")).color(NamedTextColor.WHITE));

                plugin.getDiscordManager().getCodeMap().remove(code);
            }

            case "profile" -> {
                EmbedBuilder builder = new EmbedBuilder();

                if (event.getOption("player") == null) {
                    builder.setTitle("¡Vaya! Ha ocurrido un error :(").setDescription("Debes especificar el nombre de un jugador.").setColor(0xe76161);
                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                    break;
                }

                String playerName = Objects.requireNonNull(event.getOption("player")).getAsString();

                try {
                    String profileAsJson = getProfileAsJson("https://analytics.tebex.io/api/v1/server/sessions/" + playerName);

                    if (profileAsJson == null) {
                        builder.setTitle("¡Vaya! Ha ocurrido un error :(").setDescription("No se ha podido encontrar el perfil de " + playerName + ".").setColor(0xe76161);
                        event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                        break;
                    }

                    JSONObject jsonObject = new JSONObject(profileAsJson);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

                    boolean playerIsOnline = plugin.getServer().getPlayer(playerName).isPresent();
                    String onlineStatus = playerIsOnline ? "✅ Conectado" : "❌ Desconectado";

                    UUID uniqueId = UUID.fromString(jsonObject.getJSONObject("player").getString("uuid").replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"));
                    String targetDiscord = "Sin verificar";

                    String firstSession = formatter.format(Instant.parse(jsonObject.getJSONObject("player").getString("first_joined_at")));
                    String lastSession = formatter.format(Instant.parse(jsonObject.getJSONObject("player").getString("quit_at")));

                    long discordId;

                    if (plugin.getUserManager().getUser(uniqueId) != null) {
                        discordId = plugin.getUserManager().getUser(uniqueId).getDiscordId();
                    } else {
                        discordId = plugin.getUserManager().getDiscordId(uniqueId);
                    }

                    if (discordId != 0L && Objects.requireNonNull(event.getGuild()).getMemberById(discordId) != null) {
                        targetDiscord = Objects.requireNonNull(event.getGuild().getMemberById(discordId)).getUser().getAsTag();
                    }

                    boolean isBanned = plugin.getPunishManager().isBanned(uniqueId, null, null);

                    builder.setTitle("Información de " + playerName)
                            .setColor(0xff9d3a)
                            .setThumbnail("https://crafthead.net/helm/" + uniqueId + "/64.png")
                            .setDescription("**UUID**: " + uniqueId + "\n**Rango**: " + (playerIsOnline ? "`OWNER`" : "`DEFAULT`") + "\n**Verificado**: " + (discordId != 0L ? "✅" : "❌") + " (" + targetDiscord +  ")\n\u200b")
                            .addField("Primera sesión", "• " + firstSession + "\n\u200b", true)
                            .addField("Última sesión", "• " + (playerIsOnline ? formatter.format(Instant.now()) : lastSession) + "\n\u200b", true)
                            .addField("En línea", onlineStatus + (isBanned ? "\n\u200b" : ""), true);

                    if (isBanned) {
                        builder.setFooter("¡Atención! El jugador se encuentra baneado.", "https://i.imgur.com/WFuo92m.png");
                    }

                    event.replyEmbeds(builder.build()).setEphemeral(true).queue();
                } catch (Exception exception) {
                    throw new RuntimeException(exception.getMessage(), exception);
                }
            }

            default -> {}
        }
    }

    private String getProfileAsJson(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");

        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Authorization", "");
        con.setRequestProperty("X-Server-Token", "4000001|crPSUUA6q7IlJ8lrLIqK5hNyVyuWKijPzddkPu0Kb41badbc");

        int responseCode = con.getResponseCode();

        plugin.getLogger().info("Sending 'GET' request to URL: {}", url);
        plugin.getLogger().info("Response code: {}", responseCode);

        if (responseCode != 200) {
            return null;
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;

        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        in.close();
        return response.toString();
    }
}
