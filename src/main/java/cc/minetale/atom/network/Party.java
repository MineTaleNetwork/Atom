package cc.minetale.atom.network;

import cc.minetale.atom.Atom;
import cc.minetale.commonlib.modules.pigeon.payloads.minecraft.MessagePlayerPayload;
import cc.minetale.commonlib.util.MC;
import cc.minetale.commonlib.util.PigeonUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
public class Party {

    @Getter private static final List<Party> partyList = new ArrayList<>();

    private final UUID uuid;
    private final List<UUID> members;
    @Setter private UUID leader;
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

    public void addMember(UUID player) {
        if (this.members.contains(player)) {
            Player.sendNotification(player, "Party", Component.text("You are already in that party.", MC.CC.RED.getTextColor()));
            return;
        }

        this.members.add(player);

        Atom.getAtom().getProfilesManager()
                .getProfile(player)
                .thenAccept(profile -> {
                    if (profile == null) { return; }
                    this.sendPartyMessage(Component.text()
                            .append(profile.api().getChatFormat())
                            .append(Component.text(" has joined the party.", MC.CC.GREEN.getTextColor()))
                            .build());
                });


    }

    public void removeMember(UUID uuid) {
        this.getMembers().remove(uuid);
    }

    public void sendPartyMessage(UUID initiator, String message) {
        Atom.getAtom().getProfilesManager()
                .getProfile(initiator)
                .thenAccept(profile -> {
                    if (profile == null) { return; }
                    for (UUID uuid : this.getMembers()) {
                        PigeonUtil.broadcast(new MessagePlayerPayload(uuid, MC.Chat.notificationMessage("Party",
                                Component.text()
                                        .append(profile.api().getChatFormat())
                                        .append(Component.text(": ", MC.CC.GRAY.getTextColor()))
                                        .append(Component.text(message))
                                        .build())));
                    }
                });
    }

    public void sendPartyMessage(Component message) {
        for (UUID uuid : this.getMembers()) {
            Player.sendNotification(uuid, "Party", message);
        }
    }

    public void disbandParty(String reason) {
        this.sendPartyMessage(Component.text(reason, MC.CC.RED.getTextColor()));

        partyList.remove(this);
    }

}
