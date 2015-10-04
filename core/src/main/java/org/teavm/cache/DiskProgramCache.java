/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.cache;

import java.io.*;
import java.util.*;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.parsing.ClassDateProvider;

/**
 *
 * @author Alexey Andreev
 */
public class DiskProgramCache implements ProgramCache {
    private File directory;
    private ProgramIO programIO;
    private Map<MethodReference, Item> cache = new HashMap<>();
    private Set<MethodReference> newMethods = new HashSet<>();
    private ClassDateProvider classDateProvider;

    public DiskProgramCache(File directory, SymbolTable symbolTable, SymbolTable fileTable,
            ClassDateProvider classDateProvider) {
        this.directory = directory;
        programIO = new ProgramIO(symbolTable, fileTable);
        this.classDateProvider = classDateProvider;
    }

    @Override
    public Program get(MethodReference method) {
        Item item = cache.get(method);
        if (item == null) {
            item = new Item();
            cache.put(method, item);
            File file = getMethodFile(method);
            if (file.exists()) {
                try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                    DataInput input = new DataInputStream(stream);
                    int depCount = input.readShort();
                    boolean dependenciesChanged = false;
                    for (int i = 0; i < depCount; ++i) {
                        String depClass = input.readUTF();
                        Date depDate = classDateProvider.getModificationDate(depClass);
                        if (depDate == null || depDate.after(new Date(file.lastModified()))) {
                            dependenciesChanged = true;
                            break;
                        }
                    }
                    if (!dependenciesChanged) {
                        item.program = programIO.read(stream);
                    }
                } catch (IOException e) {
                    // we could not read program, just leave it empty
                }
            }
        }
        return item.program;
    }

    @Override
    public void store(MethodReference method, Program program) {
        Item item = new Item();
        cache.put(method, item);
        item.program = program;
        newMethods.add(method);
    }

    public void flush() throws IOException {
        for (MethodReference method : newMethods) {
            File file = getMethodFile(method);
            ProgramDependencyAnalyzer analyzer = new ProgramDependencyAnalyzer();
            analyzer.dependencies.add(method.getClassName());
            Program program = cache.get(method).program;
            for (int i = 0; i < program.basicBlockCount(); ++i) {
                BasicBlock block = program.basicBlockAt(i);
                for (Instruction insn : block.getInstructions()) {
                    insn.acceptVisitor(analyzer);
                }
            }
            file.getParentFile().mkdirs();
            try (OutputStream stream = new BufferedOutputStream(new FileOutputStream(file))) {
                DataOutput output = new DataOutputStream(stream);
                output.writeShort(analyzer.dependencies.size());
                for (String dep : analyzer.dependencies) {
                    output.writeUTF(dep);
                }
                programIO.write(program, stream);
            }
        }
    }

    private File getMethodFile(MethodReference method) {
        File dir = new File(directory, method.getClassName().replace('.', '/'));
        return new File(dir, FileNameEncoder.encodeFileName(method.getDescriptor().toString()) + ".teavm-opt");
    }

    static class Item {
        Program program;
    }

    static class ProgramDependencyAnalyzer implements InstructionVisitor {
        Set<String> dependencies = new HashSet<>();
        @Override public void visit(GetFieldInstruction insn) {
            dependencies.add(insn.getField().getClassName());
        }
        @Override public void visit(PutFieldInstruction insn) {
            dependencies.add(insn.getField().getClassName());
        }
        @Override public void visit(InvokeInstruction insn) {
            dependencies.add(insn.getMethod().getClassName());
        }
        @Override
        public void visit(InvokeDynamicInstruction insn) {
            for (RuntimeConstant cst : insn.getBootstrapArguments()) {
                if (cst.getKind() == RuntimeConstant.METHOD_HANDLE) {
                    MethodHandle handle = cst.getMethodHandle();
                    dependencies.add(handle.getClassName());
                }
            }
        }
        @Override public void visit(EmptyInstruction insn) { }
        @Override public void visit(ClassConstantInstruction insn) { }
        @Override public void visit(NullConstantInstruction insn) { }
        @Override public void visit(IntegerConstantInstruction insn) { }
        @Override public void visit(LongConstantInstruction insn) { }
        @Override public void visit(FloatConstantInstruction insn) { }
        @Override public void visit(DoubleConstantInstruction insn) { }
        @Override public void visit(StringConstantInstruction insn) { }
        @Override public void visit(BinaryInstruction insn) { }
        @Override public void visit(NegateInstruction insn) { }
        @Override public void visit(AssignInstruction insn) { }
        @Override public void visit(CastInstruction insn) { }
        @Override public void visit(CastNumberInstruction insn) { }
        @Override public void visit(CastIntegerInstruction insn) { }
        @Override public void visit(BranchingInstruction insn) { }
        @Override public void visit(BinaryBranchingInstruction insn) { }
        @Override public void visit(JumpInstruction insn) { }
        @Override public void visit(SwitchInstruction insn) { }
        @Override public void visit(ExitInstruction insn) { }
        @Override public void visit(RaiseInstruction insn) { }
        @Override public void visit(ConstructArrayInstruction insn) { }
        @Override public void visit(ConstructInstruction insn) { }
        @Override public void visit(ConstructMultiArrayInstruction insn) { }
        @Override public void visit(ArrayLengthInstruction insn) { }
        @Override public void visit(CloneArrayInstruction insn) { }
        @Override public void visit(UnwrapArrayInstruction insn) { }
        @Override public void visit(GetElementInstruction insn) { }
        @Override public void visit(PutElementInstruction insn) { }
        @Override public void visit(IsInstanceInstruction insn) { }
        @Override public void visit(InitClassInstruction insn) { }
        @Override public void visit(NullCheckInstruction insn) { }
        @Override public void visit(MonitorEnterInstruction insn) { }
        @Override public void visit(MonitorExitInstruction insn) { }
    }
}
