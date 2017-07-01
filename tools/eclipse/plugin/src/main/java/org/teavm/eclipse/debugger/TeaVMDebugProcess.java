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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStreamsProxy;

public class TeaVMDebugProcess extends PlatformObject implements IProcess {
    private TeaVMDebugTarget debugTarget;
    private Map<String, String> attributes = new HashMap<>();
    private TeaVMStreamsProxy streamsProxy = new TeaVMStreamsProxy();

    public TeaVMDebugProcess(TeaVMDebugTarget debugTarget) {
        this.debugTarget = debugTarget;
    }

    @Override
    public boolean canTerminate() {
        return debugTarget.canTerminate();
    }

    @Override
    public boolean isTerminated() {
        return debugTarget.isTerminated();
    }

    @Override
    public void terminate() throws DebugException {
        debugTarget.terminate();
    }

    @Override
    public String getAttribute(String attr) {
        return attributes.get(attr);
    }

    @Override
    public int getExitValue() throws DebugException {
        return 0;
    }

    @Override
    public String getLabel() {
        return "TeaVM debug process";
    }

    @Override
    public IStreamsProxy getStreamsProxy() {
        return streamsProxy;
    }

    @Override
    public void setAttribute(String attr, String value) {
        attributes.put(attr, value);
    }

    @Override
    public ILaunch getLaunch() {
        return debugTarget.getLaunch();
    }
}
