/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.incremental.data.meta;

import static org.teavm.metaprogramming.Metaprogramming.exit;
import static org.teavm.metaprogramming.Metaprogramming.proxy;
import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Value;

@CompileTime
public class Main {
    private Main() {
    }

    public static String run() {
        return meta("ok").get();
    }

    @Meta
    private static native StringSupplier meta(String s);
    private static void meta(Value<String> s) {
        Value<StringSupplier> result = proxy(StringSupplier.class, (instance, method, args) -> {
            exit(() -> "meta: " + s.get());
        });
        exit(() -> result.get());
    }

    interface StringSupplier {
        String get();
    }
}
