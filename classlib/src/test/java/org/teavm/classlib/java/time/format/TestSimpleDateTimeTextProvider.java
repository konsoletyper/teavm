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
package org.teavm.classlib.java.time.format;

import static org.junit.Assert.assertEquals;
import static org.teavm.classlib.java.time.temporal.TChronoField.AMPM_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import org.teavm.classlib.java.util.TLocale;

import org.junit.Before;
import org.testng.annotations.DataProvider;
import org.junit.Test;
import org.teavm.classlib.java.time.temporal.TTemporalField;

@Test
public class TestSimpleDateTimeTextProvider {

    TLocale enUS = new TLocale("en", "US");
    TLocale ptBR = new TLocale("pt", "BR");
    TLocale frFR = new TLocale("fr", "FR");

    @Before
    public void setUp() {
    }

    //-----------------------------------------------------------------------
    @DataProvider(name = "Text")
    Object[][] data_text() {
        return new Object[][] {
            {DAY_OF_WEEK, 1, TTextStyle.SHORT, enUS, "Mon"},
            {DAY_OF_WEEK, 2, TTextStyle.SHORT, enUS, "Tue"},
            {DAY_OF_WEEK, 3, TTextStyle.SHORT, enUS, "Wed"},
            {DAY_OF_WEEK, 4, TTextStyle.SHORT, enUS, "Thu"},
            {DAY_OF_WEEK, 5, TTextStyle.SHORT, enUS, "Fri"},
            {DAY_OF_WEEK, 6, TTextStyle.SHORT, enUS, "Sat"},
            {DAY_OF_WEEK, 7, TTextStyle.SHORT, enUS, "Sun"},

            {DAY_OF_WEEK, 1, TTextStyle.SHORT, ptBR, "Seg"},
            {DAY_OF_WEEK, 2, TTextStyle.SHORT, ptBR, "Ter"},
            {DAY_OF_WEEK, 3, TTextStyle.SHORT, ptBR, "Qua"},
            {DAY_OF_WEEK, 4, TTextStyle.SHORT, ptBR, "Qui"},
            {DAY_OF_WEEK, 5, TTextStyle.SHORT, ptBR, "Sex"},
            {DAY_OF_WEEK, 6, TTextStyle.SHORT, ptBR, "S\u00E1b"},
            {DAY_OF_WEEK, 7, TTextStyle.SHORT, ptBR, "Dom"},

            {DAY_OF_WEEK, 1, TTextStyle.FULL, enUS, "Monday"},
            {DAY_OF_WEEK, 2, TTextStyle.FULL, enUS, "Tuesday"},
            {DAY_OF_WEEK, 3, TTextStyle.FULL, enUS, "Wednesday"},
            {DAY_OF_WEEK, 4, TTextStyle.FULL, enUS, "Thursday"},
            {DAY_OF_WEEK, 5, TTextStyle.FULL, enUS, "Friday"},
            {DAY_OF_WEEK, 6, TTextStyle.FULL, enUS, "Saturday"},
            {DAY_OF_WEEK, 7, TTextStyle.FULL, enUS, "Sunday"},

            {DAY_OF_WEEK, 1, TTextStyle.FULL, ptBR, "Segunda-feira"},
            {DAY_OF_WEEK, 2, TTextStyle.FULL, ptBR, "Ter\u00E7a-feira"},
            {DAY_OF_WEEK, 3, TTextStyle.FULL, ptBR, "Quarta-feira"},
            {DAY_OF_WEEK, 4, TTextStyle.FULL, ptBR, "Quinta-feira"},
            {DAY_OF_WEEK, 5, TTextStyle.FULL, ptBR, "Sexta-feira"},
            {DAY_OF_WEEK, 6, TTextStyle.FULL, ptBR, "S\u00E1bado"},
            {DAY_OF_WEEK, 7, TTextStyle.FULL, ptBR, "Domingo"},

            {MONTH_OF_YEAR, 1, TTextStyle.SHORT, enUS, "Jan"},
            {MONTH_OF_YEAR, 2, TTextStyle.SHORT, enUS, "Feb"},
            {MONTH_OF_YEAR, 3, TTextStyle.SHORT, enUS, "Mar"},
            {MONTH_OF_YEAR, 4, TTextStyle.SHORT, enUS, "Apr"},
            {MONTH_OF_YEAR, 5, TTextStyle.SHORT, enUS, "May"},
            {MONTH_OF_YEAR, 6, TTextStyle.SHORT, enUS, "Jun"},
            {MONTH_OF_YEAR, 7, TTextStyle.SHORT, enUS, "Jul"},
            {MONTH_OF_YEAR, 8, TTextStyle.SHORT, enUS, "Aug"},
            {MONTH_OF_YEAR, 9, TTextStyle.SHORT, enUS, "Sep"},
            {MONTH_OF_YEAR, 10, TTextStyle.SHORT, enUS, "Oct"},
            {MONTH_OF_YEAR, 11, TTextStyle.SHORT, enUS, "Nov"},
            {MONTH_OF_YEAR, 12, TTextStyle.SHORT, enUS, "Dec"},

            {MONTH_OF_YEAR, 1, TTextStyle.SHORT, frFR, "janv."},
            {MONTH_OF_YEAR, 2, TTextStyle.SHORT, frFR, "f\u00E9vr."},
            {MONTH_OF_YEAR, 3, TTextStyle.SHORT, frFR, "mars"},
            {MONTH_OF_YEAR, 4, TTextStyle.SHORT, frFR, "avr."},
            {MONTH_OF_YEAR, 5, TTextStyle.SHORT, frFR, "mai"},
            {MONTH_OF_YEAR, 6, TTextStyle.SHORT, frFR, "juin"},
            {MONTH_OF_YEAR, 7, TTextStyle.SHORT, frFR, "juil."},
            {MONTH_OF_YEAR, 8, TTextStyle.SHORT, frFR, "ao\u00FBt"},
            {MONTH_OF_YEAR, 9, TTextStyle.SHORT, frFR, "sept."},
            {MONTH_OF_YEAR, 10, TTextStyle.SHORT, frFR, "oct."},
            {MONTH_OF_YEAR, 11, TTextStyle.SHORT, frFR, "nov."},
            {MONTH_OF_YEAR, 12, TTextStyle.SHORT, frFR, "d\u00E9c."},

            {MONTH_OF_YEAR, 1, TTextStyle.FULL, enUS, "January"},
            {MONTH_OF_YEAR, 2, TTextStyle.FULL, enUS, "February"},
            {MONTH_OF_YEAR, 3, TTextStyle.FULL, enUS, "March"},
            {MONTH_OF_YEAR, 4, TTextStyle.FULL, enUS, "April"},
            {MONTH_OF_YEAR, 5, TTextStyle.FULL, enUS, "May"},
            {MONTH_OF_YEAR, 6, TTextStyle.FULL, enUS, "June"},
            {MONTH_OF_YEAR, 7, TTextStyle.FULL, enUS, "July"},
            {MONTH_OF_YEAR, 8, TTextStyle.FULL, enUS, "August"},
            {MONTH_OF_YEAR, 9, TTextStyle.FULL, enUS, "September"},
            {MONTH_OF_YEAR, 10, TTextStyle.FULL, enUS, "October"},
            {MONTH_OF_YEAR, 11, TTextStyle.FULL, enUS, "November"},
            {MONTH_OF_YEAR, 12, TTextStyle.FULL, enUS, "December"},

            {MONTH_OF_YEAR, 1, TTextStyle.FULL, ptBR, "Janeiro"},
            {MONTH_OF_YEAR, 2, TTextStyle.FULL, ptBR, "Fevereiro"},
            {MONTH_OF_YEAR, 3, TTextStyle.FULL, ptBR, "Mar\u00E7o"},
            {MONTH_OF_YEAR, 4, TTextStyle.FULL, ptBR, "Abril"},
            {MONTH_OF_YEAR, 5, TTextStyle.FULL, ptBR, "Maio"},
            {MONTH_OF_YEAR, 6, TTextStyle.FULL, ptBR, "Junho"},
            {MONTH_OF_YEAR, 7, TTextStyle.FULL, ptBR, "Julho"},
            {MONTH_OF_YEAR, 8, TTextStyle.FULL, ptBR, "Agosto"},
            {MONTH_OF_YEAR, 9, TTextStyle.FULL, ptBR, "Setembro"},
            {MONTH_OF_YEAR, 10, TTextStyle.FULL, ptBR, "Outubro"},
            {MONTH_OF_YEAR, 11, TTextStyle.FULL, ptBR, "Novembro"},
            {MONTH_OF_YEAR, 12, TTextStyle.FULL, ptBR, "Dezembro"},

            {AMPM_OF_DAY, 0, TTextStyle.SHORT, enUS, "AM"},
            {AMPM_OF_DAY, 1, TTextStyle.SHORT, enUS, "PM"},

        };
    }

    @Test(dataProvider = "Text")
    public void test_getText(TTemporalField field, Number value, TTextStyle style, TLocale locale, String expected) {
        TDateTimeTextProvider tp = TDateTimeTextProvider.getInstance();
        assertEquals(tp.getText(field, value.longValue(), style, locale).equalsIgnoreCase(expected), true, expected);
    }

}
