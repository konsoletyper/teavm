/*
 *  Copyright 2026 TeaVM contributors.
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
package repro;

import java.util.List;
import org.teavm.classlib.ReflectionContext;
import org.teavm.classlib.ReflectionSupplier;

@SuppressWarnings("deprecation")
public final class JdkApiReflectionSupplier implements ReflectionSupplier {
    @Override
    public List<String> getAccessibleFields(ReflectionContext context, String className) {
        return className.equals(JdkApiContract.class.getName())
                ? List.of("sample", "byteValue", "shortValue", "charValue", "integer", "longValue",
                        "booleanValue", "doubleValue", "staticValue")
                : List.of();
    }
}
