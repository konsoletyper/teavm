/*
 *  Copyright 2016 Alexey Andreev.
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
package org.teavm.idea.jps.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface TeaVMBuilderAssistant extends Remote {
    String REMOTE_PORT = "teavm.jps.remote-port";
    String ID = "TeaVM-JPS-Assistant";

    TeaVMElementLocation getMethodLocation(String className, String methodName, String methodDesc)
            throws RemoteException;
}
