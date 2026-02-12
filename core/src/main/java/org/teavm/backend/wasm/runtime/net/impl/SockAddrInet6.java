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
package org.teavm.backend.wasm.runtime.net.impl;

import static org.teavm.backend.wasm.wasi.Wasi.INET6;
import org.teavm.backend.wasm.runtime.WasiBuffer;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.runtime.net.SockAddr;

public class SockAddrInet6 implements SockAddr {
    private final short[] addr;
    private final int port;

    public static class SockAddrInet6Struct extends Structure {
        public int family;
        public short a0;
        public short a1;
        public short a2;
        public short a3;
        public short a4;
        public short a5;
        public short a6;
        public short a7;
        public short port;
    }

    public SockAddrInet6(short[] addr, int port) {
        validateIPv6Address(addr);
        this.addr = addr;
        this.port = validatePort(port & 0xFFFF);
    }

    public SockAddrInet6(byte[] addr, int port) {
        if (addr == null || addr.length != 16) {
            throw new IllegalArgumentException("Invalid IPv6 address. Expected a 16-byte array.");
        }
        short[] shortAddr = new short[8];
        for (int i = 0; i < 8; i++) {
            shortAddr[i] = (short) (((addr[2 * i] & 0xFF) << 8) | (addr[2 * i + 1] & 0xFF));
        }
        validateIPv6Address(shortAddr);
        this.addr = shortAddr;
        this.port = validatePort(port & 0xFFFF);
    }

    private static void validateIPv6Address(short[] addr) {
        if (addr.length != 8) {
            throw new IllegalArgumentException("IPv6 address must be exactly 16 bytes long.");
        }
    }

    private static int validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port number out of range: " + port);
        }
        return port;
    }

    @Override
    public Address sockAddr() {
        Address argsAddress = WasiBuffer.getBuffer();
        SockAddrInet6Struct s = argsAddress.toStructure();
        s.family = INET6;
        s.a0 = addr[0];
        s.a1 = addr[1];
        s.a2 = addr[2];
        s.a3 = addr[3];
        s.a4 = addr[4];
        s.a5 = addr[5];
        s.a6 = addr[6];
        s.a7 = addr[7];
        s.port = (short) port;
        return argsAddress;
    }

    @Override
    public int sockPort() {
        return port;
    }

    @Override
    public int sockFamily() {
        return INET6;
    }

    public byte[] getAddr() {
        byte[] byteArray = new byte[addr.length * 2];
        for (int i = 0; i < addr.length; i++) {
            byteArray[i * 2] = (byte) ((addr[i] >> 8) & 0xFF);
            byteArray[i * 2 + 1] = (byte) (addr[i] & 0xFF);
        }
        return byteArray;
    }

    @Override
    public String toString() {
        StringBuilder ipBuilder = new StringBuilder();
        for (int i = 0; i < addr.length; i++) {
            ipBuilder.append(String.format("%04X", addr[i] & 0xFFFF));
            if (i < addr.length - 1) {
                ipBuilder.append(":");
            }
        }
        return "[" + ipBuilder + "]:" + sockPort();
    }

    @Override
    public int sockAddrLen() {
        return Structure.sizeOf(SockAddrInet6Struct.class);
    }
}
