/*
 *  Copyright 2015 Alexey Andreev.
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

import java.util.Objects;
import org.teavm.classlib.impl.tz.DateTimeZone;

class TIANATimeZone extends TTimeZone {
    private static final long serialVersionUID = -8196006595542230951L;
    private DateTimeZone underlyingZone;
    private int rawOffset;
    private int diff;

    public TIANATimeZone(DateTimeZone underlyingZone) {
        super(underlyingZone.getID());
        this.underlyingZone = underlyingZone;
        rawOffset = underlyingZone.getStandardOffset(System.currentTimeMillis());
        diff = -rawOffset;
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int time) {
        TCalendar calendar = new TGregorianCalendar(year, month, day);
        calendar.set(TCalendar.ERA, era);
        calendar.set(TCalendar.DAY_OF_WEEK, dayOfWeek);
        calendar.add(TCalendar.MILLISECOND, time);
        return getOffset(calendar.getTimeInMillis());
    }

    @Override
    public int getOffset(long time) {
        return rawOffset + diff + underlyingZone.getOffset(time);
    }

    @Override
    public int getRawOffset() {
        return rawOffset;
    }

    @Override
    public boolean inDaylightTime(TDate time) {
        return underlyingZone.getOffset(time.getTime()) != underlyingZone.getStandardOffset(time.getTime());
    }

    @Override
    public void setRawOffset(int offset) {
        this.rawOffset = offset;
    }

    @Override
    public boolean useDaylightTime() {
        return !underlyingZone.isFixed();
    }

    @Override
    public TIANATimeZone clone() {
        TIANATimeZone copy = (TIANATimeZone) super.clone();
        copy.rawOffset = rawOffset;
        copy.underlyingZone = underlyingZone;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TIANATimeZone)) {
            return false;
        }
        TIANATimeZone other = (TIANATimeZone) obj;
        return rawOffset == other.rawOffset && underlyingZone.getID().equals(other.getID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawOffset, underlyingZone.getID());
    }
}
