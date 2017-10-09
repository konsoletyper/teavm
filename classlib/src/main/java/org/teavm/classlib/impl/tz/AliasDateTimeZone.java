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

public class AliasDateTimeZone extends StorableDateTimeZone {
    private DateTimeZone innerZone;

    public AliasDateTimeZone(String id, DateTimeZone innerZone) {
        super(id);
        this.innerZone = innerZone;
    }

    @Override
    public int getOffset(long instant) {
        return innerZone.getOffset(instant);
    }

    @Override
    public int getStandardOffset(long instant) {
        return innerZone.getStandardOffset(instant);
    }

    @Override
    public boolean isFixed() {
        return innerZone.isFixed();
    }

    @Override
    public long nextTransition(long instant) {
        return innerZone.nextTransition(instant);
    }

    @Override
    public long previousTransition(long instant) {
        return innerZone.previousTransition(instant);
    }

    @Override
    public void write(StringBuilder sb) {
        Base46.encodeUnsigned(sb, ALIAS);
        sb.append(innerZone.getID());
    }
}
