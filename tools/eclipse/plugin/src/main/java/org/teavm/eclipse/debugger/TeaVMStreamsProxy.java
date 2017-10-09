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

import java.io.IOException;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;

// TODO: implement interaction with browser console
public class TeaVMStreamsProxy implements IStreamsProxy {
    @Override
    public IStreamMonitor getErrorStreamMonitor() {
        return new TeaVMStreamMonitor();
    }

    @Override
    public IStreamMonitor getOutputStreamMonitor() {
        return new TeaVMStreamMonitor();
    }

    @Override
    public void write(String text) throws IOException {
    }
}
