/*
 *  Copyright 2020 Alexey Andreev.
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
package org.teavm.classlib.java.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAccessor;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneRulesException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.teavm.junit.TeaVMTest;

/**
 * Test ZoneId.
 */
@TeaVMTest
public class TestZoneId extends AbstractTest {

    private static final ZoneId ZONE_PARIS = ZoneId.of("Europe/Paris");
    public static final String LATEST_TZDB = "2010i";
    private static final int OVERLAP = 2;
    private static final int GAP = 0;

    //-----------------------------------------------------------------------
    // UTC
    //-----------------------------------------------------------------------
    @Test
    public void test_constant_UTC() {
        ZoneId test = ZoneOffset.UTC;
        assertEquals("Z", test.getId());
        assertEquals("Z", test.getDisplayName(TextStyle.FULL, Locale.UK));
        assertTrue(test.getRules().isFixedOffset());
        assertEquals(ZoneOffset.UTC, test.getRules().getOffset(Instant.ofEpochSecond(0L)));
        checkOffset(test.getRules(), createLDT(2008, 6, 30), ZoneOffset.UTC, 1);
    }

    //-----------------------------------------------------------------------
    // SHORT_IDS
    //-----------------------------------------------------------------------
    @Test
    public void test_constant_SHORT_IDS() {
        Map<String, String> ids = ZoneId.SHORT_IDS;
        //assertEquals(ids.get("EST"), "-05:00");
        //assertEquals(ids.get("MST"), "-07:00");
        //assertEquals(ids.get("HST"), "-10:00");
        assertEquals("Australia/Darwin", ids.get("ACT"));
        assertEquals("Australia/Sydney", ids.get("AET"));
        assertEquals("America/Argentina/Buenos_Aires", ids.get("AGT"));
        assertEquals("Africa/Cairo", ids.get("ART"));
        assertEquals("America/Anchorage", ids.get("AST"));
        assertEquals("America/Sao_Paulo", ids.get("BET"));
        assertEquals("Asia/Dhaka", ids.get("BST"));
        assertEquals("Africa/Harare", ids.get("CAT"));
        assertEquals("America/St_Johns", ids.get("CNT"));
        assertEquals("America/Chicago", ids.get("CST"));
        assertEquals("Asia/Shanghai", ids.get("CTT"));
        assertEquals("Africa/Addis_Ababa", ids.get("EAT"));
        assertEquals("Europe/Paris", ids.get("ECT"));
        assertEquals("America/Indiana/Indianapolis", ids.get("IET"));
        assertEquals("Asia/Kolkata", ids.get("IST"));
        assertEquals("Asia/Tokyo", ids.get("JST"));
        assertEquals("Pacific/Apia", ids.get("MIT"));
        assertEquals("Asia/Yerevan", ids.get("NET"));
        assertEquals("Pacific/Auckland", ids.get("NST"));
        assertEquals("Asia/Karachi", ids.get("PLT"));
        assertEquals("America/Phoenix", ids.get("PNT"));
        assertEquals("America/Puerto_Rico", ids.get("PRT"));
        assertEquals("America/Los_Angeles", ids.get("PST"));
        assertEquals("Pacific/Guadalcanal", ids.get("SST"));
        assertEquals("Asia/Ho_Chi_Minh", ids.get("VST"));
    }

    @Test
    public void test_constant_SHORT_IDS_immutable() {
        Map<String, String> ids = ZoneId.SHORT_IDS;
        assertThrows(UnsupportedOperationException.class, () -> ids.clear());
    }

    //-----------------------------------------------------------------------
    // system default
    //-----------------------------------------------------------------------
    @Test
    public void test_systemDefault() {
        ZoneId test = ZoneId.systemDefault();
        assertEquals(TimeZone.getDefault().getID(), test.getId());
    }

    // TODO: support SimpleTimeZone and unignore
    @Test
    @Disabled
    public void test_systemDefault_unableToConvert_badFormat() {
        assertThrows(DateTimeException.class, () -> {
            TimeZone current = TimeZone.getDefault();
            try {
                TimeZone.setDefault(new SimpleTimeZone(127, "Something Weird"));
                ZoneId.systemDefault();
            } finally {
                TimeZone.setDefault(current);
            }
        });
    }

    // TODO: support SimpleTimeZone and unignore
    @Test
    @Disabled
    public void test_systemDefault_unableToConvert_unknownId() {
        assertThrows(ZoneRulesException.class, () -> {
            TimeZone current = TimeZone.getDefault();
            try {
                TimeZone.setDefault(new SimpleTimeZone(127, "SomethingWeird"));
                ZoneId.systemDefault();
            } finally {
                TimeZone.setDefault(current);
            }
        });
    }

    //-----------------------------------------------------------------------
    // mapped factory
    //-----------------------------------------------------------------------
    @Test
    public void test_of_string_Map() {
        Map<String, String> map = new HashMap<>();
        map.put("LONDON", "Europe/London");
        map.put("PARIS", "Europe/Paris");
        ZoneId test = ZoneId.of("LONDON", map);
        assertEquals("Europe/London", test.getId());
    }

    @Test
    public void test_of_string_Map_lookThrough() {
        Map<String, String> map = new HashMap<>();
        map.put("LONDON", "Europe/London");
        map.put("PARIS", "Europe/Paris");
        ZoneId test = ZoneId.of("Europe/Madrid", map);
        assertEquals("Europe/Madrid", test.getId());
    }

    @Test
    public void test_of_string_Map_emptyMap() {
        Map<String, String> map = new HashMap<>();
        ZoneId test = ZoneId.of("Europe/Madrid", map);
        assertEquals("Europe/Madrid", test.getId());
    }

    @Test
    public void test_of_string_Map_badFormat() {
        Map<String, String> map = new HashMap<>();
        assertThrows(DateTimeException.class, () -> ZoneId.of("Not kknown", map));
    }

    @Test
    public void test_of_string_Map_unknown() {
        Map<String, String> map = new HashMap<>();
        assertThrows(ZoneRulesException.class, () -> ZoneId.of("Unknown", map));
    }

    //-----------------------------------------------------------------------
    // regular factory
    //-----------------------------------------------------------------------
    static Object[][] data_of_string_UTC() {
        return new Object[][] {
            {""},
            {"+00"}, {"+0000"}, {"+00:00"}, {"+000000"}, {"+00:00:00"},
            {"-00"}, {"-0000"}, {"-00:00"}, {"-000000"}, {"-00:00:00"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_of_string_UTC")
    public void test_of_string_UTC(String id) {
        ZoneId test = ZoneId.of("UTC" + id);
        assertEquals("UTC", test.getId());
        assertEquals(ZoneOffset.UTC, test.normalized());
    }

    @ParameterizedTest
    @MethodSource("data_of_string_UTC")
    public void test_of_string_GMT(String id) {
        ZoneId test = ZoneId.of("GMT" + id);
        assertEquals("GMT", test.getId());
        assertEquals(ZoneOffset.UTC, test.normalized());
    }

    @ParameterizedTest
    @MethodSource("data_of_string_UTC")
    public void test_of_string_UT(String id) {
        ZoneId test = ZoneId.of("UT" + id);
        assertEquals("UT", test.getId());
        assertEquals(ZoneOffset.UTC, test.normalized());
    }

    //-----------------------------------------------------------------------
    static Object[][] data_of_string_Fixed() {
        return new Object[][] {
            {"+0", ""},
            {"+5", "+05:00"},
            {"+01", "+01:00"},
            {"+0100", "+01:00"}, {"+01:00", "+01:00"},
            {"+010000", "+01:00"}, {"+01:00:00", "+01:00"},
            {"+12", "+12:00"},
            {"+1234", "+12:34"}, {"+12:34", "+12:34"},
            {"+123456", "+12:34:56"}, { "+12:34:56", "+12:34:56"},
            {"-02", "-02:00"},
            {"-5", "-05:00"},
            {"-0200", "-02:00"}, {"-02:00", "-02:00"},
            {"-020000", "-02:00"}, {"-02:00:00", "-02:00"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_of_string_Fixed")
    public void test_of_string_offset(String input, String id) {
        ZoneId test = ZoneId.of(input);
        ZoneOffset offset = ZoneOffset.of(id.isEmpty() ? "Z" : id);
        assertEquals(offset, test);
    }

    // TODO: JVM returns "Coordinated Universal Time" here
    @ParameterizedTest
    @MethodSource("data_of_string_Fixed")
    @Disabled
    public void test_of_string_FixedUTC(String input, String id) {
        ZoneId test = ZoneId.of("UTC" + input);
        assertEquals("UTC" + id, test.getId());
        assertEquals("UTC" + id, test.getDisplayName(TextStyle.FULL, Locale.UK));
        assertTrue(test.getRules().isFixedOffset());
        ZoneOffset offset = ZoneOffset.of(id.isEmpty() ? "Z" : id);
        assertEquals(offset, test.getRules().getOffset(Instant.ofEpochSecond(0L)));
        checkOffset(test.getRules(), createLDT(2008, 6, 30), offset, 1);
    }

    // TODO: JVM returns "Greenwich Mean Time" here
    @ParameterizedTest
    @MethodSource("data_of_string_Fixed")
    @Disabled
    public void test_of_string_FixedGMT(String input, String id) {
        ZoneId test = ZoneId.of("GMT" + input);
        assertEquals("GMT" + id, test.getId());
        assertEquals("GMT" + id, test.getDisplayName(TextStyle.FULL, Locale.UK));
        assertTrue(test.getRules().isFixedOffset());
        ZoneOffset offset = ZoneOffset.of(id.isEmpty() ? "Z" : id);
        assertEquals(offset, test.getRules().getOffset(Instant.ofEpochSecond(0L)));
        checkOffset(test.getRules(), createLDT(2008, 6, 30), offset, 1);
    }

    @ParameterizedTest
    @MethodSource("data_of_string_Fixed")
    public void test_of_string_FixedUT(String input, String id) {
        ZoneId test = ZoneId.of("UT" + input);
        assertEquals("UT" + id, test.getId());
        assertEquals("UT" + id, test.getDisplayName(TextStyle.FULL, Locale.UK));
        assertTrue(test.getRules().isFixedOffset());
        ZoneOffset offset = ZoneOffset.of(id.isEmpty() ? "Z" : id);
        assertEquals(offset, test.getRules().getOffset(Instant.ofEpochSecond(0L)));
        checkOffset(test.getRules(), createLDT(2008, 6, 30), offset, 1);
    }

    //-----------------------------------------------------------------------
    static Object[][] data_of_string_UTC_invalid() {
        return new Object[][] {
                {"A"}, {"B"}, {"C"}, {"D"}, {"E"}, {"F"}, {"G"}, {"H"}, {"I"}, {"J"}, {"K"}, {"L"}, {"M"},
                {"N"}, {"O"}, {"P"}, {"Q"}, {"R"}, {"S"}, {"T"}, {"U"}, {"V"}, {"W"}, {"X"}, {"Y"},
                {"+0:00"}, {"+00:0"}, {"+0:0"},
                {"+000"}, {"+00000"},
                {"+0:00:00"}, {"+00:0:00"}, {"+00:00:0"}, {"+0:0:0"}, {"+0:0:00"}, {"+00:0:0"}, {"+0:00:0"},
                {"+01_00"}, {"+01;00"}, {"+01@00"}, {"+01:AA"},
                {"+19"}, {"+19:00"}, {"+18:01"}, {"+18:00:01"}, {"+1801"}, {"+180001"},
                {"-0:00"}, {"-00:0"}, {"-0:0"},
                {"-000"}, {"-00000"},
                {"-0:00:00"}, {"-00:0:00"}, {"-00:00:0"}, {"-0:0:0"}, {"-0:0:00"}, {"-00:0:0"}, {"-0:00:0"},
                {"-19"}, {"-19:00"}, {"-18:01"}, {"-18:00:01"}, {"-1801"}, {"-180001"},
                {"-01_00"}, {"-01;00"}, {"-01@00"}, {"-01:AA"},
                {"@01:00"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_of_string_UTC_invalid")
    public void test_of_string_UTC_invalid(String id) {
        assertThrows(DateTimeException.class, () -> ZoneId.of("UTC" + id));
    }

    @ParameterizedTest
    @MethodSource("data_of_string_UTC_invalid")
    public void test_of_string_GMT_invalid(String id) {
        assertThrows(DateTimeException.class, () -> ZoneId.of("GMT" + id));
    }

    //-----------------------------------------------------------------------
    static Object[][] data_of_string_invalid() {
        // \u00ef is a random unicode character
        return new Object[][] {
                {""}, {":"}, {"#"},
                {"\u00ef"}, {"`"}, {"!"}, {"\""}, {"\u00ef"}, {"$"}, {"^"}, {"&"}, {"*"}, {"("}, {")"}, {"="},
                {"\\"}, {"|"}, {","}, {"<"}, {">"}, {"?"}, {";"}, {"'"}, {"["}, {"]"}, {"{"}, {"}"},
                {"\u00ef:A"}, {"`:A"}, {"!:A"}, {"\":A"}, {"\u00ef:A"}, {"$:A"}, {"^:A"}, {"&:A"}, {"*:A"}, {"(:A"},
                        {"):A"}, {"=:A"}, {"+:A"},
                {"\\:A"}, {"|:A"}, {",:A"}, {"<:A"}, {">:A"}, {"?:A"}, {";:A"}, {"::A"}, {"':A"}, {"@:A"}, {"~:A"},
                        {"[:A"}, {"]:A"}, {"{:A"}, {"}:A"},
                {"A:B#\u00ef"}, {"A:B#`"}, {"A:B#!"}, {"A:B#\""}, {"A:B#\u00ef"}, {"A:B#$"}, {"A:B#^"}, {"A:B#&"},
                        {"A:B#*"},
                {"A:B#("}, {"A:B#)"}, {"A:B#="}, {"A:B#+"},
                {"A:B#\\"}, {"A:B#|"}, {"A:B#,"}, {"A:B#<"}, {"A:B#>"}, {"A:B#?"}, {"A:B#;"}, {"A:B#:"},
                {"A:B#'"}, {"A:B#@"}, {"A:B#~"}, {"A:B#["}, {"A:B#]"}, {"A:B#{"}, {"A:B#}"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_of_string_invalid")
    public void test_of_string_invalid(String id) {
        assertThrows(DateTimeException.class, () -> ZoneId.of(id));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_of_string_GMT0() {
        ZoneId test = ZoneId.of("GMT0");
        assertEquals("GMT0", test.getId());
        assertTrue(test.getRules().isFixedOffset());
        assertEquals(ZoneOffset.UTC, test.normalized());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_of_string_London() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals("Europe/London", test.getId());
        assertFalse(test.getRules().isFixedOffset());
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_of_string_null() {
        assertThrows(NullPointerException.class, () -> ZoneId.of((String) null));
    }

    @Test
    public void test_of_string_unknown_simple() {
        assertThrows(ZoneRulesException.class, () -> ZoneId.of("Unknown"));
    }

    //-------------------------------------------------------------------------
    // TODO: test by deserialization
//    public void test_ofUnchecked_string_invalidNotChecked() {
//        ZoneRegion test = ZoneRegion.ofLenient("Unknown");
//        assertEquals(test.getId(), "Unknown");
//    }
//
//    public void test_ofUnchecked_string_invalidNotChecked_unusualCharacters() {
//        ZoneRegion test = ZoneRegion.ofLenient("QWERTYUIOPASDFGHJKLZXCVBNM~/._+-");
//        assertEquals(test.getId(), "QWERTYUIOPASDFGHJKLZXCVBNM~/._+-");
//    }

    //-----------------------------------------------------------------------
    // from()
    //-----------------------------------------------------------------------
    @Test
    public void test_factory_CalendricalObject() {
        assertEquals(ZONE_PARIS, ZoneId.from(createZDT(2007, 7, 15, 17, 30, 0, 0, ZONE_PARIS)));
    }

    @Test
    public void test_factory_CalendricalObject_invalid_noDerive() {
        assertThrows(DateTimeException.class, () -> ZoneId.from(LocalTime.of(12, 30)));
    }

    @Test
    public void test_factory_CalendricalObject_null() {
        assertThrows(NullPointerException.class, () -> ZoneId.from((TemporalAccessor) null));
    }

    //-----------------------------------------------------------------------
    // Europe/London
    //-----------------------------------------------------------------------
    @Test
    public void test_London() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals("Europe/London", test.getId());
        assertFalse(test.getRules().isFixedOffset());
    }

    @Test
    public void test_London_getOffset() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 1, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 2, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 3, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 4, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 5, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 6, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 7, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 8, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 9, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 10, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 11, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 12, 1, ZoneOffset.UTC)));
    }

    @Test
    public void test_London_getOffset_toDST() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 3, 24, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 3, 25, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 3, 26, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 3, 27, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 3, 28, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 3, 29, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 3, 30, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 3, 31, ZoneOffset.UTC)));
        // cutover at 01:00Z
        assertEquals(ZoneOffset.ofHours(0),
                test.getRules().getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1),
                test.getRules().getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    public void test_London_getOffset_fromDST() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 10, 24, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 10, 25, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 10, 26, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 10, 27, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 10, 28, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 10, 29, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 10, 30, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0), test.getRules().getOffset(createInstant(2008, 10, 31, ZoneOffset.UTC)));
        // cutover at 01:00Z
        assertEquals(ZoneOffset.ofHours(1),
                test.getRules().getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(0),
                test.getRules().getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    public void test_London_getOffsetInfo() {
        ZoneId test = ZoneId.of("Europe/London");
        checkOffset(test.getRules(), createLDT(2008, 1, 1), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 1), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 1), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 1), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 1), ZoneOffset.ofHours(0), 1);
    }

    @Test
    public void test_London_getOffsetInfo_toDST() {
        ZoneId test = ZoneId.of("Europe/London");
        checkOffset(test.getRules(), createLDT(2008, 3, 24), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 25), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 26), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 27), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 28), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 29), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 30), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 31), ZoneOffset.ofHours(1), 1);
        // cutover at 01:00Z
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 0, 59, 59, 999999999), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 1, 30, 0, 0), ZoneOffset.ofHours(0), GAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 2, 0, 0, 0), ZoneOffset.ofHours(1), 1);
    }

    @Test
    public void test_London_getOffsetInfo_fromDST() {
        ZoneId test = ZoneId.of("Europe/London");
        checkOffset(test.getRules(), createLDT(2008, 10, 24), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 25), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 26), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 27), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 28), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 29), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 30), ZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 31), ZoneOffset.ofHours(0), 1);
        // cutover at 01:00Z
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 0, 59, 59, 999999999), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 1, 30, 0, 0), ZoneOffset.ofHours(1), OVERLAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 2, 0, 0, 0), ZoneOffset.ofHours(0), 1);
    }

    @Test
    public void test_London_getOffsetInfo_gap() {
        ZoneId test = ZoneId.of("Europe/London");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 3, 30, 1, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(0), GAP);
        assertTrue(trans.isGap());
        assertFalse(trans.isOverlap());
        assertEquals(ZoneOffset.ofHours(0), trans.getOffsetBefore());
        assertEquals(ZoneOffset.ofHours(1), trans.getOffsetAfter());
        assertEquals(dateTime.toInstant(ZoneOffset.UTC), trans.getInstant());
        assertEquals(LocalDateTime.of(2008, 3, 30, 1, 0), trans.getDateTimeBefore());
        assertEquals(LocalDateTime.of(2008, 3, 30, 2, 0), trans.getDateTimeAfter());
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(-1)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(0)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(1)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(2)));
        assertEquals("Transition[Gap at 2008-03-30T01:00Z to +01:00]", trans.toString());

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(0)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(otherTrans.hashCode(), trans.hashCode());
    }

    @Test
    public void test_London_getOffsetInfo_overlap() {
        ZoneId test = ZoneId.of("Europe/London");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 10, 26, 1, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(1), OVERLAP);
        assertFalse(trans.isGap());
        assertTrue(trans.isOverlap());
        assertEquals(ZoneOffset.ofHours(1), trans.getOffsetBefore());
        assertEquals(ZoneOffset.ofHours(0), trans.getOffsetAfter());
        assertEquals(dateTime.toInstant(ZoneOffset.UTC), trans.getInstant());
        assertEquals(LocalDateTime.of(2008, 10, 26, 2, 0), trans.getDateTimeBefore());
        assertEquals(LocalDateTime.of(2008, 10, 26, 1, 0), trans.getDateTimeAfter());
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(-1)));
        assertTrue(trans.isValidOffset(ZoneOffset.ofHours(0)));
        assertTrue(trans.isValidOffset(ZoneOffset.ofHours(1)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(2)));
        assertEquals("Transition[Overlap at 2008-10-26T02:00+01:00 to Z]", trans.toString());

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(1)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(otherTrans.hashCode(), trans.hashCode());
    }

    //-----------------------------------------------------------------------
    // Europe/Paris
    //-----------------------------------------------------------------------
    @Test
    public void test_Paris() {
        ZoneId test = ZoneId.of("Europe/Paris");
        assertEquals("Europe/Paris", test.getId());
        assertFalse(test.getRules().isFixedOffset());
    }

    @Test
    public void test_Paris_getOffset() {
        ZoneId test = ZoneId.of("Europe/Paris");
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 1, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 2, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 3, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 4, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 5, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 6, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 7, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 8, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 9, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 10, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 11, 1, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 12, 1, ZoneOffset.UTC)));
    }

    @Test
    public void test_Paris_getOffset_toDST() {
        ZoneId test = ZoneId.of("Europe/Paris");
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 3, 24, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 3, 25, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 3, 26, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 3, 27, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 3, 28, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 3, 29, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 3, 30, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 3, 31, ZoneOffset.UTC)));
        // cutover at 01:00Z
        assertEquals(ZoneOffset.ofHours(1),
                test.getRules().getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2),
                test.getRules().getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    public void test_Paris_getOffset_fromDST() {
        ZoneId test = ZoneId.of("Europe/Paris");
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 10, 24, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 10, 25, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(2), test.getRules().getOffset(createInstant(2008, 10, 26, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 10, 27, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 10, 28, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 10, 29, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 10, 30, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1), test.getRules().getOffset(createInstant(2008, 10, 31, ZoneOffset.UTC)));
        // cutover at 01:00Z
        assertEquals(ZoneOffset.ofHours(2),
                test.getRules().getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, ZoneOffset.UTC)));
        assertEquals(ZoneOffset.ofHours(1),
                test.getRules().getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, ZoneOffset.UTC)));
    }

    @Test
    public void test_Paris_getOffsetInfo() {
        ZoneId test = ZoneId.of("Europe/Paris");
        checkOffset(test.getRules(), createLDT(2008, 1, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 1), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 1), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 1), ZoneOffset.ofHours(1), 1);
    }

    @Test
    public void test_Paris_getOffsetInfo_toDST() {
        ZoneId test = ZoneId.of("Europe/Paris");
        checkOffset(test.getRules(), createLDT(2008, 3, 24), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 25), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 26), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 27), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 28), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 29), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 30), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 31), ZoneOffset.ofHours(2), 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 1, 59, 59, 999999999), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 2, 30, 0, 0), ZoneOffset.ofHours(1), GAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 30, 3, 0, 0, 0), ZoneOffset.ofHours(2), 1);
    }

    @Test
    public void test_Paris_getOffsetInfo_fromDST() {
        ZoneId test = ZoneId.of("Europe/Paris");
        checkOffset(test.getRules(), createLDT(2008, 10, 24), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 25), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 26), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 27), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 28), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 29), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 30), ZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 31), ZoneOffset.ofHours(1), 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 1, 59, 59, 999999999), ZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 2, 30, 0, 0), ZoneOffset.ofHours(2), OVERLAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 10, 26, 3, 0, 0, 0), ZoneOffset.ofHours(1), 1);
    }

    @Test
    public void test_Paris_getOffsetInfo_gap() {
        ZoneId test = ZoneId.of("Europe/Paris");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 3, 30, 2, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(1), GAP);
        assertTrue(trans.isGap());
        assertFalse(trans.isOverlap());
        assertEquals(ZoneOffset.ofHours(1), trans.getOffsetBefore());
        assertEquals(ZoneOffset.ofHours(2), trans.getOffsetAfter());
        assertEquals(createInstant(2008, 3, 30, 1, 0, 0, 0, ZoneOffset.UTC), trans.getInstant());
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(0)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(1)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(2)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(3)));
        assertEquals("Transition[Gap at 2008-03-30T02:00+01:00 to +02:00]", trans.toString());

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(1)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherDis = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherDis));
        assertEquals(otherDis.hashCode(), trans.hashCode());
    }

    @Test
    public void test_Paris_getOffsetInfo_overlap() {
        ZoneId test = ZoneId.of("Europe/Paris");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 10, 26, 2, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(2), OVERLAP);
        assertFalse(trans.isGap());
        assertTrue(trans.isOverlap());
        assertEquals(ZoneOffset.ofHours(2), trans.getOffsetBefore());
        assertEquals(ZoneOffset.ofHours(1), trans.getOffsetAfter());
        assertEquals(createInstant(2008, 10, 26, 1, 0, 0, 0, ZoneOffset.UTC), trans.getInstant());
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(0)));
        assertTrue(trans.isValidOffset(ZoneOffset.ofHours(1)));
        assertTrue(trans.isValidOffset(ZoneOffset.ofHours(2)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(3)));
        assertEquals("Transition[Overlap at 2008-10-26T03:00+02:00 to +01:00]", trans.toString());

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(2)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherDis = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherDis));
        assertEquals(otherDis.hashCode(), trans.hashCode());
    }

    //-----------------------------------------------------------------------
    // America/New_York
    //-----------------------------------------------------------------------
    @Test
    public void test_NewYork() {
        ZoneId test = ZoneId.of("America/New_York");
        assertEquals("America/New_York", test.getId());
        assertFalse(test.getRules().isFixedOffset());
    }

    @Test
    public void test_NewYork_getOffset() {
        ZoneId test = ZoneId.of("America/New_York");
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 1, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 2, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 3, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 4, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 5, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 6, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 7, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 8, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 9, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 10, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 11, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 12, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 1, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 2, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 3, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 4, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 5, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 6, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 7, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 8, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 9, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 10, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 11, 28, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 12, 28, offset)));
    }

    @Test
    public void test_NewYork_getOffset_toDST() {
        ZoneId test = ZoneId.of("America/New_York");
        ZoneOffset offset = ZoneOffset.ofHours(-5);
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 3, 8, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 3, 9, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 3, 10, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 3, 11, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 3, 12, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 3, 13, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 3, 14, offset)));
        // cutover at 02:00 local
        assertEquals(ZoneOffset.ofHours(-5),
                test.getRules().getOffset(createInstant(2008, 3, 9, 1, 59, 59, 999999999, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 3, 9, 2, 0, 0, 0, offset)));
    }

    @Test
    public void test_NewYork_getOffset_fromDST() {
        ZoneId test = ZoneId.of("America/New_York");
        ZoneOffset offset = ZoneOffset.ofHours(-4);
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 11, 1, offset)));
        assertEquals(ZoneOffset.ofHours(-4), test.getRules().getOffset(createInstant(2008, 11, 2, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 11, 3, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 11, 4, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 11, 5, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 11, 6, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 11, 7, offset)));
        // cutover at 02:00 local
        assertEquals(ZoneOffset.ofHours(-4),
                test.getRules().getOffset(createInstant(2008, 11, 2, 1, 59, 59, 999999999, offset)));
        assertEquals(ZoneOffset.ofHours(-5), test.getRules().getOffset(createInstant(2008, 11, 2, 2, 0, 0, 0, offset)));
    }

    @Test
    public void test_NewYork_getOffsetInfo() {
        ZoneId test = ZoneId.of("America/New_York");
        checkOffset(test.getRules(), createLDT(2008, 1, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 1), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 1, 28), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 28), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 28), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 28), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 28), ZoneOffset.ofHours(-5), 1);
    }

    @Test
    public void test_NewYork_getOffsetInfo_toDST() {
        ZoneId test = ZoneId.of("America/New_York");
        checkOffset(test.getRules(), createLDT(2008, 3, 8), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 9), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 10), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 11), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 12), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 13), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 14), ZoneOffset.ofHours(-4), 1);
        // cutover at 02:00 local
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 9, 1, 59, 59, 999999999), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 9, 2, 30, 0, 0), ZoneOffset.ofHours(-5), GAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 3, 9, 3, 0, 0, 0), ZoneOffset.ofHours(-4), 1);
    }

    @Test
    public void test_NewYork_getOffsetInfo_fromDST() {
        ZoneId test = ZoneId.of("America/New_York");
        checkOffset(test.getRules(), createLDT(2008, 11, 1), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 2), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 3), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 4), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 5), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 6), ZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 7), ZoneOffset.ofHours(-5), 1);
        // cutover at 02:00 local
        checkOffset(test.getRules(), LocalDateTime.of(2008, 11, 2, 0, 59, 59, 999999999), ZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 11, 2, 1, 30, 0, 0), ZoneOffset.ofHours(-4), OVERLAP);
        checkOffset(test.getRules(), LocalDateTime.of(2008, 11, 2, 2, 0, 0, 0), ZoneOffset.ofHours(-5), 1);
    }

    @Test
    public void test_NewYork_getOffsetInfo_gap() {
        ZoneId test = ZoneId.of("America/New_York");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 3, 9, 2, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(-5), GAP);
        assertEquals(ZoneOffset.ofHours(-5), trans.getOffsetBefore());
        assertEquals(ZoneOffset.ofHours(-4), trans.getOffsetAfter());
        assertEquals(createInstant(2008, 3, 9, 2, 0, 0, 0, ZoneOffset.ofHours(-5)), trans.getInstant());
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(-6)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(-5)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(-4)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(-3)));
        assertEquals("Transition[Gap at 2008-03-09T02:00-05:00 to -04:00]", trans.toString());

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(-5)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));

        assertEquals(otherTrans.hashCode(), trans.hashCode());
    }

    @Test
    public void test_NewYork_getOffsetInfo_overlap() {
        ZoneId test = ZoneId.of("America/New_York");
        final LocalDateTime dateTime = LocalDateTime.of(2008, 11, 2, 1, 0, 0, 0);
        ZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, ZoneOffset.ofHours(-4), OVERLAP);
        assertEquals(ZoneOffset.ofHours(-4), trans.getOffsetBefore());
        assertEquals(ZoneOffset.ofHours(-5), trans.getOffsetAfter());
        assertEquals(createInstant(2008, 11, 2, 2, 0, 0, 0, ZoneOffset.ofHours(-4)), trans.getInstant());
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(-1)));
        assertTrue(trans.isValidOffset(ZoneOffset.ofHours(-5)));
        assertTrue(trans.isValidOffset(ZoneOffset.ofHours(-4)));
        assertFalse(trans.isValidOffset(ZoneOffset.ofHours(2)));
        assertEquals("Transition[Overlap at 2008-11-02T02:00-04:00 to -05:00]", trans.toString());

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(ZoneOffset.ofHours(-4)));
        assertTrue(trans.equals(trans));

        final ZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));

        assertEquals(otherTrans.hashCode(), trans.hashCode());
    }

    //-----------------------------------------------------------------------
    // getXxx() isXxx()
    //-----------------------------------------------------------------------
    @Test
    public void test_get_Tzdb() {
        ZoneId test = ZoneId.of("Europe/London");
        assertEquals("Europe/London", test.getId());
        assertFalse(test.getRules().isFixedOffset());
    }

    @Test
    public void test_get_TzdbFixed() {
        ZoneId test = ZoneId.of("+01:30");
        assertEquals("+01:30", test.getId());
        assertTrue(test.getRules().isFixedOffset());
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    @Test
    public void test_equals() {
        ZoneId test1 = ZoneId.of("Europe/London");
        ZoneId test2 = ZoneId.of("Europe/Paris");
        ZoneId test2b = ZoneId.of("Europe/Paris");
        assertFalse(test1.equals(test2));
        assertFalse(test2.equals(test1));

        assertTrue(test1.equals(test1));
        assertTrue(test2.equals(test2));
        assertTrue(test2.equals(test2b));

        assertTrue(test1.hashCode() == test1.hashCode());
        assertTrue(test2.hashCode() == test2.hashCode());
        assertTrue(test2.hashCode() == test2b.hashCode());
    }

    @Test
    public void test_equals_null() {
        assertFalse(ZoneId.of("Europe/London").equals(null));
    }

    @Test
    public void test_equals_notTimeZone() {
        assertFalse(ZoneId.of("Europe/London").equals("Europe/London"));
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    static Object[][] data_toString() {
        return new Object[][] {
            {"Europe/London", "Europe/London"},
            {"Europe/Paris", "Europe/Paris"},
            {"Europe/Berlin", "Europe/Berlin"},
            {"Z", "Z"},
            {"UTC", "UTC"},
            {"UTC+01:00", "UTC+01:00"},
            {"GMT+01:00", "GMT+01:00"},
            {"UT+01:00", "UT+01:00"},
        };
    }

    @ParameterizedTest
    @MethodSource("data_toString")
    public void test_toString(String id, String expected) {
        ZoneId test = ZoneId.of(id);
        assertEquals(expected, test.toString());
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    private Instant createInstant(int year, int month, int day, ZoneOffset offset) {
        return LocalDateTime.of(year, month, day, 0, 0).toInstant(offset);
    }

    private Instant createInstant(int year, int month, int day, int hour, int min, int sec, int nano,
            ZoneOffset offset) {
        return LocalDateTime.of(year, month, day, hour, min, sec, nano).toInstant(offset);
    }

    private ZonedDateTime createZDT(int year, int month, int day, int hour, int min, int sec, int nano, ZoneId zone) {
        return LocalDateTime.of(year, month, day, hour, min, sec, nano).atZone(zone);
    }

    private LocalDateTime createLDT(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 0, 0);
    }

    private ZoneOffsetTransition checkOffset(ZoneRules rules, LocalDateTime dateTime, ZoneOffset offset, int type) {
        List<ZoneOffset> validOffsets = rules.getValidOffsets(dateTime);
        assertEquals(type, validOffsets.size());
        assertEquals(offset, rules.getOffset(dateTime));
        if (type == 1) {
            assertEquals(offset, validOffsets.get(0));
            return null;
        } else {
            ZoneOffsetTransition zot = rules.getTransition(dateTime);
            assertNotNull(zot);
            assertEquals(type == 2, zot.isOverlap());
            assertEquals(type == 0, zot.isGap());
            assertEquals(type == 2, zot.isValidOffset(offset));
            return zot;
        }
    }

}
