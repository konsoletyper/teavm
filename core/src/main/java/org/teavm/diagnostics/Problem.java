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
package org.teavm.diagnostics;

import java.io.Serializable;
import java.util.Arrays;
import org.teavm.model.*;

public class Problem implements Serializable {
    private ProblemSeverity severity;
    private CallLocation location;
    private String text;
    private Object[] params;

    public Problem(ProblemSeverity severity, CallLocation location, String text, Object[] params) {
        this.severity = severity;
        this.location = location;
        this.text = text;
        this.params = Arrays.copyOf(params, params.length);
    }

    public ProblemSeverity getSeverity() {
        return severity;
    }

    public CallLocation getLocation() {
        return location;
    }

    public String getText() {
        return text;
    }

    public Object[] getParams() {
        return params;
    }

    public void render(ProblemTextConsumer consumer) {
        int index = 0;
        while (index < text.length()) {
            int next = text.indexOf("{{", index);
            if (next < 0) {
                break;
            }
            consumer.append(text.substring(index, next));
            index = parseParameter(consumer, next);
            if (index == next) {
                consumer.append("{{");
                index += 2;
            }
        }
        consumer.append(text.substring(index));
    }

    private int parseParameter(ProblemTextConsumer consumer, int index) {
        int next = index + 2;
        if (next >= text.length()) {
            return index;
        }
        ParamType type;
        switch (Character.toLowerCase(text.charAt(next++))) {
            case 'c':
                type = ParamType.CLASS;
                break;
            case 't':
                type = ParamType.TYPE;
                break;
            case 'm':
                type = ParamType.METHOD;
                break;
            case 'f':
                type = ParamType.FIELD;
                break;
            case 'l':
                type = ParamType.LOCATION;
                break;
            default:
                return index;
        }
        int digitsEnd = passDigits(next);
        if (digitsEnd == next) {
            return index;
        }
        int paramIndex = Integer.parseInt(text.substring(next, digitsEnd));
        if (paramIndex >= params.length) {
            return index;
        }
        next = digitsEnd;
        if (next + 1 >= text.length() || !text.substring(next, next + 2).equals("}}")) {
            return index;
        }
        Object param = params[paramIndex];
        switch (type) {
            case CLASS:
                if (!(param instanceof String)) {
                    return index;
                }
                consumer.appendClass((String) param);
                break;
            case TYPE:
                if (!(param instanceof ValueType)) {
                    return index;
                }
                consumer.appendType((ValueType) param);
                break;
            case METHOD:
                if (!(param instanceof MethodReference)) {
                    return index;
                }
                consumer.appendMethod((MethodReference) param);
                break;
            case FIELD:
                if (!(param instanceof FieldReference)) {
                    return index;
                }
                consumer.appendField((FieldReference) param);
                break;
            case LOCATION:
                if (!(param instanceof TextLocation)) {
                    return index;
                }
                consumer.appendLocation((TextLocation) param);
                break;
        }
        next += 2;
        return next;
    }

    private int passDigits(int index) {
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            ++index;
        }
        return index;
    }

    enum ParamType {
        CLASS,
        TYPE,
        METHOD,
        FIELD,
        LOCATION
    }
}
