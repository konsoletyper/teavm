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
package org.teavm.backend.javascript.rendering;

import java.util.Set;
import org.teavm.backend.javascript.codegen.NameFrequencyConsumer;
import org.teavm.backend.javascript.codegen.RememberedSource;
import org.teavm.backend.javascript.codegen.SourceWriterSink;
import org.teavm.backend.javascript.decompile.PreparedClass;
import org.teavm.backend.javascript.decompile.PreparedMethod;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldHolder;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;

class NameFrequencyEstimator implements SourceWriterSink {
    static final MethodReference MONITOR_ENTER_METHOD = new MethodReference(Object.class,
            "monitorEnter", Object.class, void.class);
    static final MethodReference MONITOR_ENTER_SYNC_METHOD = new MethodReference(Object.class,
            "monitorEnterSync", Object.class, void.class);
    static final MethodReference MONITOR_EXIT_METHOD = new MethodReference(Object.class,
            "monitorExit", Object.class, void.class);
    static final MethodReference MONITOR_EXIT_SYNC_METHOD = new MethodReference(Object.class,
            "monitorExitSync", Object.class, void.class);
    private static final MethodDescriptor CLINIT_METHOD = new MethodDescriptor("<clinit>", ValueType.VOID);

    private final NameFrequencyConsumer consumer;
    private final ClassReaderSource classSource;
    private final Set<MethodReference> asyncFamilyMethods;

    NameFrequencyEstimator(NameFrequencyConsumer consumer, ClassReaderSource classSource,
            Set<MethodReference> asyncFamilyMethods) {
        this.consumer = consumer;
        this.classSource = classSource;
        this.asyncFamilyMethods = asyncFamilyMethods;
    }

    public void estimate(PreparedClass cls) {
        // Declaration
        consumer.consume(cls.getName());
        if (cls.getParentName() != null) {
            consumer.consume(cls.getParentName());
        }
        for (FieldHolder field : cls.getClassHolder().getFields()) {
            consumer.consume(new FieldReference(cls.getName(), field.getName()));
            if (field.getModifiers().contains(ElementModifier.STATIC)) {
                consumer.consume(cls.getName());
            }
        }

        // Methods
        MethodReader clinit = classSource.get(cls.getName()).getMethod(CLINIT_METHOD);
        for (PreparedMethod method : cls.getMethods()) {
            consumer.consume(method.reference);
            if (asyncFamilyMethods.contains(method.reference)) {
                consumer.consume(method.reference);
            }
            if (clinit != null && (method.modifiers.contains(ElementModifier.STATIC)
                    || method.reference.getName().equals("<init>"))) {
                consumer.consume(method.reference);
            }
            if (!method.modifiers.contains(ElementModifier.STATIC)) {
                consumer.consume(method.reference.getDescriptor());
                consumer.consume(method.reference);
            }
            if (method.async) {
                consumer.consumeFunction("$rt_nativeThread");
                consumer.consumeFunction("$rt_nativeThread");
                consumer.consumeFunction("$rt_resuming");
                consumer.consumeFunction("$rt_invalidPointer");
            }

            method.body.replay(this, RememberedSource.FILTER_REF);
        }

        if (clinit != null) {
            consumer.consumeFunction("$rt_eraseClinit");
        }

        // Metadata
        consumer.consume(cls.getName());
        consumer.consume(cls.getName());
        if (cls.getParentName() != null) {
            consumer.consume(cls.getParentName());
        }
        for (String iface : cls.getClassHolder().getInterfaces()) {
            consumer.consume(iface);
        }

        boolean hasFields = false;
        for (FieldHolder field : cls.getClassHolder().getFields()) {
            if (!field.hasModifier(ElementModifier.STATIC)) {
                hasFields = true;
                break;
            }
        }
        if (!hasFields) {
            consumer.consumeFunction("$rt_classWithoutFields");
        }
    }

    @Override
    public SourceWriterSink appendClass(String cls) {
        consumer.consume(cls);
        return this;
    }

    @Override
    public SourceWriterSink appendField(FieldReference field) {
        consumer.consume(field);
        return this;
    }

    @Override
    public SourceWriterSink appendStaticField(FieldReference field) {
        consumer.consumeStatic(field);
        return this;
    }

    @Override
    public SourceWriterSink appendMethod(MethodDescriptor method) {
        consumer.consume(method);
        return this;
    }

    @Override
    public SourceWriterSink appendMethodBody(MethodReference method) {
        consumer.consume(method);
        return this;
    }

    @Override
    public SourceWriterSink appendFunction(String name) {
        consumer.consumeFunction(name);
        return this;
    }

    @Override
    public SourceWriterSink appendGlobal(String name) {
        consumer.consumeGlobal(name);
        return this;
    }

    @Override
    public SourceWriterSink appendInit(MethodReference method) {
        consumer.consumeInit(method);
        return this;
    }

    @Override
    public SourceWriterSink appendClassInit(String className) {
        consumer.consumeClassInit(className);
        return this;
    }
}
