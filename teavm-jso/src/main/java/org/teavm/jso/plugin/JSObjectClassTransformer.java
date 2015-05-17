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
package org.teavm.jso.plugin;

import org.teavm.diagnostics.Diagnostics;
import org.teavm.jso.JSBody;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev
 */
public class JSObjectClassTransformer implements ClassHolderTransformer {
    private JavascriptNativeProcessor processor;

    @Override
    public void transformClass(ClassHolder cls, ClassReaderSource innerSource, Diagnostics diagnostics) {
        if (processor == null || processor.getClassSource() != innerSource) {
            processor = new JavascriptNativeProcessor(innerSource);
        }
        processor.setDiagnostics(diagnostics);
        processor.processClass(cls);
        if (processor.isNative(cls.getName())) {
            processor.processFinalMethods(cls);
        }
        if (processor.isNativeImplementation(cls.getName())) {
            processor.makeSync(cls);
        }
        MethodReference functorMethod = processor.isFunctor(cls.getName());
        if (functorMethod != null) {
            if (processor.isFunctor(cls.getParent()) == null) {
                processor.addFunctorField(cls, functorMethod);
            }
        }
        for (MethodHolder method : cls.getMethods().toArray(new MethodHolder[0])) {
            if (method.getAnnotations().get(JSBody.class.getName()) != null) {
                processor.processJSBody(cls, method);
            } else if (method.getProgram() != null &&
                    method.getAnnotations().get(JSBodyImpl.class.getName()) == null) {
                processor.processProgram(method);
            }
        }
    }
}
