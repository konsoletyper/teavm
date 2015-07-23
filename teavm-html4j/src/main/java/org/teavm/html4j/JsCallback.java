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
package org.teavm.html4j;

/**
*
* @author Jaroslav Tulach <jtulach@netbeans.org>
*/
abstract class JsCallback {
   final String parse(String body) {
       StringBuilder sb = new StringBuilder();
       int pos = 0;
       for (;;) {
           int next = body.indexOf(".@", pos);
           if (next == -1) {
               sb.append(body.substring(pos));
               body = sb.toString();
               break;
           }
           int ident = next;
           while (ident > 0) {
               if (!Character.isJavaIdentifierPart(body.charAt(--ident))) {
                   ident++;
                   break;
               }
           }
           String refId = body.substring(ident, next);

           sb.append(body.substring(pos, ident));

           int sigBeg = body.indexOf('(', next);
           int sigEnd = body.indexOf(')', sigBeg);
           int colon4 = body.indexOf("::", next);
           if (sigBeg == -1 || sigEnd == -1 || colon4 == -1) {
               throw new IllegalStateException("Wrong format of instance callback. Should be: "
                       + "'inst.@pkg.Class::method(Ljava/lang/Object;)(param)':\n" + body);
           }
           String fqn = body.substring(next + 2, colon4);
           String method = body.substring(colon4 + 2, sigBeg);
           String params = body.substring(sigBeg, sigEnd + 1);

           int paramBeg = body.indexOf('(', sigEnd + 1);
           if (paramBeg == -1) {
               throw new IllegalStateException("Wrong format of instance callback. "
                   + "Should be: 'inst.@pkg.Class::method(Ljava/lang/Object;)(param)':\n" + body);
           }

           sb.append(callMethod(refId, fqn, method, params));
           if (body.charAt(paramBeg + 1) != ')') {
               sb.append(",");
           }
           pos = paramBeg + 1;
       }
       pos = 0;
       sb = null;
       for (;;) {
           int next = body.indexOf("@", pos);
           if (next == -1) {
               if (sb == null) {
                   return body;
               }
               sb.append(body.substring(pos));
               return sb.toString();
           }
           if (sb == null) {
               sb = new StringBuilder();
           }

           sb.append(body.substring(pos, next));

           int sigBeg = body.indexOf('(', next);
           int sigEnd = body.indexOf(')', sigBeg);
           int colon4 = body.indexOf("::", next);
           if (sigBeg == -1 || sigEnd == -1 || colon4 == -1) {
               throw new IllegalStateException("Wrong format of static callback. Should be: "
                       + "'@pkg.Class::staticMethod(Ljava/lang/Object;)(param)':\n" + body);
           }
           String fqn = body.substring(next + 1, colon4);
           String method = body.substring(colon4 + 2, sigBeg);
           String params = body.substring(sigBeg, sigEnd + 1);

           int paramBeg = body.indexOf('(', sigEnd + 1);

           sb.append(callMethod(null, fqn, method, params));
           pos = paramBeg + 1;
       }
   }

   protected abstract CharSequence callMethod(
       String ident, String fqn, String method, String params
   );

   static String mangle(String fqn, String method, String params) {
       if (params.startsWith("(")) {
           params = params.substring(1);
       }
       if (params.endsWith(")")) {
           params = params.substring(0, params.length() - 1);
       }
       return
           replace(fqn) + "$" + replace(method) + "$" + replace(params);
   }

   private static String replace(String orig) {
       return orig.replace("_", "_1").
           replace(";", "_2").
           replace("[", "_3").
           replace('.', '_').replace('/', '_');
   }
}
