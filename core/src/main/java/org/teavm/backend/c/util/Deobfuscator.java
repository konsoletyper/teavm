/*
 *  Copyright 2019 konsoletyper.
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
package org.teavm.backend.c.util;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.c.util.json.JsonAllErrorVisitor;
import org.teavm.backend.c.util.json.JsonArrayVisitor;
import org.teavm.backend.c.util.json.JsonErrorReporter;
import org.teavm.backend.c.util.json.JsonParser;
import org.teavm.backend.c.util.json.JsonPropertyVisitor;
import org.teavm.backend.c.util.json.JsonVisitingConsumer;
import org.teavm.backend.c.util.json.JsonVisitor;

public class Deobfuscator {
    private Map<Integer, CallSite> callSites = new HashMap<>();

    public Deobfuscator(Reader reader) throws IOException {
        CallSiteVisitor visitor = new CallSiteVisitor();
        new JsonParser(new JsonVisitingConsumer(new JsonArrayVisitor(visitor))).parse(reader);
        visitor.flush();
    }

    public Location[] getLocations(int callSiteId) {
        CallSite callSite = callSites.get(callSiteId);
        if (callSite == null) {
            return null;
        }

        Location[] result = new Location[callSite.locations.length];
        for (int i = 0; i < result.length; ++i) {
            result[i] = callSite.locations[i].clone();
        }
        return result;
    }

    public String deobfuscate(Reader text) throws IOException {
        StringBuilder result = new StringBuilder();

        String expectedPrefix = "\tat Obfuscated.obfuscated(Obfuscated.java:";
        int expectedPrefixPos = 0;
        boolean isInNumber = false;
        boolean expectingLineEnd = false;
        StringBuilder sb = new StringBuilder();

        while (true) {
            int c = text.read();

            if (expectedPrefixPos >= 0) {
                if (expectedPrefix.charAt(expectedPrefixPos) == c) {
                    expectedPrefixPos++;
                    if (expectedPrefixPos == expectedPrefix.length()) {
                        expectedPrefixPos = -1;
                        isInNumber = true;
                    }
                    continue;
                }
                result.append(expectedPrefix, 0, expectedPrefixPos);
                expectedPrefixPos = -1;
            } else if (isInNumber) {
                if (c >= '0' && c <= '9') {
                    sb.append((char) c);
                    continue;
                } else if (c == ')') {
                    isInNumber = false;
                    expectingLineEnd = true;
                    continue;
                }
                isInNumber = false;
                result.append(expectedPrefix).append(sb);
            }

            if (c == '\r' || c == '\n' || c < 0) {
                if (expectingLineEnd) {
                    int callSiteId = Integer.parseInt(sb.toString());
                    Location[] locations = getLocations(callSiteId);
                    if (locations == null) {
                        result.append(expectedPrefix).append(sb).append(')');
                    } else {
                        boolean first = true;
                        for (Location location : locations) {
                            if (!first) {
                                result.append('\n');
                            }
                            first = false;
                            result.append("    ");
                            result.append(location.className).append('.').append(location.methodName).append('(');
                            if (location.fileName != null & location.line >= 0) {
                                result.append(location.fileName).append(':').append(location.line);
                            } else {
                                result.append("Unknown location");
                            }
                            result.append(")");
                        }
                    }
                    sb.setLength(0);
                }
                expectingLineEnd = false;
                expectedPrefixPos = 0;
            }
            if (c < 0) {
                break;
            }
            result.append((char) c);
        }

        return result.toString();
    }

    class CallSiteVisitor extends JsonAllErrorVisitor {
        CallSite callSite;
        List<Location> locations = new ArrayList<>();
        Location location;
        JsonPropertyVisitor propertyVisitor = new JsonPropertyVisitor(false);
        JsonPropertyVisitor locationPropertyVisitor = new JsonPropertyVisitor(false);

        CallSiteVisitor() {
            propertyVisitor.addProperty("id", new JsonAllErrorVisitor() {
                @Override
                public void intValue(JsonErrorReporter reporter, long value) {
                    callSite.id = (int) value;
                }
            });
            propertyVisitor.addProperty("locations", new JsonArrayVisitor(locationVisitor));

            locationPropertyVisitor.addProperty("file", new JsonAllErrorVisitor() {
                @Override
                public void stringValue(JsonErrorReporter reporter, String value) {
                    location.fileName = value;
                }

                @Override
                public void nullValue(JsonErrorReporter reporter) {
                    location.fileName = null;
                }
            });

            locationPropertyVisitor.addProperty("class", new JsonAllErrorVisitor() {
                @Override
                public void stringValue(JsonErrorReporter reporter, String value) {
                    location.className = value;
                }

                @Override
                public void nullValue(JsonErrorReporter reporter) {
                    location.className = null;
                }
            });

            locationPropertyVisitor.addProperty("method", new JsonAllErrorVisitor() {
                @Override
                public void stringValue(JsonErrorReporter reporter, String value) {
                    location.methodName = value;
                }

                @Override
                public void nullValue(JsonErrorReporter reporter) {
                    location.methodName = null;
                }
            });

            locationPropertyVisitor.addProperty("line", new JsonAllErrorVisitor() {
                @Override
                public void intValue(JsonErrorReporter reporter, long value) {
                    location.line = (int) value;
                }
            });
        }

        @Override
        public JsonVisitor object(JsonErrorReporter reporter) {
            flush();
            callSite = new CallSite();
            return propertyVisitor;
        }

        void flush() {
            if (callSite != null) {
                callSites.put(callSite.id, callSite);
                callSite.locations = locations.toArray(new Location[0]);
                locations.clear();
            }
        }

        JsonAllErrorVisitor locationVisitor = new JsonAllErrorVisitor() {
            @Override
            public JsonVisitor object(JsonErrorReporter reporter) {
                location = new Location();
                locations.add(location);
                return locationPropertyVisitor;
            }
        };
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Two arguments expected (JSON table file, stack trace file)");
            System.exit(1);
        }

        Deobfuscator deobfuscator;
        try (InputStream input = new BufferedInputStream(new FileInputStream(args[0]));
                Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            deobfuscator = new Deobfuscator(reader);
        }

        String result;
        try (InputStream input = new BufferedInputStream(new FileInputStream(args[1]));
                Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            result = deobfuscator.deobfuscate(reader);
        }

        System.out.println(result);
    }

    static class CallSite {
        int id;
        Location[] locations;
    }

    public static class Location implements Cloneable {
        public String className;
        public String methodName;
        public String fileName;
        public int line = -1;

        @Override
        protected Location clone() {
            try {
                return (Location) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }
}
