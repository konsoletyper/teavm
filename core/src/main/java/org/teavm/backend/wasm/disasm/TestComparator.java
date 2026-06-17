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
package org.teavm.backend.wasm.disasm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import org.teavm.backend.wasm.parser.AddressListener;
import org.teavm.backend.wasm.parser.CodeSectionListener;
import org.teavm.backend.wasm.parser.CodeSectionParser;
import org.teavm.backend.wasm.parser.ImportSectionListener;
import org.teavm.backend.wasm.parser.ImportSectionParser;
import org.teavm.backend.wasm.parser.ModuleParser;
import org.teavm.backend.wasm.parser.NameMapListener;
import org.teavm.backend.wasm.parser.NameSectionListener;
import org.teavm.backend.wasm.parser.NameSectionParser;
import org.teavm.backend.wasm.parser.WasmBinaryReader;
import org.teavm.common.ByteArrayAsyncInputStream;

public class TestComparator {
    public static void main(String[] args) throws IOException {
        var base = Path.of("/home/konsoletyper/devel/teavm/tests/build/teavm-tests/wasm-gc/");
        var compareBase = Path.of("/home/konsoletyper/devel/teavm/tests/build/wasm-baseline-tests/");
        var globalDiffs = new ArrayList<Diff>();
        Files.walk(base)
                .filter(path -> path.getFileName().toString().endsWith(".wasm"))
                .filter(path -> !path.getFileName().toString().endsWith(".wasm-deobfuscator.wasm"))
                .forEach(path -> {
                    var rel = base.relativize(path);
                    var oldPath = compareBase.resolve(rel);
                    if (Files.exists(oldPath)) {
                        try {
                            var oldModule = moduleInfo(oldPath);
                            var newModule = moduleInfo(path);
                            var diff = diffFunctions(oldModule, newModule);
                            var diffPath = path.getParent().resolve(path.getFileName().toString() + ".diff.txt");
                            formatDiff(diff, diffPath);
                            globalDiffs.add(new Diff(rel.toString(), oldModule.codeSectionSize,
                                    newModule.codeSectionSize));
                        } catch (IOException e) {
                            // do nothing
                        }
                    }
                });
        formatDiff(globalDiffs, Path.of(
                "/home/konsoletyper/devel/teavm/tests/build/teavm-tests/wasm-gc/modules.diff.txt"));
    }

    private static ModuleInfo moduleInfo(Path path) throws IOException {
        var moduleInfo = new ModuleInfo();
        var bytes = Files.readAllBytes(path);
        var functionNames = new HashMap<Integer, String>();
        var input = new ByteArrayAsyncInputStream(bytes);
        var nameExtractor = new ModuleParser(input) {
            int indexOffset;

            @Override
            protected Consumer<byte[]> getSectionConsumer(int code, int pos, String name) {
                if (Objects.equals(name, "name")) {
                    return sectionBytes -> {
                        var nameParser = new NameSectionParser(new NameSectionListener() {
                            @Override
                            public NameMapListener functions() {
                                return new NameMapListener() {
                                    @Override
                                    public void name(int index, String name) {
                                        functionNames.put(index, name);
                                    }
                                };
                            };
                        });
                        nameParser.parse(new WasmBinaryReader(AddressListener.EMPTY, sectionBytes));
                    };
                } else if (code == 2) {
                    return sectionBytes -> {
                        var parser = new ImportSectionParser(new ImportSectionListener() {
                            @Override
                            public void function(int typeIndex) {
                                indexOffset++;
                            }
                        });
                        parser.parse(AddressListener.EMPTY, sectionBytes);
                    };
                } else {
                    return null;
                }
            };
        };
        input.readFully(nameExtractor::parse);

        input = new ByteArrayAsyncInputStream(bytes);
        var counter = new ModuleParser(input) {
            @Override
            protected Consumer<byte[]> getSectionConsumer(int code, int pos, String name) {
                if (code == 10) {
                    return sectionBytes -> {
                        moduleInfo.codeSectionSize = sectionBytes.length;
                        var sectionParser = new CodeSectionParser(new CodeSectionListener() {
                            @Override
                            public boolean functionStart(int index, int size) {
                                var name = functionNames.get(index);
                                if (name != null) {
                                    if (name.equals("java.nio.Buffer::capacity")) {
                                        System.out.println(path + " " + size);
                                        if (size > 9) {
                                            System.out.println("size > 9");
                                        }
                                    }
                                    moduleInfo.functionSizes.put(name, size);
                                }
                                return false;
                            }
                        });
                        sectionParser.setFunctionIndexOffset(nameExtractor.indexOffset);
                        sectionParser.parse(AddressListener.EMPTY, sectionBytes);
                    };
                }
                return null;
            }
        };
        input.readFully(counter::parse);

        return moduleInfo;
    }

    private static class ModuleInfo {
        int codeSectionSize;
        Map<String, Integer> functionSizes = new HashMap<>();
    }

    private static List<Diff> diffFunctions(ModuleInfo oldModule, ModuleInfo newModule) {
        var result = new ArrayList<Diff>();
        for (var entry : oldModule.functionSizes.entrySet()) {
            var name = entry.getKey();
            var oldSize = entry.getValue();
            var newSize = newModule.functionSizes.get(name);
            if (newSize != null) {
                result.add(new Diff(name, oldSize, newSize));
            }
        }
        return result;
    }

    private static void formatDiff(List<Diff> diff, Path out) throws IOException {
        diff.sort(Comparator.comparing(d -> d.oldSize - d.newSize));
        Files.createDirectories(out.getParent());
        try (var writer = Files.newBufferedWriter(out)) {
            for (var e : diff) {
                if (e.oldSize != e.newSize) {
                    if (e.oldSize > e.newSize) {
                        writer.append("-" + (e.oldSize - e.newSize) + ": " + e.name + " " + e.oldSize + " -> "
                                + e.newSize + "\n");
                    } else {
                        writer.append("+" + (e.newSize - e.oldSize) + ": " + e.name + " " + e.oldSize + " -> "
                                + e.newSize + "\n");
                    }
                }
            }
        }
    }

    private static class Diff {
        String name;
        int oldSize;
        int newSize;

        Diff(String name, int oldSize, int newSize) {
            this.name = name;
            this.oldSize = oldSize;
            this.newSize = newSize;
        }
    }
}
