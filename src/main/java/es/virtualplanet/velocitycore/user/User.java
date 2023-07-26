package es.virtualplanet.velocitycore.user;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
public class User {

    private UUID uniqueId;
    private String name, lastServer = null;

    private int id;

    private Timestamp firstJoin;
    private long discordId = 0L;

    public User(String name, UUID uniqueId) {
        this.name = name;
        this.uniqueId = uniqueId;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }

        if (!(object instanceof User bungeePlayer)) {
            return false;
        }

        return bungeePlayer.getUniqueId().equals(uniqueId);
    }

    public boolean isVerified() {
        return discordId != 0L;
    }
}
