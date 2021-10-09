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
package org.teavm.newir.type;

public abstract class IrTupleType extends IrType {
    IrTupleType() {
    }

    public static IrTupleType of(IrType... components) {
        switch (components.length) {
            case 0:
                throw new IllegalArgumentException("Should be at least 1 component");
            case 1:
                return new Impl1(components[0]);
            case 2:
                return new Impl2(components[0], components[1]);
            case 3:
                return new Impl3(components[0], components[1], components[2]);
            case 4:
                return new Impl4(components[0], components[1], components[2], components[3]);
            default:
                return new ImplN(components.clone());
        }
    }

    @Override
    public final IrTypeKind getKind() {
        return IrTypeKind.TUPLE;
    }

    public abstract int getComponentCount();

    public IrType getComponent(int index) {
        throw new IndexOutOfBoundsException();
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof IrTupleType)) {
            return false;
        }
        IrTupleType that = (IrTupleType) obj;
        int count = getComponentCount();
        if (that.getComponentCount() != count) {
            return false;
        }
        for (int i = 0; i < count; ++i) {
            if (!getComponent(i).equals(that.getComponent(i))) {
                return false;
            }
        }
        return true;
    }

    static final class Impl1 extends IrTupleType {
        private final IrType t1;

        Impl1(IrType t1) {
            this.t1 = t1;
        }

        @Override
        public int getComponentCount() {
            return 1;
        }

        @Override
        public IrType getComponent(int index) {
            if (index == 0) {
                return t1;
            }
            return super.getComponent(index);
        }
    }

    static final class Impl2 extends IrTupleType {
        private final IrType t1;
        private final IrType t2;

        Impl2(IrType t1, IrType t2) {
            this.t1 = t1;
            this.t2 = t2;
        }

        @Override
        public int getComponentCount() {
            return 2;
        }

        @Override
        public IrType getComponent(int index) {
            switch (index) {
                case 0:
                    return t1;
                case 1:
                    return t2;
                default:
                    return super.getComponent(index);
            }
        }
    }

    static final class Impl3 extends IrTupleType {
        private final IrType t1;
        private final IrType t2;
        private final IrType t3;

        Impl3(IrType t1, IrType t2, IrType t3) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
        }

        @Override
        public int getComponentCount() {
            return 3;
        }

        @Override
        public IrType getComponent(int index) {
            switch (index) {
                case 0:
                    return t1;
                case 1:
                    return t2;
                case 2:
                    return t3;
                default:
                    return super.getComponent(index);
            }
        }
    }

    static final class Impl4 extends IrTupleType {
        private final IrType t1;
        private final IrType t2;
        private final IrType t3;
        private final IrType t4;

        Impl4(IrType t1, IrType t2, IrType t3, IrType t4) {
            this.t1 = t1;
            this.t2 = t2;
            this.t3 = t3;
            this.t4 = t4;
        }

        @Override
        public int getComponentCount() {
            return 4;
        }

        @Override
        public IrType getComponent(int index) {
            switch (index) {
                case 0:
                    return t1;
                case 1:
                    return t2;
                case 2:
                    return t3;
                case 3:
                    return t4;
                default:
                    return super.getComponent(index);
            }
        }
    }

    static final class ImplN extends IrTupleType {
        private final IrType[] components;

        ImplN(IrType[] components) {
            this.components = components;
        }

        @Override
        public int getComponentCount() {
            return components.length;
        }

        @Override
        public IrType getComponent(int index) {
            return components[index];
        }
    }
}
