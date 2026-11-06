/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.tools.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.teavm.backend.wasm.parser.AddressListener;
import org.teavm.backend.wasm.parser.ImportSectionListener;
import org.teavm.backend.wasm.parser.ImportSectionParser;
import org.teavm.backend.wasm.parser.NameMapListener;
import org.teavm.backend.wasm.parser.NameSectionListener;
import org.teavm.backend.wasm.parser.NameSectionParser;
import org.teavm.backend.wasm.parser.WasmBinaryReader;

/**
 * Compares Wasm modules produced by the Wasm GC backend in two test output directories
 * (e.g. {@code tests/build/teavm-tests/wasm-gc} and {@code tests/build/teavm-tests/bootstrap-wasm-gc}).
 *
 * <p>Usage: {@code WasmTestComparator <bytes|functions> <dirA> <dirB> [outputDir]}</p>
 *
 * <ul>
 *   <li>{@code bytes} - compares whole files byte by byte; for differing files reports sizes, first
 *   differing offset and which sections differ.</li>
 *   <li>{@code functions} - compares code section only, function by function. Functions are matched
 *   by names from the {@code name} custom section (by index when there's no name). Body bytes
 *   (including locals declaration) are compared as is.</li>
 * </ul>
 *
 * <p>Writes {@code summary.txt} to the output directory and, in {@code functions} mode, a detailed
 * {@code <module>.functions.txt} report per differing module.</p>
 */
public final class WasmTestComparator {
    private static final int CODE_SECTION = 10;
    private static final int IMPORT_SECTION = 2;
    private static final String[] SECTION_NAMES = { "custom", "type", "import", "function", "table", "memory",
            "global", "export", "start", "element", "code", "data", "datacount", "tag" };

    private WasmTestComparator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 3 || args.length > 4) {
            System.err.println("Usage: WasmTestComparator <bytes|functions> <dirA> <dirB> [outputDir]");
            System.exit(1);
        }
        var mode = args[0];
        var dirA = Path.of(args[1]).toAbsolutePath().normalize();
        var dirB = Path.of(args[2]).toAbsolutePath().normalize();
        var outputDir = args.length > 3 ? Path.of(args[3]) : Path.of("wasm-compare-" + mode);
        Files.createDirectories(outputDir);

        var modulesA = collectModules(dirA);
        var modulesB = collectModules(dirB);
        var all = new TreeSet<String>();
        all.addAll(modulesA);
        all.addAll(modulesB);

        var onlyA = new ArrayList<String>();
        var onlyB = new ArrayList<String>();
        var common = new ArrayList<String>();
        for (var rel : all) {
            if (!modulesB.contains(rel)) {
                onlyA.add(rel);
            } else if (!modulesA.contains(rel)) {
                onlyB.add(rel);
            } else {
                common.add(rel);
            }
        }

        List<String> lines;
        switch (mode) {
            case "bytes":
                lines = compareBytewise(dirA, dirB, common);
                break;
            case "functions":
                lines = compareFunctions(dirA, dirB, common, outputDir);
                break;
            default:
                System.err.println("Unknown mode: " + mode + ", expected 'bytes' or 'functions'");
                System.exit(1);
                return;
        }

        var summaryFile = outputDir.resolve("summary.txt");
        try (var out = new PrintWriter(Files.newBufferedWriter(summaryFile, StandardCharsets.UTF_8))) {
            out.println("Mode: " + mode);
            out.println("A: " + dirA);
            out.println("B: " + dirB);
            out.println("Modules only in A: " + onlyA.size());
            out.println("Modules only in B: " + onlyB.size());
            out.println("Common modules: " + common.size());
            for (var line : lines) {
                if (line.startsWith("#")) {
                    out.println(line.substring(1));
                }
            }
            out.println();
            for (var line : lines) {
                if (!line.startsWith("#")) {
                    out.println(line);
                }
            }
            if (!onlyA.isEmpty()) {
                out.println();
                out.println("Only in A:");
                onlyA.forEach(m -> out.println("  " + m));
            }
            if (!onlyB.isEmpty()) {
                out.println();
                out.println("Only in B:");
                onlyB.forEach(m -> out.println("  " + m));
            }
        }
        System.out.println("Modules only in A: " + onlyA.size() + ", only in B: " + onlyB.size()
                + ", common: " + common.size());
        for (var line : lines) {
            if (line.startsWith("#")) {
                System.out.println(line.substring(1));
            }
        }
        System.out.println("Report written to " + summaryFile.toAbsolutePath());
    }

    private static TreeSet<String> collectModules(Path dir) throws IOException {
        try (var stream = Files.walk(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        var name = p.getFileName().toString();
                        return name.endsWith(".wasm") && !name.endsWith(".wasm-deobfuscator.wasm");
                    })
                    .map(p -> dir.relativize(p).toString().replace('\\', '/'))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    // Bytewise comparison

    private static List<String> compareBytewise(Path dirA, Path dirB, List<String> modules) throws IOException {
        var result = new ArrayList<String>();
        var identical = 0;
        var different = 0;
        var errors = 0;
        for (var rel : modules) {
            var a = Files.readAllBytes(dirA.resolve(rel));
            var b = Files.readAllBytes(dirB.resolve(rel));
            var mismatch = Arrays.mismatch(a, b);
            if (mismatch < 0) {
                identical++;
                continue;
            }
            different++;
            var sb = new StringBuilder();
            sb.append("DIFF ").append(rel).append(": size ").append(a.length).append(" vs ").append(b.length)
                    .append(", first difference at 0x").append(Integer.toHexString(mismatch));
            try {
                var sectionsA = parseSections(a);
                var sectionsB = parseSections(b);
                var where = sectionsA.stream().filter(s -> mismatch >= s.start && mismatch < s.end)
                        .findFirst().map(Section::displayName).orElse("header");
                sb.append(" (in ").append(where).append(" section of A)");
                result.add(sb.toString());
                for (var line : compareSections(sectionsA, sectionsB, a, b)) {
                    result.add("    " + line);
                }
            } catch (RuntimeException e) {
                errors++;
                result.add(sb.toString());
                result.add("    error parsing module: " + e);
            }
        }
        result.add(0, "#Identical: " + identical);
        result.add(1, "#Different: " + different);
        if (errors > 0) {
            result.add(2, "#Parse errors: " + errors);
        }
        return result;
    }

    private static List<String> compareSections(List<Section> sectionsA, List<Section> sectionsB,
            byte[] a, byte[] b) {
        var result = new ArrayList<String>();
        var mapA = indexSections(sectionsA);
        var mapB = indexSections(sectionsB);
        var keys = new ArrayList<>(mapA.keySet());
        for (var key : mapB.keySet()) {
            if (!mapA.containsKey(key)) {
                keys.add(key);
            }
        }
        for (var key : keys) {
            var sa = mapA.get(key);
            var sb = mapB.get(key);
            if (sa == null) {
                result.add(key + ": only in B (" + (sb.end - sb.contentStart) + " bytes)");
            } else if (sb == null) {
                result.add(key + ": only in A (" + (sa.end - sa.contentStart) + " bytes)");
            } else if (!Arrays.equals(a, sa.contentStart, sa.end, b, sb.contentStart, sb.end)) {
                result.add(key + ": differs, " + (sa.end - sa.contentStart) + " vs " + (sb.end - sb.contentStart)
                        + " bytes");
            }
        }
        return result;
    }

    private static Map<String, Section> indexSections(List<Section> sections) {
        var result = new LinkedHashMap<String, Section>();
        for (var section : sections) {
            var key = section.displayName();
            var n = 1;
            while (result.containsKey(key)) {
                key = section.displayName() + "#" + (++n);
            }
            result.put(key, section);
        }
        return result;
    }

    // Per-function comparison

    private static List<String> compareFunctions(Path dirA, Path dirB, List<String> modules, Path outputDir)
            throws IOException {
        var result = new ArrayList<String>();
        var identicalModules = 0;
        var differentModules = 0;
        var totalSame = 0;
        var totalDifferent = 0;
        var totalOnlyA = 0;
        var totalOnlyB = 0;
        var totalMoved = 0;
        var errors = 0;
        for (var rel : modules) {
            Map<String, FunctionInfo> fa;
            Map<String, FunctionInfo> fb;
            try {
                fa = parseFunctions(Files.readAllBytes(dirA.resolve(rel)));
                fb = parseFunctions(Files.readAllBytes(dirB.resolve(rel)));
            } catch (RuntimeException e) {
                errors++;
                result.add("ERROR " + rel + ": " + e);
                continue;
            }

            var same = 0;
            var movedDifferent = 0;
            var moved = new ArrayList<String>();
            var different = new ArrayList<String>();
            var onlyA = new ArrayList<String>();
            var onlyB = new ArrayList<String>();
            for (var entry : fa.entrySet()) {
                var name = entry.getKey();
                var fn = entry.getValue();
                var other = fb.get(name);
                if (other == null) {
                    onlyA.add(name + " [" + fn.index + "] (" + fn.body.length + " bytes)");
                } else if (Arrays.equals(fn.body, other.body)) {
                    same++;
                    if (fn.index != other.index) {
                        moved.add(name + " [" + fn.index + " -> " + other.index + "]");
                    }
                } else {
                    if (fn.index != other.index) {
                        movedDifferent++;
                    }
                    different.add(name + " " + formatIndexes(fn, other) + ": " + fn.body.length + " vs "
                            + other.body.length + " bytes");
                }
            }
            for (var entry : fb.entrySet()) {
                if (!fa.containsKey(entry.getKey())) {
                    var fn = entry.getValue();
                    onlyB.add(entry.getKey() + " [" + fn.index + "] (" + fn.body.length + " bytes)");
                }
            }
            totalMoved += moved.size() + movedDifferent;
            totalSame += same;
            totalDifferent += different.size();
            totalOnlyA += onlyA.size();
            totalOnlyB += onlyB.size();

            if (different.isEmpty() && onlyA.isEmpty() && onlyB.isEmpty()) {
                identicalModules++;
                continue;
            }
            differentModules++;
            result.add("DIFF " + rel + ": same " + same + ", different " + different.size() + ", only in A "
                    + onlyA.size() + ", only in B " + onlyB.size() + ", index changed "
                    + (moved.size() + movedDifferent));

            var detailFile = outputDir.resolve(rel + ".functions.txt");
            Files.createDirectories(detailFile.getParent());
            try (var out = new PrintWriter(Files.newBufferedWriter(detailFile, StandardCharsets.UTF_8))) {
                out.println("A: " + dirA.resolve(rel));
                out.println("B: " + dirB.resolve(rel));
                out.println("Same functions: " + same);
                out.println("Functions with changed index: " + (moved.size() + movedDifferent));
                out.println("Index notation: [index in A -> index in B], or [index] when equal");
                writeList(out, "Different", different);
                writeList(out, "Same body, different index", moved);
                writeList(out, "Only in A", onlyA);
                writeList(out, "Only in B", onlyB);
            }
        }
        result.add(0, "#Modules with identical code: " + identicalModules);
        result.add(1, "#Modules with different code: " + differentModules);
        result.add(2, "#Functions: same " + totalSame + ", different " + totalDifferent + ", only in A "
                + totalOnlyA + ", only in B " + totalOnlyB + ", index changed " + totalMoved);
        if (errors > 0) {
            result.add(3, "#Parse errors: " + errors);
        }
        return result;
    }

    private static String formatIndexes(FunctionInfo a, FunctionInfo b) {
        return a.index == b.index ? "[" + a.index + "]" : "[" + a.index + " -> " + b.index + "]";
    }

    private static void writeList(PrintWriter out, String title, List<String> items) {
        out.println();
        out.println(title + ": " + items.size());
        for (var item : items) {
            out.println("  " + item);
        }
    }

    private static Map<String, FunctionInfo> parseFunctions(byte[] data) {
        var sections = parseSections(data);
        var functionNames = new HashMap<Integer, String>();
        var importedFunctions = new int[1];
        Section code = null;
        for (var section : sections) {
            if (section.code == 0 && "name".equals(section.name)) {
                var bytes = Arrays.copyOfRange(data, section.contentStart, section.end);
                var parser = new NameSectionParser(new NameSectionListener() {
                    @Override
                    public NameMapListener functions() {
                        return functionNames::put;
                    }
                });
                parser.parse(new WasmBinaryReader(AddressListener.EMPTY, bytes));
            } else if (section.code == IMPORT_SECTION) {
                var bytes = Arrays.copyOfRange(data, section.contentStart, section.end);
                var parser = new ImportSectionParser(new ImportSectionListener() {
                    @Override
                    public void function(int typeIndex) {
                        importedFunctions[0]++;
                    }
                });
                parser.parse(AddressListener.EMPTY, bytes);
            } else if (section.code == CODE_SECTION) {
                code = section;
            }
        }

        var result = new LinkedHashMap<String, FunctionInfo>();
        if (code == null) {
            return result;
        }
        var reader = new WasmBinaryReader(AddressListener.EMPTY, data);
        reader.ptr = code.contentStart;
        var count = reader.readLEB();
        for (var i = 0; i < count; ++i) {
            var size = reader.readLEB();
            var index = i + importedFunctions[0];
            var name = functionNames.get(index);
            var key = name != null ? name : "<function " + index + ">";
            var unique = key;
            var n = 1;
            while (result.containsKey(unique)) {
                unique = key + "#" + (++n);
            }
            result.put(unique, new FunctionInfo(index, Arrays.copyOfRange(data, reader.ptr, reader.ptr + size)));
            reader.ptr += size;
        }
        return result;
    }

    // Common

    private static List<Section> parseSections(byte[] data) {
        if (data.length < 8 || data[0] != 0 || data[1] != 'a' || data[2] != 's' || data[3] != 'm') {
            throw new IllegalArgumentException("Not a Wasm module");
        }
        var result = new ArrayList<Section>();
        var reader = new WasmBinaryReader(AddressListener.EMPTY, data);
        reader.ptr = 8;
        while (reader.ptr < data.length) {
            var start = reader.ptr;
            var code = data[reader.ptr++] & 0xFF;
            var size = reader.readLEB();
            var end = reader.ptr + size;
            if (end > data.length) {
                throw new IllegalArgumentException("Section at 0x" + Integer.toHexString(start)
                        + " exceeds module size");
            }
            String name = null;
            if (code == 0) {
                name = reader.readString();
            }
            var contentStart = reader.ptr;
            result.add(new Section(code, name, start, contentStart, end));
            reader.ptr = end;
        }
        return result;
    }

    private static class FunctionInfo {
        final int index;
        final byte[] body;

        FunctionInfo(int index, byte[] body) {
            this.index = index;
            this.body = body;
        }
    }

    private static class Section {
        final int code;
        final String name;
        final int start;
        final int contentStart;
        final int end;

        Section(int code, String name, int start, int contentStart, int end) {
            this.code = code;
            this.name = name;
            this.start = start;
            this.contentStart = contentStart;
            this.end = end;
        }

        String displayName() {
            if (code == 0) {
                return "custom '" + name + "'";
            }
            return code < SECTION_NAMES.length ? SECTION_NAMES[code] : "section " + code;
        }
    }
}
