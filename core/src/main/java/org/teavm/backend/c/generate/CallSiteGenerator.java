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
import org.teavm.model.ValueType;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.CallSiteLocation;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;

public class CallSiteGenerator {
    private GenerationContext context;
    private CodeWriter writer;
    private IncludeManager includes;
    private ObjectIntMap<LocationList> locationMap = new ObjectIntHashMap<>();
    private List<LocationList> locations = new ArrayList<>();
    private List<HandlerContainer> exceptionHandlers = new ArrayList<>();
    private ObjectIntMap<MethodLocation> methodLocationMap = new ObjectIntHashMap<>();
    private List<MethodLocation> methodLocations = new ArrayList<>();
    private String callSitesName;
    private boolean isStatic;

    public CallSiteGenerator(GenerationContext context, CodeWriter writer, IncludeManager includes,
            String callSitesName) {
        this.context = context;
        this.writer = writer;
        this.includes = includes;
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
        includes.includePath("strings.h");
        if (isStatic) {
            writer.print("static ");
        }
        writer.print("TeaVM_CallSite " + callSitesName + "[" + callSites.size() + "] = {").indent();

        boolean first = true;
        for (CallSiteDescriptor callSite : callSites) {
            if (!first) {
                writer.print(", ");
            }
            first = false;

            int locationIndex = -1;
            if (!context.isObfuscated()) {
                CallSiteLocation[] locations = callSite.getLocations();
                if (locations != null && locations.length > 0) {
                    LocationList prevList = null;
                    for (int i = locations.length - 1; i >= 0; --i) {
                        LocationList list = new LocationList(locations[i], prevList);
                        locationIndex = locationMap.getOrDefault(list, -1);
                        if (locationIndex < 0) {
                            locationIndex = this.locations.size();
                            locationMap.put(list, locationIndex);
                            this.locations.add(list);
                        } else {
                            list = this.locations.get(locationIndex);
                        }
                        prevList = list;
                    }
                }
            }

            String firstHandlerExpr = !callSite.getHandlers().isEmpty()
                    ? "exceptionHandlers_" + callSitesName + " + " + exceptionHandlers.size()
                    : "NULL";
            writer.println().print("{ ");
            writer.print(".firstHandler = ").print(firstHandlerExpr).print(", ");
            writer.print(".location = ")
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
        writer.print("static TeaVM_CallSiteLocation callSiteLocations_" + callSitesName
                + "[" + locations.size() + "] = {").indent();

        boolean first = true;
        for (LocationList locationList : locations) {
            if (!first) {
                writer.print(",");
            }
            first = false;

            CallSiteLocation location = locationList.location;

            writer.println().print("{ ");
            MethodLocation methodLocation = new MethodLocation(location.getFileName(), location.getClassName(),
                    location.getMethodName());
            int index = methodLocationMap.getOrDefault(methodLocation, -1);
            if (index < 0) {
                index = methodLocations.size();
                methodLocationMap.put(methodLocation, index);
                methodLocations.add(methodLocation);
            }
            writer.print(".method = ").print("methodLocations_" + callSitesName + " + " + index)
                    .print(", ");
            writer.print(".lineNumber = ").print(String.valueOf(location.getLineNumber()));

            if (locationList.next != null) {
                int nextIndex = locationMap.get(locationList.next);
                writer.print(", .next = callSiteLocations_" + callSitesName + " + " + nextIndex);
            }

            writer.print(" }");
        }

        writer.println().outdent().println("};");
    }

    private void generateMethodLocations() {
        if (methodLocations.isEmpty()) {
            return;
        }

        writer.print("static TeaVM_MethodLocation methodLocations_" + callSitesName
                + "[" + methodLocations.size() + "] = {").indent();

        boolean first = true;
        for (MethodLocation location : methodLocations) {
            if (!first) {
                writer.print(",");
            }
            first = false;

            writer.println().print("{ ");
            String fileName = location.file != null && !location.file.isEmpty() ? location.file : null;
            writer.print(".fileName = ").print(getStringExpr(fileName)).print(", ");
            writer.print(".className = ").print(getStringExpr(location.className)).print(", ");
            writer.print(".methodName = ").print(getStringExpr(location.methodName));

            writer.print(" }");
        }

        writer.println().outdent().println("};");
    }

    private void generateHandlers() {
        if (exceptionHandlers.isEmpty()) {
            return;
        }
        String name = "exceptionHandlers_" + callSitesName;
        writer.print("static TeaVM_ExceptionHandler " + name + "["
                + exceptionHandlers.size() + "] = {").indent();

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
                    ? "(TeaVM_Class*) &"
                        + context.getNames().forClassInstance(ValueType.object(handler.descriptor.getClassName()))
                    : "NULL";
            writer.print(".id = ").print(String.valueOf(handler.descriptor.getId())).print(", ");
            writer.print(".exceptionClass = ").print(classExpr).print(", ");
            writer.print(".next = ").print(handler.nextIndex < 0 ? "NULL" : name + "+" + handler.nextIndex);

            writer.print(" }");
        }

        writer.println().outdent().println("};");
    }

    private String getStringExpr(String s) {
        return s != null ? "TEAVM_GET_STRING_ADDRESS(" + context.getStringPool().getStringIndex(s) + ")" : "NULL";
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

    final static class LocationList {
        final CallSiteLocation location;
        final LocationList next;

        LocationList(CallSiteLocation location, LocationList next) {
            this.location = location;
            this.next = next;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof LocationList)) {
                return false;
            }
            LocationList that = (LocationList) o;
            return location.equals(that.location) && Objects.equals(next, that.next);
        }

        @Override
        public int hashCode() {
            return Objects.hash(location, next);
        }
    }
}
