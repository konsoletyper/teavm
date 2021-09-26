/*
 *  Copyright 2021 konsoletyper.
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
package org.teavm.newir.expr;

import org.teavm.model.FieldReference;
import org.teavm.model.ValueType;

public final class IrGetFieldExpr extends IrSingeInputExpr {
    private FieldReference field;
    private ValueType fieldType;

    public IrGetFieldExpr(IrExpr argument, FieldReference field, ValueType fieldType) {
        super(argument);
        this.field = field;
        this.fieldType = fieldType;
    }

    public FieldReference getField() {
        return field;
    }

    public void setField(FieldReference field) {
        this.field = field;
    }

    public ValueType getFieldType() {
        return fieldType;
    }

    public void setFieldType(ValueType fieldType) {
        this.fieldType = fieldType;
    }

    @Override
    public IrType getType() {
        return IrType.fromValueType(fieldType);
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }
}
