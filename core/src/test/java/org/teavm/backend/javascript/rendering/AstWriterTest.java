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
package org.teavm.backend.javascript.rendering;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ast.AstRoot;
import org.teavm.backend.javascript.codegen.SourceWriter;
import org.teavm.backend.javascript.codegen.SourceWriterBuilder;

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
    public void writesTryCatch() throws IOException {
        assertThat(transform("try { foo(); } catch (e) { alert(e); }"), is("try {foo();}catch(e){alert(e);}"));
        assertThat(transform("try { foo(); } finally { close(); }"), is("try {foo();}finally {close();}"));
    }

    @Test
    public void writesFor() throws IOException {
        assertThat(transform("for (var i = 0; i < array.length; ++i,++j) foo(array[i]);"),
                is("for(var i=0;i<array.length;++i,++j)foo(array[i]);"));
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
                + "case '.': case '?': matchAny(); break; "
                + "case '*': matchSequence(); break;"
                + "default: matchChar(c); break; } "),
                is("switch(c){case '.':case '?':matchAny();break;case '*':matchSequence();break;"
                        + "default:matchChar(c);break;}"));
    }

    @Test
    public void writesLet() throws IOException {
        assertThat(transform("let x = 1; alert(x);"), is("let x=1;alert(x);"));
        assertThat(transform("let x = 1, y; alert(x,y);"), is("let x=1,y;alert(x,y);"));
    }

    @Test
    public void writesConst() throws IOException {
        assertThat(transform("const x = 1,y = 2; alert(x,y);"), is("const x=1,y=2;alert(x,y);"));
    }

    @Test
    public void writesElementGet() throws IOException {
        assertThat(transform("return array[i];"), is("return array[i];"));
        assertThat(transform("return array[i][j];"), is("return array[i][j];"));
        assertThat(transform("return (array[i])[j];"), is("return array[i][j];"));
        assertThat(transform("return (a + b)[i];"), is("return (a+b)[i];"));
        assertThat(transform("return a + b[i];"), is("return a+b[i];"));
    }

    @Test
    public void writesPropertyGet() throws IOException {
        assertThat(transform("return array.length;"), is("return array.length;"));
        assertThat(transform("return (array).length;"), is("return array.length;"));
        assertThat(transform("return (x + y).toString();"), is("return (x+y).toString();"));
    }

    @Test
    public void writesFunctionCall() throws IOException {
        assertThat(transform("return f(x);"), is("return f(x);"));
        assertThat(transform("return (f)(x);"), is("return f(x);"));
        assertThat(transform("return (f + g)(x);"), is("return (f+g)(x);"));
    }

    @Test
    public void writesConstructorCall() throws IOException {
        assertThat(transform("return new (f + g)(x);"), is("return new (f+g)(x);"));
        assertThat(transform("return new f + g(x);"), is("return new f()+g(x);"));
        assertThat(transform("return new f()(x);"), is("return new f()(x);"));
        assertThat(transform("return (new f())(x);"), is("return new f()(x);"));
        assertThat(transform("return new (f())(x);"), is("return new (f())(x);"));
        assertThat(transform("return new f[0](x);"), is("return new f[0](x);"));
        assertThat(transform("return (new f[0](x));"), is("return new f[0](x);"));
    }

    @Test
    public void writesConditionalExpr() throws IOException {
        assertThat(transform("return cond ? 1 : 0;"), is("return cond?1:0;"));
        assertThat(transform("return a < b ? -1 : a > b ? 1 : 0;"), is("return a<b? -1:a>b?1:0;"));
        assertThat(transform("return a < b ? -1 : (a > b ? 1 : 0);"), is("return a<b? -1:a>b?1:0;"));
        assertThat(transform("return (a < b ? x == y : x != y) ? 1 : 0;"), is("return (a<b?x==y:x!=y)?1:0;"));
        assertThat(transform("return a < b ? (x > y ? x : y) : z"), is("return a<b?(x>y?x:y):z;"));
    }

    @Test
    public void writesRegExp() throws IOException {
        assertThat(transform("return /[a-z]+/.match(text);"), is("return /[a-z]+/.match(text);"));
        assertThat(transform("return /[a-z]+/ig.match(text);"), is("return /[a-z]+/ig.match(text);"));
    }

    @Test
    public void writesArrayLiteral() throws IOException {
        assertThat(transform("return [];"), is("return [];"));
        assertThat(transform("return [a, b + c];"), is("return [a,b+c];"));
    }

    @Test
    public void writesObjectLiteral() throws IOException {
        assertThat(transform("return {};"), is("return {};"));
        assertThat(transform("return { foo : bar };"), is("return {foo:bar};"));
        assertThat(transform("return { foo : bar };"), is("return {foo:bar};"));
        assertThat(transform("return { _foo : bar, get foo() { return this._foo; } };"),
                is("return {_foo:bar,get foo(){return this._foo;}};"));
    }

    @Test
    public void writesFunction() throws IOException {
        assertThat(transform("return function f(x, y) { return x + y; };"),
                is("return function f(x,y){return x+y;};"));
    }

    @Test
    public void writesUnary() throws IOException {
        assertThat(transform("return -a;"), is("return  -a;"));
        assertThat(transform("return -(a + b);"), is("return  -(a+b);"));
        assertThat(transform("return -a + b;"), is("return  -a+b;"));
        assertThat(transform("return (-a) + b;"), is("return  -a+b;"));
        assertThat(transform("return (-f)(x);"), is("return ( -f)(x);"));
        assertThat(transform("return typeof a;"), is("return typeof a;"));
    }

    @Test
    public void writesPostfix() throws IOException {
        assertThat(transform("return a++;"), is("return a++;"));
    }

    @Test
    public void respectsPrecedence() throws IOException {
        assertThat(transform("return a + b + c;"), is("return a+b+c;"));
        assertThat(transform("return (a + b) + c;"), is("return a+b+c;"));
        assertThat(transform("return a + (b + c);"), is("return a+b+c;"));
        assertThat(transform("return a - b + c;"), is("return a -b+c;"));
        assertThat(transform("return (a - b) + c;"), is("return a -b+c;"));
        assertThat(transform("return a - (b + c);"), is("return a -(b+c);"));
    }

    @Test
    public void writesDelete() throws IOException {
        assertThat(transform("delete a.b;"), is("delete a.b;"));
    }

    private String transform(String text) throws IOException {
        sb.setLength(0);
        CompilerEnvirons env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        env.setLanguageVersion(Context.VERSION_1_8);
        JSParser factory = new JSParser(env);
        factory.enterFunction();
        AstRoot rootNode = factory.parse(new StringReader(text), null, 0);
        factory.exitFunction();
        writer.hoist(rootNode);
        writer.print(rootNode);
        return sb.toString();
    }
}
