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
package org.teavm.junit;

import java.util.List;
import java.util.Map;
import org.teavm.model.MethodReference;
import org.teavm.vm.spi.TeaVMPlugin;

public final class TestCompilationPlugins {
    private TestCompilationPlugins() {
    }

    public static TeaVMPlugin createEntryPointPlugin(String testClassName, List<MethodReference> testMethods,
            boolean wholeClass) {
        return createEntryPointPlugin(testClassName, testMethods, wholeClass, "");
    }

    /**
     * Creates plugin that generates test entry point.
     *
     * @param parameterizedPlans arguments of Jupiter parameterized tests, as provided by
     *     {@link org.teavm.junit.spi.ExternalCompilationRequest#getParameterizedPlans()}.
     */
    public static TeaVMPlugin createEntryPointPlugin(String testClassName, List<MethodReference> testMethods,
            boolean wholeClass, String parameterizedPlans) {
        Map<MethodReference, JupiterArgumentsPlan> plans = JupiterArgumentsPlanCodec.decode(parameterizedPlans);
        return wholeClass
                ? new TestEntryPointTransformerForWholeClass(testMethods, testClassName, plans)
                : new TestEntryPointTransformerForSingleMethod(testMethods.get(0), testClassName, plans);
    }

    public static TeaVMPlugin createExceptionPlugin() {
        return new TestExceptionPlugin();
    }
}
