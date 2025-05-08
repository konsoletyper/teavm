/*
 *  Copyright 2025 Alexey Andreev.
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

import java.util.Set;
import org.teavm.backend.c.CModuleExport;
import org.teavm.interop.Structure;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassReader;
import org.teavm.model.ElementModifier;
import org.teavm.model.MethodHolder;
import org.teavm.model.ValueType;

public class LibraryExportGenerator {
    private GenerationContext context;
    private CodeWriter writer;
    private IncludeManager includes;
    private CodeWriter headerWriter;
    private CodeWriter forwardTypeWriter;
    private CodeWriter typeWriter;
    private IncludeManager headerIncludes;
    private ClassGenerationContext classContext;
    private Set<String> exportedTypes;
    private boolean exportIncluded;

    public LibraryExportGenerator(GenerationContext context, CodeWriter writer, IncludeManager includes) {
        this.context = context;
        this.writer = writer;
        this.includes = includes;
        forwardTypeWriter = writer.fragment();
        typeWriter = writer.fragment();
        classContext = new ClassGenerationContext(context, includes, writer.fragment(), null, null);
    }

    public void generate(ClassHolder cls) {
        for (var method : cls.getMethods()) {
            var annot = method.getAnnotations().get(CModuleExport.class.getName());
            if (annot != null) {
                var exportName = annot.getValue("name").getString();
                generateFunction(method, exportName);
            }
        }
    }

    private void generateFunction(MethodHolder method, String exportName) {
        headerWriter.println(typeToExernString(method.getResultType(), "") + " " + exportName + "(");
        writer.println("TEAVM_EXPORT " + typeToInternString(method.getResultType(), "") + " " + exportName + "(");
        for (var i = 0; i < method.parameterCount(); ++i) {
            if (i > 0) {
                headerWriter.print(", ");
                writer.print(", ");
            }
            var paramName = "p" + i;
            headerWriter.print(typeToExernString(method.parameterType(i), ""));
            writer.print(typeToInternString(method.parameterType(i), paramName));
        }
        headerWriter.println(");");
        writer.println(") {").indent();
        var names = context.getNames();
        writer.print(names.forMethod(method.getReference())).print("(");
        for (var i = 0; i < method.parameterCount(); ++i) {
            if (i > 0) {
                writer.print(", ");
            }
            writer.print("p_1" + i);
        }
        writer.println(");");
        writer.outdent().println("}");
    }

    private String typeToInternString(ValueType type, String subj) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    includes.addInclude("<bool.h>");
                    return "bool" + subj;
                case BYTE:
                    includes.addInclude("<stdint.h>");
                    return "int8_t" + subj;
                case SHORT:
                    includes.addInclude("<stdint.h>");
                    return "int16_t" + subj;
                case CHARACTER:
                    includes.addInclude("<uchar.h>");
                    return "char16_t" + subj;
                case INTEGER:
                    includes.addInclude("<stdint.h>");
                    return "int32_t" + subj;
                case LONG:
                    includes.includePath("<stdint.h>");
                    return "int64_t" + subj;
                case FLOAT:
                    return "float" + subj;
                case DOUBLE:
                    return "double" + subj;
            }
        } else if (type instanceof ValueType.Void) {
            return "void" + subj;
        } else if (type instanceof ValueType.Object) {
            var name = ((ValueType.Object) type).getClassName();
            if (name.equals("java.lang.String")) {
                return "char*" + subj;
            } else if (context.getCharacteristics().isStructure(name)) {
                return context.getNames().forClass(name) + "*" + subj;
            } else if (context.getCharacteristics().isFunction(name)) {
                return functionInternType(name, subj);
            }
        }
        return "void*";
    }

    private String typeToExernString(ValueType type, String subj) {
        if (type instanceof ValueType.Primitive) {
            switch (((ValueType.Primitive) type).getKind()) {
                case BOOLEAN:
                    headerIncludes.addInclude("<bool.h>");
                    return "bool" + subj;
                case BYTE:
                    headerIncludes.addInclude("<stdint.h>");
                    return "int8_t" + subj;
                case SHORT:
                    headerIncludes.addInclude("<stdint.h>");
                    return "int16_t" + subj;
                case CHARACTER:
                    headerIncludes.addInclude("<uchar.h>");
                    return "char16_t" + subj;
                case INTEGER:
                    headerIncludes.addInclude("<stdint.h>");
                    return "int32_t" + subj;
                case LONG:
                    headerIncludes.includePath("<stdint.h>");
                    return "int64_t" + subj;
                case FLOAT:
                    return "float" + subj;
                case DOUBLE:
                    return "double" + subj;
            }
        } else if (type instanceof ValueType.Void) {
            return "void" + subj;
        } else if (type instanceof ValueType.Object) {
            var name = ((ValueType.Object) type).getClassName();
            if (name.equals("java.lang.String")) {
                return "char*" + subj;
            } else if (context.getCharacteristics().isStructure(name)) {
                exportStruct(name);
                return exportedStructName(name) + "*" + subj;
            } else if (context.getCharacteristics().isFunction(name)) {
                return functionExternType(name, subj);
            }
        }
        return "void*";
    }

    private void exportStruct(String name) {
        if (!exportedTypes.add(name)) {
            return;
        }
        var exportedName = exportedStructName(name);
        forwardTypeWriter.println("struct " + exportedName + ";");
        typeWriter.println("typedef struct " + exportedName + " {").indent();
        var cls = context.getClassSource().get(name);
        writeFields(cls);
        typeWriter.outdent().println("} " + exportedName + ";");
    }

    private String functionInternType(String name, String subj) {
        var cls = context.getClassSource().get(name);
        for (var method : cls.getMethods()) {
            if (!method.hasModifier(ElementModifier.STATIC) && method.hasModifier(ElementModifier.ABSTRACT)) {
                var result = new StringBuilder(typeToInternString(method.getResultType(), "(*" + subj + ")("));
                for (var i = 0; i < method.parameterCount(); ++i) {
                    if (i > 0) {
                        result.append(", ");
                    }
                    result.append(typeToInternString(method.parameterType(i), ""));
                }
                return result.toString();
            }
        }
        throw new IllegalStateException();
    }

    private String functionExternType(String name, String subj) {
        var cls = context.getClassSource().get(name);
        for (var method : cls.getMethods()) {
            if (!method.hasModifier(ElementModifier.STATIC) && method.hasModifier(ElementModifier.ABSTRACT)) {
                var result = new StringBuilder(typeToExernString(method.getResultType(), "(*" + subj + ")("));
                for (var i = 0; i < method.parameterCount(); ++i) {
                    if (i > 0) {
                        result.append(", ");
                    }
                    result.append(typeToExernString(method.parameterType(i), ""));
                }
                return result.toString();
            }
        }
        throw new IllegalStateException();
    }

    private void writeFields(ClassReader cls) {
        if (cls.getParent() == null || cls.getParent().equals(Structure.class.getName())) {
            return;
        }
        var parent = context.getClassSource().get(cls.getParent());
        if (parent != null) {
            writeFields(parent);
        }
        for (var field : cls.getFields()) {
            if (!field.hasModifier(ElementModifier.STATIC)) {
                continue;
            }
            writer.println(typeToExernString(field.getType(), " " + field.getName()) + ";");
        }
    }

    private String exportedStructName(String className) {
        return className.substring(className.lastIndexOf('.') + 1);
    }
}
