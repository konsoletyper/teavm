/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.teavm.classlib.java.sql;

/**
 *
 * @author bora
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
