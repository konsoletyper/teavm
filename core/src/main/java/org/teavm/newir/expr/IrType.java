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

import org.teavm.model.ValueType;

public abstract class IrType {
    public static IrType BOOLEAN = new SimpleImpl(IrTypeKind.BOOLEAN);
    public static IrType BYTE = new SimpleImpl(IrTypeKind.BYTE);
    public static IrType SHORT = new SimpleImpl(IrTypeKind.SHORT);
    public static IrType CHAR = new SimpleImpl(IrTypeKind.CHAR);
    public static IrType INT = new SimpleImpl(IrTypeKind.INT);
    public static IrType LONG = new SimpleImpl(IrTypeKind.LONG);
    public static IrType FLOAT = new SimpleImpl(IrTypeKind.FLOAT);
    public static IrType DOUBLE = new SimpleImpl(IrTypeKind.DOUBLE);
    public static IrType OBJECT = new SimpleImpl(IrTypeKind.OBJECT);
    public static IrType VOID = new SimpleImpl(IrTypeKind.VOID);
    public static IrType UNREACHABLE = new SimpleImpl(IrTypeKind.UNREACHABLE);

    public static IrTupleType tuple(IrType... components) {
        return IrTupleType.of(components);
    }

    public abstract IrTypeKind getKind();

    public boolean isSubtypeOf(IrType type) {
        return getKind() == IrTypeKind.UNREACHABLE || equals(type);
    }

    public static IrType fromValueType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    return IrType.BOOLEAN;
                case BYTE:
                    return IrType.BYTE;
                case SHORT:
                    return IrType.SHORT;
                case CHARACTER:
                    return IrType.CHAR;
                case INTEGER:
                    return IrType.INT;
                case LONG:
                    return IrType.LONG;
                case FLOAT:
                    return IrType.FLOAT;
                case DOUBLE:
                    return IrType.DOUBLE;
                default:
                    throw new IllegalArgumentException();
            }
        } else if (type instanceof ValueType.Void) {
            return IrType.VOID;
        } else {
            return IrType.OBJECT;
        }
    }

    static class SimpleImpl extends IrType {
        private IrTypeKind kind;

        private SimpleImpl(IrTypeKind kind) {
            this.kind = kind;
        }

        @Override
        public IrTypeKind getKind() {
            return kind;
        }
    }
}
