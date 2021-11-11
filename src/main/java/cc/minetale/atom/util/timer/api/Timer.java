package cc.minetale.atom.util.timer.api;

import cc.minetale.atom.util.timer.TimerManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.concurrent.TimeUnit;

@Getter @Setter @RequiredArgsConstructor
public abstract class Timer {

    private final TimerManager manager;

    private Long startTime = System.currentTimeMillis();
    private Long endTime = System.currentTimeMillis();

    private Long duration;

    private boolean paused = false;
    private boolean removeOnCompletion = true;

    public void start() {
        startTime = System.currentTimeMillis();
        checkCachedDuration();
        checkIfPresent();
        this.onStart();
    }

    public void start(Long startTime) {
        this.startTime = startTime;
        checkCachedDuration();
        checkIfPresent();
        this.onStart();
    }

    public void start(Long startTime, Long endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
        checkIfPresent();
        this.onStart();
    }

    public void extend() {
        if (duration != null) {
            this.endTime = System.currentTimeMillis() + duration;
        }
    }

    public void extend(Long time) {
        this.endTime = System.currentTimeMillis() + time;
    }

    public void stop() {
        manager.removeTimer(this);
        onCancel();
    }

    public void stop(boolean triggerCompleteLogic) {
        manager.removeTimer(this);
        if (triggerCompleteLogic) {
            onComplete();
        } else {
            onCancel();
        }
    }

    private void checkIfPresent() {
        if (!manager.getTimers().contains(this)) {
            manager.addTimer(this);
        }
    }

    private void checkCachedDuration() {
        if (duration != null) {
            this.endTime = startTime + duration;
        }
    }

    public boolean isComplete() {
        if (isPaused()) {
            return false;
        }
        return endTime - System.currentTimeMillis() <= 0;
    }

    public long getSeconds(long time) {
        return TimeUnit.MILLISECONDS.toSeconds(time);
    }

    public long getMinutes(long time) {
        return TimeUnit.MILLISECONDS.toMinutes(time);
    }

    public long getHours(long time) {
        return TimeUnit.MILLISECONDS.toHours(time);
    }

    public long getRemaining() {
        return endTime - System.currentTimeMillis();
    }

    public long getElapsed() {
        return System.currentTimeMillis() - startTime;
    }

    public static long getRemaining(long endTime) {
        return endTime - System.currentTimeMillis();
    }

    public static long getElapsed(long startTime) {
        return System.currentTimeMillis() - startTime;
    }

    public abstract void onSecond();

    public abstract void onStart();

    public abstract void onComplete();

    public abstract void onCancel();

}