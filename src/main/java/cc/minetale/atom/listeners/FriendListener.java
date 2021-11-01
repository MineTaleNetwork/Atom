package cc.minetale.atom.listeners;

import cc.minetale.atom.Atom;
import cc.minetale.atom.managers.PlayerManager;
import cc.minetale.atom.network.FriendRequest;
import cc.minetale.atom.network.Player;
import cc.minetale.commonlib.pigeon.payloads.friend.FriendRequestAcceptPayload;
import cc.minetale.commonlib.pigeon.payloads.friend.FriendRequestCreatePayload;
import cc.minetale.commonlib.pigeon.payloads.friend.FriendRequestDenyPayload;
import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.util.MC;
import cc.minetale.pigeon.annotations.PayloadHandler;
import cc.minetale.pigeon.annotations.PayloadListener;
import cc.minetale.pigeon.listeners.Listener;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@PayloadListener
public class FriendListener implements Listener {

    @PayloadHandler
    public void onFriendRequestCreate(FriendRequestCreatePayload payload) {
        FriendRequest friendRequest = new FriendRequest(payload.getInitiator(), payload.getTarget());

        UUID initiatorUUID = payload.getInitiator();

        if (friendRequest.isActiveRequest()) {
            Player.sendMessage(initiatorUUID,
                    MC.component("Please wait before sending this person another request.", MC.CC.RED));
            return;
        }

        friendRequest.getTimer().start();
        FriendRequest.friendRequestList.add(friendRequest);

        Atom.getAtom().getPlayerManager()
                .getProfile(payload.getTarget())
                .thenAccept(profile -> {
                    Player.sendMessage(initiatorUUID,
                            MC.component("You sent a friend request to " + profile.getName() + ", they have 5 minutes to accept it!", MC.CC.YELLOW.getTextColor()));
                });
     }

    @PayloadHandler
    public void onFriendRequestAccept(FriendRequestAcceptPayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        UUID targetUUID = payload.getTarget();

        FriendRequest friendRequest = FriendRequest.getRequestByUuid(targetUUID, initiatorUUID);

        if (friendRequest == null) {
            Player.sendMessage(initiatorUUID,
                    MC.component("That person hasn't invited you to be friends!", MC.CC.RED));
            return;
        }

        friendRequest.getTimer().stop();
        FriendRequest.removeRequests(initiatorUUID, targetUUID);
        friendRequest.accept();
    }

    @PayloadHandler
    public void onFriendRequestDeny(FriendRequestDenyPayload payload) {
        new Thread(() -> {
            try {
                final PlayerManager playerManager = Atom.getAtom().getPlayerManager();

                UUID initiatorUUID = payload.getInitiator();
                UUID targetUUID = payload.getTarget();

                FriendRequest friendRequest = FriendRequest.getRequestByUuid(targetUUID, initiatorUUID);

                if (friendRequest == null) {
                    Player.sendMessage(initiatorUUID,
                            MC.component("That player hasn't invite you to be friends.", MC.CC.RED));
                    return;
                }

                Profile targetProfile = playerManager.getProfile(targetUUID).get();

                if(targetProfile != null) {
                    Player.sendMessage(initiatorUUID,
                            MC.component("You denied " + targetProfile.getName() + "'s friend request.", MC.CC.RED));
                }

                Profile initiatorProfile = playerManager.getProfile(initiatorUUID).get();

                if(initiatorProfile != null) {
                    Player.sendMessage(targetUUID,
                            MC.component(initiatorProfile.getName() + " denied your friend request.", MC.CC.RED));
                }

                friendRequest.getTimer().stop();
                FriendRequest.removeRequests(initiatorUUID, targetUUID);
            } catch(ExecutionException | InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }).start();
    }

}
