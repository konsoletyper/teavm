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

abstract class JsCallback {
    final void parse(String body) {
        int pos = 0;
        while (true) {
            int next = body.indexOf('@', pos);
            if (next < 0) {
                append(body.substring(pos));
                break;
            }

            String refId = null;
            if (next > 0 && body.charAt(next - 1) == '.') {
                int ident = next - 1;
                while (ident > 0) {
                    if (!Character.isJavaIdentifierPart(body.charAt(--ident))) {
                        ident++;
                        break;
                    }
                }
                refId = body.substring(ident, next - 1);
                append(body.substring(pos, ident));
            } else {
                append(body.substring(pos, next));
            }

            int colon4 = body.indexOf("::", next);
            int sigBeg = body.indexOf('(', colon4 > 0 ? colon4 : next);
            int sigEnd = body.indexOf(')', sigBeg > 0 ? sigBeg : next);

            if (sigBeg == -1 || sigEnd == -1 || colon4 == -1) {
                reportDiagnostic("Wrong format of callback. Should be: "
                        + "'@pkg.Class::method(Ljava/lang/Object;)(param)'" + body);
                append(body.substring(pos, next + 1));
                pos = next + 1;
                continue;
            }

            int paramBeg = body.indexOf('(', sigEnd + 1);
            if (paramBeg < 0 || !isWhitespaces(body, sigEnd + 1, paramBeg)) {
                reportDiagnostic("Wrong format of callback. Should be: "
                        + "'@pkg.Class::method(Ljava/lang/Object;)(param)'" + body);
                append(body.substring(pos, next + 1));
                pos = next + 1;
                continue;
            }

            String fqn = body.substring(next + 1, colon4);
            String method = body.substring(colon4 + 2, sigBeg);
            String params = body.substring(sigBeg, sigEnd + 1);

            callMethod(refId, fqn, method, params);

            if (refId != null && body.charAt(paramBeg + 1) != ')') {
                append(",");
            }

            pos = paramBeg + 1;
        }
    }

    protected abstract void append(String text);

    protected abstract void callMethod(String ident, String fqn, String method, String params);

    protected abstract void reportDiagnostic(String text);

    private static boolean isWhitespaces(String text, int start, int end) {
        while (start < end) {
            if (!Character.isWhitespace(text.charAt(start++))) {
                return false;
            }
        }
        return true;
    }
}
