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

import org.teavm.backend.wasm.runtime.WasiBuffer;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;

public class AddrInfo {
    private static final int ADDR_BUF_SIZE = 18;

    public static class AddrInfoStruct extends Structure {
        public int sockKind;
        public byte b0;
        public byte b1;
        public byte b2;
        public byte b3;
        public byte b4;
        public byte b5;
        public byte b6;
        public byte b7;
        public byte b8;
        public byte b9;
        public byte b10;
        public byte b11;
        public byte b12;
        public byte b13;
        public byte b14;
        public byte b15;
        public byte b16;
        public byte b17;
        public int sockType;
    }

    private final int sockKind;
    private final byte[] addrBuf;
    private final int sockType;

    public AddrInfo() {
        this(0, new byte[ADDR_BUF_SIZE], 0);
    }

    public AddrInfo(int sockKind, byte[] addrBuf, int sockType) {
        if (addrBuf == null || addrBuf.length != ADDR_BUF_SIZE) {
            throw new IllegalArgumentException(
                    "addrBuf must be exactly " + ADDR_BUF_SIZE + " bytes long.");
        }
        this.sockKind = sockKind;
        this.addrBuf = addrBuf.clone();
        this.sockType = sockType;
    }

    public static int getBufferSize() {
        return Structure.sizeOf(AddrInfoStruct.class);
    }

    public static int getAddrBufferSize() {
        return ADDR_BUF_SIZE;
    }

    public int getSockKind() {
        return sockKind;
    }

    public byte[] getAddrBuf() {
        return addrBuf.clone();
    }

    public int getSockType() {
        return sockType;
    }

    public Address getAddress() {
        Address argsAddress = WasiBuffer.getBuffer();
        AddrInfoStruct s = argsAddress.toStructure();
        s.sockKind = sockKind;
        s.b0  = addrBuf[0];
        s.b1  = addrBuf[1];
        s.b2  = addrBuf[2];
        s.b3  = addrBuf[3];
        s.b4  = addrBuf[4];
        s.b5  = addrBuf[5];
        s.b6  = addrBuf[6];
        s.b7  = addrBuf[7];
        s.b8  = addrBuf[8];
        s.b9  = addrBuf[9];
        s.b10 = addrBuf[10];
        s.b11 = addrBuf[11];
        s.b12 = addrBuf[12];
        s.b13 = addrBuf[13];
        s.b14 = addrBuf[14];
        s.b15 = addrBuf[15];
        s.b16 = addrBuf[16];
        s.b17 = addrBuf[17];
        s.sockType = sockType;
        return argsAddress;
    }

    @Override
    public String toString() {
        StringBuilder addrBufString = new StringBuilder();
        for (byte b : addrBuf) {
            if (addrBufString.length() > 0) {
                addrBufString.append(" ");
            }
            addrBufString.append(String.format("%02X", b));
        }
        return "AddrInfo{sockKind=" + sockKind
                + ", addrBuf=[" + addrBufString
                + "], sockType=" + sockType + "}";
    }
}
