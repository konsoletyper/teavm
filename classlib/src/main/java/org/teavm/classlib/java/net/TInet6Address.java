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

public final class TInet6Address extends TInetAddress {
    static final int INADDRSZ = 16;

    TInet6Address(byte[] addr, String hostname) {
        super(addr, hostname, IPv6);
        validateIPv6Address(addr);
    }

    private static void validateIPv6Address(byte[] addr) {
        if (addr.length != INADDRSZ) {
            throw new IllegalArgumentException("IPv6 address must be exactly 16 bytes long.");
        }
    }

    @Override
    public String getHostAddress() {
        StringBuilder sb = new StringBuilder();
        int[] segments = new int[8];

        for (int i = 0; i < 8; i++) {
            segments[i] = ((address[i * 2] & 0xFF) << 8) | (address[i * 2 + 1] & 0xFF);
        }

        int startZero = -1;
        int maxZeroLength = 0;
        int zeroLength = 0;
        int currentStart = -1;
        for (int i = 0; i < segments.length; i++) {
            if (segments[i] == 0) {
                if (currentStart == -1) {
                    currentStart = i;
                }
                zeroLength++;
            } else {
                if (zeroLength > maxZeroLength) {
                    maxZeroLength = zeroLength;
                    startZero = currentStart;
                }
                zeroLength = 0;
                currentStart = -1;
            }
        }
        if (zeroLength > maxZeroLength) {
            maxZeroLength = zeroLength;
            startZero = currentStart;
        }

        for (int i = 0; i < segments.length; i++) {
            if (startZero == i && maxZeroLength > 1) {
                if (i == 0) {
                    sb.append("::");
                } else {
                    sb.append(':');
                }
                i += maxZeroLength - 1;
                continue;
            }
            sb.append(Integer.toHexString(segments[i]));
            if (i < segments.length - 1) {
                sb.append(':');
            }
        }
        return sb.toString();
    }

    @Override
    public boolean isAnyLocalAddress() {
        for (byte b : address) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public boolean isIPv4CompatibleAddress() {
        for (int i = 0; i < 12; i++) {
            if (address[i] != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isLinkLocalAddress() {
        return (address[0] & 0xFF) == 0xFE && (address[1] & 0xC0) == 0x80;
    }

    @Override
    public boolean isLoopbackAddress() {
        if (address[15] != 1) {
            return false;
        }
        for (int i = 0; i < 15; i++) {
            if (address[i] != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isMCGlobal() {
        return (address[0] & 0xFF) == 0xFF && (address[1] & 0x0F) == 0x0E;
    }

    @Override
    public boolean isMCLinkLocal() {
        return (address[0] & 0xFF) == 0xFF && (address[1] & 0x0F) == 0x02;
    }

    @Override
    public boolean isMCNodeLocal() {
        return (address[0] & 0xFF) == 0xFF && (address[1] & 0x0F) == 0x01;
    }

    @Override
    public boolean isMCOrgLocal() {
        return (address[0] & 0xFF) == 0xFF && (address[1] & 0x0F) == 0x08;
    }

    @Override
    public boolean isMCSiteLocal() {
        return (address[0] & 0xFF) == 0xFF && (address[1] & 0x0F) == 0x05;
    }

    @Override
    public boolean isMulticastAddress() {
        return (address[0] & 0xFF) == 0xFF;
    }

    @Override
    public boolean isSiteLocalAddress() {
        return (address[0] & 0xFF) == 0xFE && (address[1] & 0xC0) == 0xC0;
    }
}
