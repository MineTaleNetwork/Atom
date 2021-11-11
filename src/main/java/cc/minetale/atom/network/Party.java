package cc.minetale.atom.network;

import cc.minetale.atom.Atom;
import cc.minetale.commonlib.util.MC;
import cc.minetale.commonlib.util.PigeonUtil;
import lombok.Getter;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class Party {

    @Getter private static final List<Party> partyList = new ArrayList<>();

    private final UUID uuid;
    private UUID leader;
    private final List<UUID> members;
    private final Long createdAt;

    public Party(UUID leader) {
        this.uuid = UUID.randomUUID();
        this.leader = leader;
        this.members = new ArrayList<>(Collections.singletonList(leader));
        this.createdAt = System.currentTimeMillis();
        partyList.add(this);
    }

    public static Party getPartyByUUID(UUID uuid) {
        return partyList.stream().filter(party -> party.getUuid().equals(uuid)).findFirst().orElse(null);
    }

    public static Party getPartyByMember(UUID uuid) {
        return partyList.stream().filter(party -> party.getMembers().contains(uuid)).findFirst().orElse(null);
    }

    // TODO -> Needs additional code (Like messages)
    public void setLeader(UUID player) {
        this.leader = player;
    }

    public void addMember(UUID playerUUID) {
        Player.getPlayer(playerUUID).thenAccept(player -> {
            if (this.members.contains(playerUUID)) {
                player.sendNotification("Party", MC.component("You are already in that party.", MC.CC.RED);
                return;
            }

            this.members.add(playerUUID);

            this.sendPartyMessage(MC.component(
                    player.getProfile().api().getChatFormat(),
                    MC.component(" has joined the party.", MC.CC.GREEN)
            ));
        });
    }

    public void removeMember(UUID uuid) {
        this.getMembers().remove(uuid);
    }

    // TODO Oh god optimize this oh shit of fuck oh shit of fuck
    public void sendPartyMessage(UUID initiator, String message) {
        // TODO -> Send a payload containing a list of UUIDs maybe?
        // TODO -> Then send a message to each uuid if they are online?
//        Atom.getAtom().getPlayerManager()
//                .getProfile(initiator)
//                .thenAccept(profile -> {
//                    if (profile == null) { return; }
//                    for (UUID uuid : this.getMembers()) {
//                        PigeonUtil.broadcast(new MessagePlayerPayload(uuid, MC.Chat.notificationMessage("Party",
//                                MC.component()
//                                        .append(profile.api().getChatFormat())
//                                        .append(MC.component(": ", MC.CC.GRAY.getTextColor()))
//                                        .append(MC.component(message))
//                                        .build())));
//                    }
//                });
    }

    public void sendPartyMessage(Component message) {
        for (UUID uuid : this.getMembers()) {
            Player.sendNotification(uuid, "Party", message);
        }
    }

    public void disbandParty(String reason) {
        this.sendPartyMessage(MC.component(reason, MC.CC.RED));

        partyList.remove(this);
    }

}
