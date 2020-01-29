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
package org.threeten.bp.chrono;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import org.threeten.bp.DateTimeException;
import org.threeten.bp.LocalDate;
import org.threeten.bp.jdk8.DefaultInterfaceEra;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.ChronoField;
import org.threeten.bp.temporal.TemporalField;
import org.threeten.bp.temporal.ValueRange;

/**
 * An era in the Japanese Imperial calendar system.
 * <p>
 * This class defines the valid eras for the Japanese chronology.
 * Japan introduced the Gregorian calendar starting with Meiji 6.
 * Only Meiji and later eras are supported;
 * dates before Meiji 6, January 1 are not supported.
 * <p>
 * The four supported eras are hard-coded.
 * A single additional era may be registered using {@link #registerEra(LocalDate, String)}.
 *
 * <h3>Specification for implementors</h3>
 * This class is immutable and thread-safe.
 */
public final class JapaneseEra
        extends DefaultInterfaceEra
        implements Serializable {

    // The offset value to 0-based index from the era value.
    // i.e., getValue() + ERA_OFFSET == 0-based index; except that -999 is mapped to zero
    static final int ERA_OFFSET = 2;

    /**
     * The singleton instance for the 'Meiji' era (1868-09-08 - 1912-07-29)
     * which has the value -1.
     */
    public static final JapaneseEra MEIJI = new JapaneseEra(-1, LocalDate.of(1868, 9, 8), "Meiji");
    /**
     * The singleton instance for the 'Taisho' era (1912-07-30 - 1926-12-24)
     * which has the value 0.
     */
    public static final JapaneseEra TAISHO = new JapaneseEra(0, LocalDate.of(1912, 7, 30), "Taisho");
    /**
     * The singleton instance for the 'Showa' era (1926-12-25 - 1989-01-07)
     * which has the value 1.
     */
    public static final JapaneseEra SHOWA = new JapaneseEra(1, LocalDate.of(1926, 12, 25), "Showa");
    /**
     * The singleton instance for the 'Heisei' era (1989-01-08 - current)
     * which has the value 2.
     */
    public static final JapaneseEra HEISEI = new JapaneseEra(2, LocalDate.of(1989, 1, 8), "Heisei");
    /**
     * The value of the additional era.
     */
    private static final int ADDITIONAL_VALUE = 3;

    /**
     * Serialization version.
     */
    private static final long serialVersionUID = 1466499369062886794L;

    // array for the singleton JapaneseEra instances
    private static final AtomicReference<JapaneseEra[]> KNOWN_ERAS;

    static {
        JapaneseEra[] array = new JapaneseEra[4];
        array[0] = MEIJI;
        array[1] = TAISHO;
        array[2] = SHOWA;
        array[3] = HEISEI;
        KNOWN_ERAS = new AtomicReference<JapaneseEra[]>(array);
    }

    /**
     * The era value.
     * @serial
     */
    private final int eraValue;

    // the first day of the era
    private final transient LocalDate since;
    // the name of the era
    private final transient String name;

    /**
     * Creates an instance.
     *
     * @param eraValue  the era value, validated
     * @param since  the date representing the first date of the era, validated not null
     * @param name  the name
     */
    private JapaneseEra(int eraValue, LocalDate since, String name) {
        this.eraValue = eraValue;
        this.since = since;
        this.name = name;
    }

    /**
     * Returns the singleton {@code JapaneseEra} corresponding to this object.
     * It's possible that this version of {@code JapaneseEra} doesn't support the latest era value.
     * In that case, this method throws an {@code ObjectStreamException}.
     *
     * @return the singleton {@code JapaneseEra} for this object
     * @throws ObjectStreamException if the deserialized object has any unknown numeric era value.
     */
    private Object readResolve() throws ObjectStreamException {
        try {
            return of(eraValue);
        } catch (DateTimeException e) {
            InvalidObjectException ex = new InvalidObjectException("Invalid era");
            ex.initCause(e);
            throw ex;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Registers an additional instance of {@code JapaneseEra}.
     * <p>
     * A new Japanese era can begin at any time.
     * This method allows one new era to be registered without the need for a new library version.
     * If needed, callers should assign the result to a static variable accessible
     * across the application. This must be done once, in early startup code.
     * <p>
     * NOTE: This method does not exist in Java SE 8.
     *
     * @param since  the date representing the first date of the era, validated not null
     * @param name  the name
     * @return the {@code JapaneseEra} singleton, not null
     * @throws DateTimeException if an additional era has already been registered
     */
    public static JapaneseEra registerEra(LocalDate since, String name) {
        JapaneseEra[] known = KNOWN_ERAS.get();
        if (known.length > 4) {
            throw new DateTimeException("Only one additional Japanese era can be added");
        }
        Jdk8Methods.requireNonNull(since, "since");
        Jdk8Methods.requireNonNull(name, "name");
        if (!since.isAfter(HEISEI.since)) {
            throw new DateTimeException("Invalid since date for additional Japanese era, must be after Heisei");
        }
        JapaneseEra era = new JapaneseEra(ADDITIONAL_VALUE, since, name);
        JapaneseEra[] newArray = Arrays.copyOf(known, 5);
        newArray[4] = era;
        if (!KNOWN_ERAS.compareAndSet(known, newArray)) {
            throw new DateTimeException("Only one additional Japanese era can be added");
        }
        return era;
    }

    /**
     * Obtains an instance of {@code JapaneseEra} from an {@code int} value.
     * <p>
     * The {@link #SHOWA} era that contains 1970-01-01 (ISO calendar system) has the value 1
     * Later era is numbered 2 ({@link #HEISEI}). Earlier eras are numbered 0 ({@link #TAISHO}),
     * -1 ({@link #MEIJI}), only Meiji and later eras are supported.
     *
     * @param japaneseEra  the era to represent
     * @return the {@code JapaneseEra} singleton, not null
     * @throws DateTimeException if the value is invalid
     */
    public static JapaneseEra of(int japaneseEra) {
        JapaneseEra[] known = KNOWN_ERAS.get();
        if (japaneseEra < MEIJI.eraValue || japaneseEra > known[known.length - 1].eraValue) {
            throw new DateTimeException("japaneseEra is invalid");
        }
        return known[ordinal(japaneseEra)];
    }

    /**
     * Returns the {@code JapaneseEra} with the name.
     * <p>
     * The string must match exactly the name of the era.
     * (Extraneous whitespace characters are not permitted.)
     *
     * @param japaneseEra  the japaneseEra name; non-null
     * @return the {@code JapaneseEra} singleton, never null
     * @throws IllegalArgumentException if there is not JapaneseEra with the specified name
     */
    public static JapaneseEra valueOf(String japaneseEra) {
        Jdk8Methods.requireNonNull(japaneseEra, "japaneseEra");
        JapaneseEra[] known = KNOWN_ERAS.get();
        for (JapaneseEra era : known) {
            if (japaneseEra.equals(era.name)) {
                return era;
            }
        }
        throw new IllegalArgumentException("Era not found: " + japaneseEra);
    }

    /**
     * Returns an array of JapaneseEras.
     * <p>
     * This method may be used to iterate over the JapaneseEras as follows:
     * <pre>
     * for (JapaneseEra c : JapaneseEra.values())
     *     System.out.println(c);
     * </pre>
     *
     * @return an array of JapaneseEras
     */
    public static JapaneseEra[] values() {
        JapaneseEra[] known = KNOWN_ERAS.get();
        return Arrays.copyOf(known, known.length);
    }

    //-----------------------------------------------------------------------
    /**
     * Obtains an instance of {@code JapaneseEra} from a date.
     *
     * @param date  the date, not null
     * @return the Era singleton, never null
     */
    static JapaneseEra from(LocalDate date) {
        if (date.isBefore(MEIJI.since)) {
            throw new DateTimeException("Date too early: " + date);
        }
        JapaneseEra[] known = KNOWN_ERAS.get();
        for (int i = known.length - 1; i >= 0; i--) {
            JapaneseEra era = known[i];
            if (date.compareTo(era.since) >= 0) {
                return era;
            }
        }
        return null;
    }

    /**
     * Returns the index into the arrays from the Era value.
     * the eraValue is a valid Era number, -999, -1..2.
     * @param eraValue the era value to convert to the index
     * @return the index of the current Era
     */
    private static int ordinal(int eraValue) {
        return eraValue + 1;
    }

    /**
     * Returns the start date of the era.
     * @return the start date
     */
    LocalDate startDate() {
        return since;
    }

    /**
     * Returns the end date of the era.
     * @return the end date
     */
    LocalDate endDate() {
        int ordinal = ordinal(eraValue);
        JapaneseEra[] eras = values();
        if (ordinal >= eras.length - 1) {
            return LocalDate.MAX;
        }
        return eras[ordinal + 1].startDate().minusDays(1);
    }

    //-----------------------------------------------------------------------
    /**
     * Returns the numeric value of this {@code JapaneseEra}.
     * <p>
     * The {@link #SHOWA} era that contains 1970-01-01 (ISO calendar system) has the value 1.
     * Later eras are numbered from 2 ({@link #HEISEI}).
     * Earlier eras are numbered 0 ({@link #TAISHO}) and -1 ({@link #MEIJI}).
     *
     * @return the era value
     */
    @Override
    public int getValue() {
        return eraValue;
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field == ChronoField.ERA) {
            return JapaneseChronology.INSTANCE.range(ChronoField.ERA);
        }
        return super.range(field);
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
        return name;
    }

    //-----------------------------------------------------------------------
    private Object writeReplace() {
        return new Ser(Ser.JAPANESE_ERA_TYPE, this);
    }

    void writeExternal(DataOutput out) throws IOException {
        out.writeByte(this.getValue());
    }

    static JapaneseEra readExternal(DataInput in) throws IOException {
        byte eraValue = in.readByte();
        return JapaneseEra.of(eraValue);
    }

}
