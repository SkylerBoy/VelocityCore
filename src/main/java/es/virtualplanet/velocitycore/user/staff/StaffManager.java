package es.virtualplanet.velocitycore.user.staff;

import es.virtualplanet.velocitycore.VelocityCorePlugin;
import lombok.Getter;

import java.util.*;

@Getter
public class StaffManager {

    private final VelocityCorePlugin plugin;

    private final Map<UUID, StaffPlayer> staffList = new HashMap<>();
    private final List<String> whitelistedCommands = new ArrayList<>();

    public StaffManager(VelocityCorePlugin plugin) {
        this.plugin = plugin;

        String[] commands = {"register", "reg", "r", "login", "l", "pass", "setpass"};
        whitelistedCommands.addAll(Arrays.asList(commands));
    }

    public void loadStaffData(StaffPlayer staffPlayer) {
        plugin.getMySQL().getStaffData(staffPlayer);
        staffList.put(staffPlayer.getUniqueId(), staffPlayer);
    }

    public void saveStaffData(StaffPlayer staffPlayer) {
        plugin.getMySQL().saveStaffDataSync(staffPlayer);
        staffList.remove(staffPlayer.getUniqueId());
    }

    public StaffPlayer getStaffPlayer(UUID uniqueId) {
        return staffList.get(uniqueId);
    }
}
