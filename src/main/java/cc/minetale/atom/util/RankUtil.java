package cc.minetale.atom.util;

import cc.minetale.commonlib.profile.Profile;
import cc.minetale.commonlib.rank.Rank;

public class RankUtil {

    public static boolean hasMinimumRank(Profile profile, String rankName) {
        Rank rank = Rank.getRank(rankName);

        if(rank == null)
            return false;

        return profile.getGrant().api().getRank().getWeight() <= rank.getWeight();
    }


}
