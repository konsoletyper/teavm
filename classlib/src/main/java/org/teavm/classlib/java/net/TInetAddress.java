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

import static org.teavm.backend.wasm.wasi.Wasi.INET4;
import static org.teavm.backend.wasm.wasi.Wasi.INET6;
import static org.teavm.backend.wasm.wasi.Wasi.INET_UNSPEC;
import static org.teavm.backend.wasm.wasi.Wasi.SOCK_ANY;
import org.teavm.backend.wasm.runtime.net.impl.SockAddrInet4;
import org.teavm.backend.wasm.runtime.net.impl.SockAddrInet6;
import org.teavm.runtime.net.SockAddr;
import org.teavm.runtime.net.VirtualSocket;
import org.teavm.runtime.net.VirtualSocketProvider;

public class TInetAddress {
    protected String hostname;
    protected byte[] address;
    protected int family;

    static final int IPv4 = 1;
    static final int IPv6 = 2;

    private static VirtualSocket vs() {
        return VirtualSocketProvider.getInstance();
    }

    protected TInetAddress() {
    }

    protected TInetAddress(byte[] address, String hostname, int family) {
        this.address = address;
        this.hostname = hostname;
        this.family = family;
    }

    public byte[] getAddress() {
        return address;
    }

    public int getFamily() {
        return family;
    }

    public static TInetAddress[] getAllByName(String host) throws TUnknownHostException {
        if (host == null || host.isEmpty()) {
            TInetAddress[] ret = new TInetAddress[1];
            ret[0] = getLoopbackAddress();
            return ret;
        }
        try {
            SockAddr[] addresses = vs().getAddrInfo(host, null, INET_UNSPEC, SOCK_ANY, 0);
            if (addresses == null || addresses.length == 0) {
                throw new TUnknownHostException("No addresses found for host: " + host);
            }

            TInetAddress[] result = new TInetAddress[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                SockAddr addr = addresses[i];
                if (addr.sockFamily() == INET4) {
                    result[i] = new TInet4Address(((SockAddrInet4) addr).getAddr(), host);
                } else if (addr.sockFamily() == INET6) {
                    result[i] = new TInet6Address(((SockAddrInet6) addr).getAddr(), host);
                }
            }
            return result;
        } catch (Exception e) {
            throw new TUnknownHostException(
                    "Failed to resolve host: " + host + " " + e.getMessage());
        }
    }

    public static TInetAddress getByAddress(byte[] addr) throws TUnknownHostException {
        return getByAddress(null, addr);
    }

    public static TInetAddress getByAddress(String host, byte[] addr) throws TUnknownHostException {
        if (addr != null) {
            if (addr.length == TInet4Address.INADDRSZ) {
                return new TInet4Address(addr, host);
            } else if (addr.length == TInet6Address.INADDRSZ) {
                return new TInet6Address(addr, host);
            }
        }
        throw new TUnknownHostException("Addr is of illegal length");
    }

    public static TInetAddress getByName(String host) throws TUnknownHostException {
        TInetAddress[] addresses = getAllByName(host);
        return addresses[0];
    }

    public String getCanonicalHostName() {
        return hostname != null ? hostname : getHostAddress();
    }

    public String getHostAddress() {
        StringBuilder sb = new StringBuilder();
        for (byte b : address) {
            sb.append(b & 0xFF).append('.');
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public String getHostName() {
        return hostname != null ? hostname : getHostAddress();
    }

    public static TInetAddress getLocalHost() throws TUnknownHostException {
        return new TInetAddress(new byte[] {127, 0, 0, 1}, "localhost", IPv4);
    }

    public static TInetAddress getLoopbackAddress() {
        return new TInetAddress(new byte[] {127, 0, 0, 1}, "localhost", IPv4);
    }

    public boolean isAnyLocalAddress() {
        return false;
    }

    public boolean isLinkLocalAddress() {
        return false;
    }

    public boolean isLoopbackAddress() {
        return false;
    }

    public boolean isMulticastAddress() {
        return false;
    }

    public boolean isReachable(int timeout) {
        return false;
    }

    public boolean isReachable(TNetworkInterface netif, int ttl, int timeout) {
        return false;
    }

    public boolean isSiteLocalAddress() {
        return false;
    }

    public boolean isMCGlobal() {
        return false;
    }

    public boolean isMCNodeLocal() {
        return false;
    }

    public boolean isMCLinkLocal() {
        return false;
    }

    public boolean isMCSiteLocal() {
        return false;
    }

    public boolean isMCOrgLocal() {
        return false;
    }

    @Override
    public String toString() {
        return (hostname != null ? hostname : "") + "/" + getHostAddress();
    }
}
