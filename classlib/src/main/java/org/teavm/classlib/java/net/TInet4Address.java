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

public final class TInet4Address extends TInetAddress {
    static final int INADDRSZ = 4;

    TInet4Address(byte[] addr, String hostname) {
        super(addr, hostname, IPv4);
        validateIPv4Address(addr);
    }

    private static void validateIPv4Address(byte[] addr) {
        if (addr.length != INADDRSZ) {
            throw new IllegalArgumentException("IPv4 address must be exactly 4 bytes long.");
        }
    }

    @Override
    public boolean isAnyLocalAddress() {
        return address[0] == 0 && address[1] == 0 && address[2] == 0 && address[3] == 0;
    }

    @Override
    public boolean isLoopbackAddress() {
        return address[0] == 127;
    }

    @Override
    public boolean isLinkLocalAddress() {
        return (address[0] & 0xFF) == 169 && (address[1] & 0xFF) == 254;
    }

    @Override
    public boolean isMCGlobal() {
        return (address[0] & 0xFF) >= 224
                && (address[0] & 0xFF) <= 238
                && !(address[0] == 224 && address[1] == 0 && address[2] == 0);
    }

    @Override
    public boolean isMCLinkLocal() {
        return (address[0] & 0xFF) == 224 && (address[1] & 0xFF) == 0;
    }

    @Override
    public boolean isMCOrgLocal() {
        return (address[0] & 0xFF) == 239 && (address[1] & 0xFF) >= 192;
    }

    @Override
    public boolean isMCSiteLocal() {
        return (address[0] & 0xFF) == 239 && (address[1] & 0xFF) == 255;
    }

    @Override
    public boolean isMulticastAddress() {
        return (address[0] & 0xFF) >= 224 && (address[0] & 0xFF) <= 239;
    }

    @Override
    public boolean isSiteLocalAddress() {
        int firstOctet = address[0] & 0xFF;
        int secondOctet = address[1] & 0xFF;

        return (firstOctet == 10)
                || (firstOctet == 172 && secondOctet >= 16 && secondOctet <= 31)
                || (firstOctet == 192 && secondOctet == 168);
    }
}
