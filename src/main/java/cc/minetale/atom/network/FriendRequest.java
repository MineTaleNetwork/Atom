package cc.minetale.atom.network;

import cc.minetale.atom.Atom;
import cc.minetale.atom.managers.PlayerManager;
import cc.minetale.atom.timers.FriendRequestTimer;
import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.util.MC;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Getter
public class FriendRequest {

    @Getter private static final List<FriendRequest> friendRequestList = new ArrayList<>();

    // TODO: Handle ignoring, check if a player has an active friend request then remove it if they ignored them.

    private final UUID initiator;
    private final UUID target;
    private final Long created;
    private final FriendRequestTimer timer;

    public FriendRequest(UUID initiator, UUID target) {
        this.initiator = initiator;
        this.target = target;
        this.created = System.currentTimeMillis();
        this.timer = new FriendRequestTimer(this);
    }

    public static FriendRequest getRequestByUuid(UUID initiator, UUID target) {
        return friendRequestList.stream().filter(request -> request.getInitiator().equals(initiator) && request.getTarget().equals(target)).findFirst().orElse(null);
    }

    public boolean isActiveRequest() {
        return friendRequestList.stream().filter(request -> request.getInitiator() == this.initiator && request.getTarget() == this.target).findFirst().orElse(null) != null;
    }

    public static void removeRequests(UUID player, UUID otherPlayer) {
        Iterator<FriendRequest> iterator = friendRequestList.iterator();

        while (iterator.hasNext()) {
            FriendRequest request = iterator.next();

            if (request.getInitiator().equals(player) && request.getTarget().equals(otherPlayer) || request.getInitiator().equals(otherPlayer) && request.getTarget().equals(player)) {
                request.getTimer().stop();
                iterator.remove();
            }
        }
    }

    public void accept() {
        final PlayerManager playerManager = Atom.getAtom().getPlayerManager();

        playerManager.getProfile(initiator).thenAccept(initiatorProfile -> playerManager.getProfile(target).thenAccept(targetProfile -> {
            Player.sendNotification(target, "Friend",
                    MC.component("You are now friends with " + initiatorProfile.getName(), MC.CC.GREEN.getTextColor()));

            Player.sendNotification(initiator, "Friend",
                    MC.component("You are now friends with " + targetProfile.getName(), MC.CC.GREEN.getTextColor()));

            initiatorProfile.getFriends().add(target);
            targetProfile.getFriends().add(initiator);

            playerManager.updateProfile(initiatorProfile);
            playerManager.updateProfile(targetProfile);
        }));
    }

}
