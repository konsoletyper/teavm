package org.teavm.dependency;

import java.util.HashSet;
import java.util.Set;
import org.teavm.model.*;
import org.teavm.model.instructions.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
class TypeAnalyzer implements InstructionVisitor {
    private ClassHolderSource classSource;
    private ValueType[] types;

    public TypeAnalyzer(ClassHolderSource classSource, int variableCount) {
        this.classSource = classSource;
        types = new ValueType[variableCount];
    }

    @Override
    public void visit(EmptyInstruction insn) {
    }

    public ValueType typeOf(int variable) {
        return types[variable];
    }

    public void define(Variable var, ValueType type) {
        types[var.getIndex()] = type;
    }

    public void merge(Variable var, ValueType type) {
        if (types[var.getIndex()] == null) {
            define(var, type);
        } else {
            define(var, merge(typeOf(var.getIndex()), type));
        }
    }

    private ValueType merge(ValueType a, ValueType b) {
        if (a instanceof ValueType.Array && b instanceof ValueType.Array) {
            return merge(((ValueType.Array)a).getItemType(), ((ValueType.Array)b).getItemType());
        } else if (a instanceof ValueType.Object && b instanceof ValueType.Object) {
            String p = ((ValueType.Object)a).getClassName();
            String q = ((ValueType.Object)b).getClassName();
            if (p.equals(q)) {
                return a;
            }
            ClassHolder firstClass = classSource.getClassHolder(p);
            ClassHolder secondClass = classSource.getClassHolder(q);
            if (firstClass.getModifiers().contains(ElementModifier.INTERFACE) ||
                    secondClass.getModifiers().contains(ElementModifier.INTERFACE)) {
                return ValueType.object("java.lang.Object");
            }
            if (isSuper(secondClass, firstClass)) {
                return ValueType.object(secondClass.getName());
            }
            Set<String> path = getPathToRoot(firstClass);
            return ValueType.object(findAmoungSupertypes(secondClass, path));
        } else {
            return ValueType.object("java.lang.Object");
        }
    }

    private Set<String> getPathToRoot(ClassHolder cls) {
        Set<String> path = new HashSet<>();
        while (cls != null) {
            path.add(cls.getName());
            cls = cls.getParent() != null ? classSource.getClassHolder(cls.getParent()) : null;
        }
        return path;
    }

    private boolean isSuper(ClassHolder cls, ClassHolder superCls) {
        while (cls != null) {
            if (cls == superCls) {
                return true;
            }
            cls = cls.getParent() != null ? classSource.getClassHolder(cls.getParent()) : null;
        }
        return false;
    }

    private String findAmoungSupertypes(ClassHolder cls, Set<String> supertypes) {
        while (cls != null) {
            if (supertypes.contains(cls.getName())) {
                return cls.getName();
            }
            cls = cls.getParent() != null ? classSource.getClassHolder(cls.getParent()) : null;
        }
        return "java.lang.Object";
    }

    @Override
    public void visit(ClassConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.object("java.lang.Class"));
    }

    @Override
    public void visit(NullConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.NULL);
    }

    @Override
    public void visit(IntegerConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.INTEGER);
    }

    @Override
    public void visit(LongConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.LONG);
    }

    @Override
    public void visit(FloatConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.FLOAT);
    }

    @Override
    public void visit(DoubleConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.DOUBLE);
    }

    @Override
    public void visit(StringConstantInstruction insn) {
        define(insn.getReceiver(), ValueType.object("java.lang.String"));
    }

    @Override
    public void visit(BinaryInstruction insn) {
        switch (insn.getOperation()) {
            case ADD:
            case SUBTRACT:
            case MULTIPLY:
            case DIVIDE:
            case MODULO:
            case SHIFT_LEFT:
            case SHIFT_RIGHT:
            case SHIFT_RIGHT_UNSIGNED:
            case AND:
            case OR:
            case XOR:
                define(insn.getReceiver(), map(insn.getOperandType()));
                break;
            case COMPARE:
                define(insn.getReceiver(), ValueType.INTEGER);
                break;
        }
    }

    private ValueType map(NumericOperandType type) {
        switch (type) {
            case INT:
                return ValueType.INTEGER;
            case LONG:
                return ValueType.LONG;
            case FLOAT:
                return ValueType.FLOAT;
            case DOUBLE:
                return ValueType.DOUBLE;
        }
        throw new AssertionError("Unknown type: " + type);
    }

    @Override
    public void visit(NegateInstruction insn) {
        define(insn.getReceiver(), map(insn.getOperandType()));
    }

    @Override
    public void visit(AssignInstruction insn) {
        define(insn.getReceiver(), types[insn.getAssignee().getIndex()]);
    }

    @Override
    public void visit(CastInstruction insn) {
        define(insn.getReceiver(), insn.getTargetType());
    }

    @Override
    public void visit(CastNumberInstruction insn) {
        define(insn.getReceiver(), map(insn.getTargetType()));
    }

    @Override
    public void visit(BranchingInstruction insn) {
    }

    @Override
    public void visit(BinaryBranchingInstruction insn) {
    }

    @Override
    public void visit(JumpInstruction insn) {
    }

    @Override
    public void visit(SwitchInstruction insn) {
    }

    @Override
    public void visit(ExitInstruction insn) {
    }

    @Override
    public void visit(RaiseInstruction insn) {
    }

    @Override
    public void visit(ConstructArrayInstruction insn) {
        define(insn.getReceiver(), ValueType.arrayOf(insn.getItemType()));
    }

    @Override
    public void visit(ConstructInstruction insn) {
        define(insn.getReceiver(), ValueType.object(insn.getType()));
    }

    @Override
    public void visit(ConstructMultiArrayInstruction insn) {
        ValueType type = insn.getItemType();
        for (int i = 0; i < insn.getDimensions().size(); ++i) {
            type = ValueType.arrayOf(type);
        }
        define(insn.getReceiver(), type);
    }

    @Override
    public void visit(GetFieldInstruction insn) {
        FieldHolder field = getRealField(new FieldReference(insn.getClassName(), insn.getField()));
        if (field == null) {
            throw new RuntimeException("Field not found: " + insn.getClassName() + "." + insn.getField());
        }
        define(insn.getReceiver(), field.getType());
    }

    private FieldHolder getRealField(FieldReference ref) {
        String className = ref.getClassName();
        while (className != null) {
            ClassHolder cls = classSource.getClassHolder(className);
            if (cls == null) {
                return null;
            }
            FieldHolder field = cls.getField(ref.getFieldName());
            if (field.getLevel() == AccessLevel.PRIVATE && !className.equals(ref.getClassName())) {
                return null;
            }
            return field;
        }
        return null;
    }

    private MethodHolder getRealMethod(MethodReference ref) {
        String className = ref.getClassName();
        while (className != null) {
            ClassHolder cls = classSource.getClassHolder(className);
            if (cls == null) {
                return null;
            }
            MethodHolder method = cls.getMethod(ref.getDescriptor());
            if (method.getLevel() == AccessLevel.PRIVATE && !className.equals(ref.getClassName())) {
                return null;
            }
            return method;
        }
        return null;
    }

    @Override
    public void visit(PutFieldInstruction insn) {
    }

    @Override
    public void visit(ArrayLengthInstruction insn) {
        define(insn.getReceiver(), ValueType.INTEGER);
    }

    @Override
    public void visit(CloneArrayInstruction insn) {
        define(insn.getReceiver(), types[insn.getArray().getIndex()]);
    }

    @Override
    public void visit(GetElementInstruction insn) {
        ValueType type = types[insn.getArray().getIndex()];
        if (!(type instanceof ValueType.Array)) {
            return;
        }
        ValueType itemType = ((ValueType.Array)type).getItemType();
        define(insn.getReceiver(), itemType);
    }

    @Override
    public void visit(PutElementInstruction insn) {
    }

    @Override
    public void visit(InvokeInstruction insn) {
        MethodHolder method = getRealMethod(new MethodReference(insn.getClassName(), insn.getMethod()));
        if (method == null) {
            throw new RuntimeException("Method not found: " + insn.getMethod());
        }
        if (insn.getMethod().getResultType() != ValueType.VOID) {
            define(insn.getReceiver(), insn.getMethod().getResultType());
        }
    }

    @Override
    public void visit(IsInstanceInstruction insn) {
        define(insn.getReceiver(), ValueType.BOOLEAN);
    }
}
