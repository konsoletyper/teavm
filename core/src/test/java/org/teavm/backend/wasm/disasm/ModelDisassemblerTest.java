/*
 *  Copyright 2026 Alexey Andreev.
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
package org.teavm.backend.wasm.disasm;

import static org.junit.Assert.assertEquals;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.teavm.backend.wasm.model.WasmArray;
import org.teavm.backend.wasm.model.WasmField;
import org.teavm.backend.wasm.model.WasmFunction;
import org.teavm.backend.wasm.model.WasmFunctionType;
import org.teavm.backend.wasm.model.WasmGlobal;
import org.teavm.backend.wasm.model.WasmLocal;
import org.teavm.backend.wasm.model.WasmModule;
import org.teavm.backend.wasm.model.WasmNumType;
import org.teavm.backend.wasm.model.WasmPackedType;
import org.teavm.backend.wasm.model.WasmStorageType;
import org.teavm.backend.wasm.model.WasmStructure;
import org.teavm.backend.wasm.model.WasmTag;
import org.teavm.backend.wasm.model.WasmType;
import org.teavm.backend.wasm.model.instruction.WasmArrayCopy;
import org.teavm.backend.wasm.model.instruction.WasmArrayGet;
import org.teavm.backend.wasm.model.instruction.WasmArrayLength;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewDefault;
import org.teavm.backend.wasm.model.instruction.WasmArrayNewFixed;
import org.teavm.backend.wasm.model.instruction.WasmArraySet;
import org.teavm.backend.wasm.model.instruction.WasmBlock;
import org.teavm.backend.wasm.model.instruction.WasmBranch;
import org.teavm.backend.wasm.model.instruction.WasmBreak;
import org.teavm.backend.wasm.model.instruction.WasmCall;
import org.teavm.backend.wasm.model.instruction.WasmCallReference;
import org.teavm.backend.wasm.model.instruction.WasmCast;
import org.teavm.backend.wasm.model.instruction.WasmCastBranch;
import org.teavm.backend.wasm.model.instruction.WasmCastCondition;
import org.teavm.backend.wasm.model.instruction.WasmCatchClause;
import org.teavm.backend.wasm.model.instruction.WasmConditional;
import org.teavm.backend.wasm.model.instruction.WasmConversion;
import org.teavm.backend.wasm.model.instruction.WasmDrop;
import org.teavm.backend.wasm.model.instruction.WasmExternConversion;
import org.teavm.backend.wasm.model.instruction.WasmExternConversionType;
import org.teavm.backend.wasm.model.instruction.WasmFloat32Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloat64Constant;
import org.teavm.backend.wasm.model.instruction.WasmFloatBinary;
import org.teavm.backend.wasm.model.instruction.WasmFloatBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmFloatType;
import org.teavm.backend.wasm.model.instruction.WasmFloatUnary;
import org.teavm.backend.wasm.model.instruction.WasmFloatUnaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmFunctionReference;
import org.teavm.backend.wasm.model.instruction.WasmGetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmGetLocal;
import org.teavm.backend.wasm.model.instruction.WasmInstructionList;
import org.teavm.backend.wasm.model.instruction.WasmInt31Get;
import org.teavm.backend.wasm.model.instruction.WasmInt31Reference;
import org.teavm.backend.wasm.model.instruction.WasmInt32Constant;
import org.teavm.backend.wasm.model.instruction.WasmInt32Subtype;
import org.teavm.backend.wasm.model.instruction.WasmInt64Constant;
import org.teavm.backend.wasm.model.instruction.WasmIntBinary;
import org.teavm.backend.wasm.model.instruction.WasmIntBinaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIntType;
import org.teavm.backend.wasm.model.instruction.WasmIntUnary;
import org.teavm.backend.wasm.model.instruction.WasmIntUnaryOperation;
import org.teavm.backend.wasm.model.instruction.WasmIsNull;
import org.teavm.backend.wasm.model.instruction.WasmLoadFloat64;
import org.teavm.backend.wasm.model.instruction.WasmLoadInt32;
import org.teavm.backend.wasm.model.instruction.WasmNullBranch;
import org.teavm.backend.wasm.model.instruction.WasmNullCondition;
import org.teavm.backend.wasm.model.instruction.WasmNullConstant;
import org.teavm.backend.wasm.model.instruction.WasmReferencesEqual;
import org.teavm.backend.wasm.model.instruction.WasmReturn;
import org.teavm.backend.wasm.model.instruction.WasmSetGlobal;
import org.teavm.backend.wasm.model.instruction.WasmSetLocal;
import org.teavm.backend.wasm.model.instruction.WasmSignedType;
import org.teavm.backend.wasm.model.instruction.WasmStoreInt32;
import org.teavm.backend.wasm.model.instruction.WasmStructGet;
import org.teavm.backend.wasm.model.instruction.WasmStructNew;
import org.teavm.backend.wasm.model.instruction.WasmStructNewDefault;
import org.teavm.backend.wasm.model.instruction.WasmStructSet;
import org.teavm.backend.wasm.model.instruction.WasmSwitch;
import org.teavm.backend.wasm.model.instruction.WasmTeeLocal;
import org.teavm.backend.wasm.model.instruction.WasmTest;
import org.teavm.backend.wasm.model.instruction.WasmThrow;
import org.teavm.backend.wasm.model.instruction.WasmTry;
import org.teavm.backend.wasm.model.instruction.WasmUnreachable;
import org.teavm.backend.wasm.render.WasmBinaryRenderer;
import org.teavm.backend.wasm.render.WasmBinaryStatsCollector;
import org.teavm.backend.wasm.render.WasmBinaryVersion;
import org.teavm.backend.wasm.render.WasmBinaryWriter;

public class ModelDisassemblerTest {
    @Test
    public void function() {
        var function = new WasmFunction(new WasmFunctionType("ft", WasmType.INT32, List.of(WasmType.INT32)));
        function.setName("f");
        var param = new WasmLocal(WasmType.INT32, "p");
        function.add(param);
        var local = new WasmLocal(WasmType.FLOAT64, "x");
        function.add(local);
        function.getBody().add(new WasmGetLocal(param));
        function.getBody().add(new WasmReturn());

        assertEquals(String.join("\n",
                "(func $f (type $ft)",
                "  (param $p i32)",
                "  (result i32)",
                "  (; locals 1 ;)",
                "  (local $x f64)",
                "  local.get $p",
                "  return",
                ")"
        ), normalize(ModelDisassembler.toString(function)));
    }

    /**
     * Checks textual representation of float constants and conversions, round trip only checks consistency.
     */
    @Test
    public void floatConstantsAndConversions() {
        var function = new WasmFunction(new WasmFunctionType("ft", (WasmType) null, List.of()));
        var body = function.getBody();
        body.add(new WasmFloat32Constant(3));
        body.add(new WasmFloat64Constant(4));
        body.add(new WasmConversion(WasmNumType.INT32, WasmNumType.INT64, true));
        body.add(new WasmConversion(WasmNumType.INT64, WasmNumType.FLOAT64, false));
        var truncation = new WasmConversion(WasmNumType.FLOAT32, WasmNumType.INT32, true);
        truncation.setNonTrapping(true);
        body.add(truncation);
        var reinterpretation = new WasmConversion(WasmNumType.FLOAT64, WasmNumType.INT64, false);
        reinterpretation.setReinterpret(true);
        body.add(reinterpretation);

        assertEquals(String.join("\n",
                "f32.const 0x1.8p1",
                "f64.const 0x1.0p2",
                "i64.extend_i32_s",
                "f64.convert_i64_u",
                "i32.trunc_sat_f32_s",
                "i64.reinterpret_f64"
        ), normalize(ModelDisassembler.bodyToString(function)));
    }

    /**
     * Checks that disassembly of model matches disassembly of the same model, rendered to binary.
     */
    @Test
    public void matchesBinaryDisassembly() {
        var module = new WasmModule();
        var structure = new WasmStructure("S", fields -> {
            fields.add(new WasmField(WasmType.INT32, "x"));
            fields.add(new WasmField(WasmStorageType.packed(WasmPackedType.INT8), "y"));
        });
        var array = new WasmArray("A", WasmType.INT32.asStorage());
        var calleeType = new WasmFunctionType("calleeType", WasmType.INT32, List.of(WasmType.INT32));
        var functionType = new WasmFunctionType("functionType", (WasmType) null, List.of(WasmType.INT32));
        var tagType = new WasmFunctionType("tagType", (WasmType) null, List.of());
        module.types.add(structure);
        module.types.add(array);
        module.types.add(calleeType);
        module.types.add(functionType);
        module.types.add(tagType);

        var callee = new WasmFunction(calleeType);
        callee.setName("callee");
        callee.add(new WasmLocal(WasmType.INT32, "q"));
        callee.getBody().add(new WasmUnreachable());
        module.functions.add(callee);

        var global = new WasmGlobal("g", WasmType.INT32);
        global.getInitialValue().add(new WasmInt32Constant(0));
        module.globals.add(global);

        var tag = new WasmTag(tagType);
        module.tags.add(tag);

        var function = new WasmFunction(functionType);
        function.setName("f");
        var p = new WasmLocal(WasmType.INT32, "p");
        var r = new WasmLocal(structure.getReference(), "r");
        function.add(p);
        function.add(r);
        module.functions.add(function);

        var body = function.getBody();
        body.add(new WasmInt32Constant(1));
        body.add(new WasmInt64Constant(2));
        body.add(new WasmFloat32Constant(3));
        body.add(new WasmFloat64Constant(4));
        body.add(new WasmIntBinary(WasmIntType.INT64, WasmIntBinaryOperation.SHR_UNSIGNED));
        body.add(new WasmIntUnary(WasmIntType.INT32, WasmIntUnaryOperation.EQZ));
        body.add(new WasmFloatBinary(WasmFloatType.FLOAT32, WasmFloatBinaryOperation.LE));
        body.add(new WasmFloatUnary(WasmFloatType.FLOAT64, WasmFloatUnaryOperation.SQRT));
        addConversions(body);
        body.add(new WasmGetLocal(p));
        body.add(new WasmSetLocal(p));
        body.add(new WasmTeeLocal(r));
        body.add(new WasmGetGlobal(global));
        body.add(new WasmSetGlobal(global));
        body.add(new WasmCall(callee));
        body.add(new WasmFunctionReference(callee));
        body.add(new WasmCallReference(calleeType));
        body.add(new WasmDrop());
        body.add(new WasmLoadInt32(2, WasmInt32Subtype.UINT16));
        body.add(new WasmStoreInt32(4, WasmInt32Subtype.INT32));
        body.add(new WasmLoadFloat64(8));

        var outer = new WasmBlock(false);
        outer.setType(WasmType.INT32.asBlock());
        body.add(outer);
        var loop = new WasmBlock(true);
        outer.getBody().add(loop);
        loop.getBody().add(new WasmBranch(loop.getBody()));
        loop.getBody().add(new WasmBranch(outer.getBody()));
        var conditional = new WasmConditional();
        loop.getBody().add(conditional);
        conditional.getThenBlock().add(new WasmBreak(loop.getBody()));
        conditional.getElseBlock().add(new WasmNullBranch(WasmNullCondition.NULL, outer.getBody()));
        conditional.getElseBlock().add(new WasmNullBranch(WasmNullCondition.NOT_NULL, loop.getBody()));
        var switchInsn = new WasmSwitch(outer.getBody());
        switchInsn.getTargets().add(loop.getBody());
        switchInsn.getTargets().add(outer.getBody());
        loop.getBody().add(switchInsn);
        outer.getBody().add(new WasmCastBranch(WasmCastCondition.SUCCESS, WasmType.ANY,
                structure.getNonNullReference(), outer.getBody()));
        outer.getBody().add(new WasmCastBranch(WasmCastCondition.FAILURE, WasmType.EQ,
                array.getReference(), outer.getBody()));

        var handler = new WasmBlock(false);
        body.add(handler);
        var tryInsn = new WasmTry();
        handler.getBody().add(tryInsn);
        tryInsn.getCatches().add(new WasmCatchClause(tag, false, handler.getBody()));
        tryInsn.getCatches().add(new WasmCatchClause(null, true, handler.getBody()));
        tryInsn.getBody().add(new WasmThrow(tag));
        tryInsn.getBody().add(new WasmThrow(null));

        body.add(new WasmNullConstant(WasmType.EXTERN));
        body.add(new WasmIsNull());
        body.add(new WasmReferencesEqual());
        body.add(new WasmCast(structure.getReference()));
        body.add(new WasmTest(array.getNonNullReference()));
        body.add(new WasmExternConversion(WasmExternConversionType.ANY_TO_EXTERN));
        body.add(new WasmExternConversion(WasmExternConversionType.EXTERN_TO_ANY));
        body.add(new WasmStructNew(structure));
        body.add(new WasmStructNewDefault(structure));
        body.add(new WasmStructGet(structure, 0));
        var signedGet = new WasmStructGet(structure, 1);
        signedGet.setSignedType(WasmSignedType.SIGNED);
        body.add(signedGet);
        body.add(new WasmStructSet(structure, 1));
        body.add(new WasmArrayNewDefault(array));
        body.add(new WasmArrayNewFixed(array, 3));
        body.add(new WasmArrayGet(array));
        body.add(new WasmArraySet(array));
        body.add(new WasmArrayLength());
        body.add(new WasmArrayCopy(array, array));
        body.add(new WasmInt31Reference());
        body.add(new WasmInt31Get(WasmSignedType.UNSIGNED));
        body.add(new WasmReturn());

        var modelDisassembly = new StringWriter();
        var modelWriter = new DisassemblyTextWriter(new PrintWriter(modelDisassembly));
        modelWriter.setWithIndices(false);
        new ModelDisassembler(modelWriter).disassembleBody(function);
        modelWriter.flush();

        // Indices are assigned differently, so compare only names
        assertEquals(binaryDisassembly(module, function), normalize(modelDisassembly.toString()));
    }

    /**
     * Adds all supported conversions.
     */
    private static void addConversions(WasmInstructionList body) {
        var numTypes = WasmNumType.values();
        for (var source : numTypes) {
            for (var target : numTypes) {
                if (source == target) {
                    continue;
                }
                var isInt = source == WasmNumType.INT32 || source == WasmNumType.INT64;
                var targetIsInt = target == WasmNumType.INT32 || target == WasmNumType.INT64;
                if (isInt && targetIsInt) {
                    // extend or wrap
                    body.add(new WasmConversion(source, target, true));
                    if (source == WasmNumType.INT32) {
                        body.add(new WasmConversion(source, target, false));
                    }
                    continue;
                }
                if (!isInt && !targetIsInt) {
                    // promote or demote
                    body.add(new WasmConversion(source, target, true));
                    continue;
                }
                for (var signed : new boolean[] { true, false }) {
                    body.add(new WasmConversion(source, target, signed));
                    if (!isInt) {
                        var nonTrapping = new WasmConversion(source, target, signed);
                        nonTrapping.setNonTrapping(true);
                        body.add(nonTrapping);
                    }
                }
                if (bits(source) == bits(target)) {
                    var reinterpretation = new WasmConversion(source, target, false);
                    reinterpretation.setReinterpret(true);
                    body.add(reinterpretation);
                }
            }
        }
    }

    private static int bits(WasmNumType type) {
        return type == WasmNumType.INT32 || type == WasmNumType.FLOAT32 ? 32 : 64;
    }

    private String binaryDisassembly(WasmModule module, WasmFunction function) {
        module.prepareForRendering();
        var binaryWriter = new WasmBinaryWriter();
        new WasmBinaryRenderer(binaryWriter, WasmBinaryVersion.V_0x1, false, null, null, null, null,
                WasmBinaryStatsCollector.EMPTY).render(module);

        var output = new StringWriter();
        var writer = new DisassemblyTextWriter(new PrintWriter(output));
        writer.setWithIndices(false);
        new Disassembler(writer).disassemble(binaryWriter.getData());
        writer.flush();

        // Take body of the function from the code section, i.e. lines between the last local declaration
        // and closing parenthesis
        var lines = output.toString().lines().collect(Collectors.toList());
        var start = lines.indexOf(lines.stream().filter(line -> line.contains("$" + function.getName() + " (type"))
                .reduce((first, second) -> second).orElseThrow());
        while (lines.get(start).trim().startsWith("(")) {
            ++start;
        }
        var indent = lines.get(start).length() - lines.get(start).stripLeading().length();
        var end = start;
        while (!lines.get(end).trim().equals(")")) {
            ++end;
        }
        return lines.subList(start, end).stream()
                .map(line -> line.substring(indent))
                .collect(Collectors.joining("\n"));
    }

    private static String normalize(String text) {
        return text.lines().collect(Collectors.joining("\n"));
    }
}
