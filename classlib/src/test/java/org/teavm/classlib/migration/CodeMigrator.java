/*
 *  Copyright 2020 Joerg Hohwiller.
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
package org.teavm.classlib.migration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Migrates code for TeaVM classlib. Config is hardcoded and needs to be adopted to the according use-case (currently
 * "java.time").
 */
public class CodeMigrator {

    private LineMigrator lineMigrator;

    /**
     * The constructor.
     *
     * @param lineMigrator
     */
    public CodeMigrator(LineMigrator lineMigrator) {

        super();
        this.lineMigrator = lineMigrator;
    }

    public void migrate(Path path) {

        try {
            Files.list(path).forEach(child -> {
                if (Files.isDirectory(child)) {
                    migrate(child);
                } else {
                    migrateFile(child);
                }
            });
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param source the source-file to migrate.
     */
    protected void migrateFile(Path source) {

        String filename = source.getFileName().toString();
        if (!filename.endsWith(".java")) {
            return;
        }
        Path target = source.getParent().resolve("T" + filename);
        try (BufferedReader reader = Files.newBufferedReader(source);
                BufferedWriter writer = Files.newBufferedWriter(target)) {
            String line = null;
            do {
                line = reader.readLine();
                String migratedLine = this.lineMigrator.migrate(line);
                if (migratedLine != null) {
                    writer.write(migratedLine);
                    writer.newLine();
                }
            } while (line != null);
            if (isDeleteSource()) {
                Files.delete(source);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return {@code true} to delete the original source files after migration to new T* file, {@code false} otherwise.
     */
    protected boolean isDeleteSource() {

        return true;
    }

    public static void main(String[] args) {

        String[] types = new String[] { "java.lang.Objects", "java.lang.System", //
        "java.util.Locale", "java.util.Date", "java.util.TimeZone", //
        "java.util.Calendar", "java.util.GregorianCalendar", //
        "java.sql.Date", "java.sql.Timestamp", //
        "java.time.Clock", "java.time.DateTimeException", "java.time.DateTimeUtils", "java.time.DayOfWeek",
        "java.time.Duration", "java.time.Instant", "java.time.LocalDate", "java.time.LocalDateTime",
        "java.time.LocalTime", "java.time.Month", "java.time.MonthDay", "java.time.OffsetDateTime",
        "java.time.OffsetTime", "java.time.Period", "java.time.Year", "java.time.YearMonth", "java.time.ZonedDateTime",
        "java.time.ZoneId", "java.time.ZoneOffset", "java.time.ZoneRegion", "java.time.chrono.AbstractChronology",
        "java.time.chrono.ChonoDateImpl", "java.time.chrono.ChronoLocalDate", "java.time.chrono.ChronoLocalDateTime",
        "java.time.chrono.ChronoLocalDateTimeImpl", "java.time.chrono.Chronology", "java.time.chrono.ChronoPeriod",
        "java.time.chrono.ChronoPeriodImpl", "java.time.chrono.ChronoZonedDateTime", "java.time.chrono.Era",
        "java.time.chrono.HijrahChronology", "java.time.chrono.HijrahDate", "java.time.chrono.HijrahEra",
        "java.time.chrono.IsoChronology", "java.time.chrono.IsoEra", "java.time.chrono.JapaneseChronology",
        "java.time.chrono.JapaneseDate", "java.time.chrono.JapaneseEra", "java.time.chrono.MinguoChronology",
        "java.time.chrono.MinguoDate", "java.time.chrono.MinguoEra", "java.time.chrono.ThaiBuddhistChronology",
        "java.time.chrono.ThaiBuddhistDate", "java.time.chrono.ThaiBuddhistEra", "java.time.format.DateTimeBuilder",
        "java.time.format.DateTimeFormatStyleProvider", "java.time.format.DateTimeFormatter",
        "java.time.format.DateTimeFormatterBuilder", "java.time.format.DateTimeParseContext",
        "java.time.format.DateTimeParseException", "java.time.format.DateTimePrintContext",
        "java.time.format.DateTimeTextProvider", "java.time.format.DecimalStyle", "java.time.format.FormatStyle",
        "java.time.format.ResolverStyle", "java.time.format.SignStyle",
        "java.time.format.SimpleDateTimeFormatStyleProvider", "java.time.format.SimpleDateTimeTextProvider",
        "java.time.format.TextStyle", "java.time.jdk8.DefaultInterfaceEra", "java.time.jdk8.DefaultInterfaceTemporal",
        "java.time.jdk8.DefaultInterfaceTemporalAccessor", "java.time.jdk8.Jdk8Methods",
        "java.time.temporal.ChronoField", "java.time.temporal.ChronoUnit", "java.time.temporal.IsoFields",
        "java.time.temporal.JulianFields", "java.time.temporal.Temporal", "java.time.temporal.TemporalAccessor",
        "java.time.temporal.TemporalAdjuster", "java.time.temporal.TemporalAdjusters",
        "java.time.temporal.TemporalAmount", "java.time.temporal.TemporalField", "java.time.temporal.TemporalQueries",
        "java.time.temporal.TemporalQuery", "java.time.temporal.TemporalUnit",
        "java.time.temporal.UnsupportedTemporalTypeException", "java.time.temporal.ValueRange",
        "java.time.temporal.WeekFields", "java.time.zone.StandardZoneRules", "java.time.zone.TzdbZoneRulesCompiler",
        "java.time.zone.TzdbZoneRulesProvider", "java.time.zone.ZoneOffsetTransition",
        "java.time.zone.ZoneOffsetTransitionRule", "java.time.zone.ZoneRules", "java.time.zone.ZoneRulesBuilder",
        "java.time.zone.ZoneRulesException", "java.time.zone.ZoneRulesInitializer",
        "java.time.zone.ZoneRulesProvider" };
        LineMigrator lineMigrator = new JavaDocRemover().append(
                new StringReplaceMigrator("org.threeten.bp", "org.teavm.classlib.java.time"),
                new StringReplaceMigrator("org.testng.Assert", "org.junit.Assert"),
                new StringReplaceMigrator("org.testng.annotations.Test", "org.junit.Test"),
                new StringReplaceMigrator("org.testng.annotations.BeforeMethod", "org.junit.Before"),
                new StringReplaceMigrator("@BeforeMethod", "@Before"), LineMigrator.ofJavaTypes4Classlib(types));
        CodeMigrator migrator = new CodeMigrator(lineMigrator);
        migrator.migrate(Paths.get("src/main/java/org/teavm/classlib/java/time"));
        migrator.migrate(Paths.get("src/test/java/org/teavm/classlib/java/time"));
    }

}
