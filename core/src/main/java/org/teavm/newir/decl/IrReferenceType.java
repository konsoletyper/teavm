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
package org.teavm.newir.decl;

import org.teavm.newir.type.IrType;

public abstract class IrReferenceType {
    public static final IrReferenceType BOOLEAN_ARRAY = new SimpleImpl(IrReferenceTypeKind.BOOLEAN_ARRAY,
            IrType.BOOLEAN);
    public static final IrReferenceType BYTE_ARRAY = new SimpleImpl(IrReferenceTypeKind.BYTE_ARRAY, IrType.BYTE);
    public static final IrReferenceType CHAR_ARRAY = new SimpleImpl(IrReferenceTypeKind.CHAR_ARRAY, IrType.CHAR);
    public static final IrReferenceType SHORT_ARRAY = new SimpleImpl(IrReferenceTypeKind.SHORT_ARRAY, IrType.SHORT);
    public static final IrReferenceType INT_ARRAY = new SimpleImpl(IrReferenceTypeKind.INT_ARRAY, IrType.INT);
    public static final IrReferenceType LONG_ARRAY = new SimpleImpl(IrReferenceTypeKind.LONG_ARRAY, IrType.LONG);
    public static final IrReferenceType FLOAT_ARRAY = new SimpleImpl(IrReferenceTypeKind.FLOAT_ARRAY, IrType.FLOAT);
    public static final IrReferenceType DOUBLE_ARRAY = new SimpleImpl(IrReferenceTypeKind.DOUBLE_ARRAY, IrType.DOUBLE);

    private IrReferenceArrayType arrayType;

    IrReferenceType() {
    }

    public abstract IrReferenceTypeKind getKind();

    public final IrReferenceArrayType arrayType() {
        if (arrayType == null) {
            arrayType = new IrReferenceArrayType(this);
        }
        return arrayType;
    }

    static final class SimpleImpl extends IrArrayType {
        private final IrReferenceTypeKind kind;
        private final IrType elementExprType;

        SimpleImpl(IrReferenceTypeKind kind, IrType exprElementType) {
            this.kind = kind;
            this.elementExprType = exprElementType;
        }

        @Override
        public IrReferenceTypeKind getKind() {
            return kind;
        }

        @Override
        public IrType getElementExprType() {
            return elementExprType;
        }
    }
}
