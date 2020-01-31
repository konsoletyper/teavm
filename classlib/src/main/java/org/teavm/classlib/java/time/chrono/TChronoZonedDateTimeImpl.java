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
package org.teavm.classlib.java.time.chrono;

import static org.teavm.classlib.java.time.temporal.TChronoUnit.SECONDS;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.List;

import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TZoneId;
import org.teavm.classlib.java.time.TZoneOffset;
import org.teavm.classlib.java.time.jdk8.TJdk8Methods;
import org.teavm.classlib.java.time.temporal.TChronoField;
import org.teavm.classlib.java.time.temporal.TChronoUnit;
import org.teavm.classlib.java.time.temporal.TTemporal;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalUnit;
import org.teavm.classlib.java.time.zone.TZoneOffsetTransition;
import org.teavm.classlib.java.time.zone.TZoneRules;

final class ChronoZonedDateTimeImpl<D extends TChronoLocalDate>
        extends TChronoZonedDateTime<D>
        implements Serializable {

    private static final long serialVersionUID = -5261813987200935591L;

    private final TChronoLocalDateTimeImpl<D> dateTime;
    private final TZoneOffset offset;
    private final TZoneId zone;

    //-----------------------------------------------------------------------
    static <R extends TChronoLocalDate> TChronoZonedDateTime<R> ofBest(
            TChronoLocalDateTimeImpl<R> localDateTime, TZoneId zone, TZoneOffset preferredOffset) {
        TJdk8Methods.requireNonNull(localDateTime, "localDateTime");
        TJdk8Methods.requireNonNull(zone, "zone");
        if (zone instanceof TZoneOffset) {
            return new ChronoZonedDateTimeImpl<R>(localDateTime, (TZoneOffset) zone, zone);
        }
        TZoneRules rules = zone.getRules();
        TLocalDateTime isoLDT = TLocalDateTime.from(localDateTime);
        List<TZoneOffset> validOffsets = rules.getValidOffsets(isoLDT);
        TZoneOffset offset;
        if (validOffsets.size() == 1) {
            offset = validOffsets.get(0);
        } else if (validOffsets.size() == 0) {
            TZoneOffsetTransition trans = rules.getTransition(isoLDT);
            localDateTime = localDateTime.plusSeconds(trans.getDuration().getSeconds());
            offset = trans.getOffsetAfter();
        } else {
            if (preferredOffset != null && validOffsets.contains(preferredOffset)) {
                offset = preferredOffset;
            } else {
                offset = validOffsets.get(0);
            }
        }
        TJdk8Methods.requireNonNull(offset, "offset");  // protect against bad TZoneRules
        return new ChronoZonedDateTimeImpl<R>(localDateTime, offset, zone);
    }

    static <R extends TChronoLocalDate> ChronoZonedDateTimeImpl<R> ofInstant(TChronology chrono, TInstant instant, TZoneId zone) {
        TZoneRules rules = zone.getRules();
        TZoneOffset offset = rules.getOffset(instant);
        TJdk8Methods.requireNonNull(offset, "offset");  // protect against bad TZoneRules
        TLocalDateTime ldt = TLocalDateTime.ofEpochSecond(instant.getEpochSecond(), instant.getNano(), offset);
        @SuppressWarnings("unchecked")
        TChronoLocalDateTimeImpl<R> cldt = (TChronoLocalDateTimeImpl<R>) chrono.localDateTime(ldt);
        return new ChronoZonedDateTimeImpl<R>(cldt, offset, zone);
    }

    private ChronoZonedDateTimeImpl<D> create(TInstant instant, TZoneId zone) {
        return ofInstant(toLocalDate().getChronology(), instant, zone);
    }

    //-----------------------------------------------------------------------
    private ChronoZonedDateTimeImpl(TChronoLocalDateTimeImpl<D> dateTime, TZoneOffset offset, TZoneId zone) {
        this.dateTime = TJdk8Methods.requireNonNull(dateTime, "dateTime");
        this.offset = TJdk8Methods.requireNonNull(offset, "offset");
        this.zone = TJdk8Methods.requireNonNull(zone, "zone");
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TTemporalUnit unit) {
        if (unit instanceof TChronoUnit) {
            return unit.isDateBased() || unit.isTimeBased();
        }
        return unit != null && unit.isSupportedBy(this);
    }

    public TZoneOffset getOffset() {
        return offset;
    }

    @Override
    public TChronoZonedDateTime<D> withEarlierOffsetAtOverlap() {
        TZoneOffsetTransition trans = getZone().getRules().getTransition(TLocalDateTime.from(this));
        if (trans != null && trans.isOverlap()) {
            TZoneOffset earlierOffset = trans.getOffsetBefore();
            if (earlierOffset.equals(offset) == false) {
                return new ChronoZonedDateTimeImpl<D>(dateTime, earlierOffset, zone);
            }
        }
        return this;
    }

    @Override
    public TChronoZonedDateTime<D> withLaterOffsetAtOverlap() {
        TZoneOffsetTransition trans = getZone().getRules().getTransition(TLocalDateTime.from(this));
        if (trans != null) {
            TZoneOffset offset = trans.getOffsetAfter();
            if (offset.equals(getOffset()) == false) {
                return new ChronoZonedDateTimeImpl<D>(dateTime, offset, zone);
            }
        }
        return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public TChronoLocalDateTime<D> toLocalDateTime() {
        return dateTime;
    }

    public TZoneId getZone() {
        return zone;
    }

    public TChronoZonedDateTime<D> withZoneSameLocal(TZoneId zone) {
        return ofBest(dateTime, zone, offset);
    }

    @Override
    public TChronoZonedDateTime<D> withZoneSameInstant(TZoneId zone) {
        TJdk8Methods.requireNonNull(zone, "zone");
        return this.zone.equals(zone) ? this : create(dateTime.toInstant(offset), zone);
    }

    //-----------------------------------------------------------------------
    @Override
    public boolean isSupported(TTemporalField field) {
        return field instanceof TChronoField || (field != null && field.isSupportedBy(this));
    }

    //-----------------------------------------------------------------------
    @Override
    public TChronoZonedDateTime<D> with(TTemporalField field, long newValue) {
        if (field instanceof TChronoField) {
            TChronoField f = (TChronoField) field;
            switch (f) {
                case INSTANT_SECONDS: return plus(newValue - toEpochSecond(), SECONDS);
                case OFFSET_SECONDS: {
                    TZoneOffset offset = TZoneOffset.ofTotalSeconds(f.checkValidIntValue(newValue));
                    return create(dateTime.toInstant(offset), zone);
                }
            }
            return ofBest(dateTime.with(field, newValue), zone, offset);
        }
        return toLocalDate().getChronology().ensureChronoZonedDateTime(field.adjustInto(this, newValue));
    }

    //-----------------------------------------------------------------------
    @Override
    public TChronoZonedDateTime<D> plus(long amountToAdd, TTemporalUnit unit) {
        if (unit instanceof TChronoUnit) {
            return with(dateTime.plus(amountToAdd, unit));
        }
        return toLocalDate().getChronology().ensureChronoZonedDateTime(unit.addTo(this, amountToAdd));   /// TODO: Generics replacement Risk!
    }

    //-----------------------------------------------------------------------
    @Override
    public long until(TTemporal endExclusive, TTemporalUnit unit) {
        @SuppressWarnings("unchecked")
        TChronoZonedDateTime<D> end = (TChronoZonedDateTime<D>) toLocalDate().getChronology().zonedDateTime(endExclusive);
        if (unit instanceof TChronoUnit) {
            end = end.withZoneSameInstant(offset);
            return dateTime.until(end.toLocalDateTime(), unit);
        }
        return unit.between(this, end);
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.CHRONO_ZONEDDATETIME_TYPE, this);
    }

    private Object readResolve() throws ObjectStreamException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(dateTime);
        out.writeObject(offset);
        out.writeObject(zone);
    }

    static TChronoZonedDateTime<?> readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        TChronoLocalDateTime<?> dateTime = (TChronoLocalDateTime<?>) in.readObject();
        TZoneOffset offset = (TZoneOffset) in.readObject();
        TZoneId zone = (TZoneId) in.readObject();
        return dateTime.atZone(offset).withZoneSameLocal(zone);
        // TODO: ZDT uses ofLenient()
    }

    //-------------------------------------------------------------------------
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TChronoZonedDateTime) {
            return compareTo((TChronoZonedDateTime<?>) obj) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return toLocalDateTime().hashCode() ^ getOffset().hashCode() ^ Integer.rotateLeft(getZone().hashCode(), 3);
    }

    @Override
    public String toString() {
        String str = toLocalDateTime().toString() + getOffset().toString();
        if (getOffset() != getZone()) {
            str += '[' + getZone().toString() + ']';
        }
        return str;
    }


}
