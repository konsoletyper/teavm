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

import java.io.Externalizable;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.StreamCorruptedException;

import org.teavm.classlib.java.time.TLocalDate;
import org.teavm.classlib.java.time.TLocalDateTime;

final class Ser implements Externalizable {

    private static final long serialVersionUID = 7857518227608961174L;

    static final byte JAPANESE_DATE_TYPE = 1;
    static final byte JAPANESE_ERA_TYPE = 2;
    static final byte HIJRAH_DATE_TYPE = 3;
    static final byte HIJRAH_ERA_TYPE = 4;
    static final byte MINGUO_DATE_TYPE = 5;
    static final byte MINGUO_ERA_TYPE = 6;
    static final byte THAIBUDDHIST_DATE_TYPE = 7;
    static final byte THAIBUDDHIST_ERA_TYPE = 8;
    static final byte CHRONO_TYPE = 11;
    static final byte CHRONO_LOCALDATETIME_TYPE = 12;
    static final byte CHRONO_ZONEDDATETIME_TYPE = 13;

    private byte type;
    private Object object;

    public Ser() {
    }

    Ser(byte type, Object object) {
        this.type = type;
        this.object = object;
    }

    //-----------------------------------------------------------------------
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        writeInternal(type, object, out);
    }

    private static void writeInternal(byte type, Object object, ObjectOutput out) throws IOException {
        out.writeByte(type);
        switch (type) {
            case JAPANESE_DATE_TYPE:
                ((TJapaneseDate) object).writeExternal(out);
                break;
            case JAPANESE_ERA_TYPE:
                ((TJapaneseEra) object).writeExternal(out);
                break;
            case HIJRAH_DATE_TYPE:
                ((THijrahDate) object).writeExternal(out);
                break;
            case HIJRAH_ERA_TYPE:
                ((THijrahEra) object).writeExternal(out);
                break;
            case MINGUO_DATE_TYPE:
                ((TMinguoDate) object).writeExternal(out);
                break;
            case MINGUO_ERA_TYPE:
                ((TMinguoEra) object).writeExternal(out);
                break;
            case THAIBUDDHIST_DATE_TYPE:
                ((TThaiBuddhistDate) object).writeExternal(out);
                break;
            case THAIBUDDHIST_ERA_TYPE:
                ((TThaiBuddhistEra) object).writeExternal(out);
                break;
            case CHRONO_TYPE:
                ((TChronology) object).writeExternal(out);
                break;
            case CHRONO_LOCALDATETIME_TYPE:
                ((TChronoLocalDateTimeImpl<?>) object).writeExternal(out);
                break;
            case CHRONO_ZONEDDATETIME_TYPE:
                ((ChronoZonedDateTimeImpl<?>) object).writeExternal(out);
                break;
            default:
                throw new InvalidClassException("Unknown serialized type");
        }
    }

    //-----------------------------------------------------------------------
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        type = in.readByte();
        object = readInternal(type, in);
    }

    static Object read(ObjectInput in) throws IOException, ClassNotFoundException {
        byte type = in.readByte();
        return readInternal(type, in);
    }

    private static Object readInternal(byte type, ObjectInput in) throws IOException, ClassNotFoundException {
        switch (type) {
            case JAPANESE_DATE_TYPE:  return TJapaneseDate.readExternal(in);
            case JAPANESE_ERA_TYPE: return TJapaneseEra.readExternal(in);
            case HIJRAH_DATE_TYPE: return THijrahDate.readExternal(in);
            case HIJRAH_ERA_TYPE: return THijrahEra.readExternal(in);
            case MINGUO_DATE_TYPE: return TMinguoDate.readExternal(in);
            case MINGUO_ERA_TYPE: return TMinguoEra.readExternal(in);
            case THAIBUDDHIST_DATE_TYPE: return TThaiBuddhistDate.readExternal(in);
            case THAIBUDDHIST_ERA_TYPE: return TThaiBuddhistEra.readExternal(in);
            case CHRONO_TYPE: return TChronology.readExternal(in);
            case CHRONO_LOCALDATETIME_TYPE: return TChronoLocalDateTimeImpl.readExternal(in);
            case CHRONO_ZONEDDATETIME_TYPE: return ChronoZonedDateTimeImpl.readExternal(in);
            default:
                throw new StreamCorruptedException("Unknown serialized type");
        }
    }

    private Object readResolve() {
         return object;
    }

}
