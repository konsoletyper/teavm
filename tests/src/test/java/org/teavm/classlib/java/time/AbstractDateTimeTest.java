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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.temporal.TTemporalField;
import org.teavm.classlib.java.time.temporal.TTemporalQuery;

public abstract class AbstractDateTimeTest extends AbstractTest {

    protected abstract List<TTemporalAccessor> samples();

    protected abstract List<TTemporalField> validFields();

    protected abstract List<TTemporalField> invalidFields();

    @Test
    public void basicTest_isSupported_DateTimeField_supported() {

        for (TTemporalAccessor sample : samples()) {
            for (TTemporalField field : validFields()) {
                assertEquals("Failed on " + sample + " " + field, sample.isSupported(field), true);
            }
        }
    }

    @Test
    public void basicTest_isSupported_DateTimeField_unsupported() {

        for (TTemporalAccessor sample : samples()) {
            for (TTemporalField field : invalidFields()) {
                assertEquals("Failed on " + sample + " " + field, sample.isSupported(field), false);
            }
        }
    }

    @Test
    public void basicTest_isSupported_DateTimeField_null() {

        for (TTemporalAccessor sample : samples()) {
            assertEquals("Failed on " + sample, sample.isSupported(null), false);
        }
    }

    @Test
    public void basicTest_range_DateTimeField_unsupported() {

        for (TTemporalAccessor sample : samples()) {
            for (TTemporalField field : invalidFields()) {
                try {
                    sample.range(field);
                    fail("Failed on " + sample + " " + field);
                } catch (TDateTimeException ex) {
                    // expected
                }
            }
        }
    }

    @Test
    public void basicTest_range_DateTimeField_null() {

        for (TTemporalAccessor sample : samples()) {
            try {
                sample.range(null);
                fail("Failed on " + sample);
            } catch (NullPointerException ex) {
                // expected
            }
        }
    }

    @Test
    public void basicTest_get_DateTimeField_unsupported() {

        for (TTemporalAccessor sample : samples()) {
            for (TTemporalField field : invalidFields()) {
                try {
                    sample.get(field);
                    fail("Failed on " + sample + " " + field);
                } catch (TDateTimeException ex) {
                    // expected
                }
            }
        }
    }

    @Test
    public void basicTest_get_DateTimeField_null() {

        for (TTemporalAccessor sample : samples()) {
            try {
                sample.get(null);
                fail("Failed on " + sample);
            } catch (NullPointerException ex) {
                // expected
            }
        }
    }

    @Test
    public void basicTest_getLong_DateTimeField_unsupported() {

        for (TTemporalAccessor sample : samples()) {
            for (TTemporalField field : invalidFields()) {
                try {
                    sample.getLong(field);
                    fail("Failed on " + sample + " " + field);
                } catch (TDateTimeException ex) {
                    // expected
                }
            }
        }
    }

    @Test
    public void basicTest_getLong_DateTimeField_null() {

        for (TTemporalAccessor sample : samples()) {
            try {
                sample.getLong(null);
                fail("Failed on " + sample);
            } catch (NullPointerException ex) {
                // expected
            }
        }
    }

    @Test
    public void basicTest_query() {

        for (TTemporalAccessor sample : samples()) {
            assertEquals(sample.query(new TTemporalQuery<String>() {
                @Override
                public String queryFrom(TTemporalAccessor dateTime) {

                    return "foo";
                }
            }), "foo");
        }
    }

}
