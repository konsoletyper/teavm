/*
 *  Copyright 2018 konsoletyper.
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
package org.teavm.classlib.java.util.concurrent;

public enum TTimeUnit {
    NANOSECONDS(1),
    MICROSECONDS(1_000),
    MILLISECONDS(1_000_000),
    SECONDS(1_000_000_000),
    MINUTES(60_000_000_000L),
    HOURS(3_600_000_000_000L),
    DAYS(24 * 3_600_000_000_000L);

    private long nanoseconds;

    TTimeUnit(long nanoseconds) {
        this.nanoseconds = nanoseconds;
    }

    public long convert(long sourceDuration, TTimeUnit sourceUnit) {
        long sourceNanos = sourceUnit.nanoseconds;
        long targetNanos = nanoseconds;
        if (sourceNanos < targetNanos) {
            return sourceDuration * (targetNanos / sourceNanos);
        } else {
            return sourceDuration / (sourceNanos / targetNanos);
        }
    }

    public long toNanos(long duration) {
        return duration * nanoseconds;
    }

    public long toMicros(long duration) {
        return MICROSECONDS.convert(duration, this);
    }

    public long toMillis(long duration) {
        return MILLISECONDS.convert(duration, this);
    }

    public long toSeconds(long duration) {
        return SECONDS.convert(duration, this);
    }

    public long toMinutes(long duration) {
        return MINUTES.convert(duration, this);
    }

    public long toHours(long duration) {
        return HOURS.convert(duration, this);
    }

    public long toDays(long duration) {
        return DAYS.convert(duration, this);
    }

    public void timedWait(Object obj, long timeout) throws InterruptedException {
        obj.wait(toMillis(timeout));
    }

    public void timedJoin(Thread thread, long timeout) throws InterruptedException {
        thread.join(toMillis(timeout));
    }

    public void sleep(long timeout) throws InterruptedException {
        Thread.sleep(toMillis(timeout));
    }
}
