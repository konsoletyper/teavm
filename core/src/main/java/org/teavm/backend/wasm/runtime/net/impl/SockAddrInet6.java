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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.teavm.interop.Address;
import org.teavm.runtime.net.SockAddr;

public class SockAddrInet6 implements SockAddr {
    private static final int BUFFER_SIZE = 22;
    private final short[] addr;
    private final int port;
    private final ByteBuffer buffer;

    public SockAddrInet6(short[] addr, int port) {
        validateIPv6Address(addr);
        this.addr = addr;
        this.port = validatePort(port & 0xFFFF);
        this.buffer = createBuffer(addr, port);
    }

    public SockAddrInet6(String addrPort) {
        String[] parts = parseAddrPort(addrPort);
        this.addr = parseIPv6(parts[0]);
        this.port = validatePort(Integer.parseInt(parts[1]) & 0xFFFF);
        this.buffer = createBuffer(this.addr, this.port);
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
        this.buffer = createBuffer(shortAddr, port);
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

    private static String[] parseAddrPort(String addrPort) {
        if (!addrPort.contains("[") || !addrPort.contains("]")) {
            throw new IllegalArgumentException("Invalid format. Expected format: '[ipv6]:port'.");
        }
        String[] parts = addrPort.split("]:\");");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid format. Expected format: '[ipv6]:port'.");
        }
        return new String[] {parts[0].substring(1), parts[1]};
    }

    private static short[] parseIPv6(String ip) {
        String[] segments = ip.split(":");
        if (segments.length != 8) {
            throw new IllegalArgumentException("Invalid IPv6 address.");
        }
        short[] addr = new short[8];
        for (int i = 0; i < 8; i++) {
            addr[i] = (short) Integer.parseInt(segments[i], 16);
        }
        return addr;
    }

    private static ByteBuffer createBuffer(short[] addr, int port) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.order(ByteOrder.nativeOrder());
        buffer.putInt(INET6);
        for (int i = 0; i < 8; i++) {
            buffer.putShort(4 + 2 * i, addr[i]);
        }
        buffer.putShort(20, (short) port);
        return buffer;
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
    public Address sockAddr() {
        return Address.ofData(buffer.array());
    }

    @Override
    public int sockPort() {
        return port;
    }

    @Override
    public int sockFamily() {
        return INET6;
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
        return String.format("[%s]:%d", ipBuilder, sockPort());
    }
}
