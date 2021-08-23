package cc.minetale.atom.listeners;

import cc.minetale.commonlib.modules.pigeon.payloads.rank.RankReloadPayload;
import cc.minetale.commonlib.modules.pigeon.payloads.rank.RankRemovePayload;
import cc.minetale.commonlib.modules.rank.Rank;
import cc.minetale.pigeon.annotations.PayloadHandler;
import cc.minetale.pigeon.annotations.PayloadListener;
import cc.minetale.pigeon.listeners.Listener;

import java.util.UUID;

@PayloadListener
public class RankListener implements Listener {

    @PayloadHandler
    public void onReloadRank(RankReloadPayload payload) {
        Rank rank = Rank.getRank(payload.getRank(), false);

        Rank.getRanks().remove(payload.getRank());

        if (rank != null) {
            Rank.getRanks().put(rank.getUuid(), rank);
        }
    }

    @PayloadHandler
    public void onRemoveRank(RankRemovePayload payload) {
        UUID rank = payload.getRank();

        Rank.getRanks().remove(rank);
    }

}
