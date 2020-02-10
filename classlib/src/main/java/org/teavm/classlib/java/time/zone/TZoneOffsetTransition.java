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
package org.teavm.classlib.java.time.zone;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TZoneOffset;

public final class TZoneOffsetTransition implements Comparable<TZoneOffsetTransition>, TSerializable {

    private final TLocalDateTime transition;

    private final TZoneOffset offsetBefore;

    private final TZoneOffset offsetAfter;

    public static TZoneOffsetTransition of(TLocalDateTime transition, TZoneOffset offsetBefore,
            TZoneOffset offsetAfter) {

        Objects.requireNonNull(transition, "transition");
        Objects.requireNonNull(offsetBefore, "offsetBefore");
        Objects.requireNonNull(offsetAfter, "offsetAfter");
        if (offsetBefore.equals(offsetAfter)) {
            throw new IllegalArgumentException("Offsets must not be equal");
        }
        if (transition.getNano() != 0) {
            throw new IllegalArgumentException("Nano-of-second must be zero");
        }
        return new TZoneOffsetTransition(transition, offsetBefore, offsetAfter);
    }

    TZoneOffsetTransition(TLocalDateTime transition, TZoneOffset offsetBefore, TZoneOffset offsetAfter) {

        this.transition = transition;
        this.offsetBefore = offsetBefore;
        this.offsetAfter = offsetAfter;
    }

    TZoneOffsetTransition(long epochSecond, TZoneOffset offsetBefore, TZoneOffset offsetAfter) {

        this.transition = TLocalDateTime.ofEpochSecond(epochSecond, 0, offsetBefore);
        this.offsetBefore = offsetBefore;
        this.offsetAfter = offsetAfter;
    }

    public TInstant getInstant() {

        return this.transition.toInstant(this.offsetBefore);
    }

    public long toEpochSecond() {

        return this.transition.toEpochSecond(this.offsetBefore);
    }

    public TLocalDateTime getDateTimeBefore() {

        return this.transition;
    }

    public TLocalDateTime getDateTimeAfter() {

        return this.transition.plusSeconds(getDurationSeconds());
    }

    public TZoneOffset getOffsetBefore() {

        return this.offsetBefore;
    }

    public TZoneOffset getOffsetAfter() {

        return this.offsetAfter;
    }

    public TDuration getDuration() {

        return TDuration.ofSeconds(getDurationSeconds());
    }

    private int getDurationSeconds() {

        return getOffsetAfter().getTotalSeconds() - getOffsetBefore().getTotalSeconds();
    }

    public boolean isGap() {

        return getOffsetAfter().getTotalSeconds() > getOffsetBefore().getTotalSeconds();
    }

    public boolean isOverlap() {

        return getOffsetAfter().getTotalSeconds() < getOffsetBefore().getTotalSeconds();
    }

    public boolean isValidOffset(TZoneOffset offset) {

        return isGap() ? false : (getOffsetBefore().equals(offset) || getOffsetAfter().equals(offset));
    }

    List<TZoneOffset> getValidOffsets() {

        if (isGap()) {
            return Collections.emptyList();
        }
        return Arrays.asList(getOffsetBefore(), getOffsetAfter());
    }

    @Override
    public int compareTo(TZoneOffsetTransition transition) {

        return getInstant().compareTo(transition.getInstant());
    }

    @Override
    public boolean equals(Object other) {

        if (other == this) {
            return true;
        }
        if (other instanceof TZoneOffsetTransition) {
            TZoneOffsetTransition d = (TZoneOffsetTransition) other;
            return this.transition.equals(d.transition) && this.offsetBefore.equals(d.offsetBefore)
                    && this.offsetAfter.equals(d.offsetAfter);
        }
        return false;
    }

    @Override
    public int hashCode() {

        return this.transition.hashCode() ^ this.offsetBefore.hashCode()
                ^ Integer.rotateLeft(this.offsetAfter.hashCode(), 16);
    }

    @Override
    public String toString() {

        StringBuilder buf = new StringBuilder();
        buf.append("Transition[").append(isGap() ? "Gap" : "Overlap").append(" at ").append(this.transition)
                .append(this.offsetBefore).append(" to ").append(this.offsetAfter).append(']');
        return buf.toString();
    }

}
