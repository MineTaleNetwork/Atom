package cc.minetale.atom.timers;

import cc.minetale.atom.Atom;
import cc.minetale.atom.managers.PlayerManager;
import cc.minetale.atom.network.Party;
import cc.minetale.atom.network.PartyInvite;
import cc.minetale.atom.network.Player;
import cc.minetale.atom.util.timer.api.Timer;
import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.util.MC;
import lombok.Getter;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class PartyInviteTimer extends Timer {

    private final UUID partyUUID;
    private final UUID playerUUID;

    public PartyInviteTimer(UUID partyUUID, UUID playerUUID) {
        super(Atom.getAtom().getTimerManager());
        setDuration(TimeUnit.MINUTES.toMillis(1));

        this.partyUUID = partyUUID;
        this.playerUUID = playerUUID;
    }

    @Override
    public void onSecond() {}

    @Override
    public void onStart() {}

    @Override
    public void onComplete() {
        Player.getPlayer(this.playerUUID).thenAccept(player -> {
            var party = Party.getPartyByUUID(this.partyUUID);

            if(party != null) {
                party.sendPartyMessage(
                        MC.component(
                                MC.component("The party invite to ", MC.CC.GRAY),
                                player.getProfile().api().getChatFormat(),
                                MC.component(" has expired.", MC.CC.GRAY)
                        ));
            }

            var invite = player.getPartyInvite(this.partyUUID);

            if(invite != null) {
                Player.getPlayer(invite.getInviterUUID()).thenAccept(inviter -> inviter.sendNotification("Party", MC.component(
                        MC.component("The party invite from ", MC.CC.GRAY),
                        inviter.getProfile().api().getChatFormat(),
                        MC.component(" has expired.", MC.CC.GRAY)
                )));

                player.getPartyInvites().remove(invite.getInviterUUID());
            }
        });
    }

    @Override
    public void onCancel() {}

}
