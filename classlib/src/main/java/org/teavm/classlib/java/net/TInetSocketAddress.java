/*
 *  Copyright 2025 Maksim Tiushev.
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
package org.teavm.classlib.java.net;

import java.io.Serializable;

public class TInetSocketAddress extends TSocketAddress implements Serializable {
    private TInetAddress address;
    private String hostname;
    private int port;
    private boolean unresolved;

    public TInetSocketAddress(TInetAddress addr, int port) {
        validatePort(port);
        this.address = addr;
        this.port = port;
        this.unresolved = addr == null;
    }

    public TInetSocketAddress(int port) {
        this((String) null, port);
    }

    public TInetSocketAddress(String hostname, int port) {
        validatePort(port);
        this.hostname = hostname;
        this.port = port;
        try {
            this.address = TInetAddress.getByName(hostname);
            this.unresolved = false;
        } catch (TUnknownHostException e) {
            this.address = null;
            this.unresolved = true;
        }
    }

    public static TInetSocketAddress createUnresolved(String host, int port) {
        validatePort(port);
        TInetSocketAddress socketAddress = new TInetSocketAddress(host, port);
        socketAddress.unresolved = true;
        return socketAddress;
    }

    public TInetAddress getAddress() {
        return address;
    }

    public String getHostName() {
        return hostname;
    }

    public String getHostString() {
        if (hostname != null) {
            return hostname;
        }
        return address != null ? address.getHostAddress() : null;
    }

    public int getPort() {
        return port;
    }

    public boolean isUnresolved() {
        return unresolved;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (hostname != null) {
            sb.append(hostname);
        } else if (address != null) {
            sb.append(address.getHostAddress());
        } else {
            sb.append("<unresolved>");
        }
        sb.append(":").append(port);
        return sb.toString();
    }

    private static int validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port number out of range: " + port);
        }
        return port;
    }
}
