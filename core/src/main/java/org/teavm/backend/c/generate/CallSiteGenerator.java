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
package org.teavm.backend.c.generate;

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.model.FieldReference;
import org.teavm.model.ValueType;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.CallSiteLocation;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;

public class CallSiteGenerator {
    public static final String CALL_SITE = "org.teavm.runtime.CallSite";
    private static final String CALL_SITE_LOCATION = "org.teavm.runtime.CallSiteLocation";
    private static final String METHOD_LOCATION = "org.teavm.runtime.MethodLocation";
    private static final String EXCEPTION_HANDLER = "org.teavm.runtime.ExceptionHandler";

    private GenerationContext context;
    private CodeWriter writer;
    private IncludeManager includes;
    private ObjectIntMap<CallSiteLocation> locationMap = new ObjectIntHashMap<>();
    private List<CallSiteLocation> locations = new ArrayList<>();
    private List<HandlerContainer> exceptionHandlers = new ArrayList<>();
    private ObjectIntMap<MethodLocation> methodLocationMap = new ObjectIntHashMap<>();
    private List<MethodLocation> methodLocations = new ArrayList<>();
    private String callSiteLocationName;
    private String methodLocationName;
    private String exceptionHandlerName;
    private String callSitesName;
    private boolean isStatic;

    public CallSiteGenerator(GenerationContext context, CodeWriter writer, IncludeManager includes,
            String callSitesName) {
        this.context = context;
        this.writer = writer;
        this.includes = includes;
        callSiteLocationName = context.getNames().forClass(CALL_SITE_LOCATION);
        methodLocationName = context.getNames().forClass(METHOD_LOCATION);
        exceptionHandlerName = context.getNames().forClass(EXCEPTION_HANDLER);
        this.callSitesName = callSitesName;
    }

    public void setStatic(boolean isStatic) {
        this.isStatic = isStatic;
    }

    public void generate(List<? extends CallSiteDescriptor> callSites) {
        CodeWriter writerForMethodLocations = writer.fragment();
        CodeWriter writerForLocations = writer.fragment();
        generateCallSites(callSites);

        CodeWriter oldWriter = writer;
        writer = writerForLocations;
        generateLocations();
        writer = writerForMethodLocations;
        generateMethodLocations();
        generateHandlers();
        writer = oldWriter;
    }

    private void generateCallSites(List<? extends CallSiteDescriptor> callSites) {
        String callSiteName = context.getNames().forClass(CALL_SITE);

        includes.includeClass(CALL_SITE);
        includes.includePath("strings.h");
        if (isStatic) {
            writer.print("static ");
        }
        writer.print(callSiteName).print(" " + callSitesName + "[" + callSites.size() + "] = {").indent();
        String firstHandlerName = fieldName(CALL_SITE, "firstHandler");
        String locationName = fieldName(CALL_SITE, "location");

        boolean first = true;
        for (CallSiteDescriptor callSite : callSites) {
            if (!first) {
                writer.print(", ");
            }
            first = false;

            int locationIndex = -1;
            if (callSite.getLocation() != null) {
                locationIndex = locationMap.getOrDefault(callSite.getLocation(), -1);
                if (locationIndex < 0) {
                    locationIndex = locations.size();
                    locationMap.put(callSite.getLocation(), locationIndex);
                    locations.add(callSite.getLocation());
                }
            }

            String firstHandlerExpr = !callSite.getHandlers().isEmpty()
                    ? "exceptionHandlers_" + callSitesName + " + " + exceptionHandlers.size()
                    : "NULL";
            writer.println().print("{ ");
            writer.print(".").print(firstHandlerName).print(" = ").print(firstHandlerExpr).print(", ");
            writer.print(".").print(locationName).print(" = ")
                    .print(locationIndex >= 0 ? "callSiteLocations_" + callSitesName + " + " + locationIndex : "NULL");
            writer.print(" }");

            for (int i = 0; i < callSite.getHandlers().size(); ++i) {
                if (i > 0) {
                    exceptionHandlers.get(exceptionHandlers.size() - 1).nextIndex = exceptionHandlers.size();
                }
                exceptionHandlers.add(new HandlerContainer(callSite.getHandlers().get(i)));
            }
        }

        writer.println().outdent().println("};");
    }

    private void generateLocations() {
        if (locations.isEmpty()) {
            return;
        }
        includes.includeClass(CALL_SITE_LOCATION);
        writer.print("static ").print(callSiteLocationName).print(" callSiteLocations_" + callSitesName
                + "[" + locations.size() + "] = {").indent();

        String methodName = fieldName(CALL_SITE_LOCATION, "method");
        String lineNumberName = fieldName(CALL_SITE_LOCATION, "lineNumber");

        boolean first = true;
        for (CallSiteLocation location : locations) {
            if (!first) {
                writer.print(",");
            }
            first = false;

            writer.println().print("{ ");
            MethodLocation methodLocation = new MethodLocation(location.getFileName(), location.getClassName(),
                    location.getMethodName());
            int index = methodLocationMap.getOrDefault(methodLocation, -1);
            if (index < 0) {
                index = methodLocations.size();
                methodLocationMap.put(methodLocation, index);
                methodLocations.add(methodLocation);
            }
            writer.print(".").print(methodName).print(" = ").print("methodLocations_" + callSitesName + " + " + index)
                    .print(", ");
            writer.print(".").print(lineNumberName).print(" = ")
                    .print(String.valueOf(location.getLineNumber()));

            writer.print(" }");
        }

        writer.println().outdent().println("};");
    }

    private void generateMethodLocations() {
        if (methodLocations.isEmpty()) {
            return;
        }

        includes.includeClass(METHOD_LOCATION);
        writer.print("static ").print(methodLocationName).print(" methodLocations_" + callSitesName
                + "[" + methodLocations.size() + "] = {").indent();

        String fileNameName = fieldName(METHOD_LOCATION, "fileName");
        String classNameName = fieldName(METHOD_LOCATION, "className");
        String methodNameName = fieldName(METHOD_LOCATION, "methodName");

        boolean first = true;
        for (MethodLocation location : methodLocations) {
            if (!first) {
                writer.print(",");
            }
            first = false;

            writer.println().print("{ ");
            writer.print(".").print(fileNameName).print(" = ")
                    .print(getStringExpr(location.file)).print(", ");
            writer.print(".").print(classNameName).print(" = ")
                    .print(getStringExpr(location.className)).print(", ");
            writer.print(".").print(methodNameName).print(" = ")
                    .print(getStringExpr(location.methodName));

            writer.print(" }");
        }

        writer.println().outdent().println("};");
    }

    private void generateHandlers() {
        if (exceptionHandlers.isEmpty()) {
            return;
        }
        includes.includeClass(EXCEPTION_HANDLER);
        String name = "exceptionHandlers_" + callSitesName;
        writer.print("static ").print(exceptionHandlerName).print(" " + name + "["
                + exceptionHandlers.size() + "] = {").indent();

        String idName = fieldName(EXCEPTION_HANDLER, "id");
        String exceptionClassName = fieldName(EXCEPTION_HANDLER, "exceptionClass");
        String nextName = fieldName(EXCEPTION_HANDLER, "next");

        boolean first = true;
        for (HandlerContainer handler : exceptionHandlers) {
            if (!first) {
                writer.print(",");
            }
            first = false;

            writer.println().print("{ ");

            if (handler.descriptor.getClassName() != null) {
                includes.includeClass(handler.descriptor.getClassName());
            }
            String classExpr = handler.descriptor.getClassName() != null
                    ? "&" + context.getNames().forClassInstance(ValueType.object(handler.descriptor.getClassName()))
                    : "NULL";
            writer.print(".").print(idName).print(" = ").print(String.valueOf(handler.descriptor.getId())).print(", ");
            writer.print(".").print(exceptionClassName).print(" = ").print(classExpr).print(", ");
            writer.print(".").print(nextName).print(" = ")
                    .print(handler.nextIndex < 0 ? "NULL" : name + "+" + handler.nextIndex);

            writer.print(" }");
        }

        writer.println().outdent().println("};");
    }

    private String fieldName(String className, String fieldName) {
        return context.getNames().forMemberField(new FieldReference(className, fieldName));
    }

    private String getStringExpr(String s) {
        return s != null ? "&TEAVM_GET_STRING(" + context.getStringPool().getStringIndex(s) + ")" : "NULL";
    }

    static class HandlerContainer {
        ExceptionHandlerDescriptor descriptor;
        int nextIndex = -1;

        HandlerContainer(ExceptionHandlerDescriptor descriptor) {
            this.descriptor = descriptor;
        }
    }

    final static class MethodLocation {
        final String file;
        final String className;
        final String methodName;

        MethodLocation(String file, String className, String methodName) {
            this.file = file;
            this.className = className;
            this.methodName = methodName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MethodLocation)) {
                return false;
            }
            MethodLocation that = (MethodLocation) o;
            return Objects.equals(file, that.file)
                    && Objects.equals(className, that.className)
                    && Objects.equals(methodName, that.methodName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(file, className, methodName);
        }
    }
}
