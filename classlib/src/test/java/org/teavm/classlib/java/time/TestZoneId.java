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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import org.teavm.classlib.java.util.TLocale;
import java.util.Map;
import java.util.SimpleTimeZone;
import org.teavm.classlib.java.util.TTimeZone;

import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.format.TTextStyle;
import org.teavm.classlib.java.time.temporal.TTemporalAccessor;
import org.teavm.classlib.java.time.zone.TZoneOffsetTransition;
import org.teavm.classlib.java.time.zone.TZoneRules;
import org.teavm.classlib.java.time.zone.TZoneRulesException;

@Test
public class TestZoneId extends AbstractTest {

    private static final TZoneId ZONE_PARIS = TZoneId.of("Europe/Paris");
    public static final String LATEST_TZDB = "2010i";
    private static final int OVERLAP = 2;
    private static final int GAP = 0;

    //-----------------------------------------------------------------------
    // Basics
    //-----------------------------------------------------------------------
    public void test_immutable() {
        Class<TZoneId> cls = TZoneId.class;
        assertTrue(Modifier.isPublic(cls.getModifiers()));
        Field[] fields = cls.getDeclaredFields();
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers()) == false) {
                assertTrue(Modifier.isPrivate(field.getModifiers()));
                assertTrue(Modifier.isFinal(field.getModifiers()) ||
                        (Modifier.isVolatile(field.getModifiers()) && Modifier.isTransient(field.getModifiers())));
            }
        }
    }

    public void test_serialization_UTC() throws Exception {
        TZoneId test = TZoneOffset.UTC;
        assertSerializableAndSame(test);
    }

    public void test_serialization_fixed() throws Exception {
        TZoneId test = TZoneId.of("UTC+01:30");
        assertSerializable(test);
    }

    public void test_serialization_Europe() throws Exception {
        TZoneId test = TZoneId.of("Europe/London");
        assertSerializable(test);
    }

    public void test_serialization_America() throws Exception {
        TZoneId test = TZoneId.of("America/Chicago");
        assertSerializable(test);
    }

    @Test
    public void test_serialization_format() throws ClassNotFoundException, IOException {
        assertEqualsSerialisedForm(TZoneId.of("Europe/London"), TZoneId.class);
    }

    //-----------------------------------------------------------------------
    // UTC
    //-----------------------------------------------------------------------
    public void test_constant_UTC() {
        TZoneId test = TZoneOffset.UTC;
        assertEquals(test.getId(), "Z");
        assertEquals(test.getDisplayName(TTextStyle.FULL, TLocale.UK), "Z");
        assertEquals(test.getRules().isFixedOffset(), true);
        assertEquals(test.getRules().getOffset(TInstant.ofEpochSecond(0L)), TZoneOffset.UTC);
        checkOffset(test.getRules(), createLDT(2008, 6, 30), TZoneOffset.UTC, 1);
    }

    //-----------------------------------------------------------------------
    // SHORT_IDS
    //-----------------------------------------------------------------------
    public void test_constant_SHORT_IDS() {
        Map<String, String> ids = TZoneId.SHORT_IDS;
        assertEquals(ids.get("EST"), "-05:00");
        assertEquals(ids.get("MST"), "-07:00");
        assertEquals(ids.get("HST"), "-10:00");
        assertEquals(ids.get("ACT"), "Australia/Darwin");
        assertEquals(ids.get("AET"), "Australia/Sydney");
        assertEquals(ids.get("AGT"), "America/Argentina/Buenos_Aires");
        assertEquals(ids.get("ART"), "Africa/Cairo");
        assertEquals(ids.get("AST"), "America/Anchorage");
        assertEquals(ids.get("BET"), "America/Sao_Paulo");
        assertEquals(ids.get("BST"), "Asia/Dhaka");
        assertEquals(ids.get("CAT"), "Africa/Harare");
        assertEquals(ids.get("CNT"), "America/St_Johns");
        assertEquals(ids.get("CST"), "America/Chicago");
        assertEquals(ids.get("CTT"), "Asia/Shanghai");
        assertEquals(ids.get("EAT"), "Africa/Addis_Ababa");
        assertEquals(ids.get("ECT"), "Europe/Paris");
        assertEquals(ids.get("IET"), "America/Indiana/Indianapolis");
        assertEquals(ids.get("IST"), "Asia/Kolkata");
        assertEquals(ids.get("JST"), "Asia/Tokyo");
        assertEquals(ids.get("MIT"), "Pacific/Apia");
        assertEquals(ids.get("NET"), "Asia/Yerevan");
        assertEquals(ids.get("NST"), "Pacific/Auckland");
        assertEquals(ids.get("PLT"), "Asia/Karachi");
        assertEquals(ids.get("PNT"), "America/Phoenix");
        assertEquals(ids.get("PRT"), "America/Puerto_Rico");
        assertEquals(ids.get("PST"), "America/Los_Angeles");
        assertEquals(ids.get("SST"), "Pacific/Guadalcanal");
        assertEquals(ids.get("VST"), "Asia/Ho_Chi_Minh");
    }

    @Test(expectedExceptions=UnsupportedOperationException.class)
    public void test_constant_SHORT_IDS_immutable() {
        Map<String, String> ids = TZoneId.SHORT_IDS;
        ids.clear();
    }

    //-----------------------------------------------------------------------
    // system default
    //-----------------------------------------------------------------------
    public void test_systemDefault() {
        TZoneId test = TZoneId.systemDefault();
        assertEquals(test.getId(), TTimeZone.getDefault().getID());
    }

    @Test(expectedExceptions = TDateTimeException.class)
    public void test_systemDefault_unableToConvert_badFormat() {
        TTimeZone current = TTimeZone.getDefault();
        try {
            TTimeZone.setDefault(new SimpleTimeZone(127, "Something Weird"));
            TZoneId.systemDefault();
        } finally {
            TTimeZone.setDefault(current);
        }
    }

    @Test(expectedExceptions = TZoneRulesException.class)
    public void test_systemDefault_unableToConvert_unknownId() {
        TTimeZone current = TTimeZone.getDefault();
        try {
            TTimeZone.setDefault(new SimpleTimeZone(127, "SomethingWeird"));
            TZoneId.systemDefault();
        } finally {
            TTimeZone.setDefault(current);
        }
    }

    //-----------------------------------------------------------------------
    // mapped factory
    //-----------------------------------------------------------------------
    public void test_of_string_Map() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("LONDON", "Europe/London");
        map.put("PARIS", "Europe/Paris");
        TZoneId test = TZoneId.of("LONDON", map);
        assertEquals(test.getId(), "Europe/London");
    }

    public void test_of_string_Map_lookThrough() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("LONDON", "Europe/London");
        map.put("PARIS", "Europe/Paris");
        TZoneId test = TZoneId.of("Europe/Madrid", map);
        assertEquals(test.getId(), "Europe/Madrid");
    }

    public void test_of_string_Map_emptyMap() {
        Map<String, String> map = new HashMap<String, String>();
        TZoneId test = TZoneId.of("Europe/Madrid", map);
        assertEquals(test.getId(), "Europe/Madrid");
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_of_string_Map_badFormat() {
        Map<String, String> map = new HashMap<String, String>();
        TZoneId.of("Not kknown", map);
    }

    @Test(expectedExceptions=TZoneRulesException.class)
    public void test_of_string_Map_unknown() {
        Map<String, String> map = new HashMap<String, String>();
        TZoneId.of("Unknown", map);
    }

    //-----------------------------------------------------------------------
    // regular factory
    //-----------------------------------------------------------------------
    @DataProvider(name="String_UTC")
    Object[][] data_of_string_UTC() {
        return new Object[][] {
            {""},
            {"+00"},{"+0000"},{"+00:00"},{"+000000"},{"+00:00:00"},
            {"-00"},{"-0000"},{"-00:00"},{"-000000"},{"-00:00:00"},
        };
    }

    @Test(dataProvider="String_UTC")
    public void test_of_string_UTC(String id) {
        TZoneId test = TZoneId.of("UTC" + id);
        assertEquals(test.getId(), "UTC");
        assertEquals(test.normalized(), TZoneOffset.UTC);
    }

    @Test(dataProvider="String_UTC")
    public void test_of_string_GMT(String id) {
        TZoneId test = TZoneId.of("GMT" + id);
        assertEquals(test.getId(), "GMT");
        assertEquals(test.normalized(), TZoneOffset.UTC);
    }

    @Test(dataProvider="String_UTC")
    public void test_of_string_UT(String id) {
        TZoneId test = TZoneId.of("UT" + id);
        assertEquals(test.getId(), "UT");
        assertEquals(test.normalized(), TZoneOffset.UTC);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="String_Fixed")
    Object[][] data_of_string_Fixed() {
        return new Object[][] {
            {"+0", ""},
            {"+5", "+05:00"},
            {"+01", "+01:00"},
            {"+0100", "+01:00"},{"+01:00", "+01:00"},
            {"+010000", "+01:00"},{"+01:00:00", "+01:00"},
            {"+12", "+12:00"},
            {"+1234", "+12:34"},{"+12:34", "+12:34"},
            {"+123456", "+12:34:56"},{"+12:34:56", "+12:34:56"},
            {"-02", "-02:00"},
            {"-5", "-05:00"},
            {"-0200", "-02:00"},{"-02:00", "-02:00"},
            {"-020000", "-02:00"},{"-02:00:00", "-02:00"},
        };
    }

    @Test(dataProvider="String_Fixed")
    public void test_of_string_offset(String input, String id) {
        TZoneId test = TZoneId.of(input);
        TZoneOffset offset = TZoneOffset.of(id.isEmpty() ? "Z" : id);
        assertEquals(test, offset);
    }

    @Test(dataProvider="String_Fixed")
    public void test_of_string_FixedUTC(String input, String id) {
        TZoneId test = TZoneId.of("UTC" + input);
        assertEquals(test.getId(), "UTC" + id);
        assertEquals(test.getDisplayName(TTextStyle.FULL, TLocale.UK), "UTC" + id);
        assertEquals(test.getRules().isFixedOffset(), true);
        TZoneOffset offset = TZoneOffset.of(id.isEmpty() ? "Z" : id);
        assertEquals(test.getRules().getOffset(TInstant.ofEpochSecond(0L)), offset);
        checkOffset(test.getRules(), createLDT(2008, 6, 30), offset, 1);
    }

    @Test(dataProvider="String_Fixed")
    public void test_of_string_FixedGMT(String input, String id) {
        TZoneId test = TZoneId.of("GMT" + input);
        assertEquals(test.getId(), "GMT" + id);
        assertEquals(test.getDisplayName(TTextStyle.FULL, TLocale.UK), "GMT" + id);
        assertEquals(test.getRules().isFixedOffset(), true);
        TZoneOffset offset = TZoneOffset.of(id.isEmpty() ? "Z" : id);
        assertEquals(test.getRules().getOffset(TInstant.ofEpochSecond(0L)), offset);
        checkOffset(test.getRules(), createLDT(2008, 6, 30), offset, 1);
    }

    @Test(dataProvider="String_Fixed")
    public void test_of_string_FixedUT(String input, String id) {
        TZoneId test = TZoneId.of("UT" + input);
        assertEquals(test.getId(), "UT" + id);
        assertEquals(test.getDisplayName(TTextStyle.FULL, TLocale.UK), "UT" + id);
        assertEquals(test.getRules().isFixedOffset(), true);
        TZoneOffset offset = TZoneOffset.of(id.isEmpty() ? "Z" : id);
        assertEquals(test.getRules().getOffset(TInstant.ofEpochSecond(0L)), offset);
        checkOffset(test.getRules(), createLDT(2008, 6, 30), offset, 1);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="String_UTC_Invalid")
    Object[][] data_of_string_UTC_invalid() {
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

    @Test(dataProvider="String_UTC_Invalid", expectedExceptions=TDateTimeException.class)
    public void test_of_string_UTC_invalid(String id) {
        TZoneId.of("UTC" + id);
    }

    @Test(dataProvider="String_UTC_Invalid", expectedExceptions=TDateTimeException.class)
    public void test_of_string_GMT_invalid(String id) {
        TZoneId.of("GMT" + id);
    }

    //-----------------------------------------------------------------------
    @DataProvider(name="String_Invalid")
    Object[][] data_of_string_invalid() {
        // \u00ef is a random unicode character
        return new Object[][] {
                {""}, {":"}, {"#"},
                {"\u00ef"}, {"`"}, {"!"}, {"\""}, {"\u00ef"}, {"$"}, {"^"}, {"&"}, {"*"}, {"("}, {")"}, {"="},
                {"\\"}, {"|"}, {","}, {"<"}, {">"}, {"?"}, {";"}, {"'"}, {"["}, {"]"}, {"{"}, {"}"},
                {"\u00ef:A"}, {"`:A"}, {"!:A"}, {"\":A"}, {"\u00ef:A"}, {"$:A"}, {"^:A"}, {"&:A"}, {"*:A"}, {"(:A"}, {"):A"}, {"=:A"}, {"+:A"},
                {"\\:A"}, {"|:A"}, {",:A"}, {"<:A"}, {">:A"}, {"?:A"}, {";:A"}, {"::A"}, {"':A"}, {"@:A"}, {"~:A"}, {"[:A"}, {"]:A"}, {"{:A"}, {"}:A"},
                {"A:B#\u00ef"}, {"A:B#`"}, {"A:B#!"}, {"A:B#\""}, {"A:B#\u00ef"}, {"A:B#$"}, {"A:B#^"}, {"A:B#&"}, {"A:B#*"},
                {"A:B#("}, {"A:B#)"}, {"A:B#="}, {"A:B#+"},
                {"A:B#\\"}, {"A:B#|"}, {"A:B#,"}, {"A:B#<"}, {"A:B#>"}, {"A:B#?"}, {"A:B#;"}, {"A:B#:"},
                {"A:B#'"}, {"A:B#@"}, {"A:B#~"}, {"A:B#["}, {"A:B#]"}, {"A:B#{"}, {"A:B#}"},
        };
    }

    @Test(dataProvider="String_Invalid", expectedExceptions=TDateTimeException.class)
    public void test_of_string_invalid(String id) {
        TZoneId.of(id);
    }

    //-----------------------------------------------------------------------
    public void test_of_string_GMT0() {
        TZoneId test = TZoneId.of("GMT0");
        assertEquals(test.getId(), "GMT0");
        assertEquals(test.getRules().isFixedOffset(), true);
        assertEquals(test.normalized(), TZoneOffset.UTC);
    }

    //-----------------------------------------------------------------------
    public void test_of_string_London() {
        TZoneId test = TZoneId.of("Europe/London");
        assertEquals(test.getId(), "Europe/London");
        assertEquals(test.getRules().isFixedOffset(), false);
    }

    //-----------------------------------------------------------------------
    @Test(expectedExceptions=NullPointerException.class)
    public void test_of_string_null() {
        TZoneId.of((String) null);
    }

    @Test(expectedExceptions=TZoneRulesException.class)
    public void test_of_string_unknown_simple() {
        TZoneId.of("Unknown");
    }

    //-------------------------------------------------------------------------
    // TODO: test by deserialization
//    public void test_ofUnchecked_string_invalidNotChecked() {
//        TZoneRegion test = TZoneRegion.ofLenient("Unknown");
//        assertEquals(test.getId(), "Unknown");
//    }
//
//    public void test_ofUnchecked_string_invalidNotChecked_unusualCharacters() {
//        TZoneRegion test = TZoneRegion.ofLenient("QWERTYUIOPASDFGHJKLZXCVBNM~/._+-");
//        assertEquals(test.getId(), "QWERTYUIOPASDFGHJKLZXCVBNM~/._+-");
//    }

    //-----------------------------------------------------------------------
    // from()
    //-----------------------------------------------------------------------
    public void test_factory_CalendricalObject() {
        assertEquals(TZoneId.from(createZDT(2007, 7, 15, 17, 30, 0, 0, ZONE_PARIS)), ZONE_PARIS);
    }

    @Test(expectedExceptions=TDateTimeException.class)
    public void test_factory_CalendricalObject_invalid_noDerive() {
        TZoneId.from(TLocalTime.of(12, 30));
    }

    @Test(expectedExceptions=NullPointerException.class)
    public void test_factory_CalendricalObject_null() {
        TZoneId.from((TTemporalAccessor) null);
    }

    //-----------------------------------------------------------------------
    // Europe/London
    //-----------------------------------------------------------------------
    public void test_London() {
        TZoneId test = TZoneId.of("Europe/London");
        assertEquals(test.getId(), "Europe/London");
        assertEquals(test.getRules().isFixedOffset(), false);
    }

    public void test_London_getOffset() {
        TZoneId test = TZoneId.of("Europe/London");
        assertEquals(test.getRules().getOffset(createInstant(2008, 1, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 2, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 4, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 5, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 6, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 7, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 8, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 9, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 12, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
    }

    public void test_London_getOffset_toDST() {
        TZoneId test = TZoneId.of("Europe/London");
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 24, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 25, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 26, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 27, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 28, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 29, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 31, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        // cutover at 01:00Z
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
    }

    public void test_London_getOffset_fromDST() {
        TZoneId test = TZoneId.of("Europe/London");
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 24, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 25, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 27, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 28, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 29, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 30, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 31, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
        // cutover at 01:00Z
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, TZoneOffset.UTC)), TZoneOffset.ofHours(0));
    }

    public void test_London_getOffsetInfo() {
        TZoneId test = TZoneId.of("Europe/London");
        checkOffset(test.getRules(), createLDT(2008, 1, 1), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 1), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 1), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 1), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 1), TZoneOffset.ofHours(0), 1);
    }

    public void test_London_getOffsetInfo_toDST() {
        TZoneId test = TZoneId.of("Europe/London");
        checkOffset(test.getRules(), createLDT(2008, 3, 24), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 25), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 26), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 27), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 28), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 29), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 30), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 31), TZoneOffset.ofHours(1), 1);
        // cutover at 01:00Z
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 3, 30, 0, 59, 59, 999999999), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 3, 30, 1, 30, 0, 0), TZoneOffset.ofHours(0), GAP);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 3, 30, 2, 0, 0, 0), TZoneOffset.ofHours(1), 1);
    }

    public void test_London_getOffsetInfo_fromDST() {
        TZoneId test = TZoneId.of("Europe/London");
        checkOffset(test.getRules(), createLDT(2008, 10, 24), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 25), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 26), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 27), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 28), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 29), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 30), TZoneOffset.ofHours(0), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 31), TZoneOffset.ofHours(0), 1);
        // cutover at 01:00Z
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 10, 26, 0, 59, 59, 999999999), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 10, 26, 1, 30, 0, 0), TZoneOffset.ofHours(1), OVERLAP);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 10, 26, 2, 0, 0, 0), TZoneOffset.ofHours(0), 1);
    }

    public void test_London_getOffsetInfo_gap() {
        TZoneId test = TZoneId.of("Europe/London");
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 3, 30, 1, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, TZoneOffset.ofHours(0), GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), TZoneOffset.ofHours(0));
        assertEquals(trans.getOffsetAfter(), TZoneOffset.ofHours(1));
        assertEquals(trans.getInstant(), dateTime.toInstant(TZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), TLocalDateTime.of(2008, 3, 30, 1, 0));
        assertEquals(trans.getDateTimeAfter(), TLocalDateTime.of(2008, 3, 30, 2, 0));
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(0)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(1)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(2)), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-30T01:00Z to +01:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(TZoneOffset.ofHours(0)));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_London_getOffsetInfo_overlap() {
        TZoneId test = TZoneId.of("Europe/London");
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 10, 26, 1, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, TZoneOffset.ofHours(1), OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), TZoneOffset.ofHours(1));
        assertEquals(trans.getOffsetAfter(), TZoneOffset.ofHours(0));
        assertEquals(trans.getInstant(), dateTime.toInstant(TZoneOffset.UTC));
        assertEquals(trans.getDateTimeBefore(), TLocalDateTime.of(2008, 10, 26, 2, 0));
        assertEquals(trans.getDateTimeAfter(), TLocalDateTime.of(2008, 10, 26, 1, 0));
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(0)), true);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(1)), true);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(2)), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-10-26T02:00+01:00 to Z]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(TZoneOffset.ofHours(1)));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));
        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    //-----------------------------------------------------------------------
    // Europe/Paris
    //-----------------------------------------------------------------------
    public void test_Paris() {
        TZoneId test = TZoneId.of("Europe/Paris");
        assertEquals(test.getId(), "Europe/Paris");
        assertEquals(test.getRules().isFixedOffset(), false);
    }

    public void test_Paris_getOffset() {
        TZoneId test = TZoneId.of("Europe/Paris");
        assertEquals(test.getRules().getOffset(createInstant(2008, 1, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 2, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 4, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 5, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 6, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 7, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 8, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 9, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 12, 1, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
    }

    public void test_Paris_getOffset_toDST() {
        TZoneId test = TZoneId.of("Europe/Paris");
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 24, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 25, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 26, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 27, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 28, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 29, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 31, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        // cutover at 01:00Z
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, 0, 59, 59, 999999999, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 30, 1, 0, 0, 0, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
    }

    public void test_Paris_getOffset_fromDST() {
        TZoneId test = TZoneId.of("Europe/Paris");
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 24, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 25, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 27, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 28, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 29, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 30, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 31, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
        // cutover at 01:00Z
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, 0, 59, 59, 999999999, TZoneOffset.UTC)), TZoneOffset.ofHours(2));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 26, 1, 0, 0, 0, TZoneOffset.UTC)), TZoneOffset.ofHours(1));
    }

    public void test_Paris_getOffsetInfo() {
        TZoneId test = TZoneId.of("Europe/Paris");
        checkOffset(test.getRules(), createLDT(2008, 1, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 1), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 1), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 1), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 1), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 1), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 1), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 1), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 1), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 1), TZoneOffset.ofHours(1), 1);
    }

    public void test_Paris_getOffsetInfo_toDST() {
        TZoneId test = TZoneId.of("Europe/Paris");
        checkOffset(test.getRules(), createLDT(2008, 3, 24), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 25), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 26), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 27), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 28), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 29), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 30), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 31), TZoneOffset.ofHours(2), 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 3, 30, 1, 59, 59, 999999999), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 3, 30, 2, 30, 0, 0), TZoneOffset.ofHours(1), GAP);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 3, 30, 3, 0, 0, 0), TZoneOffset.ofHours(2), 1);
    }

    public void test_Paris_getOffsetInfo_fromDST() {
        TZoneId test = TZoneId.of("Europe/Paris");
        checkOffset(test.getRules(), createLDT(2008, 10, 24), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 25), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 26), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 27), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 28), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 29), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 30), TZoneOffset.ofHours(1), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 31), TZoneOffset.ofHours(1), 1);
        // cutover at 01:00Z which is 02:00+01:00(local Paris time)
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 10, 26, 1, 59, 59, 999999999), TZoneOffset.ofHours(2), 1);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 10, 26, 2, 30, 0, 0), TZoneOffset.ofHours(2), OVERLAP);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 10, 26, 3, 0, 0, 0), TZoneOffset.ofHours(1), 1);
    }

    public void test_Paris_getOffsetInfo_gap() {
        TZoneId test = TZoneId.of("Europe/Paris");
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 3, 30, 2, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, TZoneOffset.ofHours(1), GAP);
        assertEquals(trans.isGap(), true);
        assertEquals(trans.isOverlap(), false);
        assertEquals(trans.getOffsetBefore(), TZoneOffset.ofHours(1));
        assertEquals(trans.getOffsetAfter(), TZoneOffset.ofHours(2));
        assertEquals(trans.getInstant(), createInstant(2008, 3, 30, 1, 0, 0, 0, TZoneOffset.UTC));
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(0)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(1)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(2)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(3)), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-30T02:00+01:00 to +02:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(TZoneOffset.ofHours(1)));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherDis = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherDis));
        assertEquals(trans.hashCode(), otherDis.hashCode());
    }

    public void test_Paris_getOffsetInfo_overlap() {
        TZoneId test = TZoneId.of("Europe/Paris");
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 10, 26, 2, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, TZoneOffset.ofHours(2), OVERLAP);
        assertEquals(trans.isGap(), false);
        assertEquals(trans.isOverlap(), true);
        assertEquals(trans.getOffsetBefore(), TZoneOffset.ofHours(2));
        assertEquals(trans.getOffsetAfter(), TZoneOffset.ofHours(1));
        assertEquals(trans.getInstant(), createInstant(2008, 10, 26, 1, 0, 0, 0, TZoneOffset.UTC));
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(0)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(1)), true);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(2)), true);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(3)), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-10-26T03:00+02:00 to +01:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(TZoneOffset.ofHours(2)));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherDis = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherDis));
        assertEquals(trans.hashCode(), otherDis.hashCode());
    }

    //-----------------------------------------------------------------------
    // America/New_York
    //-----------------------------------------------------------------------
    public void test_NewYork() {
        TZoneId test = TZoneId.of("America/New_York");
        assertEquals(test.getId(), "America/New_York");
        assertEquals(test.getRules().isFixedOffset(), false);
    }

    public void test_NewYork_getOffset() {
        TZoneId test = TZoneId.of("America/New_York");
        TZoneOffset offset = TZoneOffset.ofHours(-5);
        assertEquals(test.getRules().getOffset(createInstant(2008, 1, 1, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 2, 1, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 1, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 4, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 5, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 6, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 7, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 8, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 9, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 12, 1, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 1, 28, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 2, 28, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 4, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 5, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 6, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 7, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 8, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 9, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 10, 28, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 28, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 12, 28, offset)), TZoneOffset.ofHours(-5));
    }

    public void test_NewYork_getOffset_toDST() {
        TZoneId test = TZoneId.of("America/New_York");
        TZoneOffset offset = TZoneOffset.ofHours(-5);
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 8, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 9, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 10, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 11, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 12, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 13, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 14, offset)), TZoneOffset.ofHours(-4));
        // cutover at 02:00 local
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 9, 1, 59, 59, 999999999, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 3, 9, 2, 0, 0, 0, offset)), TZoneOffset.ofHours(-4));
    }

    public void test_NewYork_getOffset_fromDST() {
        TZoneId test = TZoneId.of("America/New_York");
        TZoneOffset offset = TZoneOffset.ofHours(-4);
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 1, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 2, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 3, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 4, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 5, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 6, offset)), TZoneOffset.ofHours(-5));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 7, offset)), TZoneOffset.ofHours(-5));
        // cutover at 02:00 local
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 2, 1, 59, 59, 999999999, offset)), TZoneOffset.ofHours(-4));
        assertEquals(test.getRules().getOffset(createInstant(2008, 11, 2, 2, 0, 0, 0, offset)), TZoneOffset.ofHours(-5));
    }

    public void test_NewYork_getOffsetInfo() {
        TZoneId test = TZoneId.of("America/New_York");
        checkOffset(test.getRules(), createLDT(2008, 1, 1), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 1), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 1), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 1), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 1, 28), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 2, 28), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 4, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 5, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 6, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 7, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 8, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 9, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 10, 28), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 28), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 12, 28), TZoneOffset.ofHours(-5), 1);
    }

    public void test_NewYork_getOffsetInfo_toDST() {
        TZoneId test = TZoneId.of("America/New_York");
        checkOffset(test.getRules(), createLDT(2008, 3, 8), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 9), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 10), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 11), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 12), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 13), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 3, 14), TZoneOffset.ofHours(-4), 1);
        // cutover at 02:00 local
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 3, 9, 1, 59, 59, 999999999), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 3, 9, 2, 30, 0, 0), TZoneOffset.ofHours(-5), GAP);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 3, 9, 3, 0, 0, 0), TZoneOffset.ofHours(-4), 1);
    }

    public void test_NewYork_getOffsetInfo_fromDST() {
        TZoneId test = TZoneId.of("America/New_York");
        checkOffset(test.getRules(), createLDT(2008, 11, 1), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 2), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 3), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 4), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 5), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 6), TZoneOffset.ofHours(-5), 1);
        checkOffset(test.getRules(), createLDT(2008, 11, 7), TZoneOffset.ofHours(-5), 1);
        // cutover at 02:00 local
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 11, 2, 0, 59, 59, 999999999), TZoneOffset.ofHours(-4), 1);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 11, 2, 1, 30, 0, 0), TZoneOffset.ofHours(-4), OVERLAP);
        checkOffset(test.getRules(), TLocalDateTime.of(2008, 11, 2, 2, 0, 0, 0), TZoneOffset.ofHours(-5), 1);
    }

    public void test_NewYork_getOffsetInfo_gap() {
        TZoneId test = TZoneId.of("America/New_York");
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 3, 9, 2, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, TZoneOffset.ofHours(-5), GAP);
        assertEquals(trans.getOffsetBefore(), TZoneOffset.ofHours(-5));
        assertEquals(trans.getOffsetAfter(), TZoneOffset.ofHours(-4));
        assertEquals(trans.getInstant(), createInstant(2008, 3, 9, 2, 0, 0, 0, TZoneOffset.ofHours(-5)));
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-6)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-5)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-4)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-3)), false);
        assertEquals(trans.toString(), "Transition[Gap at 2008-03-09T02:00-05:00 to -04:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(TZoneOffset.ofHours(-5)));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));

        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    public void test_NewYork_getOffsetInfo_overlap() {
        TZoneId test = TZoneId.of("America/New_York");
        final TLocalDateTime dateTime = TLocalDateTime.of(2008, 11, 2, 1, 0, 0, 0);
        TZoneOffsetTransition trans = checkOffset(test.getRules(), dateTime, TZoneOffset.ofHours(-4), OVERLAP);
        assertEquals(trans.getOffsetBefore(), TZoneOffset.ofHours(-4));
        assertEquals(trans.getOffsetAfter(), TZoneOffset.ofHours(-5));
        assertEquals(trans.getInstant(), createInstant(2008, 11, 2, 2, 0, 0, 0, TZoneOffset.ofHours(-4)));
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-1)), false);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-5)), true);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(-4)), true);
        assertEquals(trans.isValidOffset(TZoneOffset.ofHours(2)), false);
        assertEquals(trans.toString(), "Transition[Overlap at 2008-11-02T02:00-04:00 to -05:00]");

        assertFalse(trans.equals(null));
        assertFalse(trans.equals(TZoneOffset.ofHours(-4)));
        assertTrue(trans.equals(trans));

        final TZoneOffsetTransition otherTrans = test.getRules().getTransition(dateTime);
        assertTrue(trans.equals(otherTrans));

        assertEquals(trans.hashCode(), otherTrans.hashCode());
    }

    //-----------------------------------------------------------------------
    // getXxx() isXxx()
    //-----------------------------------------------------------------------
    public void test_get_Tzdb() {
        TZoneId test = TZoneId.of("Europe/London");
        assertEquals(test.getId(), "Europe/London");
        assertEquals(test.getRules().isFixedOffset(), false);
    }

    public void test_get_TzdbFixed() {
        TZoneId test = TZoneId.of("+01:30");
        assertEquals(test.getId(), "+01:30");
        assertEquals(test.getRules().isFixedOffset(), true);
    }

    //-----------------------------------------------------------------------
    // equals() / hashCode()
    //-----------------------------------------------------------------------
    public void test_equals() {
        TZoneId test1 = TZoneId.of("Europe/London");
        TZoneId test2 = TZoneId.of("Europe/Paris");
        TZoneId test2b = TZoneId.of("Europe/Paris");
        assertEquals(test1.equals(test2), false);
        assertEquals(test2.equals(test1), false);

        assertEquals(test1.equals(test1), true);
        assertEquals(test2.equals(test2), true);
        assertEquals(test2.equals(test2b), true);

        assertEquals(test1.hashCode() == test1.hashCode(), true);
        assertEquals(test2.hashCode() == test2.hashCode(), true);
        assertEquals(test2.hashCode() == test2b.hashCode(), true);
    }

    public void test_equals_null() {
        assertEquals(TZoneId.of("Europe/London").equals(null), false);
    }

    public void test_equals_notTimeZone() {
        assertEquals(TZoneId.of("Europe/London").equals("Europe/London"), false);
    }

    //-----------------------------------------------------------------------
    // toString()
    //-----------------------------------------------------------------------
    @DataProvider(name="ToString")
    Object[][] data_toString() {
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

    @Test(dataProvider="ToString")
    public void test_toString(String id, String expected) {
        TZoneId test = TZoneId.of(id);
        assertEquals(test.toString(), expected);
    }

    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    //-----------------------------------------------------------------------
    private TInstant createInstant(int year, int month, int day, TZoneOffset offset) {
        return TLocalDateTime.of(year, month, day, 0, 0).toInstant(offset);
    }

    private TInstant createInstant(int year, int month, int day, int hour, int min, int sec, int nano, TZoneOffset offset) {
        return TLocalDateTime.of(year, month, day, hour, min, sec, nano).toInstant(offset);
    }

    private TZonedDateTime createZDT(int year, int month, int day, int hour, int min, int sec, int nano, TZoneId zone) {
        return TLocalDateTime.of(year, month, day, hour, min, sec, nano).atZone(zone);
    }

    private TLocalDateTime createLDT(int year, int month, int day) {
        return TLocalDateTime.of(year, month, day, 0, 0);
    }

    private TZoneOffsetTransition checkOffset(TZoneRules rules, TLocalDateTime dateTime, TZoneOffset offset, int type) {
        List<TZoneOffset> validOffsets = rules.getValidOffsets(dateTime);
        assertEquals(validOffsets.size(), type);
        assertEquals(rules.getOffset(dateTime), offset);
        if (type == 1) {
            assertEquals(validOffsets.get(0), offset);
            return null;
        } else {
            TZoneOffsetTransition zot = rules.getTransition(dateTime);
            assertNotNull(zot);
            assertEquals(zot.isOverlap(), type == 2);
            assertEquals(zot.isGap(), type == 0);
            assertEquals(zot.isValidOffset(offset), type == 2);
            return zot;
        }
    }

}
