package cc.minetale.atom.timers;

import cc.minetale.atom.Atom;
import cc.minetale.atom.network.Party;
import cc.minetale.atom.network.Player;
import cc.minetale.atom.util.timer.api.Timer;
import cc.minetale.commonlib.util.MC;
import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class PartyOfflineTimer extends Timer {

    private final UUID playerUUID;

    public PartyOfflineTimer(UUID playerUUID) {
        super(Atom.getAtom().getTimerManager());
        setDuration(TimeUnit.MINUTES.toMillis(5));

        this.playerUUID = playerUUID;
    }

    @Override
    public void onSecond() {}

    @Override
    public void onStart() {}

    @Override
    public void onComplete() {
        Party party = Party.getPartyByMember(this.playerUUID);

        if(party != null) {
            if(party.getLeader().equals(this.playerUUID)) {
                party.disbandParty("The party has been disbanded due to the leader being offline.");
            } else {
                party.removeMember(this.playerUUID);

                Player.getPlayer(this.playerUUID).thenAccept(player -> {
                    party.sendPartyMessage(
                            MC.component(
                                    player.getProfile().api().getChatFormat(),
                                    MC.component(" has been removed from the party for being offline.", MC.CC.RED)
                            ));
                });
            }
        }
    }

    @Override
    public void onCancel() {}

}
