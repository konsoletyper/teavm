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
package org.teavm.classlib.java.time.format;

import static org.junit.Assert.assertEquals;
import static org.teavm.classlib.java.time.temporal.TChronoField.AMPM_OF_DAY;
import static org.teavm.classlib.java.time.temporal.TChronoField.DAY_OF_WEEK;
import static org.teavm.classlib.java.time.temporal.TChronoField.MONTH_OF_YEAR;

import java.util.Locale;

import org.junit.Test;
import org.teavm.classlib.java.time.temporal.TTemporalField;

public class TestSimpleDateTimeTextProvider {

    Locale enUS = new Locale("en", "US");

    Locale ptBR = new Locale("pt", "BR");

    Locale frFR = new Locale("fr", "FR");

    Object[][] data_text() {

        return new Object[][] { { DAY_OF_WEEK, 1, TTextStyle.SHORT, this.enUS, "Mon" },
        { DAY_OF_WEEK, 2, TTextStyle.SHORT, this.enUS, "Tue" }, { DAY_OF_WEEK, 3, TTextStyle.SHORT, this.enUS, "Wed" },
        { DAY_OF_WEEK, 4, TTextStyle.SHORT, this.enUS, "Thu" }, { DAY_OF_WEEK, 5, TTextStyle.SHORT, this.enUS, "Fri" },
        { DAY_OF_WEEK, 6, TTextStyle.SHORT, this.enUS, "Sat" }, { DAY_OF_WEEK, 7, TTextStyle.SHORT, this.enUS, "Sun" },

        { DAY_OF_WEEK, 1, TTextStyle.SHORT, this.ptBR, "Seg" }, { DAY_OF_WEEK, 2, TTextStyle.SHORT, this.ptBR, "Ter" },
        { DAY_OF_WEEK, 3, TTextStyle.SHORT, this.ptBR, "Qua" }, { DAY_OF_WEEK, 4, TTextStyle.SHORT, this.ptBR, "Qui" },
        { DAY_OF_WEEK, 5, TTextStyle.SHORT, this.ptBR, "Sex" },
        { DAY_OF_WEEK, 6, TTextStyle.SHORT, this.ptBR, "S\u00E1b" },
        { DAY_OF_WEEK, 7, TTextStyle.SHORT, this.ptBR, "Dom" },

        { DAY_OF_WEEK, 1, TTextStyle.FULL, this.enUS, "Monday" },
        { DAY_OF_WEEK, 2, TTextStyle.FULL, this.enUS, "Tuesday" },
        { DAY_OF_WEEK, 3, TTextStyle.FULL, this.enUS, "Wednesday" },
        { DAY_OF_WEEK, 4, TTextStyle.FULL, this.enUS, "Thursday" },
        { DAY_OF_WEEK, 5, TTextStyle.FULL, this.enUS, "Friday" },
        { DAY_OF_WEEK, 6, TTextStyle.FULL, this.enUS, "Saturday" },
        { DAY_OF_WEEK, 7, TTextStyle.FULL, this.enUS, "Sunday" },

        { DAY_OF_WEEK, 1, TTextStyle.FULL, this.ptBR, "Segunda-feira" },
        { DAY_OF_WEEK, 2, TTextStyle.FULL, this.ptBR, "Ter\u00E7a-feira" },
        { DAY_OF_WEEK, 3, TTextStyle.FULL, this.ptBR, "Quarta-feira" },
        { DAY_OF_WEEK, 4, TTextStyle.FULL, this.ptBR, "Quinta-feira" },
        { DAY_OF_WEEK, 5, TTextStyle.FULL, this.ptBR, "Sexta-feira" },
        { DAY_OF_WEEK, 6, TTextStyle.FULL, this.ptBR, "S\u00E1bado" },
        { DAY_OF_WEEK, 7, TTextStyle.FULL, this.ptBR, "Domingo" },

        { MONTH_OF_YEAR, 1, TTextStyle.SHORT, this.enUS, "Jan" },
        { MONTH_OF_YEAR, 2, TTextStyle.SHORT, this.enUS, "Feb" },
        { MONTH_OF_YEAR, 3, TTextStyle.SHORT, this.enUS, "Mar" },
        { MONTH_OF_YEAR, 4, TTextStyle.SHORT, this.enUS, "Apr" },
        { MONTH_OF_YEAR, 5, TTextStyle.SHORT, this.enUS, "May" },
        { MONTH_OF_YEAR, 6, TTextStyle.SHORT, this.enUS, "Jun" },
        { MONTH_OF_YEAR, 7, TTextStyle.SHORT, this.enUS, "Jul" },
        { MONTH_OF_YEAR, 8, TTextStyle.SHORT, this.enUS, "Aug" },
        { MONTH_OF_YEAR, 9, TTextStyle.SHORT, this.enUS, "Sep" },
        { MONTH_OF_YEAR, 10, TTextStyle.SHORT, this.enUS, "Oct" },
        { MONTH_OF_YEAR, 11, TTextStyle.SHORT, this.enUS, "Nov" },
        { MONTH_OF_YEAR, 12, TTextStyle.SHORT, this.enUS, "Dec" },

        { MONTH_OF_YEAR, 1, TTextStyle.SHORT, this.frFR, "janv." },
        { MONTH_OF_YEAR, 2, TTextStyle.SHORT, this.frFR, "f\u00E9vr." },
        { MONTH_OF_YEAR, 3, TTextStyle.SHORT, this.frFR, "mars" },
        { MONTH_OF_YEAR, 4, TTextStyle.SHORT, this.frFR, "avr." },
        { MONTH_OF_YEAR, 5, TTextStyle.SHORT, this.frFR, "mai" },
        { MONTH_OF_YEAR, 6, TTextStyle.SHORT, this.frFR, "juin" },
        { MONTH_OF_YEAR, 7, TTextStyle.SHORT, this.frFR, "juil." },
        { MONTH_OF_YEAR, 8, TTextStyle.SHORT, this.frFR, "ao\u00FBt" },
        { MONTH_OF_YEAR, 9, TTextStyle.SHORT, this.frFR, "sept." },
        { MONTH_OF_YEAR, 10, TTextStyle.SHORT, this.frFR, "oct." },
        { MONTH_OF_YEAR, 11, TTextStyle.SHORT, this.frFR, "nov." },
        { MONTH_OF_YEAR, 12, TTextStyle.SHORT, this.frFR, "d\u00E9c." },

        { MONTH_OF_YEAR, 1, TTextStyle.FULL, this.enUS, "January" },
        { MONTH_OF_YEAR, 2, TTextStyle.FULL, this.enUS, "February" },
        { MONTH_OF_YEAR, 3, TTextStyle.FULL, this.enUS, "March" },
        { MONTH_OF_YEAR, 4, TTextStyle.FULL, this.enUS, "April" },
        { MONTH_OF_YEAR, 5, TTextStyle.FULL, this.enUS, "May" },
        { MONTH_OF_YEAR, 6, TTextStyle.FULL, this.enUS, "June" },
        { MONTH_OF_YEAR, 7, TTextStyle.FULL, this.enUS, "July" },
        { MONTH_OF_YEAR, 8, TTextStyle.FULL, this.enUS, "August" },
        { MONTH_OF_YEAR, 9, TTextStyle.FULL, this.enUS, "September" },
        { MONTH_OF_YEAR, 10, TTextStyle.FULL, this.enUS, "October" },
        { MONTH_OF_YEAR, 11, TTextStyle.FULL, this.enUS, "November" },
        { MONTH_OF_YEAR, 12, TTextStyle.FULL, this.enUS, "December" },

        { MONTH_OF_YEAR, 1, TTextStyle.FULL, this.ptBR, "Janeiro" },
        { MONTH_OF_YEAR, 2, TTextStyle.FULL, this.ptBR, "Fevereiro" },
        { MONTH_OF_YEAR, 3, TTextStyle.FULL, this.ptBR, "Mar\u00E7o" },
        { MONTH_OF_YEAR, 4, TTextStyle.FULL, this.ptBR, "Abril" },
        { MONTH_OF_YEAR, 5, TTextStyle.FULL, this.ptBR, "Maio" },
        { MONTH_OF_YEAR, 6, TTextStyle.FULL, this.ptBR, "Junho" },
        { MONTH_OF_YEAR, 7, TTextStyle.FULL, this.ptBR, "Julho" },
        { MONTH_OF_YEAR, 8, TTextStyle.FULL, this.ptBR, "Agosto" },
        { MONTH_OF_YEAR, 9, TTextStyle.FULL, this.ptBR, "Setembro" },
        { MONTH_OF_YEAR, 10, TTextStyle.FULL, this.ptBR, "Outubro" },
        { MONTH_OF_YEAR, 11, TTextStyle.FULL, this.ptBR, "Novembro" },
        { MONTH_OF_YEAR, 12, TTextStyle.FULL, this.ptBR, "Dezembro" },

        { AMPM_OF_DAY, 0, TTextStyle.SHORT, this.enUS, "AM" }, { AMPM_OF_DAY, 1, TTextStyle.SHORT, this.enUS, "PM" },

        };
    }

    @Test
    public void test_getText() {

        for (Object[] data : data_text()) {
            TTemporalField field = (TTemporalField) data[0];
            Number value = (Number) data[1];
            TTextStyle style = (TTextStyle) data[2];
            Locale locale = (Locale) data[3];
            String expected = (String) data[4];

            TDateTimeTextProvider tp = TDateTimeTextProvider.getInstance();
            assertEquals(expected, tp.getText(field, value.longValue(), style, locale).equalsIgnoreCase(expected),
                    true);
        }
    }

}
