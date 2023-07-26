package es.virtualplanet.velocitycore.listener.event.user;

import es.virtualplanet.velocitycore.user.User;
import lombok.Getter;

@Getter
public class UserDataLoadEvent {

    private final User user;

    public UserDataLoadEvent(User user) {
        this.user = user;
    }
}
