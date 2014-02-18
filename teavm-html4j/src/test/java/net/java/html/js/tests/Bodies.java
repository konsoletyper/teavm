/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2013-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Oracle. Portions Copyright 2013-2014 Oracle. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package net.java.html.js.tests;

import java.util.concurrent.Callable;
import net.java.html.js.JavaScriptBody;

/**
 *
 * @author Jaroslav Tulach <jtulach@netbeans.org>
 */
final class Bodies {
    @JavaScriptBody(args = { "a", "b" }, body = "return a + b;")
    public static native int sum(int a, int b);
    
    @JavaScriptBody(args = {"r"}, javacall = true, body = "r.@java.lang.Runnable::run()();")
    static native void callback(Runnable r);

    @JavaScriptBody(args = {"c"}, javacall = true, body = "return c.@java.util.concurrent.Callable::call()();")
    static native Object callback(Callable<? extends Object> c);

    @JavaScriptBody(args = {"c", "v"}, javacall = true, body = "var arr = c.@java.util.concurrent.Callable::call()(); arr.push(v); return arr;")
    static native Object callbackAndPush(Callable<String[]> c, String v);
    
    @JavaScriptBody(args = { "v" }, body = "return v;")
    public static native Object id(Object v);
    
    @JavaScriptBody(args = { "v" }, body = "return { 'x' : v };")
    public static native Object instance(int v);
    
    @JavaScriptBody(args = "o", body = "o.x++;")
    public static native void incrementX(Object o);

    @JavaScriptBody(args = "o", body = "return o.x;")
    public static native int readX(Object o);

    @JavaScriptBody(args = { "c" }, javacall = true, body = 
        "return c.@net.java.html.js.tests.Sum::sum(II)(40, 2);"
    )
    public static native int sumIndirect(Sum c);
    
    @JavaScriptBody(args = { "arr", "index" }, body = "return arr[index];")
    public static native Object select(Object[] arr, int index);

    @JavaScriptBody(args = { "arr" }, body = "return arr.length;")
    public static native int length(Object[] arr);
    
    @JavaScriptBody(args = { "o", "vo" }, body = "if (vo) o = o.valueOf(); return typeof o;")
    public static native String typeof(Object o, boolean useValueOf);

    @JavaScriptBody(args = { "b" }, body = "return typeof b;")
    public static native String typeof(boolean b);

    @JavaScriptBody(args = { "o" }, body = "return Array.isArray(o);")
    public static native boolean isArray(Object o);

    @JavaScriptBody(args = { "arr", "i", "value" }, body = "arr[i] = value; return arr[i];")
    public static native String modify(String[] arr, int i, String value);
    
    @JavaScriptBody(args = {}, body = "return true;")
    public static native boolean truth();
    
    @JavaScriptBody(args = { "s" }, javacall = true, body = 
        "return s.@net.java.html.js.tests.Sum::sum([Ljava/lang/Object;)([1, 2, 3]);"
    )
    public static native int sumArr(Sum s);
}
