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
package org.teavm.model.lowlevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.teavm.model.AnnotationContainer;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.AnnotationHolder;
import org.teavm.model.AnnotationReader;
import org.teavm.model.AnnotationValue;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.MethodReader;
import org.teavm.model.Program;

public class CallSiteDescriptor {
    private int id;
    private List<ExceptionHandlerDescriptor> handlers = new ArrayList<>();
    private CallSiteLocation[] locations;

    public CallSiteDescriptor(int id, CallSiteLocation[] locations) {
        this.id = id;
        this.locations = locations != null ? locations.clone() : null;
    }

    public int getId() {
        return id;
    }

    public CallSiteLocation[] getLocations() {
        return locations != null ? locations.clone() : null;
    }

    public List<ExceptionHandlerDescriptor> getHandlers() {
        return handlers;
    }

    public static void save(Collection<? extends CallSiteDescriptor> descriptors, AnnotationContainer annotations) {
        List<AnnotationValue> descriptorsValue = new ArrayList<>();
        for (CallSiteDescriptor descriptor : descriptors) {
            AnnotationHolder descriptorAnnot = new AnnotationHolder(CallSiteDescriptorAnnot.class.getName());
            descriptorAnnot.getValues().put("id", new AnnotationValue(descriptor.id));
            descriptorAnnot.getValues().put("location", new AnnotationValue(
                    CallSiteLocation.saveMany(Arrays.asList(descriptor.locations))));
            List<AnnotationValue> handlersValue = descriptor.handlers.stream()
                    .map(h -> new AnnotationValue(h.save()))
                    .collect(Collectors.toList());
            descriptorAnnot.getValues().put("handlers", new AnnotationValue(handlersValue));
            descriptorsValue.add(new AnnotationValue(descriptorAnnot));
        }

        AnnotationHolder descriptorsAnnot = new AnnotationHolder(CallSiteDescriptorsAnnot.class.getName());
        descriptorsAnnot.getValues().put("value", new AnnotationValue(descriptorsValue));
        annotations.add(descriptorsAnnot);
    }

    public static Collection<? extends CallSiteDescriptor> load(AnnotationContainerReader annotations) {
        AnnotationReader descriptorsAnnot = annotations.get(CallSiteDescriptorsAnnot.class.getName());
        if (descriptorsAnnot == null) {
            return Collections.emptyList();
        }

        List<CallSiteDescriptor> descriptors = new ArrayList<>();
        for (AnnotationValue descriptorValue : descriptorsAnnot.getValue("value").getList()) {
            AnnotationReader descriptorAnnot = descriptorValue.getAnnotation();
            int id = descriptorAnnot.getValue("id").getInt();
            List<? extends CallSiteLocation> location = CallSiteLocation.loadMany(
                    descriptorAnnot.getValue("location").getAnnotation());
            List<ExceptionHandlerDescriptor> handlers = descriptorAnnot.getValue("handlers").getList().stream()
                    .map(a -> ExceptionHandlerDescriptor.load(a.getAnnotation()))
                    .collect(Collectors.toList());
            CallSiteDescriptor descriptor = new CallSiteDescriptor(id, location.toArray(new CallSiteLocation[0]));
            descriptor.getHandlers().addAll(handlers);
            descriptors.add(descriptor);
        }

        return descriptors;
    }

    public static List<? extends CallSiteDescriptor> extract(Program program) {
        List<CallSiteDescriptor> result = new ArrayList<>();
        extractTo(load(program.getAnnotations()), result);
        return result;
    }

    public static List<? extends CallSiteDescriptor> extract(ClassReaderSource classes,
            Collection<? extends String> classNames) {
        List<CallSiteDescriptor> result = new ArrayList<>();
        for (String className : classNames) {
            ClassReader cls = classes.get(className);
            if (cls == null) {
                continue;
            }
            for (MethodReader method : cls.getMethods()) {
                if (method.getProgram() != null) {
                    extractTo(load(method.getProgram().getAnnotations()), result);
                }
            }
        }
        return result;
    }

    private static void extractTo(Collection<? extends CallSiteDescriptor> descriptors,
            List<CallSiteDescriptor> result) {
        for (CallSiteDescriptor descriptor : descriptors) {
            if (descriptor.id >= result.size()) {
                result.addAll(Collections.nCopies(descriptor.id - result.size() + 1, null));
            }
            result.set(descriptor.id, descriptor);
        }
    }

    static AnnotationValue saveNullableString(String s) {
        return new AnnotationValue(s != null ? "1" + s : "0");
    }

    static String loadNullableString(AnnotationValue value) {
        String s = value.getString();
        return s.startsWith("0") ? null : s.substring(1);
    }
}
