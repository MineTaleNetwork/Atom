package cc.minetale.atom.timers;

import cc.minetale.atom.Atom;
import cc.minetale.atom.network.SyncCode;
import cc.minetale.atom.util.timer.api.Timer;
import cc.minetale.atom.util.timer.api.TimerType;

import java.util.concurrent.TimeUnit;

public class SyncCodeTimer extends Timer {

    public final SyncCode syncCode;

    public SyncCodeTimer(SyncCode syncCode) {
        super(TimerType.COUNTDOWN, Atom.getAtom().getTimerManager());
        setDuration(TimeUnit.MINUTES.toMillis(5));
        this.syncCode = syncCode;
    }

    @Override
    public void onSecond() {}

    @Override
    public void onStart() {}

    @Override
    public void onComplete() {
        SyncCode.syncCodeList.remove(this.syncCode);
    }

    @Override
    public void onCancel() {}

}
