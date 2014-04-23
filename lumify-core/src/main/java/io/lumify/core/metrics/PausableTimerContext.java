package io.lumify.core.metrics;

import com.codahale.metrics.Clock;
import com.codahale.metrics.Timer;

import java.util.concurrent.TimeUnit;

public class PausableTimerContext {
    private final Timer timer;
    private final Clock clock;
    private long startTime;
    private long totalTime;
    private boolean paused;

    public PausableTimerContext(Timer timer) {
        this.timer = timer;
        this.clock = Clock.defaultClock();
        this.startTime = clock.getTick();
        this.totalTime = 0;
        this.paused = false;
    }

    public long stop() {
        pause();
        timer.update(totalTime, TimeUnit.NANOSECONDS);
        return totalTime;
    }

    public void pause() {
        if (paused) {
            return;
        }
        totalTime += clock.getTick() - startTime;
        paused = true;
    }

    public void resume() {
        this.startTime = clock.getTick();
        paused = false;
    }
}
