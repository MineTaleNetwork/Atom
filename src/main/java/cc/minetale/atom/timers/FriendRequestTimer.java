package cc.minetale.atom.timers;

import cc.minetale.atom.Atom;
import cc.minetale.atom.network.FriendRequest;
import cc.minetale.atom.network.Player;
import cc.minetale.atom.util.timer.api.Timer;
import cc.minetale.atom.util.timer.api.TimerType;
import cc.minetale.commonlib.util.MC;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FriendRequestTimer extends Timer {

    public final FriendRequest friendRequest;

    public FriendRequestTimer(FriendRequest friendRequest) {
        super(TimerType.COUNTDOWN, Atom.getAtom().getTimerManager());
        setDuration(TimeUnit.MINUTES.toMillis(5));
        this.friendRequest = friendRequest;
    }

    @Override
    public void onSecond() {}

    @Override
    public void onStart() {}

    @Override
    public void onComplete() {
        List<FriendRequest> friendRequestList = FriendRequest.friendRequestList;

        Atom.getAtom().getPlayerManager()
                .getProfile(this.friendRequest.getTarget())
                .thenAccept(profile -> {
                    Player.sendMessage(this.friendRequest.getInitiator(),
                            MC.component("Your friend request to " + profile.getName() + " has expired.", MC.CC.RED));
                });

        Atom.getAtom().getPlayerManager()
                .getProfile(this.friendRequest.getInitiator())
                .thenAccept(profile -> {
                    Player.sendMessage(this.friendRequest.getTarget(),
                        MC.component("The friend request from " + profile.getName() + " has expired.", MC.CC.RED));
                });

        friendRequestList.remove(this.friendRequest);
    }

    @Override
    public void onCancel() {}

}
