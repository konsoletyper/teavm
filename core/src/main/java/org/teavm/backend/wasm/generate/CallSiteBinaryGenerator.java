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

import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.teavm.backend.wasm.binary.BinaryWriter;
import org.teavm.backend.wasm.binary.DataPrimitives;
import org.teavm.backend.wasm.binary.DataStructure;
import org.teavm.backend.wasm.binary.DataValue;
import org.teavm.model.ValueType;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.CallSiteLocation;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;

public class CallSiteBinaryGenerator {
    private static final int CALL_SITE_FIRST_HANDLER = 0;
    private static final int CALL_SITE_LOCATION = 1;
    private static final int EXCEPTION_HANDLER_ID = 0;
    private static final int EXCEPTION_HANDLER_CLASS = 1;
    private static final int EXCEPTION_HANDLER_NEXT = 2;
    private static final int LOCATION_METHOD = 0;
    private static final int LOCATION_LINE = 1;
    private static final int LOCATION_NEXT = 2;
    private static final int METHOD_LOCATION_FILE = 0;
    private static final int METHOD_LOCATION_CLASS = 1;
    private static final int METHOD_LOCATION_METHOD = 2;

    private DataStructure callSiteStructure = new DataStructure((byte) 0,
            DataPrimitives.ADDRESS,
            DataPrimitives.ADDRESS);
    private DataStructure exceptionHandlerStructure = new DataStructure((byte) 0,
            DataPrimitives.INT,
            DataPrimitives.ADDRESS,
            DataPrimitives.ADDRESS);
    private DataStructure locationStructure = new DataStructure((byte) 0,
            DataPrimitives.ADDRESS,
            DataPrimitives.INT,
            DataPrimitives.ADDRESS);
    private DataStructure methodLocationStructure = new DataStructure((byte) 0,
            DataPrimitives.ADDRESS,
            DataPrimitives.ADDRESS,
            DataPrimitives.ADDRESS);

    private BinaryWriter writer;
    private WasmClassGenerator classGenerator;
    private WasmStringPool stringPool;
    private ObjectIntMap<String> stringIndirectPointerCache = new ObjectIntHashMap<>();
    private boolean obfuscated;

    public CallSiteBinaryGenerator(BinaryWriter writer, WasmClassGenerator classGenerator, WasmStringPool stringPool,
            boolean obfuscated) {
        this.writer = writer;
        this.classGenerator = classGenerator;
        this.stringPool = stringPool;
        this.obfuscated = obfuscated;
    }

    public int writeCallSites(List<? extends CallSiteDescriptor> callSites) {
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

        ObjectIntMap<LocationList> locationCache = new ObjectIntHashMap<>();
        ObjectIntMap<MethodLocation> methodLocationCache = new ObjectIntHashMap<>();

        for (int callSiteId = 0; callSiteId < callSites.size(); ++callSiteId) {
            DataValue binaryCallSite = binaryCallSites.get(callSiteId);
            CallSiteDescriptor callSite = callSites.get(callSiteId);

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
                if (i > 0) {
                    binaryExceptionHandlers.get(i - 1).setAddress(EXCEPTION_HANDLER_NEXT, address);
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

            if (!obfuscated) {
                binaryCallSite.setAddress(CALL_SITE_LOCATION,
                        generateLocations(methodLocationCache, locationCache, callSite));
            }
        }

        return firstCallSite;
    }

    private int generateLocations(ObjectIntMap<MethodLocation> methodLocationCache,
            ObjectIntMap<LocationList> locationCache, CallSiteDescriptor callSite) {
        CallSiteLocation[] locations = callSite.getLocations();
        LocationList prevList = null;
        int locationAddress = 0;
        int previousLocationAddress = 0;
        for (int i = locations.length - 1; i >= 0; --i) {
            LocationList list = new LocationList(locations[i], prevList);
            locationAddress = locationCache.getOrDefault(list, 0);
            if (locationAddress == 0) {
                DataValue binaryLocation = locationStructure.createValue();
                locationAddress = writer.append(binaryLocation);
                locationCache.put(list, locationAddress);
                CallSiteLocation location = list.location;
                MethodLocation methodLocation = new MethodLocation(location.getFileName(),
                        location.getClassName(), location.getMethodName());
                int methodLocationAddress = methodLocationCache.getOrDefault(methodLocation, -1);
                if (methodLocationAddress < 0) {
                    DataValue binaryMethodLocation = methodLocationStructure.createValue();
                    methodLocationAddress = writer.append(binaryMethodLocation);
                    methodLocationCache.put(methodLocation, methodLocationAddress);
                    if (location.getFileName() != null) {
                        binaryMethodLocation.setAddress(METHOD_LOCATION_FILE,
                                getStringIndirectPointer(location.getFileName()));
                    }
                    if (location.getClassName() != null) {
                        binaryMethodLocation.setAddress(METHOD_LOCATION_CLASS,
                                getStringIndirectPointer(location.getClassName()));
                    }
                    if (location.getMethodName() != null) {
                        binaryMethodLocation.setAddress(METHOD_LOCATION_METHOD,
                                getStringIndirectPointer(location.getMethodName()));
                    }
                }

                binaryLocation.setAddress(LOCATION_METHOD, methodLocationAddress);
                binaryLocation.setInt(LOCATION_LINE, location.getLineNumber());
                binaryLocation.setAddress(LOCATION_NEXT, previousLocationAddress);
            }
            previousLocationAddress = locationAddress;
        }

        return locationAddress;
    }

    private int getStringIndirectPointer(String str) {
        int result = stringIndirectPointerCache.getOrDefault(str, -1);
        if (result < 0) {
            DataValue indirectValue = DataPrimitives.ADDRESS.createValue();
            result = writer.append(indirectValue);
            indirectValue.setAddress(0, stringPool.getStringPointer(str));
        }
        return result;
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
