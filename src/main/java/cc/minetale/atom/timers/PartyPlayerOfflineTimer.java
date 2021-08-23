package cc.minetale.atom.timers;

import cc.minetale.atom.Atom;
import cc.minetale.atom.network.Party;
import cc.minetale.atom.util.timer.api.Timer;
import cc.minetale.atom.util.timer.api.TimerType;
import cc.minetale.commonlib.util.MC;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class PartyPlayerOfflineTimer extends Timer {

    private final UUID player;

    public PartyPlayerOfflineTimer(UUID player) {
        super(TimerType.COUNTDOWN, Atom.getAtom().getTimerManager());
        setDuration(TimeUnit.MINUTES.toMillis(5));
        this.player = player;
    }

    @Override
    public void onSecond() {}

    @Override
    public void onStart() {}

    @Override
    public void onComplete() {
        Party party = Party.getPartyByMember(this.player);

        if(party != null) {
            if(party.getLeader().equals(this.player)) {
                party.disbandParty("The party has been disbanded due to the leader being offline.");
            } else {
                party.removeMember(this.player);

                Atom.getAtom().getProfilesManager()
                        .getProfile(this.player)
                        .thenAccept(profile -> {
                            if(profile == null) { return; }
                            party.sendPartyMessage(Component.text()
                                    .append(profile.api().getChatFormat())
                                    .append(Component.text(" has been removed from the party for being offline.", MC.CC.RED.getTextColor()))
                                    .build());
                        });
            }
        }
    }

    @Override
    public void onCancel() {}

}
