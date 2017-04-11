/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.platform.plugin;

import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.vm.spi.TeaVMHost;

class ResourceAccessorTransformer implements ClassHolderTransformer {
    private TeaVMJavaScriptHost jsHost;

    public ResourceAccessorTransformer(TeaVMHost vm) {
        this.jsHost = vm.getExtension(TeaVMJavaScriptHost.class);
    }

    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (cls.getName().equals(ResourceAccessor.class.getName())) {
            ResourceAccessorInjector injector = new ResourceAccessorInjector();
            for (MethodHolder method : cls.getMethods()) {
                if (method.hasModifier(ElementModifier.NATIVE)) {
                    if (method.getName().equals("keys")) {
                        jsHost.add(method.getReference(), new ResourceAccessorGenerator());
                    } else {
                        jsHost.add(method.getReference(), injector);
                    }
                }
            }
        }
    }
}
