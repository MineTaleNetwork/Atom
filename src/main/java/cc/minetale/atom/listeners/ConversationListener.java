package cc.minetale.atom.listeners;

import cc.minetale.atom.Atom;
import cc.minetale.atom.managers.ProfilesManager;
import cc.minetale.atom.network.Player;
import cc.minetale.commonlib.modules.pigeon.payloads.conversation.ConversationMessagePayload;
import cc.minetale.commonlib.modules.pigeon.payloads.conversation.ConversationReplyPayload;
import cc.minetale.commonlib.modules.profile.Profile;
import cc.minetale.commonlib.util.MC;
import cc.minetale.pigeon.annotations.PayloadHandler;
import cc.minetale.pigeon.annotations.PayloadListener;
import cc.minetale.pigeon.listeners.Listener;
import net.kyori.adventure.text.Component;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

@PayloadListener
public class ConversationListener implements Listener {

    @PayloadHandler
    public void onConversationMessage(ConversationMessagePayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        Player initiator = Player.getPlayerByUuid(initiatorUUID);
        Player target = Player.getPlayerByUuid(payload.getTarget());

        String message = payload.getMessage();

        if (initiator == null) {
            Player.sendMessage(initiatorUUID,
                    Component.text("An error has occurred, please try rejoining the network.", MC.CC.RED.getTextColor()));
            return;
        }

        if (target == null) {
            Player.sendMessage(initiatorUUID,
                    Component.text("That player is currently not online.", MC.CC.RED.getTextColor()));
            return;
        }

        initiator.sendConversationMessage(target, message);
    }

    @PayloadHandler
    public void onConversationReply(ConversationReplyPayload payload) {
        new Thread(() -> {
            try {
                final ProfilesManager profilesManager = Atom.getAtom().getProfilesManager();

                UUID initiatorUUID = payload.getInitiator();
                Player initiator = Player.getPlayerByUuid(initiatorUUID);
                String message = payload.getMessage();

                if(initiator == null) {
                    Player.sendMessage(initiatorUUID,
                            Component.text("An error has occurred, please try rejoining the network.", MC.CC.RED.getTextColor()));
                    return;
                }

                Player lastMessaged = initiator.getLastMessaged();

                if(lastMessaged == null) {
                    Player.sendMessage(initiatorUUID,
                            Component.text("You haven't started a conversation with anybody.", MC.CC.RED.getTextColor()));
                    return;
                }

                if(Player.getPlayerByUuid(lastMessaged.getUuid()) == null) {
                    Player.sendMessage(initiatorUUID,
                            Component.text("That player is currently not online.", MC.CC.RED.getTextColor()));
                    return;
                }

                Profile targetProfile = profilesManager.getProfile(lastMessaged.getUuid()).get();

                if(targetProfile == null) {
                    Player.sendMessage(initiatorUUID,
                            Component.text("Could not resolve player information.", MC.CC.RED.getTextColor()));
                    return;
                }

                Profile initiatorProfile = profilesManager.getProfile(initiator.getUuid()).get();

                if(initiatorProfile == null) {
                    Player.sendMessage(initiatorUUID,
                            Component.text("Failed to load your profile. Try again later.", MC.CC.RED.getTextColor()));
                    return;
                }

                if(targetProfile.api().isIgnoring(initiatorProfile) || !targetProfile.getOptionsProfile().isReceivingConversations()) {
                    Player.sendMessage(initiatorUUID,
                            Component.text("That player is not receiving new conversations right now.", MC.CC.RED.getTextColor()));
                    return;
                }

                initiator.reply(message);
            } catch(ExecutionException | InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }).start();

    }

}
