/*
 *  Copyright 2001-2005 Stephen Colebourne
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

/**
 * Basic DateTimeZone implementation that has a fixed name key and offsets.
 * <p>
 * FixedDateTimeZone is thread-safe and immutable.
 *
 * @author Brian S O'Neill
 * @since 1.0
 */
public final class FixedDateTimeZone extends StorableDateTimeZone {
    private final int iWallOffset;
    private final int iStandardOffset;

    public FixedDateTimeZone(String id, int wallOffset, int standardOffset) {
        super(id);
        iWallOffset = wallOffset;
        iStandardOffset = standardOffset;
    }

    @Override
    public int getOffset(long instant) {
        return iWallOffset;
    }

    @Override
    public int getStandardOffset(long instant) {
        return iStandardOffset;
    }

    @Override
    public int getOffsetFromLocal(long instantLocal) {
        return iWallOffset;
    }

    @Override
    public boolean isFixed() {
        return true;
    }

    @Override
    public long nextTransition(long instant) {
        return instant;
    }

    @Override
    public long previousTransition(long instant) {
        return instant;
    }

    @Override
    public void write(StringBuilder sb) {
        Base46.encodeUnsigned(sb, FIXED);
        writeTime(sb, iWallOffset);
        writeTime(sb, iStandardOffset);
    }

    public static FixedDateTimeZone readZone(String id, CharFlow flow) {
        int wallOffset = (int) readTime(flow);
        int standardOffset = (int) readTime(flow);
        return new FixedDateTimeZone(id, wallOffset, standardOffset);
    }
}
