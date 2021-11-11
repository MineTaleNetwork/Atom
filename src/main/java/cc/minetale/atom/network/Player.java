package cc.minetale.atom.network;

import cc.minetale.atom.Atom;
import cc.minetale.atom.managers.PlayerManager;
import cc.minetale.atom.timers.PartyOfflineTimer;
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

@Getter @Setter
public class Player {

    private final UUID uuid;
    private final Map<UUID, PartyInvite> partyInvites;
    private boolean online;
    private String server;
    private Player lastMessaged;
    private Profile profile;

    public Player(UUID uuid) {
        this.uuid = uuid;
        this.online = false;
        this.partyInvites = new HashMap<>();
    }

    public Player(Profile profile) {
        this.uuid = profile.getId();
        this.partyInvites = new HashMap<>();
        this.online = false;
        this.profile = profile;
    }

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

    public UUID getUuid() {
        return this.profile.getId();
    }

    public String getName() {
        return this.profile.getName();
    }

    public void sendNotification(String prefix, Component message) {
        this.sendMessage(MC.Chat.notificationMessage(prefix, message));
    }

    public void sendMessage(Component message) {
        PigeonUtil.broadcast(new MessagePlayerPayload(this.getUuid(), message));
    }

    // TODO: Optimize?
    public boolean isInParty() {
        for (Party party : Party.getPartyList()) {
            if (party.getMembers().contains(this.getUuid())) {
                return true;
            }
        }

        return false;
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

    public void addPartyInvite(UUID partyUUID, UUID inviterUUID) {
        Player.getPlayer(inviterUUID).thenAccept(inviter -> {
            var inviterFormat = inviter.getProfile().api().getChatFormat();

            var party = Party.getPartyByUUID(partyUUID);

            if (party != null) {
                party.sendPartyMessage(MC.component(
                        inviterFormat,
                        MC.component(" has invited ", MC.CC.GRAY),
                        this.profile.api().getChatFormat(),
                        MC.component(" to the party!", MC.CC.GRAY)
                ));

                this.sendNotification("Party", MC.component(
                        inviterFormat,
                        MC.component(" has invited you to join their party. You have 60 seconds to accept.", MC.CC.GRAY)
                ));

                this.partyInvites.put(inviterUUID, new PartyInvite(partyUUID, inviterUUID, this.getUuid()));
            }
        });
    }

    public void sendConversationMessage(Player target, String message) {
        PigeonUtil.broadcast(new ConversationToPayload(this.getUuid(), target.getUuid(), message));
        PigeonUtil.broadcast(new ConversationFromPayload(this.getUuid(), target.getUuid(), message));

        this.lastMessaged = target;
        target.setLastMessaged(this);
    }

    // TODO -> Maybe condense these two payloads?
    public void reply(String message) {
        if(this.lastMessaged != null) {
            PigeonUtil.broadcast(new ConversationToPayload(this.getUuid(), this.lastMessaged.getUuid(), message));
            PigeonUtil.broadcast(new ConversationFromPayload(this.getUuid(), this.lastMessaged.getUuid(), message));

            this.lastMessaged.setLastMessaged(this);
        }
    }

    public PartyOfflineTimer getPartyOfflineTimer() {
        return (PartyOfflineTimer) Atom.getAtom().getTimerManager()
                .getTimers()
                .stream()
                .filter(timerType -> timerType instanceof PartyOfflineTimer timer && timer.getPlayerUUID().equals(this.getUuid()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof Player && ((Player) object).getUuid().equals(this.getUuid());
    }

}
