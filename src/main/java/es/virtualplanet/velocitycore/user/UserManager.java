package es.virtualplanet.velocitycore.user;

import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import lombok.Getter;
import net.luckperms.api.messaging.MessagingService;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.node.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class UserManager {

    private final VelocityCorePlugin plugin;

    private final Map<UUID, User> userList = new HashMap<>();
    private final Map<UUID, String> autoReconnectQueue = new HashMap<>();

    public UserManager(VelocityCorePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadUserData(User user) {
        plugin.getMySQL().getUserData(user);
        userList.put(user.getUniqueId(), user);
    }

    public void saveUserData(User user) {
        plugin.getMySQL().saveUserDataSync(user);
        userList.remove(user.getUniqueId());
    }

    public User getUser(UUID uniqueId) {
        return userList.get(uniqueId);
    }

    public long getDiscordId(UUID uniqueId) {
        return plugin.getMySQL().getDiscordId(uniqueId);
    }

    public String getGroup(Player player) {
        net.luckperms.api.model.user.User user = plugin.getLuckPerms().getPlayerAdapter(Player.class).getUser(player);
        return user.getPrimaryGroup();
    }

    public void addGroup(Player player, String group) {
        net.luckperms.api.model.user.User user = plugin.getLuckPerms().getPlayerAdapter(Player.class).getUser(player);
        DataMutateResult result = user.data().add(Node.builder("group." + group).build());

        if (result.wasSuccessful()) {
            CompletableFuture<Void> future =  plugin.getLuckPerms().getUserManager().saveUser(user);

            future.thenRunAsync(() -> {
                Optional<MessagingService> messagingService =  plugin.getLuckPerms().getMessagingService();
                messagingService.ifPresent(service -> service.pushUserUpdate(user));
            });
        }
    }

    public void removeGroup(Player player, String group) {
        net.luckperms.api.model.user.User user = plugin.getLuckPerms().getPlayerAdapter(Player.class).getUser(player);
        DataMutateResult result = user.data().remove(Node.builder("group." + group).build());

        if (result.wasSuccessful()) {
            CompletableFuture<Void> future =  plugin.getLuckPerms().getUserManager().saveUser(user);

            future.thenRunAsync(() -> {
                Optional<MessagingService> messagingService =  plugin.getLuckPerms().getMessagingService();
                messagingService.ifPresent(service -> service.pushUserUpdate(user));
            });
        }
    }
}
