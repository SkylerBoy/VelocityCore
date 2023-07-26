package es.virtualplanet.velocitycore.listener.event.staff;

import es.virtualplanet.velocitycore.user.staff.StaffPlayer;
import lombok.Getter;

@Getter
public class StaffAuthenticateEvent {

    private final StaffPlayer staffPlayer;

    public StaffAuthenticateEvent(StaffPlayer staffPlayer) {
        this.staffPlayer = staffPlayer;
    }
}
