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
package org.teavm.classlib.impl.tz;

import org.teavm.classlib.impl.Base46;
import org.teavm.classlib.impl.CharFlow;
import org.teavm.classlib.impl.tz.DateTimeZoneBuilder.DSTZone;
import org.teavm.classlib.impl.tz.DateTimeZoneBuilder.PrecalculatedZone;

public abstract class StorableDateTimeZone extends DateTimeZone {
    public static final int PRECALCULATED = 0;
    public static final int FIXED = 1;
    public static final int DST = 3;
    public static final int ALIAS = 4;

    public StorableDateTimeZone(String id) {
        super(id);
    }

    public abstract void write(StringBuilder sb);

    public static void writeTime(StringBuilder sb, long time) {
        if (time % 1800_000 == 0) {
            Base46.encode(sb, (int) ((time / 1800_000) << 1));
        } else {
            Base46.encode(sb, (int) (((time / 60_000) << 1) | 1));
        }
    }

    public static long readTime(CharFlow flow) {
        long value = Base46.decodeLong(flow);
        if ((value & 1) == 0) {
            return (value >> 1) * 1800_000;
        } else {
            return (value >> 1) * 60_000;
        }
    }

    public static void writeUnsignedTime(StringBuilder sb, long time) {
        if (time % 1800_000 == 0) {
            Base46.encodeUnsigned(sb, (int) ((time / 1800_000) << 1));
        } else {
            Base46.encodeUnsigned(sb, (int) (((time / 60_000) << 1) | 1));
        }
    }

    public static long readUnsignedTime(CharFlow flow) {
        long value = Base46.decodeUnsignedLong(flow);
        if ((value & 1) == 0) {
            return (value >>> 1) * 1800_000;
        } else {
            return (value >>> 1) * 60_000;
        }
    }

    public static void writeTimeArray(StringBuilder sb, int[] timeArray) {
        int last = 0;
        for (int i = 1; i < timeArray.length; ++i) {
            int j;
            for (j = i + 1; j < timeArray.length; ++j) {
                if (timeArray[i] != timeArray[j]) {
                    break;
                }
            }
            if (j - i >= 3) {
                if (i > last) {
                    Base46.encode(sb, ~(i - last));
                    while (last < i) {
                        writeTime(sb, timeArray[last++]);
                    }
                }
                Base46.encode(sb, j - i);
                writeTime(sb, timeArray[i]);
                last = j;
                i = j;
            }
        }
        if (timeArray.length > last) {
            Base46.encode(sb, ~(timeArray.length - last));
            while (last < timeArray.length) {
                writeTime(sb, timeArray[last++]);
            }
        }
    }

    public static void readTimeArray(CharFlow flow, int[] array) {
        int index = 0;
        while (index < array.length) {
            int count = Base46.decode(flow);
            if (count >= 0) {
                int t = (int) readTime(flow);
                while (count-- > 0) {
                    array[index++] = t;
                }
            } else {
                count = ~count;
                while (count-- > 0) {
                    array[index++] = (int) readTime(flow);
                }
            }
        }
    }

    public static StorableDateTimeZone read(String id, String text) {
        CharFlow flow = new CharFlow(text.toCharArray());
        int type = Base46.decodeUnsigned(flow);
        switch (type) {
            case PRECALCULATED:
                return PrecalculatedZone.readZone(id, flow);
            case DST:
                return DSTZone.readZone(id, flow);
            case FIXED:
                return FixedDateTimeZone.readZone(id, flow);
            default:
                throw new IllegalArgumentException("Unknown zone type: " + type);
        }
    }
}
