package es.virtualplanet.velocitycore.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import es.virtualplanet.velocitycore.listener.event.staff.StaffAuthenticateEvent;
import es.virtualplanet.velocitycore.user.User;
import es.virtualplanet.velocitycore.user.staff.StaffPlayer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.Map;
import java.util.UUID;

public class PlayerListener {

    private final VelocityCorePlugin plugin;

    public PlayerListener(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    @Subscribe (order = PostOrder.LAST)
    public void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        User user = new User(player.getUsername(), player.getUniqueId());

        plugin.getUserManager().loadUserData(user);

        if (!player.hasPermission("group.helper")) {
            return;
        }

        StaffPlayer staffPlayer = new StaffPlayer(player.getUsername(), player.getUniqueId());
        plugin.getStaffManager().loadStaffData(staffPlayer);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (event.getLoginStatus() == DisconnectEvent.LoginStatus.CANCELLED_BY_PROXY) {
            return;
        }

        Player player = event.getPlayer();
        User user = plugin.getUserManager().getUser(player.getUniqueId());

        if (user == null) {
            return;
        }

        player.getCurrentServer().ifPresent(server -> {
            String serverName = server.getServerInfo().getName();

            if (serverName.equals(plugin.getConfig().getServer().getAuth()) || plugin.getConfig().getServer().getHubs().contains(serverName)) {
                return;
            }

            user.setLastServer(server.getServerInfo().getName());
        });

        plugin.getUserManager().getAutoReconnectQueue().remove(player.getUniqueId());
        plugin.getUserManager().saveUserData(user);

        // Save staff data if player is staff
        if (!player.hasPermission("group.helper")) {
            return;
        }

        StaffPlayer staffPlayer = plugin.getStaffManager().getStaffPlayer(player.getUniqueId());

        if (staffPlayer == null) {
            return;
        }

        plugin.getStaffManager().saveStaffData(staffPlayer);

        // Send a message to the Discord log channel.
        if (!staffPlayer.isLogged()) {
            return;
        }

        Guild guild = plugin.getDiscordManager().getGuild();

        if (guild == null) {
            plugin.getLogger().info("No se ha podido obtener el servidor de Discord.");
            return;
        }

        TextChannel channel = guild.getTextChannelById("1043203832882135163");

        if (channel == null) {
            plugin.getLogger().info("No se ha podido obtener el canal de Discord.");
            return;
        }

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(0xe76161)
                .setAuthor(
                        staffPlayer.getName() + " se ha desconectado.",
                        "https://app.analyse.net/dashboard/player/" + staffPlayer.getUniqueId(),
                        "https://crafthead.net/helm/" + staffPlayer.getUniqueId() + "/64.png");

        plugin.getDiscordManager().getChannelMap().get("staff-logs").sendMessageEmbeds(builder.build()).queue();
    }

    @Subscribe
    private void onStaffAuthenticate(StaffAuthenticateEvent event) {
        Guild guild = plugin.getDiscordManager().getGuild();

        if (guild == null) {
            plugin.getLogger().info("No se ha podido obtener el servidor de Discord.");
            return;
        }

        TextChannel channel = guild.getTextChannelById("1043203832882135163");

        if (channel == null) {
            plugin.getLogger().info("No se ha podido obtener el canal de Discord.");
            return;
        }

        StaffPlayer staffPlayer = event.getStaffPlayer();
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(0xb2f5a1)
                .setAuthor(
                        staffPlayer.getName() + " se ha conectado.",
                        "https://app.analyse.net/dashboard/player/" + staffPlayer.getUniqueId(),
                        "https://crafthead.net/helm/" + staffPlayer.getUniqueId() + "/64.png");

        plugin.getDiscordManager().getChannelMap().get("staff-logs").sendMessageEmbeds(builder.build()).queue();
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getStaffManager().getStaffList().containsKey(player.getUniqueId())) {
            return;
        }

        StaffPlayer staffPlayer = plugin.getStaffManager().getStaffPlayer(player.getUniqueId());

        if (!staffPlayer.isStaffChatEnabled()) {
            return;
        }

        event.setResult(PlayerChatEvent.ChatResult.denied());

        String serverName = player.getCurrentServer().isPresent() ? player.getCurrentServer().get().getServerInfo().getName() : "Unknown";
        Component component = Component.text("SC").color(NamedTextColor.RED)
                .append(Component.text(" || ").color(NamedTextColor.GRAY))
                .append(Component.text("(").color(NamedTextColor.GRAY))
                .append(Component.text(serverName).color(NamedTextColor.YELLOW))
                .append(Component.text(") ").color(NamedTextColor.GRAY))
                .append(Component.text(player.getUsername()).color(NamedTextColor.GRAY))
                .append(Component.text(": ").color(NamedTextColor.GRAY))
                .append(Component.text(event.getMessage()).color(NamedTextColor.WHITE));

        for (Map.Entry<UUID, StaffPlayer> entry : plugin.getStaffManager().getStaffList().entrySet()) {
            Player target = plugin.getServer().getPlayer(entry.getKey()).orElse(null);

            if (target == null || !target.hasPermission("group.helper")) {
                continue;
            }

            target.sendMessage(component);
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }

        if (!player.hasPermission("group.helper")) {
            return;
        }

        if (plugin.getStaffManager().getWhitelistedCommands().contains(event.getCommand().split(" ")[0])) {
            return;
        }

        StaffPlayer staffPlayer = plugin.getStaffManager().getStaffPlayer(player.getUniqueId());

        if (staffPlayer.isLogged()) {
            return;
        }

        event.setResult(CommandExecuteEvent.CommandResult.denied());
        player.sendMessage(Component.text("¡Ey! Primero debes verificar tu identidad.").color(TextColor.color(0xFF434B)));
    }


}
