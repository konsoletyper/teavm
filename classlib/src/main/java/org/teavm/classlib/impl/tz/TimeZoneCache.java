/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.classlib.impl.tz;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.teavm.classlib.impl.Base46;
import org.teavm.classlib.impl.CharFlow;

public class TimeZoneCache {
    public void write(OutputStream output, Collection<StorableDateTimeZone> timeZones) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (StorableDateTimeZone timeZone : timeZones) {
            writer.append(timeZone.getID()).append(' ');
            timeZone.write(sb);
            writer.append(sb);
            sb.setLength(0);
            writer.append('\n');
        }
        writer.flush();
    }

    public Map<String, StorableDateTimeZone> read(InputStream input) throws IOException {
        Map<String, StorableDateTimeZone> result = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        List<String> aliasLines = new ArrayList<>();
        while (true) {
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                break;
            }
            int index = line.indexOf(' ');
            String id = line.substring(0, index);
            String data = line.substring(index + 1);
            CharFlow flow = new CharFlow(data.toCharArray());
            if (Base46.decodeUnsigned(flow) == StorableDateTimeZone.ALIAS) {
                aliasLines.add(line);
            } else {
                result.put(id, StorableDateTimeZone.read(id, data));
            }
        }
        for (String aliasLine : aliasLines) {
            int index = aliasLine.indexOf(' ');
            String id = aliasLine.substring(0, index);
            String data = aliasLine.substring(index + 1);
            CharFlow flow = new CharFlow(data.toCharArray());
            Base46.decode(flow);
            result.put(id, new AliasDateTimeZone(id, result.get(data.substring(flow.pointer))));
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        ZoneInfoCompiler compiler = new ZoneInfoCompiler();
        TimeZoneGenerator.compile(compiler, TimeZoneCache.class.getClassLoader());
        File file = new File(args[0]);
        file.getParentFile().mkdirs();
        try (OutputStream output = new FileOutputStream(file)) {
            new TimeZoneCache().write(output, compiler.compile().values());
        }
    }
}
