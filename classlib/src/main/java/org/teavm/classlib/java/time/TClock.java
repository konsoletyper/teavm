/*
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of JSR-310 nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.teavm.classlib.java.time;

import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_MINUTE;
import static org.teavm.classlib.java.time.TLocalTime.NANOS_PER_SECOND;

import java.io.Serializable;
import org.teavm.classlib.java.util.TTimeZone;

import org.teavm.classlib.java.time.jdk8.TJdk8Methods;

public abstract class TClock {

    public static TClock systemUTC() {
        return new SystemClock(TZoneOffset.UTC);
    }

    public static TClock systemDefaultZone() {
        return new SystemClock(TZoneId.systemDefault());
    }

    public static TClock system(TZoneId zone) {
        TJdk8Methods.requireNonNull(zone, "zone");
        return new SystemClock(zone);
    }

    //-------------------------------------------------------------------------
    public static TClock tickSeconds(TZoneId zone) {
        return new TickClock(system(zone), NANOS_PER_SECOND);
    }

    public static TClock tickMinutes(TZoneId zone) {
        return new TickClock(system(zone), NANOS_PER_MINUTE);
    }

    public static TClock tick(TClock baseClock, TDuration tickDuration) {
        TJdk8Methods.requireNonNull(baseClock, "baseClock");
        TJdk8Methods.requireNonNull(tickDuration, "tickDuration");
        if (tickDuration.isNegative()) {
            throw new IllegalArgumentException("Tick duration must not be negative");
        }
        long tickNanos = tickDuration.toNanos();
        if (tickNanos % 1000000 == 0) {
            // ok, no fraction of millisecond
        } else if (1000000000 % tickNanos == 0) {
            // ok, divides into one second without remainder
        } else {
            throw new IllegalArgumentException("Invalid tick duration");
        }
        if (tickNanos <= 1) {
            return baseClock;
        }
        return new TickClock(baseClock, tickNanos);
    }

    //-----------------------------------------------------------------------
    public static TClock fixed(TInstant fixedInstant, TZoneId zone) {
        TJdk8Methods.requireNonNull(fixedInstant, "fixedInstant");
        TJdk8Methods.requireNonNull(zone, "zone");
        return new FixedClock(fixedInstant, zone);
    }

    //-------------------------------------------------------------------------
    public static TClock offset(TClock baseClock, TDuration offsetDuration) {
        TJdk8Methods.requireNonNull(baseClock, "baseClock");
        TJdk8Methods.requireNonNull(offsetDuration, "offsetDuration");
        if (offsetDuration.equals(TDuration.ZERO)) {
            return baseClock;
        }
        return new OffsetClock(baseClock, offsetDuration);
    }

    //-----------------------------------------------------------------------
    protected TClock() {
    }

    //-----------------------------------------------------------------------
    public abstract TZoneId getZone();

    public abstract TClock withZone(TZoneId zone);

    //-------------------------------------------------------------------------
    public long millis() {
        return instant().toEpochMilli();
    }

    public abstract TInstant instant();

    //-----------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    //-----------------------------------------------------------------------
    static final class SystemClock extends TClock implements Serializable {
        private static final long serialVersionUID = 6740630888130243051L;
        private final TZoneId zone;

        SystemClock(TZoneId zone) {
            this.zone = zone;
        }
        @Override
        public TZoneId getZone() {
            return zone;
        }
        @Override
        public TClock withZone(TZoneId zone) {
            if (zone.equals(this.zone)) {  // intentional NPE
                return this;
            }
            return new SystemClock(zone);
        }
        @Override
        public long millis() {
            return TSystem.currentTimeMillis();
        }
        @Override
        public TInstant instant() {
            return TInstant.ofEpochMilli(millis());
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SystemClock) {
                return zone.equals(((SystemClock) obj).zone);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return zone.hashCode() + 1;
        }
        @Override
        public String toString() {
            return "SystemClock[" + zone + "]";
        }
    }

    //-----------------------------------------------------------------------
    static final class FixedClock extends TClock implements Serializable {
       private static final long serialVersionUID = 7430389292664866958L;
        private final TInstant instant;
        private final TZoneId zone;

        FixedClock(TInstant fixedInstant, TZoneId zone) {
            this.instant = fixedInstant;
            this.zone = zone;
        }
        @Override
        public TZoneId getZone() {
            return zone;
        }
        @Override
        public TClock withZone(TZoneId zone) {
            if (zone.equals(this.zone)) {  // intentional NPE
                return this;
            }
            return new FixedClock(instant, zone);
        }
        @Override
        public long millis() {
            return instant.toEpochMilli();
        }
        @Override
        public TInstant instant() {
            return instant;
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof FixedClock) {
                FixedClock other = (FixedClock) obj;
                return instant.equals(other.instant) && zone.equals(other.zone);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return instant.hashCode() ^ zone.hashCode();
        }
        @Override
        public String toString() {
            return "FixedClock[" + instant + "," + zone + "]";
        }
    }

    //-----------------------------------------------------------------------
    static final class OffsetClock extends TClock implements Serializable {
       private static final long serialVersionUID = 2007484719125426256L;
        private final TClock baseClock;
        private final TDuration offset;

        OffsetClock(TClock baseClock, TDuration offset) {
            this.baseClock = baseClock;
            this.offset = offset;
        }
        @Override
        public TZoneId getZone() {
            return baseClock.getZone();
        }
        @Override
        public TClock withZone(TZoneId zone) {
            if (zone.equals(baseClock.getZone())) {  // intentional NPE
                return this;
            }
            return new OffsetClock(baseClock.withZone(zone), offset);
        }
        @Override
        public long millis() {
            return TJdk8Methods.safeAdd(baseClock.millis(), offset.toMillis());
        }
        @Override
        public TInstant instant() {
            return baseClock.instant().plus(offset);
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof OffsetClock) {
                OffsetClock other = (OffsetClock) obj;
                return baseClock.equals(other.baseClock) && offset.equals(other.offset);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return baseClock.hashCode() ^ offset.hashCode();
        }
        @Override
        public String toString() {
            return "OffsetClock[" + baseClock + "," + offset + "]";
        }
    }

    //-----------------------------------------------------------------------
    static final class TickClock extends TClock implements Serializable {
        private static final long serialVersionUID = 6504659149906368850L;
        private final TClock baseClock;
        private final long tickNanos;

        TickClock(TClock baseClock, long tickNanos) {
            this.baseClock = baseClock;
            this.tickNanos = tickNanos;
        }
        @Override
        public TZoneId getZone() {
            return baseClock.getZone();
        }
        @Override
        public TClock withZone(TZoneId zone) {
            if (zone.equals(baseClock.getZone())) {  // intentional NPE
                return this;
            }
            return new TickClock(baseClock.withZone(zone), tickNanos);
        }
        @Override
        public long millis() {
            long millis = baseClock.millis();
            return millis - TJdk8Methods.floorMod(millis, tickNanos / 1000000L);
        }
        @Override
        public TInstant instant() {
            if ((tickNanos % 1000000) == 0) {
                long millis = baseClock.millis();
                return TInstant.ofEpochMilli(millis - TJdk8Methods.floorMod(millis, tickNanos / 1000000L));
            }
            TInstant instant = baseClock.instant();
            long nanos = instant.getNano();
            long adjust = TJdk8Methods.floorMod(nanos, tickNanos);
            return instant.minusNanos(adjust);
        }
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TickClock) {
                TickClock other = (TickClock) obj;
                return baseClock.equals(other.baseClock) && tickNanos == other.tickNanos;
            }
            return false;
        }
        @Override
        public int hashCode() {
            return baseClock.hashCode() ^ ((int) (tickNanos ^ (tickNanos >>> 32)));
        }
        @Override
        public String toString() {
            return "TickClock[" + baseClock + "," + TDuration.ofNanos(tickNanos) + "]";
        }
    }

}
