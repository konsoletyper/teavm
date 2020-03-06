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
package org.teavm.backend.c.generate;

import static org.teavm.model.lowlevel.ExceptionHandlingShadowStackContributor.isManagedMethodCall;
import com.carrotsearch.hppc.IntContainer;
import com.carrotsearch.hppc.IntHashSet;
import com.carrotsearch.hppc.IntSet;
import com.carrotsearch.hppc.ObjectIntHashMap;
import com.carrotsearch.hppc.ObjectIntMap;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.ast.ArrayFromDataExpr;
import org.teavm.ast.ArrayType;
import org.teavm.ast.AssignmentStatement;
import org.teavm.ast.BinaryExpr;
import org.teavm.ast.BlockStatement;
import org.teavm.ast.BoundCheckExpr;
import org.teavm.ast.BreakStatement;
import org.teavm.ast.CastExpr;
import org.teavm.ast.ConditionalExpr;
import org.teavm.ast.ConditionalStatement;
import org.teavm.ast.ConstantExpr;
import org.teavm.ast.ContinueStatement;
import org.teavm.ast.Expr;
import org.teavm.ast.ExprVisitor;
import org.teavm.ast.GotoPartStatement;
import org.teavm.ast.IdentifiedStatement;
import org.teavm.ast.InitClassStatement;
import org.teavm.ast.InstanceOfExpr;
import org.teavm.ast.InvocationExpr;
import org.teavm.ast.MonitorEnterStatement;
import org.teavm.ast.MonitorExitStatement;
import org.teavm.ast.NewArrayExpr;
import org.teavm.ast.NewExpr;
import org.teavm.ast.NewMultiArrayExpr;
import org.teavm.ast.OperationType;
import org.teavm.ast.PrimitiveCastExpr;
import org.teavm.ast.QualificationExpr;
import org.teavm.ast.ReturnStatement;
import org.teavm.ast.SequentialStatement;
import org.teavm.ast.Statement;
import org.teavm.ast.StatementVisitor;
import org.teavm.ast.SubscriptExpr;
import org.teavm.ast.SwitchClause;
import org.teavm.ast.SwitchStatement;
import org.teavm.ast.ThrowStatement;
import org.teavm.ast.TryCatchStatement;
import org.teavm.ast.UnaryExpr;
import org.teavm.ast.UnwrapArrayExpr;
import org.teavm.ast.VariableExpr;
import org.teavm.ast.WhileStatement;
import org.teavm.backend.c.analyze.VolatileDefinitionFinder;
import org.teavm.backend.c.intrinsic.Intrinsic;
import org.teavm.backend.c.intrinsic.IntrinsicContext;
import org.teavm.backend.c.util.InteropUtil;
import org.teavm.backend.lowlevel.generate.NameProvider;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.interop.Address;
import org.teavm.interop.DelegateTo;
import org.teavm.interop.c.Char16;
import org.teavm.interop.c.Variable;
import org.teavm.model.AnnotationContainerReader;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassReader;
import org.teavm.model.ClassReaderSource;
import org.teavm.model.ElementModifier;
import org.teavm.model.FieldReference;
import org.teavm.model.MethodDescriptor;
import org.teavm.model.MethodReader;
import org.teavm.model.MethodReference;
import org.teavm.model.TextLocation;
import org.teavm.model.ValueType;
import org.teavm.model.classes.VirtualTable;
import org.teavm.model.lowlevel.CallSiteDescriptor;
import org.teavm.model.lowlevel.CallSiteLocation;
import org.teavm.model.lowlevel.ExceptionHandlerDescriptor;
import org.teavm.runtime.Allocator;
import org.teavm.runtime.ExceptionHandling;
import org.teavm.runtime.RuntimeArray;
import org.teavm.runtime.RuntimeClass;
import org.teavm.runtime.RuntimeObject;

public class CodeGenerationVisitor implements ExprVisitor, StatementVisitor {
    public static final MethodReference ALLOC_METHOD = new MethodReference(Allocator.class,
            "allocate", RuntimeClass.class, Address.class);
    private static final MethodReference ALLOC_ARRAY_METHOD = new MethodReference(Allocator.class,
            "allocateArray", RuntimeClass.class, int.class, Address.class);
    private static final MethodReference ALLOC_MULTI_ARRAY_METHOD = new MethodReference(Allocator.class,
            "allocateMultiArray", RuntimeClass.class, Address.class, int.class, RuntimeArray.class);
    private static final MethodReference THROW_EXCEPTION_METHOD = new MethodReference(ExceptionHandling.class,
            "throwException", Throwable.class, void.class);
    private static final MethodReference MONITOR_ENTER = new MethodReference(Object.class, "monitorEnter",
            Object.class, void.class);
    private static final MethodReference MONITOR_EXIT = new MethodReference(Object.class, "monitorExit",
            Object.class, void.class);
    private static final MethodReference MONITOR_ENTER_SYNC = new MethodReference(Object.class, "monitorEnterSync",
            Object.class, void.class);
    private static final MethodReference MONITOR_EXIT_SYNC = new MethodReference(Object.class, "monitorExitSync",
            Object.class, void.class);
    private static final MethodReference CATCH_EXCEPTION = new MethodReference(ExceptionHandling.class,
            "catchException", Throwable.class);

    private static final Map<String, String> BUFFER_TYPES = new HashMap<>();

    private GenerationContext context;
    private ClassGenerationContext classContext;
    private NameProvider names;
    private CodeWriter writer;
    private VolatileDefinitionFinder volatileDefinitions;
    private int[] temporaryVariableLevel = new int[5];
    private IntSet spilledVariables = new IntHashSet();
    private int[] maxTemporaryVariableLevel = new int[5];
    private MethodReference callingMethod;
    private IncludeManager includes;
    private boolean end;
    private boolean async;
    private final Deque<LocationStackEntry> locationStack = new ArrayDeque<>();
    private List<CallSiteDescriptor> callSites;
    private List<ExceptionHandlerDescriptor> handlers = new ArrayList<>();
    private boolean managed;
    private IdentifiedStatement defaultBreakTarget;
    private IdentifiedStatement defaultContinueTarget;
    private ObjectIntMap<IdentifiedStatement> labelMap = new ObjectIntHashMap<>();
    private Set<IdentifiedStatement> usedAsBreakTarget = new HashSet<>();
    private Set<IdentifiedStatement> usedAsContinueTarget = new HashSet<>();

    static {
        BUFFER_TYPES.put(ByteBuffer.class.getName(), "int8_t");
        BUFFER_TYPES.put(ShortBuffer.class.getName(), "int16_t");
        BUFFER_TYPES.put(CharBuffer.class.getName(), "char16_t");
        BUFFER_TYPES.put(IntBuffer.class.getName(), "int32_t");
        BUFFER_TYPES.put(LongBuffer.class.getName(), "int64_t");
        BUFFER_TYPES.put(FloatBuffer.class.getName(), "float");
        BUFFER_TYPES.put(DoubleBuffer.class.getName(), "double");
    }

    public CodeGenerationVisitor(ClassGenerationContext classContext, CodeWriter writer, IncludeManager includes,
            List<CallSiteDescriptor> callSites, VolatileDefinitionFinder volatileDefinitions) {
        this.classContext = classContext;
        this.context = classContext.getContext();
        this.writer = writer;
        this.names = context.getNames();
        this.includes = includes;
        this.callSites = callSites;
        this.volatileDefinitions = volatileDefinitions;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public int[] getTemporaries() {
        return maxTemporaryVariableLevel;
    }

    public IntContainer getSpilledVariables() {
        return spilledVariables;
    }

    public void setCallingMethod(MethodReference callingMethod) {
        this.callingMethod = callingMethod;
        this.managed = context.getCharacteristics().isManaged(callingMethod);
    }

    @Override
    public void visit(BinaryExpr expr) {
        pushLocation(expr.getLocation());
        try {
            switch (expr.getOperation()) {
                case COMPARE:
                    writer.print("teavm_compare_");
                    switch (expr.getType()) {
                        case INT:
                            writer.print("i32");
                            break;
                        case LONG:
                            writer.print("i64");
                            break;
                        case FLOAT:
                            writer.print("float");
                            break;
                        case DOUBLE:
                            writer.print("double");
                            break;
                    }
                    writer.print("(");
                    expr.getFirstOperand().acceptVisitor(this);
                    writer.print(", ");
                    expr.getSecondOperand().acceptVisitor(this);
                    writer.print(")");
                    return;
                case UNSIGNED_RIGHT_SHIFT: {
                    String type = expr.getType() == OperationType.LONG ? "int64_t" : "int32_t";
                    writer.print("((" + type + ") ((u" + type + ") ");

                    expr.getFirstOperand().acceptVisitor(this);
                    writer.print(" >> ");
                    expr.getSecondOperand().acceptVisitor(this);

                    writer.print("))");
                    return;
                }

                case MODULO: {
                    switch (expr.getType()) {
                        case FLOAT:
                            writer.print("fmodf(");
                            expr.getFirstOperand().acceptVisitor(this);
                            writer.print(", ");
                            expr.getSecondOperand().acceptVisitor(this);
                            writer.print(")");
                            return;
                        case DOUBLE:
                            writer.print("fmod(");
                            expr.getFirstOperand().acceptVisitor(this);
                            writer.print(", ");
                            expr.getSecondOperand().acceptVisitor(this);
                            writer.print(")");
                            return;
                        default:
                            break;
                    }
                    break;
                }

                default:
                    break;
            }

            writer.print("(");
            expr.getFirstOperand().acceptVisitor(this);

            String op;
            switch (expr.getOperation()) {
                case ADD:
                    op = "+";
                    break;
                case SUBTRACT:
                    op = "-";
                    break;
                case MULTIPLY:
                    op = "*";
                    break;
                case DIVIDE:
                    op = "/";
                    break;
                case MODULO:
                    op = "%";
                    break;
                case BITWISE_AND:
                    op = "&";
                    break;
                case BITWISE_OR:
                    op = "|";
                    break;
                case BITWISE_XOR:
                    op = "^";
                    break;
                case LEFT_SHIFT:
                    op = "<<";
                    break;
                case RIGHT_SHIFT:
                    op = ">>";
                    break;
                case EQUALS:
                    op = "==";
                    break;
                case NOT_EQUALS:
                    op = "!=";
                    break;
                case GREATER:
                    op = ">";
                    break;
                case GREATER_OR_EQUALS:
                    op = ">=";
                    break;
                case LESS:
                    op = "<";
                    break;
                case LESS_OR_EQUALS:
                    op = "<=";
                    break;
                case AND:
                    op = "&&";
                    break;
                case OR:
                    op = "||";
                    break;
                default:
                    throw new AssertionError();
            }

            writer.print(" ").print(op).print(" ");
            expr.getSecondOperand().acceptVisitor(this);
            writer.print(")");
        } finally {
            popLocation(expr.getLocation());
        }
    }

    private void visitReference(Expr expr) {
        if (context.isVmAssertions()) {
            writer.print("TEAVM_VERIFY(");
        }
        expr.acceptVisitor(this);
        if (context.isVmAssertions()) {
            writer.print(")");
        }
    }

    @Override
    public void visit(UnaryExpr expr) {
        pushLocation(expr.getLocation());
        switch (expr.getOperation()) {
            case NOT:
                writer.print("(");
                writer.print("!");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
            case NEGATE:
                writer.print("(");
                writer.print("-");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
            case LENGTH:
                writer.print("TEAVM_ARRAY_LENGTH(");
                visitReference(expr.getOperand());
                writer.print(")");
                break;
            case NULL_CHECK: {
                boolean needParenthesis = false;
                if (needsCallSiteId()) {
                    needParenthesis = true;
                    withCallSite();
                }
                writer.print("teavm_nullCheck(");
                visitReference(expr.getOperand());
                writer.print(")");
                if (needParenthesis) {
                    writer.print(")");
                }
                break;
            }
            case INT_TO_BYTE:
                writer.print("TEAVM_TO_BYTE(");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
            case INT_TO_SHORT:
                writer.print("TEAVM_TO_SHORT(");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
            case INT_TO_CHAR:
                writer.print("TEAVM_TO_CHAR(");
                expr.getOperand().acceptVisitor(this);
                writer.print(")");
                break;
        }
        popLocation(expr.getLocation());
    }

    @Override
    public void visit(ConditionalExpr expr) {
        pushLocation(expr.getLocation());
        writer.print("(");
        expr.getCondition().acceptVisitor(this);
        writer.print(" ? ");
        expr.getConsequent().acceptVisitor(this);
        writer.print(" : ");
        expr.getAlternative().acceptVisitor(this);
        writer.print(")");
        popLocation(expr.getLocation());
    }

    @Override
    public void visit(ConstantExpr expr) {
        pushLocation(expr.getLocation());
        CodeGeneratorUtil.writeValue(writer, context, includes, expr.getValue());
        popLocation(expr.getLocation());
    }

    @Override
    public void visit(VariableExpr expr) {
        pushLocation(expr.getLocation());
        writer.print(getVariableName(expr.getIndex()));
        popLocation(expr.getLocation());
    }

    private String getVariableName(int index) {
        if (index == 0) {
            return "teavm_this_";
        } else {
            return "teavm_local_" + index;
        }
    }

    @Override
    public void visit(SubscriptExpr expr) {
        pushLocation(expr.getLocation());
        writer.print("TEAVM_ARRAY_AT(");
        visitReference(expr.getArray());
        writer.print(", ").print(getArrayType(expr.getType())).print(", ");
        expr.getIndex().acceptVisitor(this);
        writer.print(")");
        popLocation(expr.getLocation());
    }

    @Override
    public void visit(UnwrapArrayExpr expr) {
        pushLocation(expr.getLocation());
        expr.getArray().acceptVisitor(this);
        popLocation(expr.getLocation());
    }

    private static String getArrayType(ArrayType type) {
        switch (type) {
            case BYTE:
                return "int8_t";
            case SHORT:
                return "int16_t";
            case CHAR:
                return "char16_t";
            case INT:
                return "int32_t";
            case LONG:
                return "int64_t";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            case OBJECT:
                return "void*";
            default:
                throw new AssertionError();
        }
    }

    private boolean needsCallSiteId() {
        return context.isLongjmp() && managed;
    }

    @Override
    public void visit(InvocationExpr expr) {
        ClassReader cls = context.getClassSource().get(expr.getMethod().getClassName());
        if (cls != null) {
            InteropUtil.processInclude(cls.getAnnotations(), includes);
            MethodReader method = cls.getMethod(expr.getMethod().getDescriptor());
            if (method != null) {
                InteropUtil.processInclude(method.getAnnotations(), includes);
            }
        }

        boolean needParenthesis = false;

        Intrinsic intrinsic = context.getIntrinsic(expr.getMethod());
        if (intrinsic != null) {
            pushLocation(expr.getLocation());
            if (needsCallSiteId() && isManagedMethodCall(context.getCharacteristics(), expr.getMethod())) {
                needParenthesis = true;
                withCallSite();
            }
            intrinsic.apply(intrinsicContext, expr);
            popLocation(expr.getLocation());
            if (needParenthesis) {
                writer.print(")");
            }
            return;
        }

        pushLocation(expr.getLocation());

        if (needsCallSiteId() && isManagedMethodCall(context.getCharacteristics(), expr.getMethod())) {
            needParenthesis = true;
            withCallSite();
        }

        switch (expr.getType()) {
            case CONSTRUCTOR:
                generateCallToConstructor(expr.getMethod(), expr.getArguments());
                break;

            case SPECIAL:
            case STATIC:
                generateDirectCall(expr.getMethod(), expr.getArguments());
                break;

            case DYNAMIC: {
                generateVirtualCall(expr.getMethod(), expr.getArguments());
                break;
            }
        }

        if (needParenthesis) {
            writer.print(")");
        }

        popLocation(expr.getLocation());
    }

    private void withCallSite() {
        LocationStackEntry locationEntry = locationStack.peek();
        TextLocation location = locationEntry != null ? locationEntry.location : null;
        CallSiteLocation[] callSiteLocations = CallSiteLocation.fromTextLocation(location, callingMethod);
        CallSiteDescriptor callSite = new CallSiteDescriptor(callSites.size(), callSiteLocations);
        List<ExceptionHandlerDescriptor> reverseHandlers = new ArrayList<>(handlers);
        Collections.reverse(reverseHandlers);
        callSite.getHandlers().addAll(reverseHandlers);
        callSites.add(callSite);

        writer.print("TEAVM_WITH_CALL_SITE_ID(").print(String.valueOf(callSite.getId())).print(", ");
    }

    private void generateCallToConstructor(MethodReference reference, List<? extends Expr> arguments) {
        String receiver = allocTemporaryVariable(CVariableType.PTR);
        writer.print("(" + receiver + " = ");
        allocObject(reference.getClassName());
        writer.print(", ");

        MethodReader method = context.getClassSource().resolve(reference);
        if (method != null) {
            reference = method.getReference();
        }

        classContext.importMethod(reference, false);
        writer.print(names.forMethod(reference));

        writer.print("(" + receiver);
        for (Expr arg : arguments) {
            writer.print(", ");
            arg.acceptVisitor(this);
        }
        writer.print("), " + receiver + ")");

        freeTemporaryVariable(CVariableType.PTR);
    }

    private void generateDirectCall(MethodReference reference, List<? extends Expr> arguments) {
        MethodReader method = context.getClassSource().resolve(reference);
        if (method != null && isWrappedNativeCall(method)) {
            generateWrappedNativeCall(method, arguments);
        } else {
            if (method == null || method.hasModifier(ElementModifier.ABSTRACT)) {
                generateNoMethodCall(reference, arguments);
                return;
            }

            reference = method.getReference();
            if (!method.hasModifier(ElementModifier.NATIVE)
                    || method.getAnnotations().get(DelegateTo.class.getName()) != null
                    || context.getGenerator(reference) != null) {
                classContext.importMethod(reference, method.hasModifier(ElementModifier.STATIC));
            }
            writer.print(names.forMethod(reference));

            writer.print("(");
            if (!arguments.isEmpty()) {
                arguments.get(0).acceptVisitor(this);
                for (int i = 1; i < arguments.size(); ++i) {
                    writer.print(", ");
                    arguments.get(i).acceptVisitor(this);
                }
            }
            writer.print(")");
        }
    }

    private void generateVirtualCall(MethodReference reference, List<? extends Expr> arguments) {
        if (context.isIncremental()) {
            generateIncrementalVirtualCall(reference.getDescriptor(), arguments);
        } else {
            generateNormalVirtualCall(reference, arguments);
        }
    }

    private void generateNormalVirtualCall(MethodReference reference, List<? extends Expr> arguments) {
        VirtualTable vtable = context.getVirtualTableProvider().lookup(reference.getClassName());
        String vtableClass = null;
        if (vtable != null) {
            VirtualTable containingVt = vtable.findMethodContainer(reference.getDescriptor());
            if (containingVt != null) {
                vtableClass = containingVt.getClassName();
            }
        }
        if (vtableClass == null) {
            generateNoMethodCall(reference, arguments);
            return;
        }

        Expr receiverArg = arguments.get(0);
        boolean closingParenthesis = false;
        String receiver;
        if (receiverArg instanceof VariableExpr) {
            receiver = getVariableName(((VariableExpr) receiverArg).getIndex());
        } else {
            receiver = allocTemporaryVariable(CVariableType.PTR);
            writer.print("((").print(receiver).print(" = ");
            visitReference(receiverArg);
            writer.print("), ");
            closingParenthesis = true;
        }

        includes.includeClass(vtableClass);
        writer.print("TEAVM_METHOD(")
                .print(receiver).print(", ")
                .print(names.forClassClass(vtableClass)).print(", ")
                .print(names.forVirtualMethod(reference.getDescriptor()))
                .print(")(").print(receiver);
        for (int i = 1; i < arguments.size(); ++i) {
            writer.print(", ");
            arguments.get(i).acceptVisitor(this);
        }
        writer.print(")");
        if (closingParenthesis) {
            writer.print(")");
            freeTemporaryVariable(CVariableType.PTR);
        }
    }

    private void generateIncrementalVirtualCall(MethodDescriptor descriptor, List<? extends Expr> arguments) {
        Expr receiverArg = arguments.get(0);
        boolean closingParenthesis = false;
        String receiver;
        if (receiverArg instanceof VariableExpr) {
            receiver = getVariableName(((VariableExpr) receiverArg).getIndex());
        } else {
            receiver = allocTemporaryVariable(CVariableType.PTR);
            writer.print("((").print(receiver).print(" = ");
            visitReference(receiverArg);
            writer.print("), ");
            closingParenthesis = true;
        }

        writer.print("TEAVM_VC_METHOD(").print(receiver)
                .print(", ").print(classContext.getVirtualMethodId(descriptor))
                .print(", ").printType(descriptor.getResultType())
                .print(", (");
        CodeGenerator.generateMethodParameters(writer, descriptor, false, false);
        writer.print("))(").print(receiver);
        for (int i = 1; i < arguments.size(); ++i) {
            writer.print(", ");
            arguments.get(i).acceptVisitor(this);
        }
        writer.print(")");

        if (closingParenthesis) {
            writer.print(")");
            freeTemporaryVariable(CVariableType.PTR);
        }
    }

    private void generateNoMethodCall(MethodReference reference, List<? extends Expr> arguments) {
        writer.print("(");
        for (Expr arg : arguments) {
            arg.acceptVisitor(this);
            writer.print(", ");
        }
        printDefaultValue(reference.getReturnType());
        writer.print(")");
    }

    private void generateWrappedNativeCall(MethodReader method, List<? extends Expr> arguments) {
        List<String> temporaries = new ArrayList<>();
        List<String> stringTemporaries = new ArrayList<>();
        String resultTmp = null;
        if (method.getResultType() != ValueType.VOID) {
            resultTmp = allocTemporaryVariable(typeToCType(method.getResultType()));
        }

        for (int i = 0; i < arguments.size(); ++i) {
            temporaries.add(allocTemporaryVariable(parameterTypeForCall(method, i)));
        }
        boolean stringResult = method.getResultType().isObject(String.class);
        boolean string16Result = method.getAnnotations().get(Char16.class.getName()) != null;

        writer.print("(");
        AnnotationContainerReader[] parameterAnnotations = method.getParameterAnnotations();

        for (int i = 0; i < arguments.size(); ++i) {
            String tmp = temporaries.get(i);
            writer.print(tmp + " = ");
            ValueType type = method.hasModifier(ElementModifier.STATIC)
                    ? method.parameterType(i)
                    : i == 0 ? ValueType.object(method.getOwnerName()) : method.parameterType(i - 1);
            if (type.isObject(String.class)) {
                int annotIndex = method.hasModifier(ElementModifier.STATIC) ? i : i - 1;
                boolean is16Char = annotIndex >= 0
                        && parameterAnnotations[annotIndex].get(Char16.class.getName()) != null;
                String functionName = is16Char ? "teavm_stringToC16" : "teavm_stringToC";
                writer.print(functionName).print("(");
                arguments.get(i).acceptVisitor(this);
                writer.print(")");
                stringTemporaries.add(tmp);
            } else if (isPrimitiveArray(type)) {
                writer.print("TEAVM_ARRAY_DATAN(");
                arguments.get(i).acceptVisitor(this);
                writer.print(", ").printStrictType(((ValueType.Array) type).getItemType()).print(")");
            } else if (isPrimitiveBuffer(type)) {
                writer.print("TEAVM_ARRAY_DATA(TEAVM_FIELD(");
                String typeName = ((ValueType.Object) type).getClassName();
                arguments.get(i).acceptVisitor(this);
                includes.includeClass(typeName);
                writer.print(", ").print(names.forClass(typeName)).print(", ")
                        .print(names.forMemberField(new FieldReference(typeName, "array"))).print(")");
                writer.print(", ").print(BUFFER_TYPES.get(typeName)).print(")");
            } else {
                arguments.get(i).acceptVisitor(this);
            }

            writer.print(", ");
        }

        if (resultTmp != null) {
            writer.print(resultTmp + " = (" + typeToCType(method.getResultType()).text + ") ");
        }
        writer.print(names.forMethod(method.getReference()));
        if (method.getAnnotations().get(Variable.class.getName()) == null) {
            writer.print("(");
            for (int i = 0; i < temporaries.size(); ++i) {
                if (i > 0) {
                    writer.print(", ");
                }
                writer.print(temporaries.get(i));
                freeTemporaryVariable(parameterTypeForCall(method, i));
            }
            writer.print(")");
        } else if (method.parameterCount() > 0 || method.getResultType() == ValueType.VOID) {
            context.getDiagnostics().error(new CallLocation(method.getReference()),
                    "'@Variable' annotation is not applicable to method {{m0}}", method.getReference());
        }

        for (String tmp : stringTemporaries) {
            writer.print(", teavm_free(" + tmp + ")");
        }

        if (resultTmp != null) {
            writer.print(", ");
            if (stringResult) {
                String functionName = string16Result ? "teavm_c16ToString" : "teavm_cToString";
                writer.print(functionName).print("(");
            }
            writer.print(resultTmp);
            if (stringResult) {
                writer.print(")");
            }
            freeTemporaryVariable(typeToCType(method.getResultType()));
        }

        writer.print(")");
    }

    private CVariableType parameterTypeForCall(MethodReader method, int index) {
        if (method.hasModifier(ElementModifier.STATIC)) {
            return typeToCType(method.parameterType(index));
        } else {
            return index == 0 ? CVariableType.PTR : typeToCType(method.parameterType(index - 1));
        }
    }

    private static boolean isPrimitiveArray(ValueType type) {
        if (!(type instanceof ValueType.Array)) {
            return false;
        }

        return ((ValueType.Array) type).getItemType() instanceof ValueType.Primitive;
    }

    private static boolean isPrimitiveBuffer(ValueType type) {
        if (!(type instanceof ValueType.Object)) {
            return false;
        }
        return BUFFER_TYPES.containsKey(((ValueType.Object) type).getClassName());
    }

    private boolean isWrappedNativeCall(MethodReader method) {
        if (!method.hasModifier(ElementModifier.NATIVE)
                || method.getAnnotations().get(DelegateTo.class.getName()) != null) {
            return false;
        }
        if (method.getAnnotations().get(Variable.class.getName()) != null) {
            return true;
        }
        for (ValueType type : method.getParameterTypes()) {
            if (type.isObject(String.class) || isPrimitiveArray(type) || isPrimitiveBuffer(type)) {
                return true;
            }
        }
        if (method.getResultType().isObject(String.class)) {
            return true;
        }
        return false;
    }

    private String allocTemporaryVariable(CVariableType type) {
        int index = type.ordinal();
        int result = temporaryVariableLevel[index]++;
        maxTemporaryVariableLevel[index] = Math.max(maxTemporaryVariableLevel[index], temporaryVariableLevel[index]);
        return "teavm_tmp_" + type.name().toLowerCase() + "_" + result;
    }

    private void freeTemporaryVariable(CVariableType type) {
        temporaryVariableLevel[type.ordinal()]--;
    }

    private void printDefaultValue(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            writer.print("0");
        } else {
            writer.print("NULL");
        }
    }

    @Override
    public void visit(QualificationExpr expr) {
        FieldReference field = expr.getField();
        if (isMonitorField(field)) {
            pushLocation(expr.getLocation());
            String tmp = allocTemporaryVariable(CVariableType.INT);
            writer.print("(" + tmp + " = TEAVM_FIELD(");
            expr.getQualified().acceptVisitor(this);
            field = new FieldReference(RuntimeObject.class.getName(), "hashCode");
            writer.print(", ").print(names.forClass(field.getClassName()) + ", "
                    + names.forMemberField(field) + ")");
            writer.print(", TEAVM_UNPACK_MONITOR(" + tmp + "))");
            popLocation(expr.getLocation());
            return;
        }

        pushLocation(expr.getLocation());
        printFieldRef(expr.getQualified(), field);
        popLocation(expr.getLocation());
    }

    private void printFieldRef(Expr qualified, FieldReference field) {
        if (qualified != null) {
            ClassReader cls = context.getClassSource().get(field.getClassName());
            writer.print("TEAVM_FIELD(");

            boolean shouldVerify = context.isVmAssertions()
                    && context.getCharacteristics().isManaged(field.getClassName());
            if (shouldVerify) {
                writer.print("TEAVM_VERIFY(");
            }
            qualified.acceptVisitor(this);
            if (shouldVerify) {
                writer.print(")");
            }
            writer.print(", ");

            if (cls != null && isNative(cls)) {
                InteropUtil.processInclude(cls.getAnnotations(), includes);
                InteropUtil.printNativeReference(writer, cls);
                writer.print(", ").print(InteropUtil.getNativeName(cls, field.getFieldName()));
            } else {
                includes.includeClass(field.getClassName());
                writer.print(names.forClass(field.getClassName())).print(", ").print(names.forMemberField(field));
            }
            writer.print(")");
        } else {
            includes.includeClass(field.getClassName());
            writer.print(names.forStaticField(field));
        }
    }

    private boolean isNative(ClassReader cls) {
        return context.getCharacteristics().isStructure(cls.getName()) && InteropUtil.isNative(cls);
    }

    private boolean isMonitorField(FieldReference field) {
        return field.getClassName().equals("java.lang.Object") && field.getFieldName().equals("monitor");
    }

    @Override
    public void visit(NewExpr expr) {
        pushLocation(expr.getLocation());
        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }
        allocObject(expr.getConstructedClass());
        if (needParenthesis) {
            writer.print(")");
        }
        popLocation(expr.getLocation());
    }

    private void allocObject(String className) {
        includes.includeClass(className);
        classContext.importMethod(ALLOC_METHOD, true);
        writer.print(names.forMethod(ALLOC_METHOD)).print("(&")
                .print(names.forClassInstance(ValueType.object(className)))
                .print(")");
    }

    @Override
    public void visit(NewArrayExpr expr) {
        pushLocation(expr.getLocation());

        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }

        ValueType type = ValueType.arrayOf(expr.getType());
        writer.print(names.forMethod(ALLOC_ARRAY_METHOD)).print("(&")
                .print(names.forClassInstance(type)).print(", ");
        classContext.importMethod(ALLOC_ARRAY_METHOD, true);
        includes.includeType(type);
        expr.getLength().acceptVisitor(this);
        writer.print(")");

        if (needParenthesis) {
            writer.print(")");
        }

        popLocation(expr.getLocation());
    }

    @Override
    public void visit(ArrayFromDataExpr expr) {
        pushLocation(expr.getLocation());

        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }

        if (expr.getType() instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) expr.getType()).getKind()) {
                case BOOLEAN:
                    writer.print("teavm_fillBooleanArray");
                    break;
                case BYTE:
                    writer.print("teavm_fillByteArray");
                    break;
                case SHORT:
                    writer.print("teavm_fillShortArray");
                    break;
                case CHARACTER:
                    writer.print("teavm_fillCharArray");
                    break;
                case INTEGER:
                    writer.print("teavm_fillIntArray");
                    break;
                case LONG:
                    writer.print("teavm_fillLongArray");
                    break;
                case FLOAT:
                    writer.print("teavm_fillFloatArray");
                    break;
                case DOUBLE:
                    writer.print("teavm_fillDoubleArray");
                    break;
            }
        } else {
            writer.print("teavm_fillArray");
        }
        writer.print("(");

        ValueType type = ValueType.arrayOf(expr.getType());
        writer.print(names.forMethod(ALLOC_ARRAY_METHOD)).print("(&")
                .print(names.forClassInstance(type)).print(", ");
        classContext.importMethod(ALLOC_ARRAY_METHOD, true);
        includes.includeType(type);
        writer.print(expr.getData().size() + ")");

        for (Expr element : expr.getData()) {
            writer.print(", ");
            element.acceptVisitor(this);
        }

        writer.print(")");

        if (needParenthesis) {
            writer.print(")");
        }

        popLocation(expr.getLocation());
    }

    @Override
    public void visit(NewMultiArrayExpr expr) {
        pushLocation(expr.getLocation());

        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }

        writer.print(names.forMethod(ALLOC_MULTI_ARRAY_METHOD)).print("(&")
                .print(names.forClassInstance(expr.getType())).print(", ");
        classContext.importMethod(ALLOC_MULTI_ARRAY_METHOD, true);
        includes.includeType(expr.getType());

        writer.print("(int32_t[]) {");
        expr.getDimensions().get(0).acceptVisitor(this);
        for (int i = 1; i < expr.getDimensions().size(); ++i) {
            writer.print(", ");
            expr.getDimensions().get(i).acceptVisitor(this);
        }

        writer.print("}, ").print(String.valueOf(expr.getDimensions().size())).print(")");

        if (needParenthesis) {
            writer.print(")");
        }

        popLocation(expr.getLocation());
    }

    @Override
    public void visit(InstanceOfExpr expr) {
        pushLocation(expr.getLocation());
        writer.print("teavm_instanceof(");
        visitReference(expr.getExpr());
        includes.includeType(expr.getType());
        writer.print(", ").print(names.forSupertypeFunction(expr.getType())).print(")");
        popLocation(expr.getLocation());
    }

    @Override
    public void visit(CastExpr expr) {
        if (expr.getTarget() instanceof ValueType.Object) {
            String className = ((ValueType.Object) expr.getTarget()).getClassName();
            if (context.getCharacteristics().isStructure(className)
                    || className.equals(Address.class.getName())) {
                expr.getValue().acceptVisitor(this);
                return;
            }
        }

        pushLocation(expr.getLocation());

        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }

        writer.print("teavm_checkcast(");
        visitReference(expr.getValue());
        includes.includeType(expr.getTarget());
        writer.print(", ").print(names.forSupertypeFunction(expr.getTarget())).print(")");

        if (needParenthesis) {
            writer.print(")");
        }

        popLocation(expr.getLocation());
    }

    @Override
    public void visit(PrimitiveCastExpr expr) {
        pushLocation(expr.getLocation());
        writer.print("((");
        switch (expr.getTarget()) {
            case INT:
                writer.print("int32_t");
                break;
            case LONG:
                writer.print("int64_t");
                break;
            case FLOAT:
                writer.print("float");
                break;
            case DOUBLE:
                writer.print("double");
                break;
        }
        writer.print(") ");
        expr.getValue().acceptVisitor(this);
        writer.print(")");
        popLocation(expr.getLocation());
    }

    @Override
    public void visit(AssignmentStatement statement) {
        pushLocation(statement.getLocation());

        if (statement.getLeftValue() != null) {
            if (statement.getLeftValue() instanceof QualificationExpr) {
                QualificationExpr qualification = (QualificationExpr) statement.getLeftValue();
                FieldReference field = qualification.getField();
                if (isMonitorField(field)) {
                    writer.print("TEAVM_FIELD(");
                    qualification.getQualified().acceptVisitor(this);
                    field = new FieldReference(RuntimeObject.class.getName(), "hashCode");
                    writer.print(", ").print(names.forClass(field.getClassName()) + ", "
                            + names.forMemberField(field) + ") = TEAVM_PACK_MONITOR(");
                    statement.getRightValue().acceptVisitor(this);
                    writer.println(");");
                    popLocation(statement.getLocation());
                    return;
                }
            }

            statement.getLeftValue().acceptVisitor(this);
            writer.print(" = ");
        }
        statement.getRightValue().acceptVisitor(this);
        writer.println(";");

        if (volatileDefinitions.shouldBackup(statement)) {
            VariableExpr lhs = (VariableExpr) statement.getLeftValue();
            spilledVariables.add(lhs.getIndex());
            writer.println("teavm_spill_" + lhs.getIndex() + " = " + getVariableName(lhs.getIndex()) + ";");
        }

        popLocation(statement.getLocation());
    }

    @Override
    public void visit(SequentialStatement statement) {
        visitMany(statement.getSequence());
    }

    private void visitMany(List<Statement> statements) {
        if (statements.isEmpty()) {
            return;
        }
        boolean oldEnd = end;
        for (int i = 0; i < statements.size() - 1; ++i) {
            end = false;
            statements.get(i).acceptVisitor(this);
        }
        end = oldEnd;
        statements.get(statements.size() - 1).acceptVisitor(this);
        end = oldEnd;
    }

    @Override
    public void visit(ConditionalStatement statement) {
        while (true) {
            pushLocation(statement.getCondition().getLocation());
            writer.print("if (");
            statement.getCondition().acceptVisitor(this);
            writer.println(") {").indent();
            popLocation(statement.getCondition().getLocation());

            visitMany(statement.getConsequent());
            writer.outdent().print("}");

            if (statement.getAlternative().isEmpty()) {
                writer.println();
                break;
            }

            writer.print(" else ");
            if (statement.getAlternative().size() == 1
                    && statement.getAlternative().get(0) instanceof ConditionalStatement) {
                statement = (ConditionalStatement) statement.getAlternative().get(0);
            } else {
                writer.println("{").indent();
                visitMany(statement.getAlternative());
                writer.outdent().println("}");
                break;
            }
        }
    }

    @Override
    public void visit(SwitchStatement statement) {
        IdentifiedStatement oldDefaultBreakTarget = defaultBreakTarget;
        defaultBreakTarget = statement;

        int statementId = labelMap.size() + 1;
        labelMap.put(statement, statementId);

        pushLocation(statement.getValue().getLocation());
        writer.print("switch (");
        statement.getValue().acceptVisitor(this);
        writer.print(") {").println().indent();
        popLocation(statement.getValue().getLocation());

        for (SwitchClause clause : statement.getClauses()) {
            for (int condition : clause.getConditions()) {
                writer.println("case " + condition + ":");
            }

            writer.indent();
            boolean oldEnd = end;
            for (Statement part : clause.getBody()) {
                end = false;
                part.acceptVisitor(this);
            }
            end = oldEnd;
            writer.outdent();
        }

        if (!statement.getDefaultClause().isEmpty()) {
            writer.println("default:").indent();
            visitMany(statement.getDefaultClause());
            writer.outdent();
        }

        writer.outdent().println("}");

        if (usedAsBreakTarget.contains(statement)) {
            writer.outdent().println("teavm_label_" + statementId + ":;").indent();
        }

        defaultBreakTarget = oldDefaultBreakTarget;
    }

    @Override
    public void visit(WhileStatement statement) {
        IdentifiedStatement oldDefaultBreakTarget = defaultBreakTarget;
        IdentifiedStatement oldDefaultContinueTarget = defaultContinueTarget;
        defaultBreakTarget = statement;
        defaultContinueTarget = statement;

        int statementId = labelMap.size() + 1;
        labelMap.put(statement, statementId);

        writer.print("while (");
        if (statement.getCondition() != null) {
            statement.getCondition().acceptVisitor(this);
        } else {
            writer.print("1");
        }
        writer.println(") {").indent();

        boolean oldEnd = end;
        for (Statement part : statement.getBody()) {
            end = false;
            part.acceptVisitor(this);
        }
        end = oldEnd;

        if (usedAsContinueTarget.contains(statement)) {
            writer.outdent().println("teavm_cnt_" + statementId + ":;").indent();
        }
        writer.outdent().println("}");

        if (usedAsBreakTarget.contains(statement)) {
            writer.outdent().println("teavm_label_" + statementId + ":;").indent();
        }

        defaultContinueTarget = oldDefaultContinueTarget;
        defaultBreakTarget = oldDefaultBreakTarget;
    }

    @Override
    public void visit(BlockStatement statement) {
        int statementId = labelMap.size() + 1;
        labelMap.put(statement, statementId);

        visitMany(statement.getBody());

        if (usedAsBreakTarget.contains(statement)) {
            writer.outdent().println("teavm_label_" + statementId + ":;").indent();
        }
    }

    @Override
    public void visit(BreakStatement statement) {
        pushLocation(statement.getLocation());
        IdentifiedStatement target = statement.getTarget();
        if (target == null) {
            target = defaultBreakTarget;
        }
        int id = labelMap.get(target);
        writer.println("goto teavm_label_" + id + ";");
        usedAsBreakTarget.add(target);
        popLocation(statement.getLocation());
    }

    @Override
    public void visit(ContinueStatement statement) {
        pushLocation(statement.getLocation());
        IdentifiedStatement target = statement.getTarget();
        if (target == null) {
            target = defaultContinueTarget;
        }
        int id = labelMap.get(target);
        writer.println("goto teavm_cnt_" + id + ";");
        usedAsContinueTarget.add(target);
        popLocation(statement.getLocation());
    }

    @Override
    public void visit(ReturnStatement statement) {
        pushLocation(statement.getLocation());
        writer.print("return");
        if (statement.getResult() != null) {
            writer.print(" ");
            statement.getResult().acceptVisitor(this);
        }
        writer.println(";");
        popLocation(statement.getLocation());
    }

    @Override
    public void visit(ThrowStatement statement) {
        pushLocation(statement.getLocation());

        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }

        classContext.importMethod(THROW_EXCEPTION_METHOD, true);
        writer.print(names.forMethod(THROW_EXCEPTION_METHOD)).print("(");
        statement.getException().acceptVisitor(this);
        writer.print(")");

        if (needParenthesis) {
            writer.print(")");
        }
        writer.println(";");

        if (context.isLongjmp()) {
            writer.println("TEAVM_UNREACHABLE");
        }

        popLocation(statement.getLocation());
    }

    @Override
    public void visit(InitClassStatement statement) {
        pushLocation(statement.getLocation());

        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }

        includes.includeClass(statement.getClassName());
        writer.print(names.forClassInitializer(statement.getClassName()) + "()");

        if (needParenthesis) {
            writer.print(")");
        }
        writer.println(";");

        popLocation(statement.getLocation());
    }

    @Override
    public void visit(TryCatchStatement statement) {
        List<TryCatchStatement> tryCatchStatements = new ArrayList<>();
        List<int[]> restoredVariablesByHandler = new ArrayList<>();
        while (true) {
            if (statement.getProtectedBody().size() != 1) {
                break;
            }
            Statement next = statement.getProtectedBody().get(0);
            if (!(next instanceof TryCatchStatement)) {
                break;
            }
            tryCatchStatements.add(statement);
            restoredVariablesByHandler.add(volatileDefinitions.variablesToRestore(statement));
            statement = (TryCatchStatement) next;
        }
        tryCatchStatements.add(statement);
        restoredVariablesByHandler.add(volatileDefinitions.variablesToRestore(statement));

        int firstId = handlers.size();
        for (int i = 0; i < tryCatchStatements.size(); ++i) {
            TryCatchStatement tryCatch = tryCatchStatements.get(i);
            handlers.add(new ExceptionHandlerDescriptor(firstId + i + 1, tryCatch.getExceptionType()));
        }

        writer.println("TEAVM_TRY").indent();
        visitMany(statement.getProtectedBody());
        writer.outdent().println("TEAVM_CATCH").indent();

        for (int i = tryCatchStatements.size() - 1; i >= 0; --i) {
            TryCatchStatement tryCatch = tryCatchStatements.get(i);
            int[] variablesToRestore = restoredVariablesByHandler.get(i);
            writer.println("// CATCH " + (tryCatch.getExceptionType() != null ? tryCatch.getExceptionType() : "any"));
            writer.println("case " + (i + 1 + firstId) + ": {").indent();

            for (int variableIndex : variablesToRestore) {
                writer.println(getVariableName(variableIndex) + " = teavm_spill_" + variableIndex + ";");
            }

            if (tryCatch.getExceptionVariable() != null) {
                writer.print(getVariableName(tryCatch.getExceptionVariable())).print(" = ");
                writer.print(names.forMethod(CATCH_EXCEPTION)).println("();");
            }
            visitMany(tryCatch.getHandler());

            writer.println("break;");
            writer.outdent().println("}");
        }

        handlers.subList(firstId, handlers.size()).clear();

        writer.outdent().println("TEAVM_END_TRY");
    }

    @Override
    public void visit(GotoPartStatement statement) {
    }

    @Override
    public void visit(MonitorEnterStatement statement) {
        pushLocation(statement.getLocation());

        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }

        MethodReference methodRef = async ? MONITOR_ENTER : MONITOR_ENTER_SYNC;
        classContext.importMethod(methodRef, true);
        writer.print(names.forMethod(methodRef)).print("(");
        statement.getObjectRef().acceptVisitor(this);
        writer.print(")");

        if (needParenthesis) {
            writer.print(")");
        }
        writer.println(";");

        popLocation(statement.getLocation());
    }

    @Override
    public void visit(MonitorExitStatement statement) {
        pushLocation(statement.getLocation());

        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }

        MethodReference methodRef = async ? MONITOR_EXIT : MONITOR_EXIT_SYNC;
        classContext.importMethod(methodRef, true);
        writer.print(names.forMethod(methodRef)).print("(");
        statement.getObjectRef().acceptVisitor(this);
        writer.print(")");

        if (needParenthesis) {
            writer.print(")");
        }
        writer.println(";");

        popLocation(statement.getLocation());
    }

    @Override
    public void visit(BoundCheckExpr expr) {
        if (expr.getArray() == null && !expr.isLower()) {
            expr.getIndex().acceptVisitor(this);
            return;
        }

        boolean needParenthesis = false;
        if (needsCallSiteId()) {
            needParenthesis = true;
            withCallSite();
        }

        String functionName;
        if (expr.getArray() == null) {
            functionName = "teavm_checkLowerBound";
        } else if (!expr.isLower()) {
            functionName = "teavm_checkUpperBound";
        } else {
            functionName = "teavm_checkBounds";
        }

        writer.print(functionName);
        writer.print("(");
        expr.getIndex().acceptVisitor(this);
        if (expr.getArray() != null) {
            writer.print(", ");
            visitReference(expr.getArray());
        }
        writer.print(")");
        if (needParenthesis) {
            writer.print(")");
        }
    }

    private IntrinsicContext intrinsicContext = new IntrinsicContext() {
        @Override
        public CodeWriter writer() {
            return writer;
        }

        @Override
        public NameProvider names() {
            return names;
        }

        @Override
        public void emit(Expr expr) {
            expr.acceptVisitor(CodeGenerationVisitor.this);
        }

        @Override
        public Diagnostics diagnotics() {
            return context.getDiagnostics();
        }

        @Override
        public MethodReference callingMethod() {
            return callingMethod;
        }

        @Override
        public StringPool stringPool() {
            return context.getStringPool();
        }

        @Override
        public IncludeManager includes() {
            return includes;
        }

        @Override
        public String escapeFileName(String name) {
            StringBuilder sb = new StringBuilder();
            ClassGenerator.escape(name, sb);
            return sb.toString();
        }

        @Override
        public boolean isIncremental() {
            return context.isIncremental();
        }

        @Override
        public ClassReaderSource classes() {
            return context.getClassSource();
        }

        @Override
        public void importMethod(MethodReference method, boolean isStatic) {
            classContext.importMethod(method, isStatic);
        }
    };

    private static CVariableType typeToCType(ValueType type) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                case CHARACTER:
                case BYTE:
                case SHORT:
                case INTEGER:
                    return CVariableType.INT;
                case LONG:
                    return CVariableType.LONG;
                case FLOAT:
                    return CVariableType.FLOAT;
                case DOUBLE:
                    return CVariableType.DOUBLE;
            }
        }
        return CVariableType.PTR;
    }

    private void pushLocation(TextLocation location) {
        if (location == null) {
            return;
        }
        LocationStackEntry prevEntry = locationStack.peek();
        if (prevEntry == null || !location.equals(prevEntry.location)) {
            if (location.getFileName() == null) {
                writer.nosource();
            } else {
                writer.source(location.getFileName(), location.getLine());
            }
        }
        locationStack.push(new LocationStackEntry(location));
    }

    private void popLocation(TextLocation location) {
        if (location == null) {
            return;
        }
        LocationStackEntry prevEntry = locationStack.pop();
        LocationStackEntry entry = locationStack.peek();
        if (entry != null) {
            if (!entry.location.equals(prevEntry.location)) {
                if (entry.location.getFileName() == null) {
                    writer.nosource();
                } else {
                    writer.source(entry.location.getFileName(), entry.location.getLine());
                }
            }
        } else {
            writer.nosource();
        }
    }

    static class LocationStackEntry {
        final TextLocation location;

        LocationStackEntry(TextLocation location) {
            this.location = location;
        }
    }
}
