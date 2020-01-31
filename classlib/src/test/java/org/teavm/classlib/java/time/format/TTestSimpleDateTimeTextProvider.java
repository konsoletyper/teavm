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
package org.threeten.bp.format;

import static org.testng.Assert.assertEquals;
import static org.threeten.bp.temporal.ChronoField.AMPM_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.DAY_OF_WEEK;
import static org.threeten.bp.temporal.ChronoField.MONTH_OF_YEAR;

import java.util.Locale;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.threeten.bp.temporal.TemporalField;

/**
 * Test SimpleDateTimeTextProvider.
 */
@Test
public class TestSimpleDateTimeTextProvider {

    Locale enUS = new Locale("en", "US");
    Locale ptBR = new Locale("pt", "BR");
    Locale frFR = new Locale("fr", "FR");

    @BeforeMethod
    public void setUp() {
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "Text")
    Object[][] data_text() {
        return new Object[][] {
            {DAY_OF_WEEK, 1, TextStyle.SHORT, enUS, "Mon"},
            {DAY_OF_WEEK, 2, TextStyle.SHORT, enUS, "Tue"},
            {DAY_OF_WEEK, 3, TextStyle.SHORT, enUS, "Wed"},
            {DAY_OF_WEEK, 4, TextStyle.SHORT, enUS, "Thu"},
            {DAY_OF_WEEK, 5, TextStyle.SHORT, enUS, "Fri"},
            {DAY_OF_WEEK, 6, TextStyle.SHORT, enUS, "Sat"},
            {DAY_OF_WEEK, 7, TextStyle.SHORT, enUS, "Sun"},

            {DAY_OF_WEEK, 1, TextStyle.SHORT, ptBR, "Seg"},
            {DAY_OF_WEEK, 2, TextStyle.SHORT, ptBR, "Ter"},
            {DAY_OF_WEEK, 3, TextStyle.SHORT, ptBR, "Qua"},
            {DAY_OF_WEEK, 4, TextStyle.SHORT, ptBR, "Qui"},
            {DAY_OF_WEEK, 5, TextStyle.SHORT, ptBR, "Sex"},
            {DAY_OF_WEEK, 6, TextStyle.SHORT, ptBR, "S\u00E1b"},
            {DAY_OF_WEEK, 7, TextStyle.SHORT, ptBR, "Dom"},

            {DAY_OF_WEEK, 1, TextStyle.FULL, enUS, "Monday"},
            {DAY_OF_WEEK, 2, TextStyle.FULL, enUS, "Tuesday"},
            {DAY_OF_WEEK, 3, TextStyle.FULL, enUS, "Wednesday"},
            {DAY_OF_WEEK, 4, TextStyle.FULL, enUS, "Thursday"},
            {DAY_OF_WEEK, 5, TextStyle.FULL, enUS, "Friday"},
            {DAY_OF_WEEK, 6, TextStyle.FULL, enUS, "Saturday"},
            {DAY_OF_WEEK, 7, TextStyle.FULL, enUS, "Sunday"},

            {DAY_OF_WEEK, 1, TextStyle.FULL, ptBR, "Segunda-feira"},
            {DAY_OF_WEEK, 2, TextStyle.FULL, ptBR, "Ter\u00E7a-feira"},
            {DAY_OF_WEEK, 3, TextStyle.FULL, ptBR, "Quarta-feira"},
            {DAY_OF_WEEK, 4, TextStyle.FULL, ptBR, "Quinta-feira"},
            {DAY_OF_WEEK, 5, TextStyle.FULL, ptBR, "Sexta-feira"},
            {DAY_OF_WEEK, 6, TextStyle.FULL, ptBR, "S\u00E1bado"},
            {DAY_OF_WEEK, 7, TextStyle.FULL, ptBR, "Domingo"},

            {MONTH_OF_YEAR, 1, TextStyle.SHORT, enUS, "Jan"},
            {MONTH_OF_YEAR, 2, TextStyle.SHORT, enUS, "Feb"},
            {MONTH_OF_YEAR, 3, TextStyle.SHORT, enUS, "Mar"},
            {MONTH_OF_YEAR, 4, TextStyle.SHORT, enUS, "Apr"},
            {MONTH_OF_YEAR, 5, TextStyle.SHORT, enUS, "May"},
            {MONTH_OF_YEAR, 6, TextStyle.SHORT, enUS, "Jun"},
            {MONTH_OF_YEAR, 7, TextStyle.SHORT, enUS, "Jul"},
            {MONTH_OF_YEAR, 8, TextStyle.SHORT, enUS, "Aug"},
            {MONTH_OF_YEAR, 9, TextStyle.SHORT, enUS, "Sep"},
            {MONTH_OF_YEAR, 10, TextStyle.SHORT, enUS, "Oct"},
            {MONTH_OF_YEAR, 11, TextStyle.SHORT, enUS, "Nov"},
            {MONTH_OF_YEAR, 12, TextStyle.SHORT, enUS, "Dec"},

            {MONTH_OF_YEAR, 1, TextStyle.SHORT, frFR, "janv."},
            {MONTH_OF_YEAR, 2, TextStyle.SHORT, frFR, "f\u00E9vr."},
            {MONTH_OF_YEAR, 3, TextStyle.SHORT, frFR, "mars"},
            {MONTH_OF_YEAR, 4, TextStyle.SHORT, frFR, "avr."},
            {MONTH_OF_YEAR, 5, TextStyle.SHORT, frFR, "mai"},
            {MONTH_OF_YEAR, 6, TextStyle.SHORT, frFR, "juin"},
            {MONTH_OF_YEAR, 7, TextStyle.SHORT, frFR, "juil."},
            {MONTH_OF_YEAR, 8, TextStyle.SHORT, frFR, "ao\u00FBt"},
            {MONTH_OF_YEAR, 9, TextStyle.SHORT, frFR, "sept."},
            {MONTH_OF_YEAR, 10, TextStyle.SHORT, frFR, "oct."},
            {MONTH_OF_YEAR, 11, TextStyle.SHORT, frFR, "nov."},
            {MONTH_OF_YEAR, 12, TextStyle.SHORT, frFR, "d\u00E9c."},

            {MONTH_OF_YEAR, 1, TextStyle.FULL, enUS, "January"},
            {MONTH_OF_YEAR, 2, TextStyle.FULL, enUS, "February"},
            {MONTH_OF_YEAR, 3, TextStyle.FULL, enUS, "March"},
            {MONTH_OF_YEAR, 4, TextStyle.FULL, enUS, "April"},
            {MONTH_OF_YEAR, 5, TextStyle.FULL, enUS, "May"},
            {MONTH_OF_YEAR, 6, TextStyle.FULL, enUS, "June"},
            {MONTH_OF_YEAR, 7, TextStyle.FULL, enUS, "July"},
            {MONTH_OF_YEAR, 8, TextStyle.FULL, enUS, "August"},
            {MONTH_OF_YEAR, 9, TextStyle.FULL, enUS, "September"},
            {MONTH_OF_YEAR, 10, TextStyle.FULL, enUS, "October"},
            {MONTH_OF_YEAR, 11, TextStyle.FULL, enUS, "November"},
            {MONTH_OF_YEAR, 12, TextStyle.FULL, enUS, "December"},

            {MONTH_OF_YEAR, 1, TextStyle.FULL, ptBR, "Janeiro"},
            {MONTH_OF_YEAR, 2, TextStyle.FULL, ptBR, "Fevereiro"},
            {MONTH_OF_YEAR, 3, TextStyle.FULL, ptBR, "Mar\u00E7o"},
            {MONTH_OF_YEAR, 4, TextStyle.FULL, ptBR, "Abril"},
            {MONTH_OF_YEAR, 5, TextStyle.FULL, ptBR, "Maio"},
            {MONTH_OF_YEAR, 6, TextStyle.FULL, ptBR, "Junho"},
            {MONTH_OF_YEAR, 7, TextStyle.FULL, ptBR, "Julho"},
            {MONTH_OF_YEAR, 8, TextStyle.FULL, ptBR, "Agosto"},
            {MONTH_OF_YEAR, 9, TextStyle.FULL, ptBR, "Setembro"},
            {MONTH_OF_YEAR, 10, TextStyle.FULL, ptBR, "Outubro"},
            {MONTH_OF_YEAR, 11, TextStyle.FULL, ptBR, "Novembro"},
            {MONTH_OF_YEAR, 12, TextStyle.FULL, ptBR, "Dezembro"},

            {AMPM_OF_DAY, 0, TextStyle.SHORT, enUS, "AM"},
            {AMPM_OF_DAY, 1, TextStyle.SHORT, enUS, "PM"},

        };
    }

    @Test(dataProvider = "Text")
    public void test_getText(TemporalField field, Number value, TextStyle style, Locale locale, String expected) {
        DateTimeTextProvider tp = DateTimeTextProvider.getInstance();
        assertEquals(tp.getText(field, value.longValue(), style, locale).equalsIgnoreCase(expected), true, expected);
    }

}
