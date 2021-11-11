package cc.minetale.atom.network;

import cc.minetale.atom.timers.SyncCodeTimer;
import cc.minetale.atom.util.Util;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SyncCode {

    @Getter private static final List<SyncCode> syncCodeList = new ArrayList<>();

    private final Player player;
    private final int code;
    private final SyncCodeTimer timer;
    private final long generatedAt;

    public SyncCode(Player player) {
        this.player = player;
        this.code = Util.getRandomCode();
        this.timer = new SyncCodeTimer(this);
        this.generatedAt = System.currentTimeMillis();
    }

}