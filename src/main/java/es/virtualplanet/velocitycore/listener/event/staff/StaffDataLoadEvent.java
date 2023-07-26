package es.virtualplanet.velocitycore.listener.event.staff;

import es.virtualplanet.velocitycore.user.staff.StaffPlayer;
import lombok.Getter;

@Getter
public class StaffDataLoadEvent  {

    private final StaffPlayer staffPlayer;

    public StaffDataLoadEvent(StaffPlayer staffPlayer) {
        this.staffPlayer = staffPlayer;
    }
}