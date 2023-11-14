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

import static org.junit.Assert.assertEquals;
import java.io.IOException;
import java.io.StringReader;
import org.junit.Test;
import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.teavm.backend.javascript.codegen.OutputSourceWriterBuilder;
import org.teavm.backend.javascript.codegen.SourceWriter;

public class AstWriterTest {
    private StringBuilder sb = new StringBuilder();
    private SourceWriter sourceWriter;
    private AstWriter writer;
    private AstWriter writerWithGlobals;

    public AstWriterTest() {
        var builder = new OutputSourceWriterBuilder(null);
        builder.setMinified(true);
        sourceWriter = builder.build(sb);
        writer = new AstWriter(sourceWriter, null);
        writerWithGlobals = new AstWriter(sourceWriter, name -> prec -> sourceWriter.append("globals.").append(name));
    }

    @Test
    public void writesReturn() throws IOException {
        assertEquals("return x;", transform("return x;"));
    }

    @Test
    public void writesEmptyReturn() throws IOException {
        assertEquals("return;", transform("return;"));
    }

    @Test
    public void writesThrow() throws IOException {
        assertEquals("throw x;", transform("throw x;"));
    }

    @Test
    public void writesBreak() throws IOException {
        assertEquals("a:while(true){break a;}", transform("a: while (true) { break a; }"));
    }

    @Test
    public void writesEmptyBreak() throws IOException {
        assertEquals("while(true){break;}", transform("while(true) { break; }"));
    }

    @Test
    public void writesContinue() throws IOException {
        assertEquals("a:while(true){continue a;}", transform("a: while (true) { continue a; }"));
    }

    @Test
    public void writesEmptyContinue() throws IOException {
        assertEquals("while(true){continue;}", transform("while(true) { continue; }"));
    }

    @Test
    public void writesBlock() throws IOException {
        assertEquals("{foo();bar();}", transform("{ foo(); bar(); }"));
    }

    @Test
    public void writesTryCatch() throws IOException {
        assertEquals("try {foo();}catch(e){alert(e);}", transform("try { foo(); } catch (e) { alert(e); }"));
        assertEquals("try {foo();}finally {close();}", transform("try { foo(); } finally { close(); }"));
    }

    @Test
    public void writesFor() throws IOException {
        assertEquals(
                "for(var i=0;i<array.length;++i,++j)foo(array[i]);",
                transform("for (var i = 0; i < array.length; ++i,++j) foo(array[i]);")
        );
    }

    @Test
    public void writesEmptyFor() throws IOException {
        assertEquals("for(;;)foo();", transform("for (;;) foo();"));
    }

    @Test
    public void writesForIn() throws IOException {
        assertEquals(
                "for(var property in window)alert(property);",
                transform("for (var property in window) alert(property);")
        );
    }

    @Test
    public void writesWhile() throws IOException {
        assertEquals("while(shouldProceed())proceed();", transform("while (shouldProceed()) proceed();"));
    }

    @Test
    public void writesDoWhile() throws IOException {
        assertEquals("do proceed();while(shouldRepeat());", transform("do proceed(); while(shouldRepeat());"));
    }

    @Test
    public void writesIfElse() throws IOException {
        assertEquals(
                "if(test())performTrue();else performFalse();",
                transform("if (test()) performTrue(); else performFalse();")
        );
    }

    @Test
    public void writesIf() throws IOException {
        assertEquals("if(shouldPerform())perform();", transform("if (shouldPerform()) perform();"));
    }

    @Test
    public void writesSwitch() throws IOException {
        assertEquals(
                "switch(c){case '.':case '?':matchAny();break;case '*':matchSequence();break;"
                        + "default:matchChar(c);break;}",
                transform("switch (c) { "
                + "case '.': case '?': matchAny(); break; "
                + "case '*': matchSequence(); break;"
                + "default: matchChar(c); break; } ")
        );
    }

    @Test
    public void writesLet() throws IOException {
        assertEquals("let x=1;alert(x);", transform("let x = 1; alert(x);"));
        assertEquals("let x=1,y;alert(x,y);", transform("let x = 1, y; alert(x,y);"));
    }

    @Test
    public void writesConst() throws IOException {
        assertEquals("const xx=1,yy=2;alert(xx,yy);", transform("const xx = 1,yy = 2; alert(xx,yy);"));
    }

    @Test
    public void writesElementGet() throws IOException {
        assertEquals("return array[i];", transform("return array[i];"));
        assertEquals("return array[i][j];", transform("return array[i][j];"));
        assertEquals("return array[i][j];", transform("return (array[i])[j];"));
        assertEquals("return (a+b)[i];", transform("return (a + b)[i];"));
        assertEquals("return a+b[i];", transform("return a + b[i];"));
    }

    @Test
    public void writesPropertyGet() throws IOException {
        assertEquals("return array.length;", transform("return array.length;"));
        assertEquals("return array.length;", transform("return (array).length;"));
        assertEquals("return (x+y).toString();", transform("return (x + y).toString();"));
    }

    @Test
    public void writesFunctionCall() throws IOException {
        assertEquals("return f(x);", transform("return f(x);"));
        assertEquals("return f(x);", transform("return (f)(x);"));
        assertEquals("return (f+g)(x);", transform("return (f + g)(x);"));
    }

    @Test
    public void writesConstructorCall() throws IOException {
        assertEquals("return new (f+g)(x);", transform("return new (f + g)(x);"));
        assertEquals("return new f()+g(x);", transform("return new f + g(x);"));
        assertEquals("return new f()(x);", transform("return new f()(x);"));
        assertEquals("return new f()(x);", transform("return (new f())(x);"));
        assertEquals("return new (f())(x);", transform("return new (f())(x);"));
        assertEquals("return new f[0](x);", transform("return new f[0](x);"));
        assertEquals("return new f[0](x);", transform("return (new f[0](x));"));
    }

    @Test
    public void writesConditionalExpr() throws IOException {
        assertEquals("return cond?1:0;", transform("return cond ? 1 : 0;"));
        assertEquals("return a<b? -1:a>b?1:0;", transform("return a < b ? -1 : a > b ? 1 : 0;"));
        assertEquals("return a<b? -1:a>b?1:0;", transform("return a < b ? -1 : (a > b ? 1 : 0);"));
        assertEquals("return (a<b?x==y:x!=y)?1:0;", transform("return (a < b ? x == y : x != y) ? 1 : 0;"));
        assertEquals("return a<b?(x>y?x:y):z;", transform("return a < b ? (x > y ? x : y) : z"));
    }

    @Test
    public void writesRegExp() throws IOException {
        assertEquals("return /[a-z]+/.match(text);", transform("return /[a-z]+/.match(text);"));
        assertEquals("return /[a-z]+/ig.match(text);", transform("return /[a-z]+/ig.match(text);"));
    }

    @Test
    public void writesArrayLiteral() throws IOException {
        assertEquals("return [];", transform("return [];"));
        assertEquals("return [a,b+c];", transform("return [a, b + c];"));
    }

    @Test
    public void writesObjectLiteral() throws IOException {
        assertEquals("return {};", transform("return {};"));
        assertEquals("return {foo:bar};", transform("return { foo : bar };"));
        assertEquals("return {foo:bar};", transform("return { foo : bar };"));
        assertEquals(
                "return {_foo:bar,get foo(){return this._foo;}};",
                transform("return { _foo : bar, get foo() { return this._foo; } };")
        );
    }

    @Test
    public void writesFunction() throws IOException {
        assertEquals(
                "return function f(x,y){return x+y;};",
                transform("return function f(x, y) { return x + y; };")
        );
    }

    @Test
    public void writesUnary() throws IOException {
        assertEquals("return  -a;", transform("return -a;"));
        assertEquals("return  -(a+b);", transform("return -(a + b);"));
        assertEquals("return  -a+b;", transform("return -a + b;"));
        assertEquals("return  -a+b;", transform("return (-a) + b;"));
        assertEquals("return ( -f)(x);", transform("return (-f)(x);"));
        assertEquals("return typeof a;", transform("return typeof a;"));
    }

    @Test
    public void writesPostfix() throws IOException {
        assertEquals("return a++;", transform("return a++;"));
    }

    @Test
    public void respectsPrecedence() throws IOException {
        assertEquals("return a+b+c;", transform("return a + b + c;"));
        assertEquals("return a+b+c;", transform("return (a + b) + c;"));
        assertEquals("return a+b+c;", transform("return a + (b + c);"));
        assertEquals("return a -b+c;", transform("return a - b + c;"));
        assertEquals("return a -b+c;", transform("return (a - b) + c;"));
        assertEquals("return a -(b+c);", transform("return a - (b + c);"));
    }

    @Test
    public void writesDelete() throws IOException {
        assertEquals("delete a.b;", transform("delete a.b;"));
    }

    @Test
    public void writesGlobalRef() throws IOException {
        assertEquals(
                "function(x){return x+globals.y;}",
                transformGlobals("function(x) { return x + y; }")
        );
        assertEquals(
                "try {globals.foo();}catch(x){globals.foo(x);}",
                transformGlobals("try { foo(); } catch (x) { foo(x); }")
        );
        assertEquals(
                "for(var i=0;i<10;++i){globals.foo(i,globals.j);}",
                transformGlobals("for (var i = 0; i < 10; ++i) { foo(i, j); }")
        );
        assertEquals(
                "function(){var x=0;globals.foo(x,globals.y);}",
                transformGlobals("function() { var x = 0; foo(x, y); }")
        );
    }

    private String transform(String text) throws IOException {
        return transform(text, writer);
    }

    private String transformGlobals(String text) throws IOException {
        return transform(text, writerWithGlobals);
    }

    private String transform(String text, AstWriter writer) throws IOException {
        sb.setLength(0);
        var env = new CompilerEnvirons();
        env.setRecoverFromErrors(true);
        env.setLanguageVersion(Context.VERSION_1_8);
        var factory = new JSParser(env);
        factory.enterFunction();
        var rootNode = factory.parse(new StringReader(text), null, 0);
        factory.exitFunction();
        writer.hoist(rootNode);
        writer.print(rootNode);
        return sb.toString();
    }
}
