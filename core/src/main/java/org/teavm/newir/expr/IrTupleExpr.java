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

import java.util.Objects;

public abstract class IrTupleExpr extends IrExpr {
    IrTupleType typeCache;

    IrTupleExpr() {
    }

    public static IrTupleExpr of(IrExpr... components) {
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
    public final IrType getType() {
        if (typeCache == null) {
            IrType[] componentTypes = new IrType[getInputCount()];
            for (int i = 0; i < getInputCount(); ++i) {
                componentTypes[i] = getInput(i).getType();
            }
            typeCache = IrTupleType.of(componentTypes);
        }
        return typeCache;
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeCache);
    }

    @Override
    public final void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }

    static class Impl1 extends IrTupleExpr {
        private IrExpr c1;

        Impl1(IrExpr c1) {
            this.c1 = c1;
        }

        @Override
        public int getInputCount() {
            return 1;
        }

        @Override
        public IrExpr getInput(int index) {
            if (index == 0) {
                return c1;
            }
            return super.getInput(index);
        }

        @Override
        public void setInput(int index, IrExpr value) {
            if (index == 0) {
                c1 = value;
                typeCache = null;
            } else {
                super.setInput(index, value);
            }
        }
    }

    static class Impl2 extends IrTupleExpr {
        private IrExpr c1;
        private IrExpr c2;

        Impl2(IrExpr c1, IrExpr c2) {
            this.c1 = c1;
            this.c2 = c2;
        }

        @Override
        public int getInputCount() {
            return 2;
        }

        @Override
        public IrExpr getInput(int index) {
            switch (index) {
                case 0:
                    return c1;
                case 1:
                    return c2;
                default:
                    return super.getInput(index);
            }
        }

        @Override
        public void setInput(int index, IrExpr value) {
            switch (index) {
                case 0:
                    c1 = value;
                    typeCache = null;
                    break;
                case 1:
                    c2 = value;
                    typeCache = null;
                    break;
                default:
                    super.setInput(index, value);
            }
        }
    }

    static class Impl3 extends IrTupleExpr {
        private IrExpr c1;
        private IrExpr c2;
        private IrExpr c3;

        Impl3(IrExpr c1, IrExpr c2, IrExpr c3) {
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
        }

        @Override
        public int getInputCount() {
            return 3;
        }

        @Override
        public IrExpr getInput(int index) {
            switch (index) {
                case 0:
                    return c1;
                case 1:
                    return c2;
                case 2:
                    return c3;
                default:
                    return super.getInput(index);
            }
        }

        @Override
        public void setInput(int index, IrExpr value) {
            switch (index) {
                case 0:
                    c1 = value;
                    typeCache = null;
                    break;
                case 1:
                    c2 = value;
                    typeCache = null;
                    break;
                case 2:
                    c3 = value;
                    typeCache = null;
                    break;
                default:
                    super.setInput(index, value);
            }
        }
    }

    static class Impl4 extends IrTupleExpr {
        private IrExpr c1;
        private IrExpr c2;
        private IrExpr c3;
        private IrExpr c4;

        Impl4(IrExpr c1, IrExpr c2, IrExpr c3, IrExpr c4) {
            this.c1 = c1;
            this.c2 = c2;
            this.c3 = c3;
            this.c4 = c4;
        }

        @Override
        public int getInputCount() {
            return 4;
        }

        @Override
        public IrExpr getInput(int index) {
            switch (index) {
                case 0:
                    return c1;
                case 1:
                    return c2;
                case 2:
                    return c3;
                case 3:
                    return c4;
                default:
                    return super.getInput(index);
            }
        }

        @Override
        public void setInput(int index, IrExpr value) {
            switch (index) {
                case 0:
                    c1 = value;
                    typeCache = null;
                    break;
                case 1:
                    c2 = value;
                    typeCache = null;
                    break;
                case 2:
                    c3 = value;
                    typeCache = null;
                    break;
                case 3:
                    c4 = value;
                    typeCache = null;
                    break;
                default:
                    super.setInput(index, value);
            }
        }
    }

    static class ImplN extends IrTupleExpr {
        private final IrExpr[] components;

        ImplN(IrExpr[] components) {
            this.components = components;
        }

        @Override
        public int getInputCount() {
            return components.length;
        }

        @Override
        public IrExpr getInput(int index) {
            return components[index];
        }

        @Override
        public void setInput(int index, IrExpr value) {
            components[index] = value;
            typeCache = null;
        }
    }
}
