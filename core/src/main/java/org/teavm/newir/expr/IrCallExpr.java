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

import org.teavm.model.MethodReference;

public abstract class IrCallExpr extends IrExpr {
    private MethodReference method;
    private IrCallType callType;

    IrCallExpr(MethodReference method, IrCallType callType) {
        this.method = method;
        this.callType = callType;
    }

    public static IrCallExpr of(MethodReference method, IrCallType callType, IrExpr... arguments) {
        switch (arguments.length) {
            case 0:
                return new Impl0(method, callType);
            case 1:
                return new Impl1(method, callType, arguments[0]);
            case 2:
                return new Impl2(method, callType, arguments[0], arguments[1]);
            case 3:
                return new Impl3(method, callType, arguments[0], arguments[1], arguments[2]);
            case 4:
                return new Impl4(method, callType, arguments[0], arguments[1], arguments[2], arguments[3]);
            default:
                return new ImplN(method, callType, arguments.clone());
        }
    }

    public MethodReference getMethod() {
        return method;
    }

    public void setMethod(MethodReference method) {
        this.method = method;
    }

    public IrCallType getCallType() {
        return callType;
    }

    public void setCallType(IrCallType callType) {
        this.callType = callType;
    }

    @Override
    public void acceptVisitor(IrExprVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public IrType getType() {
        return IrType.fromValueType(method.getReturnType());
    }

    static final class Impl0 extends IrCallExpr {
        Impl0(MethodReference method, IrCallType callType) {
            super(method, callType);
        }
    }

    static final class Impl1 extends IrCallExpr {
        private IrExpr a1;

        Impl1(MethodReference method, IrCallType callType, IrExpr a1) {
            super(method, callType);
        }

        @Override
        public int getInputCount() {
            return 1;
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

        Impl2(MethodReference method, IrCallType callType, IrExpr a1, IrExpr a2) {
            super(method, callType);
            this.a1 = a1;
            this.a2 = a2;
        }

        @Override
        public int getInputCount() {
            return 2;
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

        Impl3(MethodReference method, IrCallType callType, IrExpr a1, IrExpr a2, IrExpr a3) {
            super(method, callType);
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

        Impl4(MethodReference method, IrCallType callType, IrExpr a1, IrExpr a2, IrExpr a3, IrExpr a4) {
            super(method, callType);
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

        ImplN(MethodReference method, IrCallType callType, IrExpr[] arguments) {
            super(method, callType);
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
