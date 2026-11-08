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
package org.teavm.buildutil;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JMH command line arguments from {@code benchmark.*} properties. The same arguments are understood
 * by JMH itself and by TeaVM benchmark runner ({@code :tools:perf}).
 * See {@code gradle-local-template.properties} in benchmark modules for description of properties.
 */
public final class JmhArguments {
    private JmhArguments() {
    }

    public static List<String> fromProperties(LocalPropertiesExtension properties) {
        var args = new ArrayList<String>();
        option(args, properties, "benchmark.warmupIterations", "-wi");
        option(args, properties, "benchmark.warmupTime", "-w");
        option(args, properties, "benchmark.iterations", "-i");
        option(args, properties, "benchmark.time", "-r");
        option(args, properties, "benchmark.forks", "-f");
        option(args, properties, "benchmark.mode", "-bm");
        option(args, properties, "benchmark.timeUnit", "-tu");
        for (var param : properties.list("benchmark.params", ";")) {
            if (!param.contains("=")) {
                throw new IllegalArgumentException("benchmark.params: expected name=value1,value2,..., got '"
                        + param + "'");
            }
            args.add("-p");
            args.add(param);
        }
        for (var exclude : properties.list("benchmark.exclude")) {
            args.add("-e");
            args.add(exclude);
        }
        args.addAll(LocalPropertiesExtension.split(properties.string("benchmark.extraArgs"), "\\s+"));
        args.addAll(properties.list("benchmark.include"));
        return args;
    }

    private static void option(List<String> args, LocalPropertiesExtension properties, String property,
            String option) {
        var value = properties.string(property);
        if (value != null) {
            args.add(option);
            args.add(value);
        }
    }
}
