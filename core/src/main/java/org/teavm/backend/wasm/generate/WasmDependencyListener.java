/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.backend.wasm.generate;

import org.teavm.dependency.AbstractDependencyListener;
import org.teavm.dependency.DependencyAgent;
import org.teavm.dependency.MethodDependency;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.Export;
import org.teavm.model.AnnotationReader;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReader;

public class WasmDependencyListener extends AbstractDependencyListener implements ClassHolderTransformer {
    @Override
    public void classReached(DependencyAgent agent, String className) {
        for (MethodReader reader : agent.getClassSource().get(className).getMethods()) {
            AnnotationReader annotation = reader.getAnnotations().get(Export.class.getName());
            if (annotation != null) {
                agent.linkMethod(reader.getReference()).use();
            }
        }
    }

    @Override
    public void methodReached(DependencyAgent agent, MethodDependency method) {
        AnnotationReader delegateAnnot = method.getMethod().getAnnotations().get(DelegateTo.class.getName());
        if (delegateAnnot != null) {
            String delegateMethodName = delegateAnnot.getValue("value").getString();
            ClassReader cls = agent.getClassSource().get(method.getReference().getClassName());
            for (MethodReader delegate : cls.getMethods()) {
                if (delegate.getName().equals(delegateMethodName)) {
                    if (delegate != method.getMethod()) {
                        MethodDependency dep = agent.linkMethod(delegate.getReference());
                        dep.use();
                        method.addLocationListener(dep::addLocation);
                    }
                }
            }
        }
    }

    @Override
    public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
        for (MethodHolder method : cls.getMethods()) {
            AnnotationReader delegateAnnot = method.getAnnotations().get(DelegateTo.class.getName());
            if (delegateAnnot != null) {
                method.setProgram(null);
                method.getModifiers().add(ElementModifier.NATIVE);
            }
        }
    }
}