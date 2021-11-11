package cc.minetale.atom.listeners;

import cc.minetale.atom.Atom;
import cc.minetale.atom.network.Party;
import cc.minetale.atom.network.PartyInvite;
import cc.minetale.atom.network.Player;
import cc.minetale.commonlib.pigeon.payloads.party.*;
import cc.minetale.commonlib.util.MC;
import cc.minetale.pigeon.annotations.PayloadHandler;
import cc.minetale.pigeon.annotations.PayloadListener;
import cc.minetale.pigeon.listeners.Listener;
import net.kyori.adventure.text.Component;

import java.util.UUID;

@PayloadListener
public class PartyListener implements Listener {

    @PayloadHandler
    public void onPartyChat(PartyChatPayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        Player initiator = Player.getPlayer(initiatorUUID);

        if (initiator == null) {
            Player.sendMessage(initiatorUUID,
                    MC.component("An error has occurred, please try rejoining the network.", MC.CC.RED));
            return;
        }

        Party party = Party.getPartyByMember(initiatorUUID);

        if(party == null) {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("You are not in a party. Consider creating one.", MC.CC.RED));
        } else {
            party.sendPartyMessage(initiatorUUID, payload.getMessage());
        }
    }

    @PayloadHandler
    public void onPartyDisband(PartyDisbandPayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        Player initiator = Player.getPlayer(initiatorUUID);

        if (initiator == null) {
            Player.sendMessage(initiatorUUID,
                    MC.component("An error has occurred, please try rejoining the network.", MC.CC.RED));
            return;
        }

        Party party = Party.getPartyByMember(initiatorUUID);

        if(party == null) {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("You are not in a party. Consider creating one.", MC.CC.RED));
            return;
        } else {
            if(!party.getLeader().equals(initiator.getUuid())) {
                Player.sendNotification(initiatorUUID, "Party",
                        MC.component("You are not the leader of the party.", MC.CC.RED));
                return;
            }
        }

        party.disbandParty("The party has been disbanded by the leader.");
    }

    @PayloadHandler
    public void onPartyInvite(PartyInvitePayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        UUID targetUUID = payload.getTarget();
        Player initiator = Player.getPlayer(initiatorUUID);
        Player target = Player.getPlayer(targetUUID);

        if (initiator == null) {
            Player.sendMessage(initiatorUUID,
                    MC.component("An error has occurred, please try rejoining the network.", MC.CC.RED));
            return;
        }

        if (target == null) {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("That player is currently not online.", MC.CC.RED));
            return;
        }

        Party party = Party.getPartyByMember(initiatorUUID);

        if(party == null) {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("You've created a new party!", MC.CC.GRAY.getTextColor()));
            party = new Party(initiatorUUID);
        }

        if (!party.getLeader().equals(initiator.getUuid())) {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("You must be the leader to invite players.", MC.CC.RED));
            return;
        }

        if(party.getMembers().contains(targetUUID)) {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("That player is already in the party.", MC.CC.RED));
            return;
        }

        if(target.getPartyInvite(party.getUuid()) != null) {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("That player already has a pending invite.", MC.CC.RED));
            return;
        }

        // TODO: Check if they have receiving party invites toggled

        target.addPartyInvite(party.getUuid(), initiatorUUID);
    }

    @PayloadHandler
    public void onPartyJoin(PartyJoinPayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        UUID targetUUID = payload.getTarget();
        Player initiator = Player.getPlayer(initiatorUUID);

        if (initiator == null) {
            Player.sendMessage(initiatorUUID,
                    MC.component("An error has occurred, please try rejoining the network.", MC.CC.RED));
            return;
        }

        if(initiator.isInParty()) {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("You are already in a party. Try leaving it first.", MC.CC.RED));
            return;
        }

        PartyInvite invite = initiator.getPlayerPartyInvite(targetUUID);

        if(invite != null) {
            Party party = Party.getPartyByUUID(invite.getPartyUUID());

            if(party != null) {
                party.addMember(initiatorUUID);
            } else {
                Player.sendNotification(initiatorUUID, "Party",
                        MC.component("The party you attempted to join has already been disbanded.", MC.CC.RED));
            }

            invite.getTimer().stop();

            initiator.getPartyInvites().remove(invite.getInviterUUID());
        } else {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("That player hasn't invited you to join their party.", MC.CC.RED));
        }
    }

    @PayloadHandler
    public void onPartyKick(PartyKickPayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        UUID targetUUID = payload.getTarget();
        Player initiator = Player.getPlayer(initiatorUUID);

        if (initiator == null) {
            Player.sendMessage(initiatorUUID,
                    MC.component("An error has occurred, please try rejoining the network.", MC.CC.RED));
            return;
        }

        Party party = Party.getPartyByMember(initiatorUUID);

        if (party != null) {
            if(!party.getLeader().equals(initiatorUUID)) {
                Player.sendMessage(initiatorUUID,
                        MC.component("Only the leader can kick party members.", MC.CC.RED));
                return;
            }

            if(targetUUID.equals(initiatorUUID)) {
                Player.sendMessage(initiatorUUID,
                        MC.component("You cannot kick yourself from the party.", MC.CC.RED));
                return;
            }

            party.removeMember(targetUUID);

            Atom.getAtom().getPlayerManager()
                    .getProfile(targetUUID)
                    .thenAccept(profile -> {
                        if(profile == null) { return; }
                        party.sendPartyMessage(MC.component()
                                .append(profile.api().getChatFormat())
                                .append(MC.component(" has been kicked from the party.", MC.CC.RED))
                                .build());
                    });

            Player target = Player.getPlayer(targetUUID);

            if(target != null) {
                Player.sendNotification(targetUUID, "Party",
                        MC.component("You were kicked from the party.", MC.CC.RED));
            }
        } else {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("You are not in a party. Consider creating one.", MC.CC.RED));
        }
    }

    @PayloadHandler
    public void onPartyLeave(PartyLeavePayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        Player initiator = Player.getPlayer(initiatorUUID);

        if (initiator == null) {
            Player.sendMessage(initiatorUUID,
                    MC.component("An error has occurred, please try rejoining the network.", MC.CC.RED));
            return;
        }

        Party party = Party.getPartyByMember(initiatorUUID);

        if(party != null) {
            if(party.getLeader().equals(initiatorUUID)) {
                Player.sendNotification(initiatorUUID, "Party",
                        MC.component("You cannot leave your own party. Try disbanding it or promoting someone.", MC.CC.RED));
                return;
            }

            party.removeMember(initiatorUUID);

            Atom.getAtom().getPlayerManager()
                    .getProfile(initiatorUUID)
                    .thenAccept(profile -> {
                        if(profile == null) { return; }
                        party.sendPartyMessage(MC.component()
                                .append(profile.api().getChatFormat())
                                .append(MC.component(" has left the party.", MC.CC.RED))
                                .build());
                    });


            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("You left the party.", MC.CC.RED));
        } else {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("You are not in a party. Consider creating one.", MC.CC.RED));
        }

    }

//    @PayloadHandler
//    public void onPartyRequest(PartyRequestPayload payload) {
//        // TODO: WIP
//    }

    @PayloadHandler
    public void onPartySummon(PartySummonPayload payload) {
        // TODO: WIP
        // TODO: ProxySendAllPayload -> You are being sent to your party leaders server.
        // TODO: Message initiator   -> You have warped your party to your current server.
    }

    @PayloadHandler
    public void onPartyTransfer(PartyTransferPayload payload) {
        UUID initiatorUUID = payload.getInitiator();
        UUID targetUUID = payload.getTarget();
        Player initiator = Player.getPlayer(initiatorUUID);

        if (initiator == null) {
            Player.sendMessage(initiatorUUID,
                    MC.component("An error has occurred, please try rejoining the network.", MC.CC.RED));
            return;
        }

        Party party = Party.getPartyByMember(initiatorUUID);

        if(party != null) {

            if(party.getLeader().equals(initiatorUUID)) {
                Atom.getAtom().getPlayerManager()
                        .getProfile(targetUUID)
                        .thenAccept(profile -> {
                            if(profile == null) { return; }
                            party.sendPartyMessage(MC.component()
                                    .append(MC.component("The party has been transferred to ", MC.CC.RED))
                                    .append(profile.api().getChatFormat())
                                    .build());
                        });

                party.setLeader(targetUUID);
            } else {
                Player.sendNotification(initiatorUUID, "Party",
                        MC.component("You are not the leader of the party.", MC.CC.RED));
            }
        } else {
            Player.sendNotification(initiatorUUID, "Party",
                    MC.component("You are not in a party. Consider creating one.", MC.CC.RED));
        }
    }

}
