/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.java.util;

import java.util.Date;
import java.util.Objects;
import org.teavm.classlib.java.lang.TIllegalStateException;
import org.teavm.classlib.java.lang.TObject;
import org.teavm.jso.browser.TimerHandler;
import org.teavm.jso.browser.Window;

public class TTimer extends TObject {
    TSet<TTimerTask> tasks = new THashSet<>();
    private volatile boolean cancelled;
    private String threadName;
    private boolean daemon;

    public TTimer() {
    }

    public TTimer(String name) {
        Objects.requireNonNull(name);
        threadName = name;
    }

    public TTimer(boolean daemon) {
        this.daemon = daemon;
    }

    public TTimer(String name, boolean daemon) {
        Objects.requireNonNull(name);
        threadName = name;
        this.daemon = daemon;
    }

    public void cancel() {
        if (cancelled) {
            return;
        }
        cancelled = true;
        for (TTimerTask task : tasks.toArray(new TTimerTask[0])) {
            task.cancel();
        }
    }

    public void schedule(TTimerTask task, Date time) {
        long delay = Math.max(0, time.getTime() - System.currentTimeMillis());
        schedule(task, delay);
    }

    public void schedule(TTimerTask task, long delay) {
        if (cancelled || task.timer != null || task.nativeTimerId >= 0) {
            throw new TIllegalStateException();
        }
        task.timer = this;
        task.nativeTimerId = Window.setTimeout(() -> {
            runThread(new Thread(() -> {
                if (cancelled || task.timer == null) {
                    return;
                }
                TTimerTask.performOnce(task);
            }));
        }, (int) delay);
    }

    public void schedule(TTimerTask task, Date time, long period) {
        long delay = Math.max(0, time.getTime() - System.currentTimeMillis());
        schedule(task, delay, period);
    }

    public void schedule(TTimerTask task, long delay, long period) {
        if (cancelled || task.timer != null || task.nativeTimerId >= 0) {
            throw new TIllegalStateException();
        }
        task.timer = this;
        TimerHandler handler = new TimerHandler() {
            @Override public void onTimer() {
                runThread(new Thread(() -> {
                    if (cancelled || task.timer == null) {
                        return;
                    }
                    task.nativeTimerId = Window.setTimeout(this, (int) period);
                    TTimerTask.performOnce(task);
                    if (!cancelled) {
                        task.timer = TTimer.this;
                    }
                }));
            }
        };
        task.nativeTimerId = Window.setTimeout(handler, (int) delay);
    }

    public void scheduleAtFixedRate(TTimerTask task, Date time, long period) {
        long delay = Math.max(0, time.getTime() - System.currentTimeMillis());
        scheduleAtFixedRate(task, delay, period);
    }
    
    public void scheduleAtFixedRate(TTimerTask task, long delay, long period) {
        if (cancelled || task.timer != null || task.nativeTimerId >= 0) {
            throw new TIllegalStateException();
        }
        final long[] nextStartTime = new long[]{System.currentTimeMillis() + delay};
        task.timer = this;
        TimerHandler handler = new TimerHandler() {
            @Override public void onTimer() {
                runThread(new Thread(() -> {
                    if (cancelled || task.timer == null) {
                        return;
                    }
                    long nextDelay = nextStartTime[0] - System.currentTimeMillis();
                    if (nextDelay < 0) {
                        nextDelay = 0;
                    }
                    task.nativeTimerId = Window.setTimeout(this, (int) nextDelay);
                    nextStartTime[0] += period;
                    TTimerTask.performOnce(task);
                    if (!cancelled) {
                        task.timer = TTimer.this;
                    }
                }));
            }
        };
        task.nativeTimerId = Window.setTimeout(handler, (int) delay);
        nextStartTime[0] += period;
    }

    private void runThread(Thread thread) {
        if (threadName != null) {
            thread.setName(threadName);
            thread.setDaemon(daemon);
        }
        thread.start();
    }
}
