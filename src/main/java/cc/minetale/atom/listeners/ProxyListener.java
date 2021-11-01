package cc.minetale.atom.listeners;

import cc.minetale.atom.Atom;
import cc.minetale.atom.network.Party;
import cc.minetale.atom.network.Player;
import cc.minetale.atom.timers.PartyPlayerOfflineTimer;
import cc.minetale.atom.util.RankUtil;
import cc.minetale.commonlib.network.server.Server;
import cc.minetale.commonlib.pigeon.payloads.atom.AtomPlayerCountRequestPayload;
import cc.minetale.commonlib.pigeon.payloads.minecraft.MessagePlayerPayload;
import cc.minetale.commonlib.pigeon.payloads.network.*;
import cc.minetale.commonlib.util.PigeonUtil;
import cc.minetale.pigeon.annotations.PayloadHandler;
import cc.minetale.pigeon.annotations.PayloadListener;
import cc.minetale.pigeon.feedback.RequiredState;
import cc.minetale.pigeon.listeners.Listener;

import java.util.UUID;

@PayloadListener
public class ProxyListener implements Listener {

    @PayloadHandler(requiredState = RequiredState.REQUEST)
    public void onPlayerCountRequest(AtomPlayerCountRequestPayload payload) {
        payload.sendResponse(new AtomPlayerCountRequestPayload(Server.getOnlinePlayerCount()));
    }

    @PayloadHandler
    public void onServerUpdate(ServerUpdatePayload payload) {
        Server server = payload.getServer();

        switch (payload.getAction()) {
            case ADD -> {
                PigeonUtil.broadcast(new ServerOnlinePayload(server.getName()));
                server.updateServer();
            }
            case DELETE -> {
                Server.getServerList()
                        .removeIf(filter -> filter.getName().equals(server.getName()));
                PigeonUtil.broadcast(new ServerOfflinePayload(server.getName()));
            }
            case STATE_CHANGE -> {
                Server foundServer = Server.getServerByName(server.getName());
                if (foundServer == null)
                    PigeonUtil.broadcast(new ServerOnlinePayload(server.getName()));
                server.updateServer();
            }
        }
    }

    @PayloadHandler
    public void onProxyPlayerConnect(ProxyPlayerConnectPayload payload) {
        UUID uuid = payload.getUuid();
        Player player = Player.getPlayer(payload.getUuid());

        if (player == null) {
            Atom.getAtom().getPlayerManager().getPlayer()
            player = new Player(payload.getUuid(), payload.getPlayer(), payload.getServer());
        }

        PartyPlayerOfflineTimer playerOfflineTimer = player.getPartyPlayerOfflineTimer();

        if (playerOfflineTimer != null) {
            playerOfflineTimer.stop();
        }

        Atom.getAtom().getPlayerManager()
                .getProfile(uuid)
                .thenAccept(profile -> {
                    if(profile == null) { return; }
                    // TODO: PERMS
                    if (RankUtil.hasMinimumRank(profile, "Helper")) {
                        PigeonUtil.broadcast(new MessagePlayerPayload("flame.staff", MC.component()
                                .append(profile.api().getChatFormat())
                                .append(MC.component(" has connected to ", MC.CC.GRAY.getTextColor()))
                                .append(MC.component(payload.getServer(), MC.CC.WHITE.getTextColor()))
                                .build()));
                    }
                });

        Player.getCachedPlayerList().put(uuid, player);
    }

    @PayloadHandler
    public void onProxyPlayerSwitch(ProxyPlayerSwitchPayload payload) {
        UUID uuid = payload.getUuid();
        Player player = Player.getPlayer(payload.getUuid());

        if(player == null) {
            player = new Player(payload.getUuid(), payload.getPlayer());
        }

        PartyPlayerOfflineTimer playerOfflineTimer = player.getPartyPlayerOfflineTimer();

        if (playerOfflineTimer != null) {
            playerOfflineTimer.stop();
        }

        Atom.getAtom().getPlayerManager()
                .getProfile(uuid)
                .thenAccept(profile -> {
                    if (profile == null) { return; }
                    // TODO: PERMS
//                    if (profile.api().getAllPermissions().contains("flame.staff")) {
//                        PigeonUtil.broadcast(new MessagePlayerPayload("flame.staff", MC.component()
//                                .append(profile.api().getChatFormat())
//                                .append(MC.component(" has switched to ", MC.CC.GRAY.getTextColor()))
//                                .append(MC.component(payload.getServerTo(), MC.CC.WHITE.getTextColor()))
//                                .append(MC.component(" from ", MC.CC.GRAY.getTextColor()))
//                                .append(MC.component(payload.getServerFrom(), MC.CC.WHITE.getTextColor()))
//                                .build()));
//                    }
                });

        player.setCurrentServer(payload.getServerTo());

        Player.getCachedPlayerList().put(uuid, player);
    }

    @PayloadHandler
    public void onProxyDisconnect(ProxyPlayerDisconnectPayload payload) {
        Player player = Player.getPlayer(payload.getUuid());
        Player.getCachedPlayerList().remove(payload.getUuid());

        if(player != null) {
            Party party = Party.getPartyByMember(payload.getUuid());

            if(party != null) {
                PartyPlayerOfflineTimer timer = new PartyPlayerOfflineTimer(payload.getUuid());
                timer.start();
            }
        }

        Atom.getAtom()
                .getPlayerManager()
                .getProfile(payload.getUuid())
                .thenAccept(profile -> {
                    if (profile == null) { return; }
                    // TODO: PERMS
//                    if (profile.api().getAllPermissions().contains("flame.staff")) {
//                        PigeonUtil.broadcast(new MessagePlayerPayload("flame.staff", MC.component()
//                                .append(profile.api().getChatFormat())
//                                .append(MC.component(" disconnected from ", MC.CC.GRAY.getTextColor()))
//                                .append(MC.component(payload.getServer(), MC.CC.WHITE.getTextColor()))
//                                .build()));
//                    }
                });

    }

}
