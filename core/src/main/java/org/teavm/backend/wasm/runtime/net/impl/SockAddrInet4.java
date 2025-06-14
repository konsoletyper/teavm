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

import static org.teavm.backend.wasm.wasi.Wasi.INET4;
import org.teavm.backend.wasm.runtime.WasiBuffer;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.runtime.net.SockAddr;

public class SockAddrInet4 implements SockAddr {
    private final byte[] addr;
    private final int port;

    public static class SockAddrInet4Struct extends Structure {
        public int family;
        public byte a0;
        public byte a1;
        public byte a2;
        public byte a3;
        public short port;
    }

    public SockAddrInet4(byte[] addr, int port) {
        validateIPv4Address(addr);
        this.addr = addr;
        this.port = validatePort(port & 0xFFFF);
    }

    public SockAddrInet4(int addr, int port) {
        this(intToIPv4(addr), port);
    }

    private static byte[] intToIPv4(int addr) {
        return new byte[] {
            (byte) ((addr >> 24) & 0xFF),
            (byte) ((addr >> 16) & 0xFF),
            (byte) ((addr >> 8) & 0xFF),
            (byte) (addr & 0xFF)
        };
    }

    private static void validateIPv4Address(byte[] addr) {
        if (addr.length != 4) {
            throw new IllegalArgumentException("IPv4 address must be exactly 4 bytes long.");
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
        SockAddrInet4Struct s = argsAddress.toStructure();
        s.family = INET4;
        s.a0 = addr[0];
        s.a1 = addr[1];
        s.a2 = addr[2];
        s.a3 = addr[3];
        s.port = (short) port;
        return argsAddress;
    }

    @Override
    public int sockPort() {
        return port;
    }

    @Override
    public int sockFamily() {
        return INET4;
    }

    public byte[] getAddr() {
        return addr;
    }

    @Override
    public String toString() {
        return (addr[0] & 0xFF) + "." + (addr[1] & 0xFF) + "." + (addr[2] & 0xFF) + "."
                + (addr[3] & 0xFF) + ":" + port;
    }

    @Override
    public int sockAddrLen() {
        return Structure.sizeOf(SockAddrInet4Struct.class);
    }
}
