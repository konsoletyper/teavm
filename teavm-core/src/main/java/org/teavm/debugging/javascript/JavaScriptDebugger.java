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
package org.teavm.debugging.javascript;

/**
 *
 * @author Alexey Andreev
 */
public interface JavaScriptDebugger {
    void addListener(JavaScriptDebuggerListener listener);

    void removeListener(JavaScriptDebuggerListener listener);

    void suspend();

    void resume();

    void stepInto();

    void stepOut();

    void stepOver();

    void continueToLocation(JavaScriptLocation location);

    boolean isSuspended();

    boolean isAttached();

    void detach();

    JavaScriptCallFrame[] getCallStack();

    JavaScriptBreakpoint createBreakpoint(JavaScriptLocation location);
}
