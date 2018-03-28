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
import org.teavm.model.FieldReference;
import org.teavm.model.ValueType;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.CallSiteLocation;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;

public class CallSiteGenerator {
    private static final String CALL_SITE = "org.teavm.runtime.CallSite";
    private static final String CALL_SITE_LOCATION = "org.teavm.runtime.CallSiteLocation";
    private static final String EXCEPTION_HANDLER = "org.teavm.runtime.ExceptionHandler";

    private GenerationContext context;
    private CodeWriter writer;
    private ObjectIntMap<CallSiteLocation> locationMap = new ObjectIntHashMap<>();
    private List<CallSiteLocation> locations = new ArrayList<>();
    private List<ExceptionHandlerDescriptor> exceptionHandlers = new ArrayList<>();
    private String callSiteLocationName;
    private String exceptionHandlerName;

    public CallSiteGenerator(GenerationContext context, CodeWriter writer) {
        this.context = context;
        this.writer = writer;
        callSiteLocationName = context.getNames().forClass(CALL_SITE_LOCATION);
        exceptionHandlerName = context.getNames().forClass(EXCEPTION_HANDLER);
    }

    public void generate(List<CallSiteDescriptor> callSites) {
        CodeWriter writerForLocations = writer.fragment();
        generateCallSites(callSites);

        CodeWriter oldWriter = writer;
        writer = writerForLocations;
        generateLocations();
        generateHandlers();
        writer = oldWriter;
    }

    private void generateCallSites(List<CallSiteDescriptor> callSites) {
        String callSiteName = context.getNames().forClass(CALL_SITE);

        writer.print("static ").print(callSiteName).print(" callSites[" + callSites.size() + "] = {").indent();
        String handlerCountName = fieldName(CALL_SITE, "handlerCount");
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
                    ? "exceptionHandlers + " + exceptionHandlers.size()
                    : "NULL";
            writer.println().print("{ ");
            writer.print(".").print(handlerCountName).print(" = ")
                    .print(String.valueOf(callSite.getHandlers().size())).print(", ");
            writer.print(".").print(firstHandlerName).print(" = ").print(firstHandlerExpr).print(", ");
            writer.print(".").print(locationName).print(" = ")
                    .print(locationIndex >= 0 ? "callSiteLocations + " + locationIndex : "NULL");
            writer.print(" }");

            exceptionHandlers.addAll(callSite.getHandlers());
        }

        writer.println().outdent().println("};");
    }

    private void generateLocations() {
        writer.print("static ").print(callSiteLocationName).print(" callSiteLocations[" + locations.size() + "] = {")
                .indent();

        String fileNameName = fieldName(CALL_SITE_LOCATION, "fileName");
        String classNameName = fieldName(CALL_SITE_LOCATION, "className");
        String methodNameName = fieldName(CALL_SITE_LOCATION, "methodName");
        String lineNumberName = fieldName(CALL_SITE_LOCATION, "lineNumber");

        boolean first = true;
        for (CallSiteLocation location : locations) {
            if (!first) {
                writer.print(",");
            }
            first = false;

            writer.println().print("{ ");
            writer.print(".").print(fileNameName).print(" = ")
                    .print(getStringExpr(location.getFileName())).print(", ");
            writer.print(".").print(classNameName).print(" = ")
                    .print(getStringExpr(location.getClassName())).print(", ");
            writer.print(".").print(methodNameName).print(" = ")
                    .print(getStringExpr(location.getMethodName())).print(", ");
            writer.print(".").print(lineNumberName).print(" = ")
                    .print(String.valueOf(location.getLineNumber()));

            writer.print(" }");
        }

        writer.println().outdent().println("};");
    }

    private void generateHandlers() {
        writer.print("static ").print(exceptionHandlerName).print(" exceptionHandlers[" + exceptionHandlers.size()
                + "] = {").indent();

        String idName = fieldName(EXCEPTION_HANDLER, "id");
        String exceptionClassName = fieldName(EXCEPTION_HANDLER, "exceptionClass");

        boolean first = true;
        for (ExceptionHandlerDescriptor handler : exceptionHandlers) {
            if (!first) {
                writer.print(",");
            }
            first = false;

            writer.println().print("{ ");

            String classExpr = handler.getClassName() != null
                    ? "&" + context.getNames().forClassInstance(ValueType.object(handler.getClassName()))
                    : "NULL";
            writer.print(".").print(idName).print(" = ").print(String.valueOf(handler.getId())).print(",");
            writer.print(".").print(exceptionClassName).print(" = ").print(classExpr);

            writer.print(" }");
        }

        writer.println().outdent().println("};");
    }

    private String fieldName(String className, String fieldName) {
        return context.getNames().forMemberField(new FieldReference(className, fieldName));
    }

    private String getStringExpr(String s) {
        return s != null ? "stringPool + " + context.getStringPool().getStringIndex(s) : "NULL";
    }
}
