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
package org.teavm.backend.wasm.model.expression;

import org.teavm.backend.wasm.model.WasmType;
import org.teavm.model.TextLocation;

public abstract class WasmExpression {
    private TextLocation location;

    WasmExpression() {
    }

    public TextLocation getLocation() {
        return location;
    }

    public void setLocation(TextLocation location) {
        this.location = location;
    }

    public abstract void acceptVisitor(WasmExpressionVisitor visitor);

    public boolean isTerminating() {
        return false;
    }

    public static WasmExpression defaultValueOfType(WasmType type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case INT32:
                return new WasmInt32Constant(0);
            case INT64:
                return new WasmInt64Constant(0);
            case FLOAT32:
                return new WasmFloat32Constant(0);
            case FLOAT64:
                return new WasmFloat64Constant(0);
            default:
                throw new IllegalArgumentException();
        }
    }
}
