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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.binary.DataStructure;
import org.teavm.backend.wasm.binary.DataValue;
import org.teavm.model.ValueType;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.CallSiteLocation;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;

public class CallSiteBinaryGenerator {
    private static final int CALL_SITE_HANDLER_COUNT = 0;
    private static final int CALL_SITE_FIRST_HANDLER = 1;
    private static final int CALL_SITE_LOCATION = 2;
    private static final int EXCEPTION_HANDLER_ID = 0;
    private static final int EXCEPTION_HANDLER_CLASS = 1;
    private static final int LOCATION_FILE = 0;
    private static final int LOCATION_CLASS = 1;
    private static final int LOCATION_METHOD = 2;
    private static final int LOCATION_LINE_NUMBER = 3;

    private DataStructure callSiteStructure = new DataStructure((byte) 0,
            DataPrimitives.INT,
            DataPrimitives.ADDRESS,
            DataPrimitives.ADDRESS);
    private DataStructure exceptionHandlerStructure = new DataStructure((byte) 0,
            DataPrimitives.INT,
            DataPrimitives.ADDRESS);
    private DataStructure locationStructure = new DataStructure((byte) 0,
            DataPrimitives.ADDRESS,
            DataPrimitives.ADDRESS,
            DataPrimitives.ADDRESS,
            DataPrimitives.INT);

    private BinaryWriter writer;
    private WasmClassGenerator classGenerator;
    private WasmStringPool stringPool;

    public CallSiteBinaryGenerator(BinaryWriter writer, WasmClassGenerator classGenerator, WasmStringPool stringPool) {
        this.writer = writer;
        this.classGenerator = classGenerator;
        this.stringPool = stringPool;
    }

    public int writeCallSites(List<CallSiteDescriptor> callSites) {
        if (callSites.isEmpty()) {
            return 0;
        }

        int firstCallSite = -1;
        List<DataValue> binaryCallSites = new ArrayList<>();
        for (int i = 0; i < callSites.size(); i++) {
            DataValue binaryCallSite = callSiteStructure.createValue();
            int address = writer.append(binaryCallSite);
            if (firstCallSite < 0) {
                firstCallSite = address;
            }
            binaryCallSites.add(binaryCallSite);
        }

        Map<CallSiteLocation, Integer> locationCache = new HashMap<>();

        for (int callSiteId = 0; callSiteId < callSites.size(); ++callSiteId) {
            DataValue binaryCallSite = binaryCallSites.get(callSiteId);
            CallSiteDescriptor callSite = callSites.get(callSiteId);
            binaryCallSite.setInt(CALL_SITE_HANDLER_COUNT, callSite.getHandlers().size());

            boolean firstHandlerSet = false;
            List<DataValue> binaryExceptionHandlers = new ArrayList<>();
            for (int i = 0; i < callSite.getHandlers().size(); ++i) {
                DataValue binaryExceptionHandler = exceptionHandlerStructure.createValue();
                int address = writer.append(binaryExceptionHandler);
                binaryExceptionHandlers.add(binaryExceptionHandler);

                binaryExceptionHandler.setInt(EXCEPTION_HANDLER_ID, callSiteId + i + 1);

                if (!firstHandlerSet) {
                    binaryCallSite.setAddress(CALL_SITE_FIRST_HANDLER, address);
                    firstHandlerSet = true;
                }
            }

            for (int i = 0; i < callSite.getHandlers().size(); ++i) {
                ExceptionHandlerDescriptor exceptionHandler = callSite.getHandlers().get(i);
                DataValue binaryExceptionHandler = binaryExceptionHandlers.get(i);
                if (exceptionHandler.getClassName() != null) {
                    ValueType type = ValueType.object(exceptionHandler.getClassName());
                    int classPointer = classGenerator.getClassPointer(type);
                    binaryExceptionHandler.setAddress(EXCEPTION_HANDLER_CLASS, classPointer);
                }
            }

            int locationAddress = locationCache.computeIfAbsent(callSite.getLocation(), location -> {
                DataValue binaryLocation = locationStructure.createValue();
                int address = writer.append(binaryLocation);
                if (location.getFileName() != null) {
                    binaryLocation.setAddress(LOCATION_FILE, stringPool.getStringPointer(location.getFileName()));
                }
                if (location.getClassName() != null) {
                    binaryLocation.setAddress(LOCATION_CLASS, stringPool.getStringPointer(location.getClassName()));
                }
                if (location.getMethodName() != null) {
                    binaryLocation.setAddress(LOCATION_METHOD, stringPool.getStringPointer(location.getMethodName()));
                }
                binaryLocation.setInt(LOCATION_LINE_NUMBER, location.getLineNumber());
                return address;
            });
            binaryCallSite.setAddress(CALL_SITE_LOCATION, locationAddress);
        }

        return firstCallSite;
    }
}
