package cc.minetale.atom.timers;

import cc.minetale.atom.Atom;
import cc.minetale.atom.managers.PlayerManager;
import cc.minetale.atom.network.Party;
import cc.minetale.atom.network.PartyInvite;
import cc.minetale.atom.network.Player;
import cc.minetale.atom.util.timer.api.Timer;
import cc.minetale.atom.util.timer.api.TimerType;
import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.util.MC;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Getter
public class PartyInviteTimer extends Timer {

    private final UUID partyUUID;
    private final UUID playerUUID;

    public PartyInviteTimer(UUID partyUUID, UUID playerUUID) {
        super(TimerType.COUNTDOWN, Atom.getAtom().getTimerManager());
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
            try {
                final PlayerManager playerManager = Atom.getAtom().getPlayerManager();

                Party party = Party.getPartyByUUID(this.partyUUID);
                Player player = Player.getPlayer(this.playerUUID);

                Profile profile = playerManager.getProfile(this.playerUUID).get();
                if(profile == null) { return; }
                if(party != null) {
                    party.sendPartyMessage(MC.component()
                            .append(MC.component("The party invite to ", MC.CC.GRAY.getTextColor()))
                            .append(profile.api().getChatFormat())
                            .append(MC.component(" has expired.", MC.CC.GRAY.getTextColor()))
                            .build());
                }

                if(player != null) {
                    PartyInvite invite = player.getPartyInvite(this.partyUUID);

                    if(invite != null) {
                        Profile inviter = playerManager.getProfile(invite.getInviterUUID()).get();

                        Player.sendNotification(this.playerUUID, "Party", MC.component()
                                .append(MC.component("The party invite from ", MC.CC.GRAY.getTextColor()))
                                .append(inviter.api().getChatFormat())
                                .append(MC.component(" has expired.", MC.CC.GRAY.getTextColor()))
                                .build());

                        player.getPartyInvites().remove(invite.getInviterUUID());
                    }
                }
        });
    }

    @Override
    public void onCancel() {}

}
