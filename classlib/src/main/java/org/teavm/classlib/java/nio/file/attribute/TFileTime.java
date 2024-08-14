/*
 *  Copyright 2024 Jonathan Coates.
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
package org.teavm.classlib.java.nio.file.attribute;

import org.teavm.classlib.java.util.concurrent.TTimeUnit;
import org.threeten.bp.Instant;

public class TFileTime implements Comparable<TFileTime> {
    private final long value;
    private final TTimeUnit unit;
    private Instant instant;

    private TFileTime(long value, TTimeUnit unit, Instant instant) {
        this.value = value;
        this.unit = unit;
    }

    public static TFileTime from(long value, TTimeUnit timeUnit) {
        return new TFileTime(value, timeUnit, null);
    }

    public static TFileTime from(Instant instant) {
        return new TFileTime(instant.toEpochMilli(), TTimeUnit.MILLISECONDS, instant);
    }

    public static TFileTime fromMillis(long value) {
        return new TFileTime(value, TTimeUnit.MILLISECONDS, null);
    }

    public long toMillis() {
        return unit.toMillis(value);
    }

    public long to(TTimeUnit target) {
        return target.convert(value, unit);
    }

    public Instant toInstant() {
        if (instant == null) {
            instant = Instant.ofEpochMilli(toMillis());
        }
        return instant;
    }

    @Override
    public int compareTo(TFileTime o) {
        return toInstant().compareTo(o.toInstant());
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof TFileTime && toInstant().equals(((TFileTime) obj).toInstant()));
    }

    @Override
    public int hashCode() {
        return toInstant().hashCode();
    }
}
