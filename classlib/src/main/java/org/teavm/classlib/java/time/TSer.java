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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;

final class Ser implements Externalizable {

    private static final long serialVersionUID = -7683839454370182990L;

    static final byte DURATION_TYPE = 1;
    static final byte INSTANT_TYPE = 2;
    static final byte LOCAL_DATE_TYPE = 3;
    static final byte LOCAL_DATE_TIME_TYPE = 4;
    static final byte LOCAL_TIME_TYPE = 5;
    static final byte ZONED_DATE_TIME_TYPE = 6;
    static final byte ZONE_REGION_TYPE = 7;
    static final byte ZONE_OFFSET_TYPE = 8;

    static final byte MONTH_DAY_TYPE = 64;
    static final byte OFFSET_TIME_TYPE = 66;
    static final byte YEAR_TYPE = 67;
    static final byte YEAR_MONTH_TYPE = 68;
    static final byte OFFSET_DATE_TIME_TYPE = 69;

    private byte type;
    private Object object;

    public Ser() {
    }

    Ser(byte type, Object object) {
        this.type = type;
        this.object = object;
    }

    //-----------------------------------------------------------------------
    public void writeExternal(ObjectOutput out) throws IOException {
        writeInternal(type, object, out);
    }

    static void writeInternal(byte type, Object object, DataOutput out) throws IOException {
        out.writeByte(type);
        switch (type) {
            case DURATION_TYPE:
                ((TDuration) object).writeExternal(out);
                break;
            case INSTANT_TYPE:
                ((TInstant) object).writeExternal(out);
                break;
            case LOCAL_DATE_TYPE:
                ((TLocalDate) object).writeExternal(out);
                break;
            case LOCAL_DATE_TIME_TYPE:
                ((TLocalDateTime) object).writeExternal(out);
                break;
            case LOCAL_TIME_TYPE:
                ((TLocalTime) object).writeExternal(out);
                break;
            case MONTH_DAY_TYPE:
                ((TMonthDay) object).writeExternal(out);
                break;
            case OFFSET_DATE_TIME_TYPE:
                ((TOffsetDateTime) object).writeExternal(out);
                break;
            case OFFSET_TIME_TYPE:
                ((TOffsetTime) object).writeExternal(out);
                break;
            case YEAR_MONTH_TYPE:
                ((TYearMonth) object).writeExternal(out);
                break;
            case YEAR_TYPE:
                ((TYear) object).writeExternal(out);
                break;
            case ZONE_REGION_TYPE:
                ((TZoneRegion) object).writeExternal(out);
                break;
            case ZONE_OFFSET_TYPE:
                ((TZoneOffset) object).writeExternal(out);
                break;
            case ZONED_DATE_TIME_TYPE:
                ((TZonedDateTime) object).writeExternal(out);
                break;
            default:
                throw new InvalidClassException("Unknown serialized type");
        }
    }

    //-----------------------------------------------------------------------
    public void readExternal(ObjectInput in) throws IOException {
        type = in.readByte();
        object = readInternal(type, in);
    }

    static Object read(DataInput in) throws IOException {
        byte type = in.readByte();
        return readInternal(type, in);
    }

    private static Object readInternal(byte type, DataInput in) throws IOException {
        switch (type) {
            case DURATION_TYPE: return TDuration.readExternal(in);
            case INSTANT_TYPE: return TInstant.readExternal(in);
            case LOCAL_DATE_TYPE: return TLocalDate.readExternal(in);
            case LOCAL_DATE_TIME_TYPE: return TLocalDateTime.readExternal(in);
            case LOCAL_TIME_TYPE: return TLocalTime.readExternal(in);
            case MONTH_DAY_TYPE: return TMonthDay.readExternal(in);
            case OFFSET_DATE_TIME_TYPE: return TOffsetDateTime.readExternal(in);
            case OFFSET_TIME_TYPE: return TOffsetTime.readExternal(in);
            case YEAR_TYPE: return TYear.readExternal(in);
            case YEAR_MONTH_TYPE: return TYearMonth.readExternal(in);
            case ZONED_DATE_TIME_TYPE: return TZonedDateTime.readExternal(in);
            case ZONE_OFFSET_TYPE: return TZoneOffset.readExternal(in);
            case ZONE_REGION_TYPE: return TZoneRegion.readExternal(in);
            default:
                throw new StreamCorruptedException("Unknown serialized type");
        }
    }

    private Object readResolve() {
         return object;
    }

}
