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
package org.threeten.bp.zone;

import static org.threeten.bp.temporal.ChronoField.HOUR_OF_DAY;
import static org.threeten.bp.temporal.ChronoField.MINUTE_OF_HOUR;
import static org.threeten.bp.temporal.ChronoField.SECOND_OF_MINUTE;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.threeten.bp.DayOfWeek;
import org.threeten.bp.LocalDate;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.LocalTime;
import org.threeten.bp.Month;
import org.threeten.bp.Year;
import org.threeten.bp.ZoneOffset;
import org.threeten.bp.format.DateTimeFormatter;
import org.threeten.bp.format.DateTimeFormatterBuilder;
import org.threeten.bp.jdk8.Jdk8Methods;
import org.threeten.bp.temporal.TemporalAccessor;
import org.threeten.bp.temporal.TemporalAdjusters;
import org.threeten.bp.zone.ZoneOffsetTransitionRule.TimeDefinition;

/**
 * A builder that can read the TZDB time-zone files and build {@code ZoneRules} instances.
 *
 * <h3>Specification for implementors</h3>
 * This class is a mutable builder. A new instance must be created for each compile.
 */
final class TzdbZoneRulesCompiler {

    /**
     * Time parser.
     */
    private static final DateTimeFormatter TIME_PARSER;
    static {
        TIME_PARSER = new DateTimeFormatterBuilder()
            .appendValue(HOUR_OF_DAY)
            .optionalStart().appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2)
            .optionalStart().appendLiteral(':').appendValue(SECOND_OF_MINUTE, 2)
            .toFormatter();
    }

    /**
     * Reads a set of TZDB files and builds a single combined data file.
     *
     * @param args  the arguments
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            outputHelp();
            return;
        }

        // parse args
        String version = null;
        File baseSrcDir = null;
        File dstDir = null;
        boolean unpacked = false;
        boolean verbose = false;

        // parse options
        int i;
        for (i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-") == false) {
                break;
            }
            if ("-srcdir".equals(arg)) {
                if (baseSrcDir == null && ++i < args.length) {
                    baseSrcDir = new File(args[i]);
                    continue;
                }
            } else if ("-dstdir".equals(arg)) {
                if (dstDir == null && ++i < args.length) {
                    dstDir = new File(args[i]);
                    continue;
                }
            } else if ("-version".equals(arg)) {
                if (version == null && ++i < args.length) {
                    version = args[i];
                    continue;
                }
            } else if ("-unpacked".equals(arg)) {
                if (unpacked == false) {
                    unpacked = true;
                    continue;
                }
            } else if ("-verbose".equals(arg)) {
                if (verbose == false) {
                    verbose = true;
                    continue;
                }
            } else if ("-help".equals(arg) == false) {
                System.out.println("Unrecognised option: " + arg);
            }
            outputHelp();
            return;
        }

        // check source directory
        if (baseSrcDir == null) {
            System.out.println("Source directory must be specified using -srcdir: " + baseSrcDir);
            return;
        }
        if (baseSrcDir.isDirectory() == false) {
            System.out.println("Source does not exist or is not a directory: " + baseSrcDir);
            return;
        }
        dstDir = (dstDir != null ? dstDir : baseSrcDir);

        // parse source file names
        List<String> srcFileNames = Arrays.asList(Arrays.copyOfRange(args, i, args.length));
        if (srcFileNames.isEmpty()) {
            System.out.println("Source filenames not specified, using default set");
            System.out.println("(africa antarctica asia australasia backward etcetera europe northamerica southamerica)");
            srcFileNames = Arrays.asList("africa", "antarctica", "asia", "australasia", "backward",
                    "etcetera", "europe", "northamerica", "southamerica");
        }

        // find source directories to process
        List<File> srcDirs = new ArrayList<File>();
        if (version != null) {
            File srcDir = new File(baseSrcDir, version);
            if (srcDir.isDirectory() == false) {
                System.out.println("Version does not represent a valid source directory : " + srcDir);
                return;
            }
            srcDirs.add(srcDir);
        } else {
            File[] dirs = baseSrcDir.listFiles();
            for (File dir : dirs) {
                if (dir.isDirectory() && dir.getName().matches("[12][0-9][0-9][0-9][A-Za-z0-9._-]+")) {
                    srcDirs.add(dir);
                }
            }
        }
        if (srcDirs.isEmpty()) {
            System.out.println("Source directory contains no valid source folders: " + baseSrcDir);
            return;
        }

        // check destination directory
        if (dstDir.exists() == false && dstDir.mkdirs() == false) {
            System.out.println("Destination directory could not be created: " + dstDir);
            return;
        }
        if (dstDir.isDirectory() == false) {
            System.out.println("Destination is not a directory: " + dstDir);
            return;
        }
        process(srcDirs, srcFileNames, dstDir, unpacked, verbose);
    }

    /**
     * Output usage text for the command line.
     */
    private static void outputHelp() {
        System.out.println("Usage: TzdbZoneRulesCompiler <options> <tzdb source filenames>");
        System.out.println("where options include:");
        System.out.println("   -srcdir <directory>   Where to find source directories (required)");
        System.out.println("   -dstdir <directory>   Where to output generated files (default srcdir)");
        System.out.println("   -version <version>    Specify the version, such as 2009a (optional)");
        System.out.println("   -unpacked             Generate dat files without jar files");
        System.out.println("   -help                 Print this usage message");
        System.out.println("   -verbose              Output verbose information during compilation");
        System.out.println(" There must be one directory for each version in srcdir");
        System.out.println(" Each directory must have the name of the version, such as 2009a");
        System.out.println(" Each directory must contain the unpacked tzdb files, such as asia or europe");
        System.out.println(" Directories must match the regex [12][0-9][0-9][0-9][A-Za-z0-9._-]+");
        System.out.println(" There will be one jar file for each version and one combined jar in dstdir");
        System.out.println(" If the version is specified, only that version is processed");
    }

    /**
     * Process to create the jar files.
     */
    private static void process(List<File> srcDirs, List<String> srcFileNames, File dstDir, boolean unpacked, boolean verbose) {
        // build actual jar files
        Map<Object, Object> deduplicateMap = new HashMap<Object, Object>();
        Map<String, SortedMap<String, ZoneRules>> allBuiltZones = new TreeMap<String, SortedMap<String, ZoneRules>>();
        Set<String> allRegionIds = new TreeSet<String>();
        Set<ZoneRules> allRules = new HashSet<ZoneRules>();
        SortedMap<LocalDate, Byte> bestLeapSeconds = null;

        for (File srcDir : srcDirs) {
            // source files in this directory
            List<File> srcFiles = new ArrayList<File>();
            for (String srcFileName : srcFileNames) {
                File file = new File(srcDir, srcFileName);
                if (file.exists()) {
                    srcFiles.add(file);
                }
            }
            if (srcFiles.isEmpty()) {
                continue;  // nothing to process
            }
            File leapSecondsFile = new File(srcDir, "leapseconds");
            if (!leapSecondsFile.exists()) {
                System.out.println("Version " + srcDir.getName() + " does not include leap seconds information.");
                leapSecondsFile = null;
            }

            // compile
            String loopVersion = srcDir.getName();
            TzdbZoneRulesCompiler compiler = new TzdbZoneRulesCompiler(loopVersion, srcFiles, leapSecondsFile, verbose);
            compiler.setDeduplicateMap(deduplicateMap);
            try {
                // compile
                compiler.compile();
                SortedMap<String, ZoneRules> builtZones = compiler.getZones();
                SortedMap<LocalDate, Byte> parsedLeapSeconds = compiler.getLeapSeconds();

                // output version-specific file
                if (unpacked == false) {
                    File dstFile = new File(dstDir, "threeten-TZDB-" + loopVersion + ".jar");
                    if (verbose) {
                        System.out.println("Outputting file: " + dstFile);
                    }
                    outputFile(dstFile, loopVersion, builtZones, parsedLeapSeconds);
                }

                // create totals
                allBuiltZones.put(loopVersion, builtZones);
                allRegionIds.addAll(builtZones.keySet());
                allRules.addAll(builtZones.values());

                // track best possible leap seconds collection
                if (compiler.getMostRecentLeapSecond() != null) {
                    // we've got a live one!
                    if (bestLeapSeconds == null || compiler.getMostRecentLeapSecond().compareTo(bestLeapSeconds.lastKey()) > 0) {
                        // found the first one, or found a better one
                        bestLeapSeconds = parsedLeapSeconds;
                    }
                }
            } catch (Exception ex) {
                System.out.println("Failed: " + ex.toString());
                ex.printStackTrace();
                System.exit(1);
            }
        }

        // output merged file
        if (unpacked) {
            if (verbose) {
                System.out.println("Outputting combined files: " + dstDir);
            }
            outputFilesDat(dstDir, allBuiltZones, allRegionIds, allRules, bestLeapSeconds);
        } else {
            File dstFile = new File(dstDir, "threeten-TZDB-all.jar");
            if (verbose) {
                System.out.println("Outputting combined file: " + dstFile);
            }
            outputFile(dstFile, allBuiltZones, allRegionIds, allRules, bestLeapSeconds);
        }
    }

    /**
     * Outputs the DAT files.
     */
    private static void outputFilesDat(File dstDir, Map<String, SortedMap<String, ZoneRules>> allBuiltZones,
            Set<String> allRegionIds, Set<ZoneRules> allRules, SortedMap<LocalDate, Byte> leapSeconds) {
        File tzdbFile = new File(dstDir, "TZDB.dat");
        tzdbFile.delete();
        try {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(tzdbFile);
                outputTzdbDat(fos, allBuiltZones, allRegionIds, allRules);
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        } catch (Exception ex) {
            System.out.println("Failed: " + ex.toString());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Outputs the file.
     */
    private static void outputFile(File dstFile, String version, SortedMap<String, ZoneRules> builtZones, SortedMap<LocalDate, Byte> leapSeconds) {
        Map<String, SortedMap<String, ZoneRules>> loopAllBuiltZones = new TreeMap<String, SortedMap<String, ZoneRules>>();
        loopAllBuiltZones.put(version, builtZones);
        Set<String> loopAllRegionIds = new TreeSet<String>(builtZones.keySet());
        Set<ZoneRules> loopAllRules = new HashSet<ZoneRules>(builtZones.values());
        outputFile(dstFile, loopAllBuiltZones, loopAllRegionIds, loopAllRules, leapSeconds);
    }

    /**
     * Outputs the file.
     */
    private static void outputFile(File dstFile, Map<String, SortedMap<String, ZoneRules>> allBuiltZones,
            Set<String> allRegionIds, Set<ZoneRules> allRules, SortedMap<LocalDate, Byte> leapSeconds) {
        JarOutputStream jos = null;
        try {
            jos = new JarOutputStream(new FileOutputStream(dstFile));
            outputTzdbEntry(jos, allBuiltZones, allRegionIds, allRules);
        } catch (Exception ex) {
            System.out.println("Failed: " + ex.toString());
            ex.printStackTrace();
            System.exit(1);
        } finally {
            if (jos != null) {
                try {
                    jos.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    /**
     * Outputs the timezone entry in the JAR file.
     */
    private static void outputTzdbEntry(
            JarOutputStream jos, Map<String, SortedMap<String, ZoneRules>> allBuiltZones,
            Set<String> allRegionIds, Set<ZoneRules> allRules) {
        // this format is not publicly specified
        try {
            jos.putNextEntry(new ZipEntry("org/threeten/bp/TZDB.dat"));
            outputTzdbDat(jos, allBuiltZones, allRegionIds, allRules);
            jos.closeEntry();
        } catch (Exception ex) {
            System.out.println("Failed: " + ex.toString());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Outputs the timezone DAT file.
     */
    private static void outputTzdbDat(OutputStream jos,
            Map<String, SortedMap<String, ZoneRules>> allBuiltZones,
            Set<String> allRegionIds, Set<ZoneRules> allRules) throws IOException {
        DataOutputStream out = new DataOutputStream(jos);

        // file version
        out.writeByte(1);
        // group
        out.writeUTF("TZDB");
        // versions
        String[] versionArray = allBuiltZones.keySet().toArray(new String[allBuiltZones.size()]);
        out.writeShort(versionArray.length);
        for (String version : versionArray) {
            out.writeUTF(version);
        }
        // regions
        String[] regionArray = allRegionIds.toArray(new String[allRegionIds.size()]);
        out.writeShort(regionArray.length);
        for (String regionId : regionArray) {
            out.writeUTF(regionId);
        }
        // rules
        List<ZoneRules> rulesList = new ArrayList<ZoneRules>(allRules);
        out.writeShort(rulesList.size());
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        for (ZoneRules rules : rulesList) {
            baos.reset();
            DataOutputStream dataos = new DataOutputStream(baos);
            Ser.write(rules, dataos);
            dataos.close();
            byte[] bytes = baos.toByteArray();
            out.writeShort(bytes.length);
            out.write(bytes);
        }
        // link version-region-rules
        for (String version : allBuiltZones.keySet()) {
            out.writeShort(allBuiltZones.get(version).size());
            for (Map.Entry<String, ZoneRules> entry : allBuiltZones.get(version).entrySet()) {
                 int regionIndex = Arrays.binarySearch(regionArray, entry.getKey());
                 int rulesIndex = rulesList.indexOf(entry.getValue());
                 out.writeShort(regionIndex);
                 out.writeShort(rulesIndex);
            }
        }
        out.flush();
    }

    //-----------------------------------------------------------------------
    /** The TZDB rules. */
    private final Map<String, List<TZDBRule>> rules = new HashMap<String, List<TZDBRule>>();
    /** The TZDB zones. */
    private final Map<String, List<TZDBZone>> zones = new HashMap<String, List<TZDBZone>>();
    /** The TZDB links. */
    private final Map<String, String> links = new HashMap<String, String>();
    /** The built zones. */
    private final SortedMap<String, ZoneRules> builtZones = new TreeMap<String, ZoneRules>();
    /** A map to deduplicate object instances. */
    private Map<Object, Object> deduplicateMap = new HashMap<Object, Object>();
    /** Sorted collection of LeapSecondRules. */
    private final SortedMap<LocalDate, Byte> leapSeconds = new TreeMap<LocalDate, Byte>();

    /** The version to produce. */
    private final String version;
    /** The source files. */
    private final List<File> sourceFiles;
    /** The leap seconds file. */
    private final File leapSecondsFile;
    /** The version to produce. */
    private final boolean verbose;

    /**
     * Creates an instance if you want to invoke the compiler manually.
     *
     * @param version  the version, such as 2009a, not null
     * @param sourceFiles  the list of source files, not empty, not null
     * @param verbose  whether to output verbose messages
     */
    public TzdbZoneRulesCompiler(String version, List<File> sourceFiles, File leapSecondsFile, boolean verbose) {
        this.version = version;
        this.sourceFiles = sourceFiles;
        this.leapSecondsFile = leapSecondsFile;
        this.verbose = verbose;
    }

    /**
     * Compile the rules file.
     * <p>
     * Use {@link #getZones()} and {@link #getLeapSeconds()} to retrieve the parsed data.
     *
     * @throws Exception if an error occurs
     */
    public void compile() throws Exception {
        printVerbose("Compiling TZDB version " + version);
        parseFiles();
        parseLeapSecondsFile();
        buildZoneRules();
        printVerbose("Compiled TZDB version " + version);
    }

    /**
     * Gets the parsed zone rules.
     *
     * @return the parsed zone rules, not null
     */
    public SortedMap<String, ZoneRules> getZones() {
        return builtZones;
    }

    /**
     * Gets the parsed leap seconds.
     *
     * @return the parsed and sorted leap seconds, not null
     */
    public SortedMap<LocalDate, Byte> getLeapSeconds() {
        return leapSeconds;
    }

    /**
     * Gets the most recent leap second.
     *
     * @return the most recent leap second, null if none
     */
    private LocalDate getMostRecentLeapSecond() {
        return leapSeconds.isEmpty() ? null : leapSeconds.lastKey();
    }

    /**
     * Sets the deduplication map.
     *
     * @param deduplicateMap  the map to deduplicate items
     */
    void setDeduplicateMap(Map<Object, Object> deduplicateMap) {
        this.deduplicateMap = deduplicateMap;
    }

    //-----------------------------------------------------------------------
    /**
     * Parses the source files.
     *
     * @throws Exception if an error occurs
     */
    private void parseFiles() throws Exception {
        for (File file : sourceFiles) {
            printVerbose("Parsing file: " + file);
            parseFile(file);
        }
    }

    /**
     * Parses the leap seconds file.
     *
     * @throws Exception if an error occurs
     */
    private void parseLeapSecondsFile() throws Exception {
        printVerbose("Parsing leap second file: " + leapSecondsFile);
        int lineNumber = 1;
        String line = null;
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(leapSecondsFile));
            for ( ; (line = in.readLine()) != null; lineNumber++) {
                int index = line.indexOf('#');  // remove comments (doesn't handle # in quotes)
                if (index >= 0) {
                    line = line.substring(0, index);
                }
                if (line.trim().length() == 0) {  // ignore blank lines
                    continue;
                }
                LeapSecondRule secondRule = parseLeapSecondRule(line);
                leapSeconds.put(secondRule.leapDate, secondRule.secondAdjustment);
            }
        } catch (Exception ex) {
            throw new Exception("Failed while processing file '" + leapSecondsFile + "' on line " + lineNumber + " '" + line + "'", ex);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception ex) {
                // ignore NPE and IOE
            }
        }
    }

    private LeapSecondRule parseLeapSecondRule(String line) {
        //    # Leap    YEAR    MONTH    DAY    HH:MM:SS    CORR    R/S
        //    Leap    1972    Jun    30    23:59:60    +    S
        //    Leap    1972    Dec    31    23:59:60    +    S
        //    Leap    1973    Dec    31    23:59:60    +    S
        //    Leap    1974    Dec    31    23:59:60    +    S
        //    Leap    1975    Dec    31    23:59:60    +    S
        //    Leap    1976    Dec    31    23:59:60    +    S
        //    Leap    1977    Dec    31    23:59:60    +    S
        //    Leap    1978    Dec    31    23:59:60    +    S
        //    Leap    1979    Dec    31    23:59:60    +    S
        //    Leap    1981    Jun    30    23:59:60    +    S
        //    Leap    1982    Jun    30    23:59:60    +    S
        //    Leap    1983    Jun    30    23:59:60    +    S

        StringTokenizer st = new StringTokenizer(line, " \t");
        String first = st.nextToken();
        if (first.equals("Leap")) {
            if (st.countTokens() < 6) {
                printVerbose("Invalid leap second line in file: " + leapSecondsFile + ", line: " + line);
                throw new IllegalArgumentException("Invalid leap second line");
            }
        } else {
            throw new IllegalArgumentException("Unknown line");
        }

        int year = Integer.parseInt(st.nextToken());
        Month month = parseMonth(st.nextToken());
        int dayOfMonth = Integer.parseInt(st.nextToken());
        LocalDate leapDate = LocalDate.of(year, month, dayOfMonth);
        String timeOfLeapSecond = st.nextToken();

        byte adjustmentByte = 0;
        String adjustment = st.nextToken();
        if (adjustment.equals("+")) {
            if (!("23:59:60".equals(timeOfLeapSecond))) {
                throw new IllegalArgumentException("Leap seconds can only be inserted at 23:59:60 - Date:" + leapDate);
            }
            adjustmentByte = +1;
        } else if (adjustment.equals("-")) {
            if (!("23:59:59".equals(timeOfLeapSecond))) {
                throw new IllegalArgumentException("Leap seconds can only be removed at 23:59:59 - Date:" + leapDate);
            }
            adjustmentByte = -1;
        } else {
            throw new IllegalArgumentException("Invalid adjustment '" + adjustment + "' in leap second rule for " + leapDate);
        }

        String rollingOrStationary = st.nextToken();
        if (!"S".equalsIgnoreCase(rollingOrStationary)) {
            throw new IllegalArgumentException("Only stationary ('S') leap seconds are supported, not '" + rollingOrStationary + "'");
        }
        return new LeapSecondRule(leapDate, adjustmentByte);
    }

    /**
     * Parses a source file.
     *
     * @param file  the file being read, not null
     * @throws Exception if an error occurs
     */
    private void parseFile(File file) throws Exception {
        int lineNumber = 1;
        String line = null;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            List<TZDBZone> openZone = null;
            for ( ; (line = in.readLine()) != null; lineNumber++) {
                int index = line.indexOf('#');  // remove comments (doesn't handle # in quotes)
                if (index >= 0) {
                    line = line.substring(0, index);
                }
                if (line.trim().length() == 0) {  // ignore blank lines
                    continue;
                }
                StringTokenizer st = new StringTokenizer(line, " \t");
                if (openZone != null && Character.isWhitespace(line.charAt(0)) && st.hasMoreTokens()) {
                    if (parseZoneLine(st, openZone)) {
                        openZone = null;
                    }
                } else {
                    if (st.hasMoreTokens()) {
                        String first = st.nextToken();
                        if (first.equals("Zone")) {
                            if (st.countTokens() < 3) {
                                printVerbose("Invalid Zone line in file: " + file + ", line: " + line);
                                throw new IllegalArgumentException("Invalid Zone line");
                            }
                            openZone = new ArrayList<TZDBZone>();
                            zones.put(st.nextToken(), openZone);
                            if (parseZoneLine(st, openZone)) {
                                openZone = null;
                            }
                        } else {
                            openZone = null;
                            if (first.equals("Rule")) {
                                if (st.countTokens() < 9) {
                                    printVerbose("Invalid Rule line in file: " + file + ", line: " + line);
                                    throw new IllegalArgumentException("Invalid Rule line");
                                }
                                parseRuleLine(st);

                            } else if (first.equals("Link")) {
                                if (st.countTokens() < 2) {
                                    printVerbose("Invalid Link line in file: " + file + ", line: " + line);
                                    throw new IllegalArgumentException("Invalid Link line");
                                }
                                String realId = st.nextToken();
                                String aliasId = st.nextToken();
                                links.put(aliasId, realId);

                            } else {
                                throw new IllegalArgumentException("Unknown line");
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new Exception("Failed while processing file '" + file + "' on line " + lineNumber + " '" + line + "'", ex);
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Parses a Rule line.
     *
     * @param st  the tokenizer, not null
     */
    private void parseRuleLine(StringTokenizer st) {
        TZDBRule rule = new TZDBRule();
        String name = st.nextToken();
        if (rules.containsKey(name) == false) {
            rules.put(name, new ArrayList<TZDBRule>());
        }
        rules.get(name).add(rule);
        rule.startYear = parseYear(st.nextToken(), 0);
        rule.endYear = parseYear(st.nextToken(), rule.startYear);
        if (rule.startYear > rule.endYear) {
            throw new IllegalArgumentException("Year order invalid: " + rule.startYear + " > " + rule.endYear);
        }
        parseOptional(st.nextToken());  // type is unused
        parseMonthDayTime(st, rule);
        rule.savingsAmount = parsePeriod(st.nextToken());
        rule.text = parseOptional(st.nextToken());
    }

    /**
     * Parses a Zone line.
     *
     * @param st  the tokenizer, not null
     * @return true if the zone is complete
     */
    private boolean parseZoneLine(StringTokenizer st, List<TZDBZone> zoneList) {
        TZDBZone zone = new TZDBZone();
        zoneList.add(zone);
        zone.standardOffset = parseOffset(st.nextToken());
        String savingsRule = parseOptional(st.nextToken());
        if (savingsRule == null) {
            zone.fixedSavingsSecs = 0;
            zone.savingsRule = null;
        } else {
            try {
                zone.fixedSavingsSecs = parsePeriod(savingsRule);
                zone.savingsRule = null;
            } catch (Exception ex) {
                zone.fixedSavingsSecs = null;
                zone.savingsRule = savingsRule;
            }
        }
        zone.text = st.nextToken();
        if (st.hasMoreTokens()) {
            zone.year = Year.of(Integer.parseInt(st.nextToken()));
            if (st.hasMoreTokens()) {
                parseMonthDayTime(st, zone);
            }
            return false;
        } else {
            return true;
        }
    }

    /**
     * Parses a Rule line.
     *
     * @param st  the tokenizer, not null
     * @param mdt  the object to parse into, not null
     */
    private void parseMonthDayTime(StringTokenizer st, TZDBMonthDayTime mdt) {
        mdt.month = parseMonth(st.nextToken());
        if (st.hasMoreTokens()) {
            String dayRule = st.nextToken();
            if (dayRule.startsWith("last")) {
                mdt.dayOfMonth = -1;
                mdt.dayOfWeek = parseDayOfWeek(dayRule.substring(4));
                mdt.adjustForwards = false;
            } else {
                int index = dayRule.indexOf(">=");
                if (index > 0) {
                    mdt.dayOfWeek = parseDayOfWeek(dayRule.substring(0, index));
                    dayRule = dayRule.substring(index + 2);
                } else {
                    index = dayRule.indexOf("<=");
                    if (index > 0) {
                        mdt.dayOfWeek = parseDayOfWeek(dayRule.substring(0, index));
                        mdt.adjustForwards = false;
                        dayRule = dayRule.substring(index + 2);
                    }
                }
                mdt.dayOfMonth = Integer.parseInt(dayRule);
            }
            if (st.hasMoreTokens()) {
                String timeStr = st.nextToken();
                int timeOfDaySecs = parseSecs(timeStr);
                LocalTime time = deduplicate(LocalTime.ofSecondOfDay(Jdk8Methods.floorMod(timeOfDaySecs, 86400)));
                mdt.time = time;
                mdt.adjustDays = Jdk8Methods.floorDiv(timeOfDaySecs, 86400);
                mdt.timeDefinition = parseTimeDefinition(timeStr.charAt(timeStr.length() - 1));
            }
        }
    }

    private int parseYear(String str, int defaultYear) {
        str = str.toLowerCase();
        if (matches(str, "minimum")) {
            return Year.MIN_VALUE;
        } else if (matches(str, "maximum")) {
            return Year.MAX_VALUE;
        } else if (str.equals("only")) {
            return defaultYear;
        }
        return Integer.parseInt(str);
    }

    private Month parseMonth(String str) {
        str = str.toLowerCase();
        for (Month moy : Month.values()) {
            if (matches(str, moy.name().toLowerCase())) {
                return moy;
            }
        }
        throw new IllegalArgumentException("Unknown month: " + str);
    }

    private DayOfWeek parseDayOfWeek(String str) {
        str = str.toLowerCase();
        for (DayOfWeek dow : DayOfWeek.values()) {
            if (matches(str, dow.name().toLowerCase())) {
                return dow;
            }
        }
        throw new IllegalArgumentException("Unknown day-of-week: " + str);
    }

    private boolean matches(String str, String search) {
        return str.startsWith(search.substring(0, 3)) && search.startsWith(str) && str.length() <= search.length();
    }

    private String parseOptional(String str) {
        return str.equals("-") ? null : str;
    }

    private int parseSecs(String str) {
        if (str.equals("-")) {
            return 0;
        }
        int pos = 0;
        if (str.startsWith("-")) {
            pos = 1;
        }
        ParsePosition pp = new ParsePosition(pos);
        TemporalAccessor parsed = TIME_PARSER.parseUnresolved(str, pp);
        if (parsed == null || pp.getErrorIndex() >= 0) {
            throw new IllegalArgumentException(str);
        }
        long hour = parsed.getLong(HOUR_OF_DAY);
        Long min = (parsed.isSupported(MINUTE_OF_HOUR) ? parsed.getLong(MINUTE_OF_HOUR) : null);
        Long sec = (parsed.isSupported(SECOND_OF_MINUTE) ? parsed.getLong(SECOND_OF_MINUTE) : null);
        int secs = (int) (hour * 60 * 60 + (min != null ? min : 0) * 60 + (sec != null ? sec : 0));
        if (pos == 1) {
            secs = -secs;
        }
        return secs;
    }

    private ZoneOffset parseOffset(String str) {
        int secs = parseSecs(str);
        return ZoneOffset.ofTotalSeconds(secs);
    }

    private int parsePeriod(String str) {
        return parseSecs(str);
    }

    private TimeDefinition parseTimeDefinition(char c) {
        switch (c) {
            case 's':
            case 'S':
                // standard time
                return TimeDefinition.STANDARD;
            case 'u':
            case 'U':
            case 'g':
            case 'G':
            case 'z':
            case 'Z':
                // UTC
                return TimeDefinition.UTC;
            case 'w':
            case 'W':
            default:
                // wall time
                return TimeDefinition.WALL;
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Build the rules, zones and links into real zones.
     *
     * @throws Exception if an error occurs
     */
    private void buildZoneRules() throws Exception {
        // build zones
        for (String zoneId : zones.keySet()) {
            printVerbose("Building zone " + zoneId);
            zoneId = deduplicate(zoneId);
            List<TZDBZone> tzdbZones = zones.get(zoneId);
            ZoneRulesBuilder bld = new ZoneRulesBuilder();
            for (TZDBZone tzdbZone : tzdbZones) {
                bld = tzdbZone.addToBuilder(bld, rules);
            }
            ZoneRules buildRules = bld.toRules(zoneId, deduplicateMap);
            builtZones.put(zoneId, deduplicate(buildRules));
        }

        // build aliases
        for (String aliasId : links.keySet()) {
            aliasId = deduplicate(aliasId);
            String realId = links.get(aliasId);
            printVerbose("Linking alias " + aliasId + " to " + realId);
            ZoneRules realRules = builtZones.get(realId);
            if (realRules == null) {
                realId = links.get(realId);  // try again (handle alias liked to alias)
                printVerbose("Relinking alias " + aliasId + " to " + realId);
                realRules = builtZones.get(realId);
                if (realRules == null) {
                    throw new IllegalArgumentException("Alias '" + aliasId + "' links to invalid zone '" + realId + "' for '" + version + "'");
                }
            }
            builtZones.put(aliasId, realRules);
        }

        // remove UTC and GMT
        builtZones.remove("UTC");
        builtZones.remove("GMT");
        builtZones.remove("GMT0");
        builtZones.remove("GMT+0");
        builtZones.remove("GMT-0");
    }

    //-----------------------------------------------------------------------
    /**
     * Deduplicates an object instance.
     *
     * @param <T> the generic type
     * @param object  the object to deduplicate
     * @return the deduplicated object
     */
    @SuppressWarnings("unchecked")
    <T> T deduplicate(T object) {
        if (deduplicateMap.containsKey(object) == false) {
            deduplicateMap.put(object, object);
        }
        return (T) deduplicateMap.get(object);
    }

    //-----------------------------------------------------------------------
    /**
     * Prints a verbose message.
     *
     * @param message  the message, not null
     */
    private void printVerbose(String message) {
        if (verbose) {
            System.out.println(message);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Class representing a month-day-time in the TZDB file.
     */
    abstract class TZDBMonthDayTime {
        /** The month of the cutover. */
        Month month = Month.JANUARY;
        /** The day-of-month of the cutover. */
        int dayOfMonth = 1;
        /** Whether to adjust forwards. */
        boolean adjustForwards = true;
        /** The day-of-week of the cutover. */
        DayOfWeek dayOfWeek;
        /** The time of the cutover. */
        LocalTime time = LocalTime.MIDNIGHT;
        /** The time days adjustment. */
        int adjustDays;
        /** The time of the cutover. */
        TimeDefinition timeDefinition = TimeDefinition.WALL;

        void adjustToFowards(int year) {
            if (adjustForwards == false && dayOfMonth > 0) {
                LocalDate adjustedDate = LocalDate.of(year, month, dayOfMonth).minusDays(6);
                dayOfMonth = adjustedDate.getDayOfMonth();
                month = adjustedDate.getMonth();
                adjustForwards = true;
            }
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Class representing a rule line in the TZDB file.
     */
    final class TZDBRule extends TZDBMonthDayTime {
        /** The start year. */
        int startYear;
        /** The end year. */
        int endYear;
        /** The amount of savings. */
        int savingsAmount;
        /** The text name of the zone. */
        String text;

        void addToBuilder(ZoneRulesBuilder bld) {
            adjustToFowards(2004);  // irrelevant, treat as leap year
            bld.addRuleToWindow(startYear, endYear, month, dayOfMonth, dayOfWeek, time, adjustDays, timeDefinition, savingsAmount);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Class representing a linked set of zone lines in the TZDB file.
     */
    final class TZDBZone extends TZDBMonthDayTime {
        /** The standard offset. */
        ZoneOffset standardOffset;
        /** The fixed savings amount. */
        Integer fixedSavingsSecs;
        /** The savings rule. */
        String savingsRule;
        /** The text name of the zone. */
        String text;
        /** The year of the cutover. */
        Year year;

        ZoneRulesBuilder addToBuilder(ZoneRulesBuilder bld, Map<String, List<TZDBRule>> rules) {
            if (year != null) {
                bld.addWindow(standardOffset, toDateTime(year.getValue()), timeDefinition);
            } else {
                bld.addWindowForever(standardOffset);
            }

            if (fixedSavingsSecs != null) {
                bld.setFixedSavingsToWindow(fixedSavingsSecs);
            } else {
                List<TZDBRule> tzdbRules = rules.get(savingsRule);
                if (tzdbRules == null) {
                    throw new IllegalArgumentException("Rule not found: " + savingsRule);
                }
                for (TZDBRule tzdbRule : tzdbRules) {
                    tzdbRule.addToBuilder(bld);
                }
            }

            return bld;
        }

        private LocalDateTime toDateTime(int year) {
            adjustToFowards(year);
            LocalDate date;
            if (dayOfMonth == -1) {
                dayOfMonth = month.length(Year.isLeap(year));
                date = LocalDate.of(year, month, dayOfMonth);
                if (dayOfWeek != null) {
                    date = date.with(TemporalAdjusters.previousOrSame(dayOfWeek));
                }
            } else {
                date = LocalDate.of(year, month, dayOfMonth);
                if (dayOfWeek != null) {
                    date = date.with(TemporalAdjusters.nextOrSame(dayOfWeek));
                }
            }
            date = deduplicate(date.plusDays(adjustDays));
            return LocalDateTime.of(date, time);
        }
    }

    //-----------------------------------------------------------------------
    /**
     * Class representing a rule line in the TZDB file.
     */
    static final class LeapSecondRule {
        /**
         * Constructs a rule using fields.
         * @param leapDate Date which has gets leap second adjustment (at the end)
         * @param secondAdjustment +1 or -1 for inserting or dropping a second
         */
        public LeapSecondRule(LocalDate leapDate, byte secondAdjustment) {
            this.leapDate = leapDate;
            this.secondAdjustment = secondAdjustment;
        }
        /** The date of the leap second. */
        final LocalDate leapDate;
        /** The adjustment (in seconds), +1 means a second is inserted,
         * -1 means a second is dropped. */
        byte secondAdjustment;
    }

}
