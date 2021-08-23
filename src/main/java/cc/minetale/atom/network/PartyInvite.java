package cc.minetale.atom.network;

import cc.minetale.atom.timers.PartyInviteTimer;
import lombok.Getter;

import java.util.UUID;

@Getter
public class PartyInvite {

    private final UUID partyUUID;
    private final UUID inviterUUID;
    private final UUID targetUUID;
    private final PartyInviteTimer timer;

    public PartyInvite(UUID partyUUID, UUID inviterUUID, UUID targetUUID) {
        this.partyUUID = partyUUID;
        this.inviterUUID = inviterUUID;
        this.targetUUID = targetUUID;
        this.timer = new PartyInviteTimer(partyUUID, targetUUID);
        this.timer.start();
    }

}
