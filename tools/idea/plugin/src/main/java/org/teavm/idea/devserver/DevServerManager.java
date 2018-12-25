/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.idea.devserver;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DevServerManager extends Remote {
    String ID = "TeaVM-BuildServer";

    void stop() throws RemoteException;

    void invalidateCache() throws RemoteException;

    void buildProject() throws RemoteException;

    void cancelBuild() throws RemoteException;

    void addListener(DevServerManagerListener listener) throws RemoteException;

    void removeListener(DevServerManagerListener listener) throws RemoteException;
}
