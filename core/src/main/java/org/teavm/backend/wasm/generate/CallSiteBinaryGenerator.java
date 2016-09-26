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
import java.util.List;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.binary.DataStructure;
import org.teavm.backend.wasm.binary.DataValue;
import org.teavm.model.ValueType;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;

public class CallSiteBinaryGenerator {
    private static final int CALL_SITE_HANDLER_COUNT = 0;
    private static final int CALL_SITE_FIRST_HANDLER = 1;
    private static final int EXCEPTION_HANDLER_ID = 0;
    private static final int EXCEPTION_HANDLER_CLASS = 1;

    private DataStructure callSiteStructure = new DataStructure((byte) 0,
            DataPrimitives.INT,
            DataPrimitives.ADDRESS);
    private DataStructure exceptionHandlerStructure = new DataStructure((byte) 0,
            DataPrimitives.INT,
            DataPrimitives.ADDRESS);

    private BinaryWriter writer;
    private WasmClassGenerator classGenerator;

    public CallSiteBinaryGenerator(BinaryWriter writer, WasmClassGenerator classGenerator) {
        this.writer = writer;
        this.classGenerator = classGenerator;
    }

    public int writeCallSites(List<CallSiteDescriptor> callSites) {
        if (callSites.isEmpty()) {
            return 0;
        }

        int firstCallSite = -1;
        List<DataValue> binaryCallSites = new ArrayList<>();
        for (CallSiteDescriptor callSite : callSites) {
            DataValue binaryCallSite = callSiteStructure.createValue();
            int address = writer.append(binaryCallSite);
            if (firstCallSite < 0) {
                firstCallSite = address;
            }
            binaryCallSites.add(binaryCallSite);
        }

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
        }

        return firstCallSite;
    }
}
