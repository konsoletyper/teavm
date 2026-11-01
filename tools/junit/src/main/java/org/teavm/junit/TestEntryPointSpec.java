/*
 *  Copyright 2025 Alexey Andreev.
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
import java.util.Properties;
import org.teavm.model.MethodReference;
import org.teavm.vm.TeaVM;

final class TestEntryPointSpec {
    final String testClassName;
    final List<MethodReference> testMethods;
    final boolean wholeClass;
    final Map<MethodReference, JupiterArgumentsPlan> plans;
    final Properties properties;

    TestEntryPointSpec(String testClassName, List<MethodReference> testMethods, boolean wholeClass,
            Map<MethodReference, JupiterArgumentsPlan> plans, Properties properties) {
        this.testClassName = testClassName;
        this.testMethods = testMethods;
        this.wholeClass = wholeClass;
        this.plans = plans;
        this.properties = properties;
    }

    void install(TeaVM vm) {
        vm.setProperties(properties);
        if (wholeClass) {
            new TestEntryPointTransformerForWholeClass(testMethods, testClassName, plans).install(vm);
        } else {
            new TestEntryPointTransformerForSingleMethod(testMethods.get(0), testClassName, plans).install(vm);
        }
    }
}
