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

import org.teavm.newir.type.IrType;

public abstract class IrCallExpr extends IrExpr {
    private IrCallTarget<?> target;

    IrCallExpr(IrCallTarget<?> target) {
        this.target = target;
    }

    public static IrCallExpr of(IrCallTarget<?> target, IrExpr... arguments) {
        if (target.getCallable().getParameterCount() != arguments.length) {
            throw new IllegalArgumentException("Expected " + target.getCallable().getParameterCount()
                    + " parameters but got " + arguments.length + " arguments");
        }
        switch (arguments.length) {
            case 0:
                return new Impl0(target);
            case 1:
                return new Impl1(target, arguments[0]);
            case 2:
                return new Impl2(target, arguments[0], arguments[1]);
            case 3:
                return new Impl3(target, arguments[0], arguments[1], arguments[2]);
            case 4:
                return new Impl4(target, arguments[0], arguments[1], arguments[2], arguments[3]);
            default:
                return new ImplN(target, arguments.clone());
        }
    }

    public IrCallTarget<?> getTarget() {
        return target;
    }

    @Override
    public IrType getType() {
        return target.getCallable().getType();
    }

    @Override
    public IrType getInputType(int index) {
        return target.getCallable().getParameterType(index);
    }

    @Override
    public int getInputCount() {
        return target.getCallable().getParameterCount();
    }

    @Override
    public boolean needsOrdering() {
        return true;
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }

    static final class Impl0 extends IrCallExpr {
        Impl0(IrCallTarget<?> target) {
            super(target);
        }
    }

    static final class Impl1 extends IrCallExpr {
        private IrExpr a1;

        Impl1(IrCallTarget<?> target, IrExpr a1) {
            super(target);
            this.a1 = a1;
        }

        @Override
        public IrExpr getInput(int index) {
            return index == 0 ? a1 : super.getInput(index);
        }

        @Override
        public void setInput(int index, IrExpr value) {
            if (index == 0) {
                a1 = value;
            } else {
                super.setInput(index, value);
            }
        }
    }

    static final class Impl2 extends IrCallExpr {
        private IrExpr a1;
        private IrExpr a2;

        Impl2(IrCallTarget<?> target, IrExpr a1, IrExpr a2) {
            super(target);
            this.a1 = a1;
            this.a2 = a2;
        }

        @Override
        public IrExpr getInput(int index) {
            switch (index) {
                case 0:
                    return a1;
                case 1:
                    return a2;
                default:
                    return super.getInput(index);
            }
        }

        @Override
        public void setInput(int index, IrExpr value) {
            switch (index) {
                case 0:
                    a1 = value;
                    break;
                case 1:
                    a2 = value;
                    break;
                default:
                    super.setInput(index, value);
                    break;
            }
        }
    }

    static final class Impl3 extends IrCallExpr {
        private IrExpr a1;
        private IrExpr a2;
        private IrExpr a3;

        Impl3(IrCallTarget<?> target, IrExpr a1, IrExpr a2, IrExpr a3) {
            super(target);
            this.a1 = a1;
            this.a2 = a2;
            this.a3 = a3;
        }

        @Override
        public int getInputCount() {
            return 3;
        }

        @Override
        public IrExpr getInput(int index) {
            switch (index) {
                case 0:
                    return a1;
                case 1:
                    return a2;
                case 2:
                    return a3;
                default:
                    return super.getInput(index);
            }
        }

        @Override
        public void setInput(int index, IrExpr value) {
            switch (index) {
                case 0:
                    a1 = value;
                    break;
                case 1:
                    a2 = value;
                    break;
                case 2:
                    a3 = value;
                    break;
                default:
                    super.setInput(index, value);
                    break;
            }
        }
    }

    static final class Impl4 extends IrCallExpr {
        private IrExpr a1;
        private IrExpr a2;
        private IrExpr a3;
        private IrExpr a4;

        Impl4(IrCallTarget<?> target, IrExpr a1, IrExpr a2, IrExpr a3, IrExpr a4) {
            super(target);
            this.a1 = a1;
            this.a2 = a2;
            this.a3 = a3;
            this.a4 = a4;
        }

        @Override
        public int getInputCount() {
            return 4;
        }

        @Override
        public IrExpr getInput(int index) {
            switch (index) {
                case 0:
                    return a1;
                case 1:
                    return a2;
                case 2:
                    return a3;
                case 3:
                    return a4;
                default:
                    return super.getInput(index);
            }
        }

        @Override
        public void setInput(int index, IrExpr value) {
            switch (index) {
                case 0:
                    a1 = value;
                    break;
                case 1:
                    a2 = value;
                    break;
                case 2:
                    a3 = value;
                    break;
                case 3:
                    a4 = value;
                    break;
                default:
                    super.setInput(index, value);
                    break;
            }
        }
    }

    static final class ImplN extends IrCallExpr {
        private final IrExpr[] arguments;

        ImplN(IrCallTarget<?> target, IrExpr[] arguments) {
            super(target);
            this.arguments = arguments;
        }

        @Override
        public int getInputCount() {
            return arguments.length;
        }

        @Override
        public IrExpr getInput(int index) {
            return arguments[index];
        }

        @Override
        public void setInput(int index, IrExpr value) {
            arguments[index] = value;
        }
    }
}
