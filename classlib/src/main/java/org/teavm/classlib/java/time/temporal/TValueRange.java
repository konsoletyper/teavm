/*
 *  Copyright 2020 adopted to TeaVM by Joerg Hohwiller
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
package org.teavm.classlib.java.time.temporal;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.TDateTimeException;

public final class TValueRange implements TSerializable {

    private final long minSmallest;

    private final long minLargest;

    private final long maxSmallest;

    private final long maxLargest;

    public static TValueRange of(long min, long max) {

        if (min > max) {
            throw new IllegalArgumentException("Minimum value must be less than maximum value");
        }
        return new TValueRange(min, min, max, max);
    }

    public static TValueRange of(long min, long maxSmallest, long maxLargest) {

        return of(min, min, maxSmallest, maxLargest);
    }

    public static TValueRange of(long minSmallest, long minLargest, long maxSmallest, long maxLargest) {

        if (minSmallest > minLargest) {
            throw new IllegalArgumentException("Smallest minimum value must be less than largest minimum value");
        }
        if (maxSmallest > maxLargest) {
            throw new IllegalArgumentException("Smallest maximum value must be less than largest maximum value");
        }
        if (minLargest > maxLargest) {
            throw new IllegalArgumentException("Minimum value must be less than maximum value");
        }
        return new TValueRange(minSmallest, minLargest, maxSmallest, maxLargest);
    }

    private TValueRange(long minSmallest, long minLargest, long maxSmallest, long maxLargest) {

        this.minSmallest = minSmallest;
        this.minLargest = minLargest;
        this.maxSmallest = maxSmallest;
        this.maxLargest = maxLargest;
    }

    public boolean isFixed() {

        return this.minSmallest == this.minLargest && this.maxSmallest == this.maxLargest;
    }

    public long getMinimum() {

        return this.minSmallest;
    }

    public long getLargestMinimum() {

        return this.minLargest;
    }

    public long getSmallestMaximum() {

        return this.maxSmallest;
    }

    public long getMaximum() {

        return this.maxLargest;
    }

    public boolean isIntValue() {

        return getMinimum() >= Integer.MIN_VALUE && getMaximum() <= Integer.MAX_VALUE;
    }

    public boolean isValidValue(long value) {

        return (value >= getMinimum() && value <= getMaximum());
    }

    public boolean isValidIntValue(long value) {

        return isIntValue() && isValidValue(value);
    }

    public long checkValidValue(long value, TTemporalField field) {

        if (isValidValue(value) == false) {
            if (field != null) {
                throw new TDateTimeException("Invalid value for " + field + " (valid values " + this + "): " + value);
            } else {
                throw new TDateTimeException("Invalid value (valid values " + this + "): " + value);
            }
        }
        return value;
    }

    public int checkValidIntValue(long value, TTemporalField field) {

        if (isValidIntValue(value) == false) {
            throw new TDateTimeException("Invalid int value for " + field + ": " + value);
        }
        return (int) value;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this) {
            return true;
        }
        if (obj instanceof TValueRange) {
            TValueRange other = (TValueRange) obj;
            return this.minSmallest == other.minSmallest && this.minLargest == other.minLargest
                    && this.maxSmallest == other.maxSmallest && this.maxLargest == other.maxLargest;
        }
        return false;
    }

    @Override
    public int hashCode() {

        long hash = this.minSmallest + this.minLargest << 16 + this.minLargest >> 48 + this.maxSmallest << 32
                + this.maxSmallest >> 32 + this.maxLargest << 48 + this.maxLargest >> 16;
        return (int) (hash ^ (hash >>> 32));
    }

    @Override
    public String toString() {

        StringBuilder buf = new StringBuilder();
        buf.append(this.minSmallest);
        if (this.minSmallest != this.minLargest) {
            buf.append('/').append(this.minLargest);
        }
        buf.append(" - ").append(this.maxSmallest);
        if (this.maxSmallest != this.maxLargest) {
            buf.append('/').append(this.maxLargest);
        }
        return buf.toString();
    }

}
