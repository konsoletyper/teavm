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
package org.teavm.classlib.java.sql;

/**
 *
 * @author Bora Ertung
 */
public class TDate extends org.teavm.classlib.java.util.TDate {
 
    public TDate(int year, int month, int day) {
        super(year, month, day);
    }
 
    public TDate(long date) {
        super(date);

    }
 
    @Override
    public void setTime(long date) {
        super.setTime(date);
    }

    @Override
    public String toString () {
        char buf[] = "2000-00-00".toCharArray();
        
        int year = super.getYear() + 1900;
        buf[0] = Character.forDigit(year/1000,10);
        buf[1] = Character.forDigit((year/100)%10,10);
        buf[2] = Character.forDigit((year/10)%10,10);
        buf[3] = Character.forDigit(year%10,10);
        
        int month = super.getMonth() + 1;
        buf[5] = Character.forDigit(month/10,10);
        buf[6] = Character.forDigit(month%10,10);
        
        int day = super.getDate();
        buf[8] = Character.forDigit(day/10,10);
        buf[9] = Character.forDigit(day%10,10);

        return new String(buf);
    }
}
