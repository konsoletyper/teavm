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
package org.teavm.eclipse.debugger;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.teavm.debugging.CallFrame;
import org.teavm.debugging.information.SourceLocation;
import org.teavm.debugging.javascript.JavaScriptLocation;

/**
 *
 * @author Alexey Andreev <konsoletyper@gmail.com>
 */
public class TeaVMSourceLookupParticipant extends AbstractSourceLookupParticipant {
    @Override
    public String getSourceName(Object object) throws CoreException {
        if (object instanceof TeaVMStackFrame) {
            TeaVMStackFrame stackFrame = (TeaVMStackFrame)object;
            SourceLocation location = stackFrame.callFrame.getLocation();
            if (location != null) {
                return location.getFileName();
            }
            JavaScriptLocation jsLocation = stackFrame.callFrame.getOriginalLocation();
            return jsLocation != null ? jsLocation.getScript() : null;
        } else if (object instanceof TeaVMJSStackFrame) {
            TeaVMJSStackFrame stackFrame = (TeaVMJSStackFrame)object;
            JavaScriptLocation location = stackFrame.callFrame.getLocation();
            return location != null ? location.getScript() : null;
        } else {
            return null;
        }
    }

    @Override
    public Object[] findSourceElements(Object object) throws CoreException {
        Object[] result = super.findSourceElements(object);
        if (object instanceof TeaVMJSStackFrame) {
            TeaVMJSStackFrame stackFrame = (TeaVMJSStackFrame)object;
            JavaScriptLocation location = stackFrame.getCallFrame().getLocation();
            if (location != null) {
                result = addElement(result, location);
            }
        } else if (object instanceof TeaVMStackFrame) {
            TeaVMStackFrame stackFrame = (TeaVMStackFrame)object;
            CallFrame callFrame = stackFrame.getCallFrame();
            if (callFrame.getMethod() == null && callFrame.getLocation() != null) {
                result = addElement(result, callFrame.getOriginalLocation());
            }
        }
        return result;
    }

    private Object[] addElement(Object[] elements, JavaScriptLocation location) {
        URL url;
        try {
            url = new URL(location.getScript());
        } catch (MalformedURLException e) {
            url = null;
        }
        if (url != null) {
            elements = Arrays.copyOf(elements, elements.length + 1);
            elements[elements.length - 1] = url;
        }
        return elements;
    }
}
