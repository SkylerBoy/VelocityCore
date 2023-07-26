package es.virtualplanet.velocitycore.user;

import es.virtualplanet.velocitycore.VelocityCorePlugin;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
}
