/*
 *  Copyright 2013 Alexey Andreev.
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
package org.teavm.classlib.java.lang;

import java.io.IOException;
import org.teavm.codegen.SourceWriter;
import org.teavm.dependency.*;
import org.teavm.javascript.ni.Generator;
import org.teavm.javascript.ni.GeneratorContext;
import org.teavm.javascript.ni.Injector;
import org.teavm.javascript.ni.InjectorContext;
import org.teavm.model.CallLocation;
import org.teavm.model.MethodReference;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class ObjectNativeGenerator implements Generator, Injector, DependencyPlugin {
    @Override
    public void generate(GeneratorContext context, SourceWriter writer, MethodReference methodRef) throws IOException {
        switch (methodRef.getDescriptor().getName()) {
            case "<init>":
                generateInit(context, writer);
                break;
            case "hashCode":
            case "identity":
                generateHashCode(context, writer);
                break;
            case "clone":
                generateClone(context, writer);
                break;
            case "wait":
                generateWait(context, writer);
                break;
            case "notify":
                generateNotify(context, writer);
                break;
            case "notifyAll":
                generateNotifyAll(context, writer);
                break;
        }
    }

    @Override
    public void generate(InjectorContext context, MethodReference methodRef) throws IOException {
        switch (methodRef.getName()) {
            case "getClass":
                generateGetClass(context);
                break;
            case "wrap":
                generateWrap(context);
                break;
        }
    }

    @Override
    public void methodAchieved(DependencyAgent agent, MethodDependency method, CallLocation location) {
        switch (method.getReference().getName()) {
            case "clone":
                method.getVariable(0).connect(method.getResult());
                break;
            case "getClass":
                achieveGetClass(agent, method);
                break;
            case "wrap":
                method.getVariable(1).connect(method.getResult());
                break;
            //case "wait":
            //    method.getVariable(0).connect(method.getResult());
            //    break;
                
        }
    }

    private void generateInit(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append(context.getParameterName(0)).append(".$id = $rt_nextId();").softNewLine();
    }

    private void generateGetClass(InjectorContext context) throws IOException {
        SourceWriter writer = context.getWriter();
        writer.append("$rt_cls(");
        context.writeExpr(context.getArgument(0));
        writer.append(".constructor)");
    }

    private void achieveGetClass(DependencyAgent agent, MethodDependency method) {
        MethodReference initMethod = new MethodReference(Class.class, "createNew", Class.class);
        agent.linkMethod(initMethod, null).use();
        method.getResult().propagate(agent.getType("java.lang.Class"));
    }

    private void generateHashCode(GeneratorContext context, SourceWriter writer) throws IOException {
        writer.append("return ").append(context.getParameterName(0)).append(".$id;").softNewLine();
    }

    private void generateClone(GeneratorContext context, SourceWriter writer) throws IOException {
        String obj = context.getParameterName(0);
        writer.append("var copy = new ").append(obj).append(".constructor();").softNewLine();
        writer.append("for (var field in " + obj + ") {").softNewLine().indent();
        writer.append("if (!" + obj + ".hasOwnProperty(field)) {").softNewLine().indent();
        writer.append("continue;").softNewLine().outdent().append("}").softNewLine();
        writer.append("copy[field] = " + obj + "[field];").softNewLine().outdent().append("}").softNewLine();
        writer.append("return copy;").softNewLine();
    }

    private void generateWrap(InjectorContext context) throws IOException {
        context.writeExpr(context.getArgument(0));
    }
    
    private void generateWait(GeneratorContext context, SourceWriter writer) throws IOException {
        String pname = context.getParameterName(1);
        String obj = context.getParameterName(0);
        writer.append("(function(){").indent().softNewLine();
        writer.append("var completed = false;").softNewLine();
        writer.append("var retCallback = ").append(context.getCompleteContinuation()).append(";").softNewLine();
        writer.append("console.log(retCallback);").softNewLine();
        writer.append("var callback = function(){").indent().softNewLine();
        writer.append("if (completed){return;} completed=true;").softNewLine();
        writer.append("retCallback();").softNewLine();
        writer.outdent().append("};").softNewLine();
        writer.append("if (").append(pname).append(">0){").indent().softNewLine();
        writer.append("setTimeout(callback, ").append(pname).append(");").softNewLine();
        writer.outdent().append("}").softNewLine();
        addNotifyListener(context, writer, "callback");
        writer.outdent().append("})();").softNewLine();
        
        
        
    }
    
    private void generateNotify(GeneratorContext context, SourceWriter writer) throws IOException {
        sendNotify(context, writer);
    }
    
    private void generateNotifyAll(GeneratorContext context, SourceWriter writer) throws IOException {
        sendNotifyAll(context, writer);
    }
    
    private String getNotifyListeners(GeneratorContext context){
        return context.getParameterName(0)+".__notifyListeners";
    }
    
    private void addNotifyListener(GeneratorContext context, SourceWriter writer, String callback) throws IOException {
        String lArr = getNotifyListeners(context);
        writer.append(lArr).append("=").append(lArr).append("||[];").softNewLine();
        writer.append(lArr).append(".push(").append(callback).append(");").softNewLine();
    }
    
    private void sendNotify(GeneratorContext context, SourceWriter writer) throws IOException {
        String lArr = getNotifyListeners(context);
        writer.append("setTimeout(function(){").indent().softNewLine();
        writer.append("if (!").append(lArr).append(" || ").append(lArr).append(".length===0){return;}").softNewLine();
        writer.append("var m = ").append(lArr).append(".shift();").softNewLine();
        writer.append("console.log('Notify callback : '+m);").softNewLine();
        writer.append("m.apply(null);").softNewLine();
        writer.outdent().append("}, 0);").softNewLine();
    }
    
    private void sendNotifyAll(GeneratorContext context, SourceWriter writer) throws IOException {
        String obj = context.getParameterName(0);
        String lArr = getNotifyListeners(context);
        writer.append("setTimeout(function(){").indent().softNewLine();
        writer.append("if (!").append(lArr).append("){return;}").softNewLine();
        writer.append("while (").append(lArr).append(".length>0){").indent().softNewLine();
        writer.append(lArr).append(".shift().call(null);").softNewLine();
        writer.outdent().append("}");
        writer.outdent().append("}, 0);").softNewLine();
        
    }
    
}
