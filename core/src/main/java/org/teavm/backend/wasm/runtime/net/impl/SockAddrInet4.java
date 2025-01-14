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
import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.teavm.interop.Address;
import org.teavm.runtime.net.SockAddr;

public class SockAddrInet4 implements SockAddr {
    private static final int BUFFER_SIZE = 10;
    private final byte[] addr;
    private final int port;
    private final ByteBuffer buffer;

    public SockAddrInet4(byte[] addr, int port) {
        validateIPv4Address(addr);
        this.addr = addr;
        this.port = validatePort(port & 0xFFFF);
        this.buffer = createBuffer(addr, port);
    }

    public SockAddrInet4(int addr, int port) {
        this.addr = intToIPv4(addr);
        this.port = validatePort(port & 0xFFFF);
        this.buffer = createBuffer(this.addr, port);
    }

    public SockAddrInet4(String addrPort) {
        String[] parts = parseAddrPort(addrPort);
        this.addr = parseIPv4(parts[0]);
        this.port = validatePort(Integer.parseInt(parts[1]) & 0xFFFF);
        this.buffer = createBuffer(this.addr, this.port);
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

    private static byte[] intToIPv4(int addr) {
        return new byte[] {
            (byte) ((addr >> 24) & 0xFF),
            (byte) ((addr >> 16) & 0xFF),
            (byte) ((addr >> 8) & 0xFF),
            (byte) (addr & 0xFF)
        };
    }

    private static String[] parseAddrPort(String addrPort) {
        Pattern pattern = Pattern.compile("^(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d+)$");
        Matcher matcher = pattern.matcher(addrPort);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid format. Expected format: 'ip:port'.");
        }
        return new String[] {matcher.group(1), matcher.group(2)};
    }

    private static byte[] parseIPv4(String ip) {
        String[] segments = ip.split("\\.");
        if (segments.length != 4) {
            throw new IllegalArgumentException("Invalid IPv4 address.");
        }
        byte[] addr = new byte[4];
        for (int i = 0; i < 4; i++) {
            int segment = Integer.parseInt(segments[i]);
            if (segment < 0 || segment > 255) {
                throw new IllegalArgumentException("Invalid IPv4 address segment: " + segment);
            }
            addr[i] = (byte) segment;
        }
        return addr;
    }

    private static ByteBuffer createBuffer(byte[] addr, int port) {
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
        buffer.putInt(0, INET4);
        for (int i = 0; i < 4; i++) {
            buffer.put(4 + i, addr[i]);
        }
        buffer.putShort(8, (short) ntohs((short) port));
        return buffer;
    }

    private static int ntohs(short port) {
        return Short.reverseBytes(port) & 0xFFFF;
    }

    public byte[] getAddr() {
        return addr;
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
        return INET4;
    }

    @Override
    public String toString() {
        return String.format(
                "%d.%d.%d.%d:%d",
                addr[0] & 0xFF, addr[1] & 0xFF, addr[2] & 0xFF, addr[3] & 0xFF, port);
    }
}
