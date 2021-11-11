package cc.minetale.atom.listeners;

import cc.minetale.atom.Atom;
import cc.minetale.atom.managers.PlayerManager;
import cc.minetale.atom.network.Player;
import cc.minetale.commonlib.pigeon.payloads.conversation.ConversationMessagePayload;
import cc.minetale.commonlib.pigeon.payloads.conversation.ConversationReplyPayload;
import cc.minetale.commonlib.pigeon.payloads.minecraft.CommonMessagePayload;
import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.util.MC;
import cc.minetale.pigeon.annotations.PayloadHandler;
import cc.minetale.pigeon.annotations.PayloadListener;
import cc.minetale.pigeon.listeners.Listener;

import java.util.UUID;

@PayloadListener
public class ConversationListener implements Listener {

    @PayloadHandler
    public void onConversationMessage(ConversationMessagePayload payload) {
        Player.getPlayer(payload.getInitiator()).thenAccept(initiator -> Player.getPlayer(payload.getTarget()).thenAccept(target -> {
            String message = payload.getMessage();

            if(!target.isOnline()) {
                initiator.sendMessage(MC.component("That player is currently not online.", MC.CC.RED));
                return;
            }

            initiator.sendConversationMessage(target, message);
        }));
    }

    @PayloadHandler
    public void onConversationReply(ConversationReplyPayload payload) {
                final PlayerManager playerManager = Atom.getAtom().getPlayerManager();

                UUID initiatorUUID = payload.getInitiator();
                Player initiator = Player.getPlayer(initiatorUUID);
                String message = payload.getMessage();

                if(initiator == null) {
                    Player.sendMessage(initiatorUUID, MC.component("An error has occurred, please try rejoining the network.", MC.CC.RED));
                    return;
                }

                Player lastMessaged = initiator.getLastMessaged();

                if(lastMessaged == null) {
                    Player.sendMessage(initiatorUUID, MC.component("You haven't started a conversation with anybody.", MC.CC.RED));
                    return;
                }

                if(Player.getPlayer(lastMessaged.getUuid()) == null) {
                    Player.sendMessage(initiatorUUID, MC.component("That player is currently not online.", MC.CC.RED));
                    return;
                }

                Profile targetProfile = playerManager.getProfile(lastMessaged.getUuid()).get();

                if(targetProfile == null) {
                    Player.sendMessage(initiatorUUID, MC.component("Could not resolve player information.", MC.CC.RED));
                    return;
                }

                Profile initiatorProfile = playerManager.getProfile(initiator.getUuid()).get();

                if(initiatorProfile == null) {
                    Player.sendMessage(initiatorUUID, MC.component("Failed to load your profile. Try again later.", MC.CC.RED));
                    return;
                }

                if(targetProfile.api().isIgnoring(initiatorProfile) || !targetProfile.getOptionsProfile().isReceivingConversations()) {
                    Player.sendMessage(initiatorUUID, MC.component("That player is not receiving new conversations right now.", MC.CC.RED));
                    return;
                }

                initiator.reply(message);

    }

}
