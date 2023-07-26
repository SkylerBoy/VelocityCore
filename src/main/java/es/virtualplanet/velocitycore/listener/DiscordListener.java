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

    public DiscordListener(VelocityCorePlugin plugin) {
        this.plugin = plugin;
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
                    event.reply("El código introducido no existe.").setEphemeral(true).queue();
                    break;
                }

                Player player = plugin.getServer().getPlayer(plugin.getDiscordManager().getCodeMap().get(code)).orElse(null);

                if (player == null) {
                    event.reply("Cuenta asociada con " + plugin.getDiscordManager().getCodeMap().get(code).toString() + ".").setEphemeral(true).queue();
                    plugin.getDiscordManager().getCodeMap().remove(code);
                    break;
                }

                if (event.getGuild() == null) {
                    event.reply("No se ha podido verificar tu cuenta. (Error 7.0.1)").setEphemeral(true).queue();
                    break;
                }

                net.luckperms.api.model.user.User lpUser = plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId());

                if (lpUser == null) {
                    event.reply("No se ha podido verificar tu cuenta. (Error 7.0.2)").setEphemeral(true).queue();
                    break;
                }

                User user = plugin.getUserManager().getUser(player.getUniqueId());

                boolean donator = false;

                user.setDiscordId(event.getUser().getIdLong());
                plugin.getDiscordManager().getCodeMap().remove(code);

                for (String roleMap : plugin.getDiscordManager().getDiscordInfo().getVerify().getMapping()) {
                    String rankName = roleMap.split(":")[0];

                    if (!lpUser.getPrimaryGroup().equalsIgnoreCase(rankName)) {
                        continue;
                    }

                    long roleId = Long.parseLong(roleMap.split(":")[1]);

                    if (Objects.requireNonNull(event.getGuild()).getRoleById(roleId) == null) {
                        event.reply("No se ha podido verificar tu cuenta. (Error 7.0.3)").setEphemeral(true).queue();
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

                event.reply("Has vinculado la cuenta '" + player.getUsername() + "' a tu cuenta.").setEphemeral(true).queue();
                player.sendMessage(Component.text("Has vinculado tu cuenta de Discord a tu cuenta de Minecraft.").color(NamedTextColor.GREEN));
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
                    String profileAsJson = getProfileAsJson("https://app.analyse.net/api/v1/server/player/" + playerName);

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
                    String lastSession = formatter.format(Instant.parse(jsonObject.getJSONObject("player").getString("last_logged_in_at")));

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
                    exception.printStackTrace();
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
