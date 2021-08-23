package cc.minetale.atom.listeners;

import cc.minetale.atom.Atom;
import cc.minetale.atom.managers.ProfilesManager;
import cc.minetale.atom.network.FriendRequest;
import cc.minetale.atom.network.Player;
import cc.minetale.commonlib.modules.pigeon.payloads.friend.FriendRequestAcceptPayload;
import cc.minetale.commonlib.modules.pigeon.payloads.friend.FriendRequestCreatePayload;
import cc.minetale.commonlib.modules.pigeon.payloads.friend.FriendRequestDenyPayload;
import cc.minetale.commonlib.modules.profile.Profile;
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
                    Component.text("Please wait before sending this person another request.", MC.CC.RED.getTextColor()));
            return;
        }

        friendRequest.getTimer().start();
        FriendRequest.friendRequestList.add(friendRequest);

        Atom.getAtom().getProfilesManager()
                .getProfile(payload.getTarget())
                .thenAccept(profile -> {
                    Player.sendMessage(initiatorUUID,
                            Component.text("You sent a friend request to " + profile.getName() + ", they have 5 minutes to accept it!", MC.CC.YELLOW.getTextColor()));
                });
     }

    @PayloadHandler
    public void onFriendRequestAccept(FriendRequestAcceptPayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        UUID targetUUID = payload.getTarget();

        FriendRequest friendRequest = FriendRequest.getRequestByUuid(targetUUID, initiatorUUID);

        if (friendRequest == null) {
            Player.sendMessage(initiatorUUID,
                    Component.text("That person hasn't invited you to be friends!", MC.CC.RED.getTextColor()));
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
                final ProfilesManager profilesManager = Atom.getAtom().getProfilesManager();

                UUID initiatorUUID = payload.getInitiator();
                UUID targetUUID = payload.getTarget();

                FriendRequest friendRequest = FriendRequest.getRequestByUuid(targetUUID, initiatorUUID);

                if (friendRequest == null) {
                    Player.sendMessage(initiatorUUID,
                            Component.text("That player hasn't invite you to be friends.", MC.CC.RED.getTextColor()));
                    return;
                }

                Profile targetProfile = profilesManager.getProfile(targetUUID).get();

                if(targetProfile != null) {
                    Player.sendMessage(initiatorUUID,
                            Component.text("You denied " + targetProfile.getName() + "'s friend request.", MC.CC.RED.getTextColor()));
                }

                Profile initiatorProfile = profilesManager.getProfile(initiatorUUID).get();

                if(initiatorProfile != null) {
                    Player.sendMessage(targetUUID,
                            Component.text(initiatorProfile.getName() + " denied your friend request.", MC.CC.RED.getTextColor()));
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
