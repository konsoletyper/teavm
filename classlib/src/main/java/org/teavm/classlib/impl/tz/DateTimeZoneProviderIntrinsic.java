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
package org.teavm.classlib.impl.tz;

import java.util.Properties;
import org.teavm.ast.InvocationExpr;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsic;
import org.teavm.backend.wasm.intrinsics.WasmIntrinsicManager;
import org.teavm.backend.wasm.model.expression.WasmExpression;
import org.teavm.backend.wasm.model.expression.WasmInt32Constant;
import org.teavm.model.MethodReference;

public class DateTimeZoneProviderIntrinsic implements Intrinsic, WasmIntrinsic {
    private Properties properties;

    public DateTimeZoneProviderIntrinsic(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean canHandle(MethodReference method) {
        if (!method.getClassName().equals(DateTimeZoneProvider.class.getName())) {
            return false;
        }

        switch (method.getName()) {
            case "timeZoneDetectionEnabled":
            case "getNativeOffset":
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean isApplicable(MethodReference methodReference) {
        if (!methodReference.getClassName().equals(DateTimeZoneProvider.class.getName())) {
            return false;
        }

        switch (methodReference.getName()) {
            case "timeZoneDetectionEnabled":
                return true;
            default:
                return false;
        }
    }

    @Override
    public void apply(IntrinsicContext context, InvocationExpr invocation) {
        switch (invocation.getMethod().getName()) {
            case "timeZoneDetectionEnabled": {
                boolean enabled = properties.getProperty("java.util.TimeZone.autodetect", "false").equals("true");
                context.writer().print(enabled ? "1" : "0");
                break;
            }
            case "getNativeOffset":
                context.writer().print("teavm_timeZoneOffset()");
                break;
        }
    }

    @Override
    public WasmExpression apply(InvocationExpr invocation, WasmIntrinsicManager manager) {
        switch (invocation.getMethod().getName()) {
            case "timeZoneDetectionEnabled": {
                boolean enabled = properties.getProperty("java.util.TimeZone.autodetect", "false").equals("true");
                return new WasmInt32Constant(enabled ? 1 : 0);
            }
            default:
                throw new AssertionError();
        }
    }
}
