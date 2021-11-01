package cc.minetale.atom.network;

import cc.minetale.atom.Atom;
import cc.minetale.atom.managers.PlayerManager;
import cc.minetale.atom.timers.PartyPlayerOfflineTimer;
import cc.minetale.commonlib.pigeon.payloads.conversation.ConversationFromPayload;
import cc.minetale.commonlib.pigeon.payloads.conversation.ConversationToPayload;
import cc.minetale.commonlib.pigeon.payloads.minecraft.MessagePlayerPayload;
import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.util.MC;
import cc.minetale.commonlib.util.PigeonUtil;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
public class Player {

    private final UUID uuid;
    private final Map<UUID, PartyInvite> partyInvites;
    private Player lastMessaged;
    private Profile profile;

    public Player(UUID uuid) {
        this.uuid = uuid;
        this.partyInvites = new HashMap<>();
    }

    public Player(Profile profile) {
        this.uuid = profile.getId();
        this.partyInvites = new HashMap<>();

        this.profile = profile;
    }

    public static void sendNotification(UUID player, String prefix, Component message) {
        PigeonUtil.broadcast(new MessagePlayerPayload(player, MC.Chat.notificationMessage(prefix, message)));
    }

    public static void sendMessage(UUID player, Component message) {
        PigeonUtil.broadcast(new MessagePlayerPayload(player, message));
    }

    // TODO -> Fix this
    public static @NotNull CompletableFuture<Player> getPlayer(UUID uuid) {
        PlayerManager manager = Atom.getAtom().getPlayerManager();
        Player player = manager.getCache().get(uuid);

        if (player == null) {
            player = new Player(uuid);

            final Player finalPlayer = player;

            return manager.getProfile(uuid).thenCompose(profile -> {
                finalPlayer.setProfile(profile);
                return CompletableFuture.completedFuture(finalPlayer);
            });
        }

        return CompletableFuture.completedFuture(player);
    }

    public PartyInvite getPartyInvite(UUID partyUUID) {
        for (PartyInvite invite : this.partyInvites.values()) {
            if (invite.getPartyUUID().equals(partyUUID)) {
                return invite;
            }
        }

        return null;
    }

    public PartyInvite getPlayerPartyInvite(UUID playerUUID) {
        return this.partyInvites.get(playerUUID);
    }

    public boolean isInParty() {
        for (Party party : Party.getPartyList()) {
            if (party.getMembers().contains(this.getUuid())) {
                return true;
            }
        }

        return false;
    }

    public void addPartyInvite(UUID partyUUID, UUID inviterUUID) {
        // TODO: Check if they have receiving party invites toggled
        final PlayerManager playerManager = Atom.getAtom().getPlayerManager();

        Profile initiatorProfile = playerManager.getProfile(inviterUUID).get(10, TimeUnit.SECONDS);
        Profile targetProfile = playerManager.getProfile(this.getUuid()).get(10, TimeUnit.SECONDS);

        if (initiatorProfile == null || targetProfile == null) {
            PigeonUtil.broadcast(new MessagePlayerPayload(inviterUUID, MC.Chat.notificationMessage("Party",
                    MC.component("Unable to invite that player to the party.", MC.CC.GRAY.getTextColor()))));

            return;
        }

        Party party = Party.getPartyByUUID(partyUUID);

        if (party != null) {
            party.sendPartyMessage(MC.component()
                    .append(initiatorProfile.api().getChatFormat())
                    .append(MC.component(" has invited ", MC.CC.GRAY.getTextColor()))
                    .append(targetProfile.api().getChatFormat())
                    .append(MC.component(" to the party!", MC.CC.GRAY.getTextColor()))
                    .build());
        }

        Player.sendNotification(this.getUuid(), "Party", MC.component()
                .append(initiatorProfile.api().getChatFormat())
                .append(MC.component(" has invited you to join their party. You have 60 seconds to accept.", MC.CC.GRAY.getTextColor()))
                .build());

        this.partyInvites.put(inviterUUID, new PartyInvite(partyUUID, inviterUUID, this.getUuid()));
    }

    public void sendConversationMessage(Player target, String message) {
        PigeonUtil.broadcast(new ConversationToPayload(this.getUuid(), target.getUuid(), message));
        PigeonUtil.broadcast(new ConversationFromPayload(this.getUuid(), target.getUuid(), message));

        this.lastMessaged = target;
        target.setLastMessaged(this);
    }

    // TODO -> This could fuck up?
    public void reply(String message) {
        PigeonUtil.broadcast(new ConversationToPayload(this.getUuid(), this.lastMessaged.getUuid(), message));
        PigeonUtil.broadcast(new ConversationFromPayload(this.getUuid(), this.lastMessaged.getUuid(), message));

        this.lastMessaged.setLastMessaged(this);
    }

    public PartyPlayerOfflineTimer getPartyPlayerOfflineTimer() {
        return (PartyPlayerOfflineTimer) Atom.getAtom().getTimerManager()
                .getTimers()
                .stream()
                .filter(timer -> timer instanceof PartyPlayerOfflineTimer && ((PartyPlayerOfflineTimer) timer).getPlayer()
                        .equals(this.getUuid()))
                .findFirst().orElse(null);
    }

    public UUID getUuid() {
        return this.profile.getId();
    }

    public String getName() {
        return this.profile.getName();
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Player && ((Player) object).getUuid().equals(this.getUuid());
    }

}
