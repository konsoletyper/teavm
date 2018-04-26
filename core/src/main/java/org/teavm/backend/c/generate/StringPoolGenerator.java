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

import java.util.List;
import org.teavm.model.FieldReference;

public class StringPoolGenerator {
    private CodeWriter writer;
    private NameProvider names;

    public StringPoolGenerator(CodeWriter writer, NameProvider names) {
        this.writer = writer;
        this.names = names;
    }

    public void generate(List<? extends String> strings) {
        generateStringArrays(strings);
        generateStringObjects(strings);
    }

    private void generateStringArrays(List<? extends String> strings) {
        for (int i = 0; i < strings.size(); ++i) {
            String s = strings.get(i);
            writer.print("static struct { JavaArray hdr; char16_t data[" + (s.length() + 1) + "]; } str_array_" + i)
                    .println(" = {").indent();
            writer.println(".hdr = { .size = " + s.length() + "},");
            writer.print(".data = ");
            generateStringLiteral(s);
            writer.println();

            writer.outdent().println("};");
        }
    }

    private void generateStringObjects(List<? extends String> strings) {
        String charactersName = names.forMemberField(new FieldReference(String.class.getName(), "characters"));
        String hashCodeName = names.forMemberField(new FieldReference(String.class.getName(), "hashCode"));

        writer.println("static JavaString stringPool[" + strings.size() + "] = {").indent();
        for (int i = 0; i < strings.size(); ++i) {
            writer.println("{").indent();
            writer.println("." + charactersName + " = (JavaArray*) &str_array_" + i + ",");
            writer.println("." + hashCodeName + " = INT32_C(" + strings.get(i).hashCode() + ")");
            writer.outdent().print("}");

            if (i < strings.size() - 1) {
                writer.print(",");
            }
            writer.println();
        }
        writer.outdent().println("};");
    }

    private void generateStringLiteral(String string) {
        writer.print("u\"");

        for (int j = 0; j < string.length(); ++j) {
            char c = string.charAt(j);
            switch (c) {
                case '\\':
                    writer.print("\\\\");
                    break;
                case '"':
                    writer.print("\\\"");
                    break;
                case '\r':
                    writer.print("\\r");
                    break;
                case '\n':
                    writer.print("\\n");
                    break;
                case '\t':
                    writer.print("\\t");
                    break;
                default:
                    if (c < 32) {
                        writer.print("\\x" + Character.forDigit(c >> 4, 16) + Character.forDigit(c & 0xF, 16));
                    } else if (c > 127) {
                        writer.print("\\u"
                                + Character.forDigit(c >> 12, 16)
                                + Character.forDigit((c >> 8) & 15, 16)
                                + Character.forDigit((c >> 4) & 15, 16)
                                + Character.forDigit(c & 15, 16));
                    } else {
                        writer.print(String.valueOf(c));
                    }
                    break;
            }
        }

        writer.print("\"");
    }
}
