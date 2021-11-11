package cc.minetale.atom.timers;

import cc.minetale.atom.Atom;
import cc.minetale.atom.network.FriendRequest;
import cc.minetale.atom.network.Player;
import cc.minetale.atom.util.timer.api.Timer;
import cc.minetale.commonlib.util.MC;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FriendRequestTimer extends Timer {

    public final FriendRequest friendRequest;

    public FriendRequestTimer(FriendRequest friendRequest) {
        super(Atom.getAtom().getTimerManager());
        setDuration(TimeUnit.MINUTES.toMillis(5));

        this.friendRequest = friendRequest;
    }

    @Override
    public void onSecond() {}

    @Override
    public void onStart() {}

    @Override
    public void onComplete() {
        Player.getPlayer(this.friendRequest.getInitiator()).thenAccept(initiator -> Player.getPlayer(this.friendRequest.getTarget()).thenAccept(target -> {
            initiator.sendMessage(MC.component("Your friend request to " + target.getName() + " has expired.", MC.CC.RED));
            target.sendMessage(MC.component("The friend request from " + initiator.getName() + " has expired.", MC.CC.RED));
        }));

        FriendRequest.getFriendRequestList().remove(this.friendRequest);
    }

    @Override
    public void onCancel() {}

}
