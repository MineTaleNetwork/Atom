package cc.minetale.atom.listeners;

import cc.minetale.atom.Atom;
import cc.minetale.atom.network.Party;
import cc.minetale.atom.network.Player;
import cc.minetale.atom.timers.PartyPlayerOfflineTimer;
import cc.minetale.commonlib.modules.network.Server;
import cc.minetale.commonlib.modules.network.server.Server;
import cc.minetale.commonlib.modules.pigeon.payloads.atom.AtomOnlinePlayerCountPayload;
import cc.minetale.commonlib.modules.pigeon.payloads.atom.AtomPlayerCountRequestPayload;
import cc.minetale.commonlib.modules.pigeon.payloads.network.*;
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
            case ADD:
                server.updateServer();
                PigeonUtil.broadcast(new ServerOnlinePayload(server.getName()));
                break;
            case DELETE:
                Server.getServerList().removeIf(filter -> filter.getName().equals(server.getName()));
                PigeonUtil.broadcast(new ServerOfflinePayload(server.getName()));
                break;
            case STATE_CHANGE:
                server.updateServer();
                break;
        }
    }

    @PayloadHandler
    public void onProxyPlayerConnect(ProxyPlayerConnectPayload payload) {
        UUID uuid = payload.getUuid();
        Player player = Player.getPlayerByUuid(payload.getUuid());

        if (player == null) {
            player = new Player(payload.getUuid(), payload.getPlayer(), payload.getServer());
        }

        PartyPlayerOfflineTimer playerOfflineTimer = player.getPartyPlayerOfflineTimer();

        if (playerOfflineTimer != null) {
            playerOfflineTimer.stop();
        }

        Atom.getAtom().getProfilesManager()
                .getProfile(uuid)
                .thenAccept(profile -> {
                    if(profile == null) { return; }
                    // TODO: PERMS
//                    if (profile.api().getAllPermissions().contains("flame.staff")) {
//                        PigeonUtil.broadcast(new MessagePlayerPayload("flame.staff", Component.text()
//                                .append(profile.api().getChatFormat())
//                                .append(Component.text(" has connected to ", MC.CC.GRAY.getTextColor()))
//                                .append(Component.text(payload.getServer(), MC.CC.WHITE.getTextColor()))
//                                .build()));
//                    }
                });

        Player.getCachedPlayerList().put(uuid, player);
    }

    @PayloadHandler
    public void onProxyPlayerSwitch(ProxyPlayerSwitchPayload payload) {
        UUID uuid = payload.getUuid();
        Player player = Player.getPlayerByUuid(payload.getUuid());

        if(player == null) {
            player = new Player(payload.getUuid(), payload.getPlayer(), payload.getServerTo());
        }

        PartyPlayerOfflineTimer playerOfflineTimer = player.getPartyPlayerOfflineTimer();

        if (playerOfflineTimer != null) {
            playerOfflineTimer.stop();
        }

        Atom.getAtom().getProfilesManager()
                .getProfile(uuid)
                .thenAccept(profile -> {
                    if (profile == null) { return; }
                    // TODO: PERMS
//                    if (profile.api().getAllPermissions().contains("flame.staff")) {
//                        PigeonUtil.broadcast(new MessagePlayerPayload("flame.staff", Component.text()
//                                .append(profile.api().getChatFormat())
//                                .append(Component.text(" has switched to ", MC.CC.GRAY.getTextColor()))
//                                .append(Component.text(payload.getServerTo(), MC.CC.WHITE.getTextColor()))
//                                .append(Component.text(" from ", MC.CC.GRAY.getTextColor()))
//                                .append(Component.text(payload.getServerFrom(), MC.CC.WHITE.getTextColor()))
//                                .build()));
//                    }
                });

        player.setCurrentServer(payload.getServerTo());

        Player.getCachedPlayerList().put(uuid, player);
    }

    @PayloadHandler
    public void onProxyDisconnect(ProxyPlayerDisconnectPayload payload) {
        Player player = Player.getPlayerByUuid(payload.getUuid());
        Player.getCachedPlayerList().remove(payload.getUuid());

        if(player != null) {
            Party party = Party.getPartyByMember(payload.getUuid());

            if(party != null) {
                PartyPlayerOfflineTimer timer = new PartyPlayerOfflineTimer(payload.getUuid());
                timer.start();
            }
        }

        Atom.getAtom().getProfilesManager()
                .getProfile(payload.getUuid())
                .thenAccept(profile -> {
                    if (profile == null) { return; }
                    // TODO: PERMS
//                    if (profile.api().getAllPermissions().contains("flame.staff")) {
//                        PigeonUtil.broadcast(new MessagePlayerPayload("flame.staff", Component.text()
//                                .append(profile.api().getChatFormat())
//                                .append(Component.text(" disconnected from ", MC.CC.GRAY.getTextColor()))
//                                .append(Component.text(payload.getServer(), MC.CC.WHITE.getTextColor()))
//                                .build()));
//                    }
                });

    }

}
