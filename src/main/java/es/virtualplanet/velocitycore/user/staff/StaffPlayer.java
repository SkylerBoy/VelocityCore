package es.virtualplanet.velocitycore.user.staff;

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
    private boolean staffChatEnabled, logged = false;

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
}
