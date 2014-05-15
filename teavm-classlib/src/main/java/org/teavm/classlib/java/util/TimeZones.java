/*
 *  Copyright 2014 Alexey Andreev.
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
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.teavm.classlib.java.util;

final class TimeZones {

    private static final int HALF_HOUR = 1800000;
    private static final int ONE_HOUR = HALF_HOUR * 2;

    public static TimeZone[] getTimeZones() {
        return new TimeZone[] {
                new SimpleTimeZone(-11 * ONE_HOUR, "MIT"), //$NON-NLS-1$
                new SimpleTimeZone(-10 * ONE_HOUR, "HST"), //$NON-NLS-1$
                new SimpleTimeZone(-9 * ONE_HOUR, "AST", Calendar.APRIL, 1, //$NON-NLS-1$
                        -Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-8 * ONE_HOUR, "PST", Calendar.APRIL, 1, //$NON-NLS-1$
                        -Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-7 * ONE_HOUR, "MST", Calendar.APRIL, 1, //$NON-NLS-1$
                        -Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-7 * ONE_HOUR, "PNT"), //$NON-NLS-1$
                new SimpleTimeZone(-6 * ONE_HOUR, "CST", Calendar.APRIL, 1, //$NON-NLS-1$
                        -Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-5 * ONE_HOUR, "EST", Calendar.APRIL, 1, //$NON-NLS-1$
                        -Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-5 * ONE_HOUR, "IET"), //$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "PRT"), //$NON-NLS-1$
                new SimpleTimeZone(-3 * ONE_HOUR - 1800000,
                        "CNT", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 60000,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 60000),
                new SimpleTimeZone(-3 * ONE_HOUR, "AGT"), //$NON-NLS-1$
                new SimpleTimeZone(-3 * ONE_HOUR, "BET", Calendar.OCTOBER, 8, //$NON-NLS-1$
                        -Calendar.SUNDAY, 0 * ONE_HOUR, Calendar.FEBRUARY, 15,
                        -Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(0 * ONE_HOUR, "UTC"), //$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "WET", Calendar.MARCH, -1, //$NON-NLS-1$
                        Calendar.SUNDAY, 1 * ONE_HOUR, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR, "ECT", Calendar.MARCH, -1, //$NON-NLS-1$
                        Calendar.SUNDAY, 1 * ONE_HOUR, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR, "MET", Calendar.MARCH, 21, 0, //$NON-NLS-1$
                        0 * ONE_HOUR, Calendar.SEPTEMBER, 23, 0, 0 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR, "ART", Calendar.APRIL, -1, //$NON-NLS-1$
                        Calendar.FRIDAY, 0 * ONE_HOUR, Calendar.SEPTEMBER, -1,
                        Calendar.THURSDAY, 23 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR, "CAT"), //$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "EET", Calendar.MARCH, -1, //$NON-NLS-1$
                        Calendar.SUNDAY, 1 * ONE_HOUR, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(3 * ONE_HOUR, "EAT"), //$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR + 1800000,
                        "Asia/Tehran", //$NON-NLS-1$
                        Calendar.MARCH, 21, 0, 0 * ONE_HOUR,
                        Calendar.SEPTEMBER, 23, 0, 0 * ONE_HOUR),
                new SimpleTimeZone(4 * ONE_HOUR, "NET", Calendar.MARCH, -1, //$NON-NLS-1$
                        Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(5 * ONE_HOUR, "PLT"), //$NON-NLS-1$
                new SimpleTimeZone(5 * ONE_HOUR + 1800000, "IST"), //$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR, "BST"), //$NON-NLS-1$
                new SimpleTimeZone(7 * ONE_HOUR, "VST"), //$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "CTT"), //$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR, "JST"), //$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR + 1800000, "ACT"), //$NON-NLS-1$
                new SimpleTimeZone(10 * ONE_HOUR, "AET", Calendar.OCTOBER, -1, //$NON-NLS-1$
                        Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.MARCH, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(11 * ONE_HOUR, "SST"), //$NON-NLS-1$
                new SimpleTimeZone(12 * ONE_HOUR, "NST", Calendar.OCTOBER, 1, //$NON-NLS-1$
                        -Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.MARCH, 15,
                        -Calendar.SUNDAY, 2 * ONE_HOUR),

                new SimpleTimeZone(-6 * ONE_HOUR, "America/Costa_Rica"), //$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR,
                        "America/Halifax", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-2 * ONE_HOUR, "Atlantic/South_Georgia"), //$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR,
                        "Europe/London", //$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Algiers"), //$NON-NLS-1$
                new SimpleTimeZone(
                        2 * ONE_HOUR,
                        "Africa/Cairo", //$NON-NLS-1$
                        Calendar.APRIL, -1, Calendar.FRIDAY, 0 * ONE_HOUR,
                        Calendar.SEPTEMBER, -1, Calendar.THURSDAY,
                        23 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Harare"), //$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Asia/Jerusalem", //$NON-NLS-1$
                        Calendar.APRIL, 9, 0, 1 * ONE_HOUR, Calendar.SEPTEMBER,
                        24, 0, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Europe/Bucharest", //$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(3 * ONE_HOUR,
                        "Europe/Moscow", //$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(4 * ONE_HOUR + 1800000, "Asia/Kabul"), //$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR + 1800000,
                        "Australia/Adelaide", Calendar.OCTOBER, -1, //$NON-NLS-1$
                        Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.MARCH, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(10 * ONE_HOUR, "Australia/Brisbane"), //$NON-NLS-1$
                new SimpleTimeZone(10 * ONE_HOUR,
                        "Australia/Hobart", //$NON-NLS-1$
                        Calendar.OCTOBER, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR),

                new SimpleTimeZone(-9 * ONE_HOUR - 1800000, "Pacific/Marquesas"), //$NON-NLS-1$
                new SimpleTimeZone(-1 * ONE_HOUR,
                        "Atlantic/Azores", //$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(4 * ONE_HOUR, "Asia/Dubai"), //$NON-NLS-1$
                new SimpleTimeZone(20700000, "Asia/Katmandu"), //$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR + 1800000, "Asia/Rangoon"), //$NON-NLS-1$
                new SimpleTimeZone(45900000,
                        "Pacific/Chatham", //$NON-NLS-1$
                        Calendar.OCTOBER, 1, -Calendar.SUNDAY, 9900000,
                        Calendar.MARCH, 15, -Calendar.SUNDAY, 9900000),

                new SimpleTimeZone(-11 * ONE_HOUR, "Pacific/Apia"), //$NON-NLS-1$
                new SimpleTimeZone(-11 * ONE_HOUR, "Pacific/Niue"), //$NON-NLS-1$
                new SimpleTimeZone(-11 * ONE_HOUR, "Pacific/Pago_Pago"), //$NON-NLS-1$
                new SimpleTimeZone(-10 * ONE_HOUR,
                        "America/Adak", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-10 * ONE_HOUR, "Pacific/Fakaofo"), //$NON-NLS-1$
                new SimpleTimeZone(-10 * ONE_HOUR, "Pacific/Honolulu"), //$NON-NLS-1$
                new SimpleTimeZone(-10 * ONE_HOUR, "Pacific/Rarotonga"), //$NON-NLS-1$
                new SimpleTimeZone(-10 * ONE_HOUR, "Pacific/Tahiti"), //$NON-NLS-1$
                new SimpleTimeZone(-9 * ONE_HOUR,
                        "America/Anchorage", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-9 * ONE_HOUR, "Pacific/Gambier"), //$NON-NLS-1$
                new SimpleTimeZone(-8 * ONE_HOUR,
                        "America/Los_Angeles", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-8 * ONE_HOUR,
                        "America/Tijuana", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-8 * ONE_HOUR,
                        "America/Vancouver", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-8 * ONE_HOUR, "Pacific/Pitcairn"), //$NON-NLS-1$
                new SimpleTimeZone(-7 * ONE_HOUR, "America/Dawson_Creek"), //$NON-NLS-1$
                new SimpleTimeZone(-7 * ONE_HOUR,
                        "America/Denver", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-7 * ONE_HOUR,
                        "America/Edmonton", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-7 * ONE_HOUR,
                        "America/Mazatlan", //$NON-NLS-1$
                        Calendar.MAY, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.SEPTEMBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-7 * ONE_HOUR, "America/Phoenix"), //$NON-NLS-1$
                new SimpleTimeZone(-6 * ONE_HOUR, "America/Belize"), //$NON-NLS-1$
                new SimpleTimeZone(-6 * ONE_HOUR,
                        "America/Chicago", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-6 * ONE_HOUR, "America/El_Salvador"), //$NON-NLS-1$
                new SimpleTimeZone(-6 * ONE_HOUR, "America/Managua"), //$NON-NLS-1$
                new SimpleTimeZone(-6 * ONE_HOUR,
                        "America/Mexico_City", //$NON-NLS-1$
                        Calendar.MAY, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.SEPTEMBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-6 * ONE_HOUR, "America/Regina"), //$NON-NLS-1$
                new SimpleTimeZone(-6 * ONE_HOUR, "America/Tegucigalpa"), //$NON-NLS-1$
                new SimpleTimeZone(-6 * ONE_HOUR,
                        "America/Winnipeg", //$NON-NLS-1$ //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-6 * ONE_HOUR,
                        "Pacific/Easter", //$NON-NLS-1$
                        Calendar.OCTOBER, 9, -Calendar.SUNDAY, 4 * ONE_HOUR,
                        Calendar.MARCH, 9, -Calendar.SUNDAY, 3 * ONE_HOUR),
                new SimpleTimeZone(-6 * ONE_HOUR, "Pacific/Galapagos"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Bogota"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Cayman"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR,
                        "America/Grand_Turk",//$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 0 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Guayaquil"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR,
                        "America/Havana", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 0 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Indianapolis"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Jamaica"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Lima"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR,
                        "America/Montreal", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-5 * ONE_HOUR,
                        "America/Nassau", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-5 * ONE_HOUR,
                        "America/New_York", //$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Panama"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Port-au-Prince"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Porto_Acre"), //$NON-NLS-1$
                new SimpleTimeZone(-5 * ONE_HOUR, "America/Rio_Branco"), //$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Anguilla"), //$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Antigua"), //$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Aruba"), //$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR,
                        "America/Asuncion", //$NON-NLS-1$
                        Calendar.OCTOBER, 1, -Calendar.SUNDAY, 0 * ONE_HOUR,
                        Calendar.MARCH, 1, -Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Barbados"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Caracas"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR,
                        "America/Cuiaba",//$NON-NLS-1$
                        Calendar.OCTOBER, 8, -Calendar.SUNDAY, 0 * ONE_HOUR,
                        Calendar.FEBRUARY, 15, -Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Curacao"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Dominica"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Grenada"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Guadeloupe"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Guyana"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/La_Paz"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Manaus"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Martinique"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Montserrat"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Port_of_Spain"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Puerto_Rico"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR,
                        "America/Santiago",//$NON-NLS-1$
                        Calendar.OCTOBER, 9, -Calendar.SUNDAY, 4 * ONE_HOUR,
                        Calendar.MARCH, 9, -Calendar.SUNDAY, 3 * ONE_HOUR),
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Santo_Domingo"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/St_Kitts"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/St_Lucia"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/St_Thomas"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR, "America/St_Vincent"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR,
                        "America/Thule",//$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-4 * ONE_HOUR, "America/Tortola"),//$NON-NLS-1$
                new SimpleTimeZone(-4 * ONE_HOUR,
                        "Antarctica/Palmer",//$NON-NLS-1$
                        Calendar.OCTOBER, 9, -Calendar.SUNDAY, 0 * ONE_HOUR,
                        Calendar.MARCH, 9, -Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(-4 * ONE_HOUR,
                        "Atlantic/Bermuda",//$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-4 * ONE_HOUR,
                        "Atlantic/Stanley",//$NON-NLS-1$
                        Calendar.SEPTEMBER, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.APRIL, 15, -Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-3 * ONE_HOUR - 1800000,
                        "America/St_Johns",//$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 60000,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 60000),
                new SimpleTimeZone(-3 * ONE_HOUR, "America/Buenos_Aires"),//$NON-NLS-1$
                new SimpleTimeZone(-3 * ONE_HOUR, "America/Cayenne"),//$NON-NLS-1$
                new SimpleTimeZone(-3 * ONE_HOUR, "America/Fortaleza"),//$NON-NLS-1$
                new SimpleTimeZone(-3 * ONE_HOUR,
                        "America/Godthab",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(-3 * ONE_HOUR,
                        "America/Miquelon",//$NON-NLS-1$
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(-3 * ONE_HOUR, "America/Montevideo"),//$NON-NLS-1$
                new SimpleTimeZone(-3 * ONE_HOUR, "America/Paramaribo"),//$NON-NLS-1$
                new SimpleTimeZone(-3 * ONE_HOUR,
                        "America/Sao_Paulo",//$NON-NLS-1$
                        Calendar.OCTOBER, 8, -Calendar.SUNDAY, 0 * ONE_HOUR,
                        Calendar.FEBRUARY, 15, -Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(-2 * ONE_HOUR, "America/Noronha"),//$NON-NLS-1$
                new SimpleTimeZone(-1 * ONE_HOUR,
                        "America/Scoresbysund",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(-1 * ONE_HOUR, "Atlantic/Cape_Verde"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Abidjan"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Accra"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Banjul"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Bissau"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Casablanca"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Conakry"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Dakar"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Freetown"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Lome"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Monrovia"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Nouakchott"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Ouagadougou"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Sao_Tome"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Africa/Timbuktu"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR,
                        "Atlantic/Canary",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(0 * ONE_HOUR,
                        "Atlantic/Faeroe",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(0 * ONE_HOUR, "Atlantic/Reykjavik"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR, "Atlantic/St_Helena"),//$NON-NLS-1$
                new SimpleTimeZone(0 * ONE_HOUR,
                        "Europe/Dublin",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(0 * ONE_HOUR,
                        "Europe/Lisbon",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Bangui"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Douala"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Kinshasa"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Lagos"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Libreville"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Luanda"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Malabo"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Ndjamena"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Niamey"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Porto-Novo"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR, "Africa/Tunis"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Africa/Windhoek",//$NON-NLS-1$
                        Calendar.SEPTEMBER, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.APRIL, 1, -Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR, "Atlantic/Jan_Mayen"),//$NON-NLS-1$
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Amsterdam",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Andorra",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Belgrade",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Berlin",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Brussels",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Budapest",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Copenhagen",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Gibraltar",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Luxembourg",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Madrid",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Malta",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Monaco",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR, "Europe/Oslo", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR, Calendar.OCTOBER,
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Paris",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Prague",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR, "Europe/Rome", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR, Calendar.OCTOBER,
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Stockholm",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Tirane",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Vaduz",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Vienna",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Warsaw",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(1 * ONE_HOUR,
                        "Europe/Zurich",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Blantyre"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Bujumbura"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Gaborone"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Johannesburg"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Kigali"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Lubumbashi"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Lusaka"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Maputo"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Maseru"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Mbabane"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Africa/Tripoli"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Asia/Amman", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.THURSDAY, 0 * ONE_HOUR,
                        Calendar.SEPTEMBER, -1, Calendar.THURSDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR, "Asia/Beirut", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.SUNDAY, 0 * ONE_HOUR, Calendar.OCTOBER,
                        -1, Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Asia/Damascus",//$NON-NLS-1$
                        Calendar.APRIL, 1, 0, 0 * ONE_HOUR, Calendar.OCTOBER,
                        1, 0, 0 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Asia/Nicosia",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Europe/Athens",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Europe/Chisinau",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Europe/Helsinki",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Europe/Istanbul",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Europe/Kaliningrad",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR, "Europe/Kiev", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR, Calendar.OCTOBER,
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Europe/Minsk",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR, "Europe/Riga", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR, Calendar.OCTOBER,
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Europe/Simferopol",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR,
                        "Europe/Sofia",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 1 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(2 * ONE_HOUR, "Europe/Tallinn"),//$NON-NLS-1$
                new SimpleTimeZone(2 * ONE_HOUR, "Europe/Vilnius"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Africa/Addis_Ababa"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Africa/Asmera"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Africa/Dar_es_Salaam"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Africa/Djibouti"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Africa/Kampala"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Africa/Khartoum"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Africa/Mogadishu"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Africa/Nairobi"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Asia/Aden"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR,
                        "Asia/Baghdad",//$NON-NLS-1$
                        Calendar.APRIL, 1, 0, 3 * ONE_HOUR, Calendar.OCTOBER,
                        1, 0, 3 * ONE_HOUR),
                new SimpleTimeZone(3 * ONE_HOUR, "Asia/Bahrain"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Asia/Kuwait"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Asia/Qatar"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Asia/Riyadh"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Indian/Antananarivo"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Indian/Comoro"),//$NON-NLS-1$
                new SimpleTimeZone(3 * ONE_HOUR, "Indian/Mayotte"),//$NON-NLS-1$
                new SimpleTimeZone(4 * ONE_HOUR, "Asia/Aqtau", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.SUNDAY, 0 * ONE_HOUR, Calendar.OCTOBER,
                        -1, Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(4 * ONE_HOUR, "Asia/Baku", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR, Calendar.OCTOBER,
                        -1, Calendar.SUNDAY, 1 * ONE_HOUR),
                new SimpleTimeZone(4 * ONE_HOUR, "Asia/Muscat"),//$NON-NLS-1$
                new SimpleTimeZone(4 * ONE_HOUR,
                        "Asia/Tbilisi",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 0 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(4 * ONE_HOUR,
                        "Asia/Yerevan",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(4 * ONE_HOUR,
                        "Europe/Samara",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(4 * ONE_HOUR, "Indian/Mahe"),//$NON-NLS-1$
                new SimpleTimeZone(4 * ONE_HOUR, "Indian/Mauritius"),//$NON-NLS-1$
                new SimpleTimeZone(4 * ONE_HOUR, "Indian/Reunion"),//$NON-NLS-1$
                new SimpleTimeZone(5 * ONE_HOUR, "Asia/Aqtobe", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.SUNDAY, 0 * ONE_HOUR, Calendar.OCTOBER,
                        -1, Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(5 * ONE_HOUR, "Asia/Ashgabat"),//$NON-NLS-1$
                new SimpleTimeZone(5 * ONE_HOUR, "Asia/Ashkhabad"),//$NON-NLS-1$
                new SimpleTimeZone(
                        5 * ONE_HOUR,
                        "Asia/Bishkek",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY,
                        2 * ONE_HOUR + 1800000, Calendar.OCTOBER, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR + 1800000),
                new SimpleTimeZone(5 * ONE_HOUR, "Asia/Dushanbe"),//$NON-NLS-1$
                new SimpleTimeZone(5 * ONE_HOUR, "Asia/Karachi"),//$NON-NLS-1$
                new SimpleTimeZone(5 * ONE_HOUR, "Asia/Tashkent"),//$NON-NLS-1$
                new SimpleTimeZone(5 * ONE_HOUR,
                        "Asia/Yekaterinburg",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(5 * ONE_HOUR, "Indian/Kerguelen"),//$NON-NLS-1$
                new SimpleTimeZone(5 * ONE_HOUR, "Indian/Maldives"),//$NON-NLS-1$
                new SimpleTimeZone(5 * ONE_HOUR + 1800000, "Asia/Calcutta"),//$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR, "Antarctica/Mawson"),//$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR, "Asia/Almaty", Calendar.MARCH,//$NON-NLS-1$
                        -1, Calendar.SUNDAY, 0 * ONE_HOUR, Calendar.OCTOBER,
                        -1, Calendar.SUNDAY, 0 * ONE_HOUR),
                new SimpleTimeZone(5 * ONE_HOUR + HALF_HOUR, "Asia/Colombo"),//$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR, "Asia/Dacca"),//$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR, "Asia/Dhaka"),//$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR,
                        "Asia/Novosibirsk",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(6 * ONE_HOUR, "Asia/Thimbu"),//$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR, "Asia/Thimphu"),//$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR, "Indian/Chagos"),//$NON-NLS-1$
                new SimpleTimeZone(6 * ONE_HOUR + 1800000, "Indian/Cocos"),//$NON-NLS-1$
                new SimpleTimeZone(7 * ONE_HOUR, "Asia/Bangkok"),//$NON-NLS-1$
                new SimpleTimeZone(7 * ONE_HOUR, "Asia/Jakarta"),//$NON-NLS-1$
                new SimpleTimeZone(7 * ONE_HOUR,
                        "Asia/Krasnoyarsk",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(7 * ONE_HOUR, "Asia/Phnom_Penh"),//$NON-NLS-1$
                new SimpleTimeZone(7 * ONE_HOUR, "Asia/Saigon"),//$NON-NLS-1$
                new SimpleTimeZone(7 * ONE_HOUR, "Asia/Vientiane"),//$NON-NLS-1$
                new SimpleTimeZone(7 * ONE_HOUR, "Indian/Christmas"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Antarctica/Casey"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Brunei"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Hong_Kong"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR,
                        "Asia/Irkutsk",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Kuala_Lumpur"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Macao"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Manila"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Shanghai"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Singapore"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Taipei"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Ujung_Pandang"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Ulaanbaatar"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Asia/Ulan_Bator"),//$NON-NLS-1$
                new SimpleTimeZone(8 * ONE_HOUR, "Australia/Perth"),//$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR, "Asia/Jayapura"),//$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR, "Asia/Pyongyang"),//$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR, "Asia/Seoul"),//$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR, "Asia/Tokyo"),//$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR,
                        "Asia/Yakutsk",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),//$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR, "Pacific/Palau"),//$NON-NLS-1$
                new SimpleTimeZone(9 * ONE_HOUR + 1800000,
                        "Australia/Broken_Hill", Calendar.OCTOBER, -1,//$NON-NLS-1$
                        Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.MARCH, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(9 * ONE_HOUR + 1800000, "Australia/Darwin"),//$NON-NLS-1$
                new SimpleTimeZone(10 * ONE_HOUR, "Antarctica/DumontDUrville"),//$NON-NLS-1$
                new SimpleTimeZone(10 * ONE_HOUR,
                        "Asia/Vladivostok",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(10 * ONE_HOUR,
                        "Australia/Sydney",//$NON-NLS-1$
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(10 * ONE_HOUR, "Pacific/Guam"),//$NON-NLS-1$
                new SimpleTimeZone(10 * ONE_HOUR, "Pacific/Port_Moresby"),//$NON-NLS-1$
                new SimpleTimeZone(10 * ONE_HOUR, "Pacific/Saipan"),//$NON-NLS-1$
                new SimpleTimeZone(10 * ONE_HOUR, "Pacific/Truk"),//$NON-NLS-1$
                new SimpleTimeZone(10 * ONE_HOUR + 1800000,
                        "Australia/Lord_Howe", Calendar.OCTOBER, -1,//$NON-NLS-1$
                        Calendar.SUNDAY, 2 * ONE_HOUR, Calendar.MARCH, -1,
                        Calendar.SUNDAY, 2 * ONE_HOUR, 1800000),
                new SimpleTimeZone(11 * ONE_HOUR,
                        "Asia/Magadan",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(11 * ONE_HOUR, "Pacific/Efate"),//$NON-NLS-1$
                new SimpleTimeZone(11 * ONE_HOUR, "Pacific/Guadalcanal"),//$NON-NLS-1$
                new SimpleTimeZone(11 * ONE_HOUR, "Pacific/Kosrae"),//$NON-NLS-1$
                new SimpleTimeZone(11 * ONE_HOUR, "Pacific/Noumea"),//$NON-NLS-1$
                new SimpleTimeZone(11 * ONE_HOUR, "Pacific/Ponape"),//$NON-NLS-1$
                new SimpleTimeZone(11 * ONE_HOUR + 1800000, "Pacific/Norfolk"),//$NON-NLS-1$
                new SimpleTimeZone(12 * ONE_HOUR,
                        "Antarctica/McMurdo",//$NON-NLS-1$
                        Calendar.OCTOBER, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.MARCH, 15, -Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(12 * ONE_HOUR,
                        "Asia/Anadyr",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(12 * ONE_HOUR,
                        "Asia/Kamchatka",//$NON-NLS-1$
                        Calendar.MARCH, -1, Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(12 * ONE_HOUR,
                        "Pacific/Auckland",//$NON-NLS-1$
                        Calendar.OCTOBER, 1, -Calendar.SUNDAY, 2 * ONE_HOUR,
                        Calendar.MARCH, 15, -Calendar.SUNDAY, 2 * ONE_HOUR),
                new SimpleTimeZone(12 * ONE_HOUR, "Pacific/Fiji"),//$NON-NLS-1$
                new SimpleTimeZone(12 * ONE_HOUR, "Pacific/Funafuti"),//$NON-NLS-1$
                new SimpleTimeZone(12 * ONE_HOUR, "Pacific/Majuro"),//$NON-NLS-1$
                new SimpleTimeZone(12 * ONE_HOUR, "Pacific/Nauru"),//$NON-NLS-1$
                new SimpleTimeZone(12 * ONE_HOUR, "Pacific/Tarawa"),//$NON-NLS-1$
                new SimpleTimeZone(12 * ONE_HOUR, "Pacific/Wake"),//$NON-NLS-1$
                new SimpleTimeZone(12 * ONE_HOUR, "Pacific/Wallis"),//$NON-NLS-1$
                new SimpleTimeZone(13 * ONE_HOUR, "Pacific/Enderbury"),//$NON-NLS-1$
                new SimpleTimeZone(13 * ONE_HOUR, "Pacific/Tongatapu"),//$NON-NLS-1$
                new SimpleTimeZone(14 * ONE_HOUR, "Pacific/Kiritimati"), };//$NON-NLS-1$
    }
}
