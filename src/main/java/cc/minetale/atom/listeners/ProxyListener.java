package cc.minetale.atom.listeners;

import cc.minetale.atom.network.Party;
import cc.minetale.atom.network.Player;
import cc.minetale.atom.timers.PartyOfflineTimer;
import cc.minetale.atom.util.RankUtil;
import cc.minetale.commonlib.network.server.Server;
import cc.minetale.commonlib.pigeon.payloads.atom.AtomPlayerCountRequestPayload;
import cc.minetale.commonlib.pigeon.payloads.network.*;
import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.util.PigeonUtil;
import cc.minetale.pigeon.annotations.PayloadHandler;
import cc.minetale.pigeon.annotations.PayloadListener;
import cc.minetale.pigeon.feedback.RequiredState;
import cc.minetale.pigeon.listeners.Listener;

@PayloadListener
public class ProxyListener implements Listener {

    // TODO -> Rework so it requests player count from all proxies
    @PayloadHandler(requiredState = RequiredState.REQUEST)
    public void onPlayerCountRequest(AtomPlayerCountRequestPayload payload) {
        payload.sendResponse(new AtomPlayerCountRequestPayload(Server.getOnlinePlayerCount()));
    }

    // TODO -> Either remove this or rework it
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
        Player.getPlayer(payload.getUuid()).thenAccept(player -> {
            player.setOnline(true);
            player.setServer(payload.getServer());

            var partyTimer = player.getPartyOfflineTimer();

            if (partyTimer != null) {
                partyTimer.stop();
            }

            Profile profile = player.getProfile();

            if (RankUtil.hasMinimumRank(profile, "Helper")) {
                // TODO -> Broadcast that they have connected to staff
            }
        });
    }

    @PayloadHandler
    public void onProxyPlayerSwitch(ProxyPlayerSwitchPayload payload) {
        Player.getPlayer(payload.getUuid()).thenAccept(player -> {
            player.setOnline(true);
            player.setServer(payload.getServerTo());

            var profile = player.getProfile();

            if (RankUtil.hasMinimumRank(profile, "Helper")) {
                // TODO -> Broadcast that they have switched to staff
            }
        });
    }

    @PayloadHandler
    public void onProxyDisconnect(ProxyPlayerDisconnectPayload payload) {
        Player.getPlayer(payload.getUuid()).thenAccept(player -> {
            player.setOnline(false);
            player.setServer(null);

            var party = Party.getPartyByMember(payload.getUuid());

            if(party != null) {
                var timer = new PartyOfflineTimer(payload.getUuid());
                timer.start();
            }

            var profile = player.getProfile();

            if (RankUtil.hasMinimumRank(profile, "Helper")) {
                // TODO -> Broadcast that they have disconnected to staff
            }
        });
    }

}
