package es.virtualplanet.velocitycore.user.staff;

import com.velocitypowered.api.proxy.Player;
import es.virtualplanet.velocitycore.VelocityCorePlugin;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class StaffPlayer {

    private UUID uniqueId;
    private String name;

    private int id;

    private String password;
    private boolean staffChatEnabled = false, logged = false;

    public StaffPlayer(String name, UUID uniqueId) {
        this.name = name;
        this.uniqueId = uniqueId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }

        if (!(object instanceof StaffPlayer staffPlayer)) {
            return false;
        }

        return staffPlayer.getUniqueId().equals(uniqueId);
    }

    public Player toPlayer() {
        return VelocityCorePlugin.getInstance().getServer().getPlayer(uniqueId).orElse(null);
    }
}
