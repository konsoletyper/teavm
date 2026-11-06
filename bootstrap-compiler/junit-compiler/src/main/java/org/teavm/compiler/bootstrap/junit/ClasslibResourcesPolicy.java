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
package org.teavm.compiler.bootstrap.junit;

import java.util.Collection;
import org.teavm.compiler.bootstrap.TeaVMBootstrapCompiler;
import org.teavm.extension.Autoregistered;
import org.teavm.extension.spi.resources.DefaultResourcesPolicy;

@Autoregistered
public class ClasslibResourcesPolicy extends DefaultResourcesPolicy {
    @Override
    public String[] supplyResources(Collection<? extends String> availableClassNames) {
        if (!availableClassNames.contains(TeaVMBootstrapCompiler.class.getName())) {
            return new String[0];
        }
        return new String[] { TeaVMBootstrapCompiler.CLASSLIB_RESOURCE };
    }
}
