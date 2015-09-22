/*
 *  Copyright 2015 Alexey Andreev.
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
package org.teavm.jso.plugin;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import org.junit.experimental.theories.suppliers.TestedOn;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.ast.AstRoot;
import org.teavm.codegen.SourceWriter;
import org.teavm.codegen.SourceWriterBuilder;

/**
 *
 * @author Alexey Andreev
 */
public class AstWriterTest {
    private StringBuilder sb = new StringBuilder();
    private SourceWriter sourceWriter;
    private AstWriter writer;

    public AstWriterTest() {
        SourceWriterBuilder builder = new SourceWriterBuilder(null);
        builder.setMinified(true);
        sourceWriter = builder.build(sb);
        writer = new AstWriter(sourceWriter);
    }

    @Test
    public void renamesVariable() throws IOException {
        writer.declareAlias("a", "b");
        assertThat(transform("return a;"), is("return b;"));
    }

    @Test
    public void renamesDeclaredVariable() throws IOException {
        writer.declareAlias("a", "b");
        assertThat(transform("var b; return a + b;"), is("var b_0;return b+b_0;"));
    }

    @Test
    public void writesReturn() throws IOException {
        assertThat(transform("return x;"), is("return x;"));
    }

    @Test
    public void writesEmptyReturn() throws IOException {
        assertThat(transform("return;"), is("return;"));
    }

    @Test
    public void writesThrow() throws IOException {
        assertThat(transform("throw x;"), is("throw x;"));
    }

    @Test
    public void writesBreak() throws IOException {
        assertThat(transform("a: while (true) { break a; }"), is("a:while(true){break a;}"));
    }

    @Test
    public void writesEmptyBreak() throws IOException {
        assertThat(transform("while(true) { break; }"), is("while(true){break;}"));
    }

    @Test
    public void writesContinue() throws IOException {
        assertThat(transform("a: while (true) { continue a; }"), is("a:while(true){continue a;}"));
    }

    @Test
    public void writesEmptyContinue() throws IOException {
        assertThat(transform("while(true) { continue; }"), is("while(true){continue;}"));
    }

    @Test
    public void writesBlock() throws IOException {
        assertThat(transform("{ foo(); bar(); }"), is("{foo();bar();}"));
    }

    @Test
    public void writesFor() throws IOException {
        assertThat(transform("for (var i = 0; i < array.length; ++i) foo(array[i]);"),
                is("for(var i=0;i<array.length;++i)foo(array[i]);"));
    }

    @Test
    public void writesEmptyFor() throws IOException {
        assertThat(transform("for (;;) foo();"), is("for(;;)foo();"));
    }

    @Test
    public void writesForIn() throws IOException {
        assertThat(transform("for (var property in window) alert(property);"),
                is("for(var property in window)alert(property);"));
    }

    @Test
    public void writesWhile() throws IOException {
        assertThat(transform("while (shouldProceed()) proceed();"), is("while(shouldProceed())proceed();"));
    }

    @Test
    public void writesDoWhile() throws IOException {
        assertThat(transform("do proceed(); while(shouldRepeat());"), is("do proceed();while(shouldRepeat());"));
    }

    @Test
    public void writesIfElse() throws IOException {
        assertThat(transform("if (test()) performTrue(); else performFalse();"),
                is("if(test())performTrue();else performFalse();"));
    }

    @Test
    public void writesIf() throws IOException {
        assertThat(transform("if (shouldPerform()) perform();"), is("if(shouldPerform())perform();"));
    }

    @Test
    public void writesSwitch() throws IOException {
        assertThat(transform("switch (c) { "
                + "case '?': matchAny(); break; "
                + "case '*': matchSequence(); break;"
                + "default: matchChar(c); break; } "),
                is("switch(c){case '?':matchAny();break;case '*':matchSequence();break;"
                        + "default:matchChar(c);break;}"));
    }

    private String transform(String text) throws IOException {
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        JSParser factory = new JSParser(env);
        factory.enterFunction();
        AstRoot rootNode = factory.parse(new StringReader(text), null, 0);
        factory.exitFunction();
        writer.hoist(rootNode);
        writer.print(rootNode);
        return sb.toString();
    }
}
