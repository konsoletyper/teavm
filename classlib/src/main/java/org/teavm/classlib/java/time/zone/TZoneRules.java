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

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.teavm.classlib.java.io.TSerializable;
import org.teavm.classlib.java.time.TDuration;
import org.teavm.classlib.java.time.TInstant;
import org.teavm.classlib.java.time.TLocalDateTime;
import org.teavm.classlib.java.time.TZoneOffset;

public abstract class TZoneRules {

    public static TZoneRules of(TZoneOffset baseStandardOffset, TZoneOffset baseWallOffset,
            List<TZoneOffsetTransition> standardOffsetTransitionList, List<TZoneOffsetTransition> transitionList,
            List<TZoneOffsetTransitionRule> lastRules) {

        Objects.requireNonNull(baseStandardOffset, "baseStandardOffset");
        Objects.requireNonNull(baseWallOffset, "baseWallOffset");
        Objects.requireNonNull(standardOffsetTransitionList, "standardOffsetTransitionList");
        Objects.requireNonNull(transitionList, "transitionList");
        Objects.requireNonNull(lastRules, "lastRules");
        return new TStandardZoneRules(baseStandardOffset, baseWallOffset, standardOffsetTransitionList, transitionList,
                lastRules);
    }

    public static TZoneRules of(TZoneOffset offset) {

        Objects.requireNonNull(offset, "offset");
        return new Fixed(offset);
    }

    TZoneRules() {

    }

    public abstract boolean isFixedOffset();

    public abstract TZoneOffset getOffset(TInstant instant);

    public abstract TZoneOffset getOffset(TLocalDateTime localDateTime);

    public abstract List<TZoneOffset> getValidOffsets(TLocalDateTime localDateTime);

    public abstract TZoneOffsetTransition getTransition(TLocalDateTime localDateTime);

    public abstract TZoneOffset getStandardOffset(TInstant instant);

    public TDuration getDaylightSavings(TInstant instant) {

        TZoneOffset standardOffset = getStandardOffset(instant);
        TZoneOffset actualOffset = getOffset(instant);
        return TDuration.ofSeconds(actualOffset.getTotalSeconds() - standardOffset.getTotalSeconds());
    }

    public boolean isDaylightSavings(TInstant instant) {

        return (getStandardOffset(instant).equals(getOffset(instant)) == false);
    }

    public boolean isValidOffset(TLocalDateTime localDateTime, TZoneOffset offset) {

        return getValidOffsets(localDateTime).contains(offset);
    }

    public abstract TZoneOffsetTransition nextTransition(TInstant instant);

    public abstract TZoneOffsetTransition previousTransition(TInstant instant);

    public abstract List<TZoneOffsetTransition> getTransitions();

    public abstract List<TZoneOffsetTransitionRule> getTransitionRules();

    @Override
    public abstract boolean equals(Object otherRules);

    @Override
    public abstract int hashCode();

    static final class Fixed extends TZoneRules implements TSerializable {

        private final TZoneOffset offset;

        Fixed(TZoneOffset offset) {

            this.offset = offset;
        }

        @Override
        public boolean isFixedOffset() {

            return true;
        }

        @Override
        public TZoneOffset getOffset(TInstant instant) {

            return this.offset;
        }

        @Override
        public TZoneOffset getOffset(TLocalDateTime localDateTime) {

            return this.offset;
        }

        @Override
        public List<TZoneOffset> getValidOffsets(TLocalDateTime localDateTime) {

            return Collections.singletonList(this.offset);
        }

        @Override
        public TZoneOffsetTransition getTransition(TLocalDateTime localDateTime) {

            return null;
        }

        @Override
        public boolean isValidOffset(TLocalDateTime dateTime, TZoneOffset offset) {

            return this.offset.equals(offset);
        }

        @Override
        public TZoneOffset getStandardOffset(TInstant instant) {

            return this.offset;
        }

        @Override
        public TDuration getDaylightSavings(TInstant instant) {

            return TDuration.ZERO;
        }

        @Override
        public boolean isDaylightSavings(TInstant instant) {

            return false;
        }

        @Override
        public TZoneOffsetTransition nextTransition(TInstant instant) {

            return null;
        }

        @Override
        public TZoneOffsetTransition previousTransition(TInstant instant) {

            return null;
        }

        @Override
        public List<TZoneOffsetTransition> getTransitions() {

            return Collections.emptyList();
        }

        @Override
        public List<TZoneOffsetTransitionRule> getTransitionRules() {

            return Collections.emptyList();
        }

        @Override
        public boolean equals(Object obj) {

            if (this == obj) {
                return true;
            }
            if (obj instanceof Fixed) {
                return this.offset.equals(((Fixed) obj).offset);
            }
            if (obj instanceof TStandardZoneRules) {
                TStandardZoneRules szr = (TStandardZoneRules) obj;
                return szr.isFixedOffset() && this.offset.equals(szr.getOffset(TInstant.EPOCH));
            }
            return false;
        }

        @Override
        public int hashCode() {

            return 1 ^ (31 + this.offset.hashCode()) ^ 1 ^ (31 + this.offset.hashCode()) ^ 1;
        }

        @Override
        public String toString() {

            return "FixedRules:" + this.offset;
        }
    }

}
