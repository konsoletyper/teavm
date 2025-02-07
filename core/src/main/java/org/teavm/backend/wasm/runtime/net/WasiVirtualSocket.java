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
package org.teavm.backend.wasm.runtime.net;

import static org.teavm.backend.wasm.wasi.Wasi.AF_INET;
import static org.teavm.backend.wasm.wasi.Wasi.AF_INET6;
import static org.teavm.backend.wasm.wasi.Wasi.AF_UNIX;
import static org.teavm.backend.wasm.wasi.Wasi.SOCK_ANY;
import static org.teavm.backend.wasm.wasi.Wasi.SOCK_DGRAM;
import static org.teavm.backend.wasm.wasi.Wasi.SOCK_STREAM;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.teavm.backend.wasm.runtime.WasiBuffer;
import org.teavm.backend.wasm.runtime.net.impl.SockAddrInet4;
import org.teavm.backend.wasm.runtime.net.impl.SockAddrInet6;
import org.teavm.backend.wasm.wasi.Wasi;
import org.teavm.interop.Address;
import org.teavm.interop.Structure;
import org.teavm.runtime.net.SockAddr;
import org.teavm.runtime.net.VirtualSocket;

public class WasiVirtualSocket implements VirtualSocket {

    private static final int IPV4_ADDR_SIZE = 4;
    private static final int IPV6_ADDR_SIZE = 8;
    private static final int ADDR_SIZE = 22;
    private static final int ADDR_INFO_BUFFER_SIZE = 28;
    private static final int ADDR_INFO_ADDR_BUFFER_SIZE = 18;
    private static final int MAX_RESOLVED_ADDRESSES = 16;

    public static final int HINTS_ENABLED = 1;
    public static final int HINTS_DISABLED = 2;

    public class AddrInfoHints extends Structure {
        public int type;
        public int family;
        public int hintsEnabled;
    }

    public static class CIOVec extends Structure {
        public int address;
        public int len;
    }

    @Override
    public int socket(int proto, int sotype) throws SocketException {
        validateProto(proto);
        validateSotype(sotype);

        int[] newFd = new int[1];
        int errno = Wasi.sockOpen(0, proto, sotype, Address.ofData(newFd));
        if (errno != 0) {
            throw new SocketException("socket: " + errno);
        }
        return newFd[0];
    }

    @Override
    public void connect(int fd, SockAddr sa) throws SocketException {
        try {
            Address rawAddr = sa.sockAddr();
            int errno = Wasi.sockConnect(fd, rawAddr);
            if (errno != 0) {
                throw new SocketException("сonnect: " + errno);
            }
        } catch (Exception e) {
            throw new SocketException("Failed to get SockAddr: " + e.getMessage());
        }
    }

    @Override
    public void bind(int fd, SockAddr sa) throws SocketException {
        try {
            Address rawAddr = sa.sockAddr();
            int errno = Wasi.sockBind(fd, rawAddr);
            if (errno != 0) {
                throw new SocketException("bind: " + errno);
            }
        } catch (Exception e) {
            throw new SocketException("Failed to get SockAddr: " + e.getMessage());
        }
    }

    @Override
    public void listen(int fd, int backlog) throws SocketException {
        int errno = Wasi.sockListen(fd, backlog);
        if (errno != 0) {
            throw new SocketException("listen: " + errno);
        }
    }

    @Override
    public int sendTo(int fd, byte[] buf, int len, SockAddr sa) throws SocketException {
        Address rawAddr;

        try {
            rawAddr = sa.sockAddr();
        } catch (Exception e) {
            throw new SocketException("Failed to get SockAddr: " + e.getMessage());
        }

        Address argsAddress = WasiBuffer.getBuffer();
        argsAddress = Address.align(argsAddress.add(sa.sockAddrLen()), 16);
        CIOVec s = argsAddress.toStructure();
        s.address = (int) Address.ofData(buf).toLong();
        s.len = len;

        int[] dataLen = new int[1];
        int errno = Wasi.sockSendTo(fd, argsAddress, 1, 0, rawAddr, Address.ofData(dataLen));

        if (errno != 0) {
            throw new SocketException("send_to: " + errno);
        }
        return dataLen[0];
    }

    @Override
    public int recvFrom(int fd, byte[] buf, int len, SockAddr sa) throws SocketException {
        byte[] addr = new byte[ADDR_SIZE];
        Address rawAddr = Address.ofData(addr);

        Address argsAddress = WasiBuffer.getBuffer();
        CIOVec s = argsAddress.toStructure();
        s.address = (int) Address.ofData(buf).toLong();
        s.len = len;

        int[] dataLen = new int[1];

        int errno = Wasi.sockRecvFrom(fd, argsAddress, 1, 0, rawAddr, Address.ofData(dataLen));
        if (errno != 0) {
            throw new SocketException("recv_from: " + errno);
        }
        return dataLen[0];
    }

    @Override
    public void shutdown(int fd, int how) throws SocketException {
        int errno = Wasi.sockShutdown(fd, how);
        if (errno != 0) {
            throw new SocketException("shutdown: " + errno);
        }
    }

    @Override
    public int accept(int fd, int flags) throws SocketException {
        int[] newFd = new int[1];
        int errno = Wasi.sockAccept(fd, flags, Address.ofData(newFd));
        if (errno != 0) {
            throw new SocketException("accept: " + errno);
        }
        return newFd[0];
    }

    @Override
    public SockAddr getSockName(int fd) throws SocketException {
        byte[] addr = new byte[ADDR_SIZE];
        int errno = Wasi.sockAddrLocal(fd, Address.ofData(addr));
        if (errno != 0) {
            throw new SocketException("get_sock_name: " + errno);
        }
        return parseSockAddr(addr);
    }

    @Override
    public SockAddr getPeerName(int fd) throws SocketException {
        byte[] addr = new byte[ADDR_SIZE];
        int errno = Wasi.sockAddrRemote(fd, Address.ofData(addr));
        if (errno != 0) {
            throw new SocketException("get_peer_name: " + errno);
        }
        return parseSockAddr(addr);
    }

    @Override
    public void setSockBroadcast(int fd, int value) throws SocketException {
        int errno = Wasi.sockSetBroadcast(fd, value);
        if (errno != 0) {
            throw new SocketException("sockopt_set_broadcast: " + errno);
        }
    }

    public SockAddr[] getAddrInfo(
            String name, String service, int proto, int sotype, int hintsEnabled)
            throws SocketException {
        byte[] nameNT = toByteArrayWithNullTerminator(name);
        byte[] serviceNT = toByteArrayWithNullTerminator(service);

        if (hintsEnabled == HINTS_ENABLED) {
            validateProto(proto);
            validateSotype(sotype);
        }

        Address hintsAddr = WasiBuffer.getBuffer();
        AddrInfoHints hints = hintsAddr.toStructure();

        hints.type = sotype;
        hints.family = proto;
        hints.hintsEnabled = hintsEnabled;

        byte[] resultBuffer = new byte[ADDR_INFO_BUFFER_SIZE * MAX_RESOLVED_ADDRESSES];
        int[] resolvedCount = new int[1];

        int errno = Wasi.sockAddrResolve(
                Address.ofData(nameNT),
                Address.ofData(serviceNT),
                hintsAddr,
                Address.ofData(resultBuffer),
                resultBuffer.length,
                Address.ofData(resolvedCount));

        if (errno != 0) {
            throw new SocketException("get_addr_info: " + errno);
        }

        SockAddr[] addresses = new SockAddr[resolvedCount[0]];

        for (int i = 0; i < resolvedCount[0]; i++) {
            ByteBuffer buffer = ByteBuffer.wrap(resultBuffer, i * ADDR_INFO_BUFFER_SIZE, ADDR_INFO_BUFFER_SIZE)
                    .order(ByteOrder.nativeOrder());
            int sockKind = buffer.getInt();
            byte[] addrBuf = new byte[ADDR_INFO_ADDR_BUFFER_SIZE];
            buffer.get(addrBuf);
            buffer.getInt();

            ByteBuffer rawBuffer = ByteBuffer.allocate(ADDR_SIZE).order(ByteOrder.nativeOrder());
            rawBuffer.putInt(sockKind);
            rawBuffer.put(addrBuf);

            addresses[i] = parseSockAddr(rawBuffer.array());
        }

        return addresses;
    }

    @Override
    public int getSockKeepAlive(int fd) throws SocketException {
        int[] res = new int[1];
        int errno = Wasi.sockGetKeepAlive(fd, Address.ofData(res));
        if (errno != 0) {
            throw new SocketException("sock_get_keep_alive: " + errno);
        }
        return res[0];
    }

    @Override
    public void setSockKeepAlive(int fd, int value) throws SocketException {
        int errno = Wasi.sockSetKeepAlive(fd, value);
        if (errno != 0) {
            throw new SocketException("sock_set_keep_alive: " + errno);
        }
    }

    @Override
    public int getSockReuseAddr(int fd) throws SocketException {
        int[] res = new int[1];
        int errno = Wasi.sockGetReuseAddr(fd, Address.ofData(res));
        if (errno != 0) {
            throw new SocketException("sock_get_reuse_addr: " + errno);
        }
        return res[0];
    }

    @Override
    public void setSockReuseAddr(int fd, int value) throws SocketException {
        int errno = Wasi.sockSetReuseAddr(fd, value);
        if (errno != 0) {
            throw new SocketException("sockopt_set_reuse_addr: " + errno);
        }
    }

    @Override
    public int getSockRecvBufSize(int fd) throws SocketException {
        int[] res = new int[1];
        int errno = Wasi.sockGetRecvBufSize(fd, Address.ofData(res));
        if (errno != 0) {
            throw new SocketException("sock_get_recv_buf_size: " + errno);
        }
        return res[0];
    }

    @Override
    public void setSockRecvBufSize(int fd, int size) throws SocketException {
        int errno = Wasi.sockSetRecvBufSize(fd, size);
        if (errno != 0) {
            throw new SocketException("sock_set_recv_buf_size: " + errno);
        }
    }

    @Override
    public int getSockSendBufSize(int fd) throws SocketException {
        int[] res = new int[1];
        int errno = Wasi.sockGetSendBufSize(fd, Address.ofData(res));
        if (errno != 0) {
            throw new SocketException("sock_get_send_buf_size: " + errno);
        }
        return res[0];
    }

    @Override
    public void setSockSendBufSize(int fd, int size) throws SocketException {
        int errno = Wasi.sockSetSendBufSize(fd, size);
        if (errno != 0) {
            throw new SocketException("sock_set_send_buf_size: " + errno);
        }
    }

    @Override
    public int getSockLinger(int fd) throws SocketException {
        int[] isEnabled = new int[1];
        int[] linger = new int[1];
        int errno = Wasi.sockGetLinger(fd, Address.ofData(isEnabled), Address.ofData(linger));
        if (errno != 0) {
            throw new SocketException("sock_get_linger: " + errno);
        }
        if (isEnabled[0] == 0) {
            return linger[0];
        }
        return -1;
    }

    @Override
    public void setSockLinger(int fd, int value, int linger) throws SocketException {
        int errno = Wasi.sockSetLinger(fd, value, linger);
        if (errno != 0) {
            throw new SocketException("sock_set_linger: " + errno);
        }
    }

    @Override
    public int getSockRecvTimeout(int fd) throws SocketException {
        int[] res = new int[1];
        int errno = Wasi.sockGetRecvTimeout(fd, Address.ofData(res));
        if (errno != 0) {
            throw new SocketException("sock_get_recv_timeout: " + errno);
        }
        return res[0] / 1000;
    }

    @Override
    public void setSockRecvTimeout(int fd, int value) throws SocketException {
        int errno = Wasi.sockSetRecvTimeout(fd, value * 1000);
        if (errno != 0) {
            throw new SocketException("sock_set_recv_timeout: " + errno);
        }
    }

    @Override
    public int getSockTcpNoDelay(int fd) throws SocketException {
        int[] res = new int[1];
        int errno = Wasi.sockGetTcpNoDelay(fd, Address.ofData(res));
        if (errno != 0) {
            throw new SocketException("sock_get_tcp_no_delay: " + errno);
        }
        return res[0];
    }

    @Override
    public void setSockTcpNoDelay(int fd, int value) throws SocketException {
        int errno = Wasi.sockSetTcpNoDelay(fd, value);
        if (errno != 0) {
            throw new SocketException("sock_set_tcp_no_delay: " + errno);
        }
    }

    // ============================== Helpers ==============================

    private void validateProto(int proto) throws SocketException {
        if (!isValidProto(proto)) {
            throw new SocketException("Invalid protocol type: " + proto);
        }
    }

    private void validateSotype(int sotype) throws SocketException {
        if (!isValidSotype(sotype)) {
            throw new SocketException("Invalid socket type: " + sotype);
        }
    }

    private boolean isValidProto(int proto) {
        return proto == AF_INET || proto == AF_INET6 || proto == AF_UNIX;
    }

    private boolean isValidSotype(int sotype) {
        return sotype == SOCK_ANY || sotype == SOCK_DGRAM || sotype == SOCK_STREAM;
    }

    public static byte[] toByteArrayWithNullTerminator(String input) {
        if (input == null) {
            return new byte[] {0};
        }

        byte[] originalBytes = input.getBytes();
        byte[] result = new byte[originalBytes.length + 1];
        System.arraycopy(originalBytes, 0, result, 0, originalBytes.length);
        result[result.length - 1] = 0;
        return result;
    }

    private static SockAddr parseSockAddr(byte[] data) throws SocketException {
        if (data.length != ADDR_SIZE) {
            throw new SocketException(
                    "Expected " + ADDR_SIZE + " bytes of data, but got " + data.length);
        }

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.nativeOrder());
        int kind = buffer.getInt();

        if (kind == AF_INET) {
            byte[] addr = new byte[IPV4_ADDR_SIZE];
            buffer.get(addr);
            short port = buffer.getShort();
            return new SockAddrInet4(addr, port);
        } else if (kind == AF_INET6) {
            short[] addr = new short[IPV6_ADDR_SIZE];
            for (int i = 0; i < IPV6_ADDR_SIZE; i++) {
                addr[i] = buffer.getShort();
            }
            short port = buffer.getShort();
            return new SockAddrInet6(addr, port);
        } else {
            throw new SocketException("Unknown address family: " + kind);
        }
    }
}
