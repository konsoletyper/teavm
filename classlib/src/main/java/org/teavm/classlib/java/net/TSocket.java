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
import static org.teavm.backend.wasm.wasi.Wasi.SHUT_RD;
import static org.teavm.backend.wasm.wasi.Wasi.SHUT_RDWR;
import static org.teavm.backend.wasm.wasi.Wasi.SHUT_WR;
import static org.teavm.backend.wasm.wasi.Wasi.SOCK_DGRAM;
import static org.teavm.backend.wasm.wasi.Wasi.SOCK_STREAM;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.teavm.backend.wasm.runtime.net.impl.SockAddrInet4;
import org.teavm.backend.wasm.runtime.net.impl.SockAddrInet6;
import org.teavm.runtime.net.SockAddr;
import org.teavm.runtime.net.VirtualSocket;
import org.teavm.runtime.net.VirtualSocketProvider;

public class TSocket implements Closeable {
    protected int fd;
    protected boolean created;

    private static final int FD_CLOSED = -1;

    private static VirtualSocket vs() {
        return VirtualSocketProvider.getInstance();
    }

    public TSocket(int fd) {
        this.fd = fd;
        this.created = true;
    }

    public int getFd() {
        return fd;
    }

    public TSocket() throws IOException {
        this.fd = vs().socket(INET_UNSPEC, SOCK_STREAM);
        this.created = true;
    }

    public TSocket(TInetAddress address, int port) throws IOException {
        this(address, port, true);
    }

    public TSocket(TInetAddress host, int port, boolean stream) throws IOException {
        this(host != null ? new TInetSocketAddress(host, port) : null,
            (TSocketAddress) null, stream);
    }

    public TSocket(TInetAddress address, int port, TInetAddress localAddr, int localPort)
            throws IOException {
        this(address != null ? new TInetSocketAddress(address, port) : null,
            new TInetSocketAddress(localAddr, localPort), true);
    }

    public TSocket(TProxy proxy) throws IOException {
        throw new UnsupportedOperationException();
    }

    public TSocket(String host, int port) throws IOException {
        this(host, port, true);
    }

    public TSocket(String host, int port, boolean stream) throws IOException {
        this(host != null ? new TInetSocketAddress(host, port)
                : new TInetSocketAddress(TInetAddress.getByName(null), port),
                (TSocketAddress) null, stream);
    }

    public TSocket(String host, int port, TInetAddress localAddr, int localPort)
            throws IOException {
        this(host != null ? new TInetSocketAddress(host, port)
                : new TInetSocketAddress(TInetAddress.getByName(null), port),
                new TInetSocketAddress(localAddr, localPort), true);
    }

    private TSocket(TSocketAddress addr, TSocketAddress localAddr, boolean stream)
            throws IOException {
        assert addr instanceof TInetSocketAddress;
        int sotype = stream ? SOCK_STREAM : SOCK_DGRAM;

        SockAddr sa = getSockAddr(addr);
        int family = sa.sockFamily();
        this.fd = vs().socket(family, sotype);
        this.created = true;

        if (localAddr != null) {
            bind(localAddr);
        }
        connect(sa);
    }

    private SockAddr getSockAddr(TSocketAddress addr) throws TSocketException {
        assert addr instanceof TInetSocketAddress;
        TInetSocketAddress isa = (TInetSocketAddress) addr;
        return getSockAddr(isa.getAddress(), isa.getPort());
    }

    private SockAddr getSockAddr(TInetAddress addr, int port) throws TSocketException {
        if (addr.getFamily() == TInetAddress.IPv4) {
            return new SockAddrInet4(addr.getAddress(), port);
        } else if (addr.getFamily() == TInetAddress.IPv6) {
            return new SockAddrInet6(addr.getAddress(), port);
        }
        throw new TSocketException("Unknown address family");
    }

    public void bind(TSocketAddress bindpoint) throws IOException {
        SockAddr sa = getSockAddr(bindpoint);
        vs().bind(fd, sa);
    }

    private void connect(SockAddr sa) throws IOException {
        vs().connect(fd, sa);
    }

    public void connect(TSocketAddress sa) throws IOException {
        assert sa instanceof TInetSocketAddress;
        SockAddr sAddr = getSockAddr(sa);
        connect(sAddr);
    }

    public void connect(TSocketAddress endpoint, int timeout) throws IOException {
        connect(endpoint);
        setSoTimeout(timeout);
    }

    public void close() throws IOException {
        ensureSocketOpen();
        vs().shutdown(fd, SHUT_RDWR);
        this.fd = FD_CLOSED;
    }

    public TInetAddress getInetAddress() throws IOException {
        ensureSocketOpen();
        SockAddr remoteAddress = vs().getSockName(fd);

        if (remoteAddress.sockFamily() == INET4) {
            SockAddrInet4 ipv4Address = (SockAddrInet4) remoteAddress;
            return new TInet4Address(ipv4Address.getAddr(), (String) null);
        } else if (remoteAddress.sockFamily() == INET6) {
            SockAddrInet6 ipv6Address = (SockAddrInet6) remoteAddress;
            return new TInet6Address(ipv6Address.getAddr(), (String) null);
        }
        throw new TSocketException("Unsupported address type: " + remoteAddress.sockFamily());
    }

    public TInetAddress getLocalAddress() throws IOException {
        ensureSocketOpen();
        SockAddr localAddress = vs().getSockName(fd);

        if (localAddress.sockFamily() == INET4) {
            SockAddrInet4 ipv4Address = (SockAddrInet4) localAddress;
            return new TInet4Address(ipv4Address.getAddr(), (String) null);
        } else if (localAddress.sockFamily() == INET6) {
            SockAddrInet6 ipv6Address = (SockAddrInet6) localAddress;
            return new TInet6Address(ipv6Address.getAddr(), (String) null);
        }
        throw new TSocketException("Unsupported address type: " + localAddress.sockFamily());
    }

    public int getLocalPort() throws IOException {
        ensureSocketOpen();
        SockAddr localAddress = vs().getSockName(fd);
        return localAddress.sockPort();
    }

    public TSocketAddress getLocalSocketAddress() throws IOException {
        ensureSocketOpen();
        SockAddr localAddress = vs().getSockName(fd);

        TInetAddress inetAddress;
        if (localAddress.sockFamily() == INET4) {
            SockAddrInet4 ipv4Address = (SockAddrInet4) localAddress;
            inetAddress = new TInet4Address(ipv4Address.getAddr(), (String) null);
        } else if (localAddress.sockFamily() == INET6) {
            SockAddrInet6 ipv6Address = (SockAddrInet6) localAddress;
            inetAddress = new TInet6Address(ipv6Address.getAddr(), (String) null);
        } else {
            throw new TSocketException("Unsupported address type: " + localAddress.sockFamily());
        }
        return new TInetSocketAddress(inetAddress, localAddress.sockPort());
    }

    public int getPort() throws IOException {
        ensureSocketOpen();
        SockAddr remoteAddress = vs().getPeerName(fd);
        return remoteAddress.sockPort();
    }

    public TSocketAddress getRemoteSocketAddress() throws IOException {
        ensureSocketOpen();
        SockAddr remoteAddress = vs().getPeerName(fd);

        TInetAddress inetAddress;
        if (remoteAddress.sockFamily() == INET4) {
            SockAddrInet4 ipv4Address = (SockAddrInet4) remoteAddress;
            inetAddress = new TInet4Address(ipv4Address.getAddr(), (String) null);
        } else if (remoteAddress.sockFamily() == INET6) {
            SockAddrInet6 ipv6Address = (SockAddrInet6) remoteAddress;
            inetAddress = new TInet6Address(ipv6Address.getAddr(), (String) null);
        } else {
            throw new TSocketException("Unsupported address type: " + remoteAddress.sockFamily());
        }
        return new TInetSocketAddress(inetAddress, remoteAddress.sockPort());
    }

    public InputStream getInputStream() throws IOException {
        ensureSocketOpen();
        final SockAddr remoteAddress = vs().getPeerName(fd);

        return new InputStream() {
            private static final int MAX_LENGTH = 8192;
            private final byte[] buffer = new byte[MAX_LENGTH];

            @Override
            public int read() throws IOException {
                int bytesRead = read(buffer, 0, 1);
                return (bytesRead == -1) ? -1 : (buffer[0] & 0xFF);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException("Buffer is null");
                }
                if (off < 0 || len < 0 || off + len > b.length) {
                    throw new IndexOutOfBoundsException("Invalid offset or length");
                }
                if (len == 0) {
                    return 0;
                }

                try {
                    int bytesRead = vs().recvFrom(fd, buffer, len, remoteAddress);
                    if (bytesRead <= 0) {
                        return -1;
                    }

                    System.arraycopy(buffer, 0, b, off, bytesRead);
                    return bytesRead;
                } catch (Exception e) {
                    throw new IOException("Error reading from socket", e);
                }
            }

            @Override
            public int available() throws IOException {
                return vs().getSockRecvBufSize(fd);
            }
        };
    }

    public OutputStream getOutputStream() throws IOException {
        ensureSocketOpen();
        final SockAddr remoteAddress = vs().getPeerName(fd);

        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                byte[] singleByte = {(byte) b};
                write(singleByte, 0, 1);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                if (b == null) {
                    throw new NullPointerException("Buffer is null");
                }
                if (off < 0 || len < 0 || off + len > b.length) {
                    throw new IndexOutOfBoundsException("Invalid offset or length");
                }
                if (len == 0) {
                    return;
                }

                try {
                    int bytesSent = vs().sendTo(fd, b, len, remoteAddress);
                    if (bytesSent != len) {
                        throw new IOException("Failed to send all data to socket");
                    }
                } catch (Exception e) {
                    throw new IOException("Error writing to socket", e);
                }
            }
        };
    }

    public boolean getKeepAlive() throws IOException {
        ensureSocketOpen();
        return vs().getSockKeepAlive(fd) == 1;
    }

    public void setKeepAlive(boolean on) throws IOException {
        ensureSocketOpen();
        vs().setSockKeepAlive(fd, on ? 1 : 0);
    }

    public boolean isBound() {
        return fd > 0 && created;
    }

    public boolean isClosed() {
        return fd == FD_CLOSED || !created;
    }

    public boolean isConnected() {
        try {
            return isBound() && !isAddressZero(vs().getPeerName(fd));
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isInputShutdown() {
        if (isClosed()) {
            return true;
        }
        try {
            return vs().recvFrom(fd, new byte[1], 0, null) == -1;
        } catch (Exception e) {
            return true;
        }
    }

    public boolean isOutputShutdown() {
        if (isClosed()) {
            return true;
        }
        try {
            SockAddr remoteAddress = vs().getPeerName(fd);
            vs().sendTo(fd, new byte[0], 0, remoteAddress);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    public void shutdownInput() throws IOException {
        if (!isClosed()) {
            vs().shutdown(fd, SHUT_RD);
        }
    }

    public void shutdownOutput() throws IOException {
        if (!isClosed()) {
            vs().shutdown(fd, SHUT_WR);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Socket[");

        if (!isConnected()) {
            sb.append("unconnected");
        } else {
            try {
                sb.append("addr=").append(getInetAddress());
                sb.append(",port=").append(getPort());
                sb.append(",localport=").append(getLocalPort());
            } catch (IOException e) {
                sb.append("error while retrieving socket information");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    public void setReuseAddress(boolean on) throws IOException {
        ensureSocketOpen();
        vs().setSockReuseAddr(fd, on ? 1 : 0);
    }

    public boolean getReuseAddress() throws IOException {
        ensureSocketOpen();
        return vs().getSockReuseAddr(fd) == 1;
    }

    public int getReceiveBufferSize() throws IOException {
        ensureSocketOpen();
        return vs().getSockRecvBufSize(fd);
    }

    public void setReceiveBufferSize(int size) throws IOException {
        if (size <= 0) {
            throw new IllegalArgumentException("invalid receive size");
        }
        ensureSocketOpen();
        vs().setSockRecvBufSize(fd, size);
    }

    public int getSendBufferSize() throws IOException {
        ensureSocketOpen();
        return vs().getSockSendBufSize(fd);
    }

    public void setSendBufferSize(int size) throws IOException {
        if (size <= 0) {
            throw new IllegalArgumentException("invalid receive size");
        }
        ensureSocketOpen();
        vs().setSockSendBufSize(fd, size);
    }

    public int getSoLinger() throws IOException {
        ensureSocketOpen();
        return vs().getSockLinger(fd);
    }

    public void setSoLinger(boolean on, int linger) throws IOException {
        ensureSocketOpen();
        if (linger < 0) {
            throw new IllegalArgumentException("invalid value for SO_LINGER");
        }
        if (linger > 65535) {
            linger = 65535;
        }
        vs().setSockLinger(fd, on ? 1 : 0, linger);
    }

    public int getSoTimeout() throws IOException {
        ensureSocketOpen();
        return vs().getSockRecvTimeout(fd);
    }

    public void setSoTimeout(int timeout) throws IOException {
        if (timeout <= 0) {
            throw new IllegalArgumentException("invalid timeout");
        }
        ensureSocketOpen();
        vs().setSockRecvTimeout(fd, timeout);
    }

    public boolean getTcpNoDelay() throws IOException {
        ensureSocketOpen();
        return vs().getSockTcpNoDelay(fd) == 1;
    }

    public void setTcpNoDelay(boolean on) throws IOException {
        ensureSocketOpen();
        vs().setSockTcpNoDelay(fd, on ? 1 : 0);
    }

    /**
     * Sends a single byte of data on the socket. This method is intended to simulate the behavior
     * of sending urgent data but does not implement true TCP Urgent Data (out-of-band data with the
     * URG flag) due to limitations in the WASI environment.
     *
     * <p>Note: True TCP Urgent Data requires support for the URG flag in the TCP header, which is
     * not currently available in WASI. This implementation simply sends the byte as normal in-band
     * data.
     */
    public void sendUrgentData(int data) throws IOException {
        ensureSocketOpen();
        if (data < 0 || data > 255) {
            throw new IllegalArgumentException("Data must be a single byte (0-255).");
        }
        SockAddr remoteAddress = vs().getPeerName(fd);
        byte[] b = new byte[] {(byte) data};
        int len = 1;
        int bytesSent = vs().sendTo(fd, b, len, remoteAddress);
        if (bytesSent != len) {
            throw new IOException("Failed to send all data to socket");
        }
    }

    public static void printByteArrayAsChars(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null.");
        }

        for (byte b : data) {
            System.out.print((char) b);
        }
        System.out.println();
    }

    private static boolean isAddressZero(SockAddr addr) {
        if (addr instanceof SockAddrInet4) {
            SockAddrInet4 inet4 = (SockAddrInet4) addr;
            byte[] address = inet4.getAddr();
            for (byte b : address) {
                if (b != 0) {
                    return false;
                }
            }
        } else if (addr instanceof SockAddrInet6) {
            SockAddrInet6 inet6 = (SockAddrInet6) addr;
            byte[] address = inet6.getAddr();
            for (byte s : address) {
                if (s != 0) {
                    return false;
                }
            }
        }
        return addr.sockPort() == 0;
    }

    private void ensureSocketOpen() throws TSocketException {
        if (isClosed()) {
            throw new TSocketException("Socket is closed");
        }
    }
}
