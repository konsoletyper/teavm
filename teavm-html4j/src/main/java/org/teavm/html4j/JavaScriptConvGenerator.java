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
package org.teavm.html4j;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.model.*;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class JavaScriptConvGenerator implements Generator {
    private static final String convCls = JavaScriptConv.class.getName();
    static final MethodReference intValueMethod = new MethodReference("java.lang.Integer",
            new MethodDescriptor("intValue", ValueType.INTEGER));
    static final MethodReference booleanValueMethod = new MethodReference("java.lang.Boolean",
            new MethodDescriptor("booleanValue", ValueType.BOOLEAN));
    static final MethodReference doubleValueMethod = new MethodReference("java.lang.Double",
            new MethodDescriptor("doubleValue", ValueType.DOUBLE));
    static final MethodReference charValueMethod = new MethodReference("java.lang.Character",
            new MethodDescriptor("charValue", ValueType.CHARACTER));
    static final MethodReference valueOfIntMethod = new MethodReference("java.lang.Integer",
            new MethodDescriptor("valueOf", ValueType.INTEGER, ValueType.object("java.lang.Integer")));
    static final MethodReference valueOfBooleanMethod = new MethodReference("java.lang.Boolean",
            new MethodDescriptor("valueOf", ValueType.BOOLEAN, ValueType.object("java.lang.Boolean")));
    static final MethodReference valueOfDoubleMethod = new MethodReference("java.lang.Double",
            new MethodDescriptor("valueOf", ValueType.DOUBLE, ValueType.object("java.lang.Double")));
    static final MethodReference valueOfCharMethod = new MethodReference("java.lang.Character",
            new MethodDescriptor("valueOf", ValueType.CHARACTER, ValueType.object("java.lang.Character")));
    private static final ValueType objType = ValueType.object("java.lang.Object");
    static final MethodReference toJsMethod = new MethodReference(convCls, new MethodDescriptor(
            "toJavaScript", objType, objType));
    static final MethodReference fromJsMethod = new MethodReference(convCls, new MethodDescriptor(
            "fromJavaScript", objType, objType, objType));

    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "toJavaScript":
                generateToJavaScript(context, writer);
                break;
            case "fromJavaScript":
                generateFromJavaScript(context, writer);
                break;
        }
    }

    private void generateToJavaScript(GeneratorContext context, SourceWriter writer) throws IOException {
        String obj = context.getParameterName(1);
        writer.append("if (" + obj + " === null || " + obj + " === undefined) {").softNewLine().indent();
        writer.append("return " + obj + ";").softNewLine();
        writer.outdent().append("} else if (typeof " + obj + " === 'number') {").indent().softNewLine();
        writer.append("return " + obj + ";").softNewLine();
        writer.outdent().append("} else if (" + obj + ".constructor.$meta && " + obj + ".constructor.$meta.item) {")
                .indent().softNewLine();
        writer.append("var arr = new Array(" + obj + ".data.length);").softNewLine();
        writer.append("for (var i = 0; i < arr.length; ++i) {").indent().softNewLine();
        writer.append("arr[i] = ").appendMethodBody(toJsMethod).append("(" + obj + ".data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return arr;").softNewLine();
        writer.outdent().append("} else if (" + obj + ".constructor === ").appendClass("java.lang.String")
                .append(") {").indent().softNewLine();
        generateStringToJavaScript(context, writer);
        writer.outdent().append("} else if (" + obj + ".constructor === ").appendClass("java.lang.Integer")
                .append(") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(intValueMethod).append("(" + obj + ")|0;").softNewLine();
        writer.outdent().append("} else if (" + obj + ".constructor === ").appendClass("java.lang.Boolean")
                .append(") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(booleanValueMethod).append("(" + obj + ")!==0;").softNewLine();
        writer.outdent().append("} else if (" + obj + ".constructor === ").appendClass("java.lang.Double")
                .append(") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(doubleValueMethod).append("(" + obj + ");").softNewLine();
        writer.outdent().append("} else if (" + obj + ".constructor === ").appendClass("java.lang.Character")
                .append(") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(charValueMethod).append("(" + obj + ");").softNewLine();
        writer.outdent().append("} else {").indent().softNewLine();
        writer.append("return " + obj + ";").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateFromJavaScript(GeneratorContext context, SourceWriter writer) throws IOException {
        String obj = context.getParameterName(1);
        String type = context.getParameterName(2);
        writer.append("if (" + obj +" === null || " + obj + " === undefined)").ws().append("{")
                .softNewLine().indent();
        writer.append("return " + obj +";").softNewLine();
        writer.outdent().append("} else if (" + type + ".$meta.item) {").indent().softNewLine();
        writer.append("var arr = $rt_createArray(" + type + ".$meta.item, " + obj + ".length);").softNewLine();
        writer.append("for (var i = 0; i < arr.data.length; ++i) {").indent().softNewLine();
        writer.append("arr.data[i] = ").appendMethodBody(fromJsMethod).append("(" + obj + "[i], " +
                type + ".$meta.item);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return arr;").softNewLine();
        writer.outdent().append("} else if (" + type + " === ").appendClass("java.lang.String")
                .append(") {").indent().softNewLine();
        writer.append("return $rt_str(" + obj + ");").softNewLine();
        writer.outdent().append("} else if (" + type + " === ").appendClass("java.lang.Integer")
                .append(") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(valueOfIntMethod).append("(" + obj + ");").softNewLine();
        writer.outdent().append("} else if (" + type + " === ").appendClass("java.lang.Double")
                .append(") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(valueOfDoubleMethod).append("(" + obj + ");").softNewLine();
        writer.outdent().append("} else if (" + type + " === $rt_intcls()) {").indent().softNewLine();
        writer.append("return " + obj + "|0;").softNewLine();
        writer.outdent().append("} else if (" + type + " === ").appendClass("java.lang.Boolean")
                .append(") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(valueOfBooleanMethod).append("(" + obj + "?1:0);").softNewLine();
        writer.outdent().append("} else if (" + type + " === ").appendClass("java.lang.Character")
                .append(") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(valueOfCharMethod).append("(typeof " + obj + " === 'number' ? " +
                obj + "0xFFFF : " + obj + ".charCodeAt(0));").softNewLine();
        writer.outdent().append("} else if (" + type + " === $rt_booleancls()) {").indent().softNewLine();
        writer.append("return " + obj + "?1:0;").softNewLine();
        writer.outdent().append("} else if (" + obj + " instanceof Array) {").indent().softNewLine();
        writer.append("var arr = $rt_createArray($rt_objcls(), " + obj + ".length);").softNewLine();
        writer.append("for (var i = 0; i < arr.data.length; ++i) {").indent().softNewLine();
        writer.append("arr.data[i] = ").appendMethodBody(fromJsMethod).append("(" + obj + "[i], $rt_objcls());")
                .softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return arr;").softNewLine();
        writer.outdent().append("} else if (typeof " + obj + " === 'string') {").indent().softNewLine();
        writer.append("return $rt_str(" + obj + ");").softNewLine();
        writer.outdent().append("} else if (typeof " + obj + " === 'number') {").indent().softNewLine();
        writer.append("if (" + obj + "|0 === " + obj + ") {").indent().softNewLine();
        writer.append("return ").appendMethodBody(valueOfIntMethod).append("(" + obj + ");").softNewLine();
        writer.outdent().append("} else {").indent().softNewLine();
        writer.append("return ").appendMethodBody(valueOfDoubleMethod).append("(" + obj + ");").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.outdent().append("} else if (typeof " + obj + " === 'boolean') {").indent().softNewLine();
        writer.append("return ").appendMethodBody(valueOfBooleanMethod).append("(" + obj + "?1:0);").softNewLine();
        writer.outdent().append("} else {").indent().softNewLine();
        writer.append("return ").append(obj).append(";").softNewLine();
        writer.outdent().append("}").softNewLine();
    }

    private void generateStringToJavaScript(GeneratorContext context, SourceWriter writer) throws IOException {
        FieldReference charsField = new FieldReference("java.lang.String", "characters");
        writer.append("var result = \"\";").softNewLine();
        writer.append("var data = ").append(context.getParameterName(1)).append('.')
                .appendField(charsField).append(".data;").softNewLine();
        writer.append("for (var i = 0; i < data.length; i = (i + 1) | 0) {").indent().softNewLine();
        writer.append("result += String.fromCharCode(data[i]);").softNewLine();
        writer.outdent().append("}").softNewLine();
        writer.append("return result;").softNewLine();
    }
}
