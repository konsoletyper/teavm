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

import java.io.IOException;
import org.teavm.runtime.net.VirtualSocket;
import org.teavm.runtime.net.VirtualSocketProvider;

public class TServerSocket {
    private TSocket socket;
    private boolean bound;

    private static final int FD_CLOSED = -1;

    private static VirtualSocket vs() {
        return VirtualSocketProvider.getInstance();
    }
    
    public TServerSocket() throws IOException {
        this.socket = new TSocket();
        this.bound = false;
    }

    public TServerSocket(int port) throws IOException {
        this();
        bind(new TInetSocketAddress(port));
    }

    public TServerSocket(int port, int backlog) throws IOException {
        this();
        bind(new TInetSocketAddress(port), backlog);
    }

    public TServerSocket(int port, int backlog, TInetAddress bindAddr) throws IOException {
        this();
        bind(new TInetSocketAddress(bindAddr, port), backlog);
    }

    public void bind(TSocketAddress endpoint) throws IOException {
        bind(endpoint, 50);
    }

    public void bind(TSocketAddress endpoint, int backlog) throws IOException {
        if (bound) {
            throw new IOException("Socket is already bound");
        }
        if (!(endpoint instanceof TInetSocketAddress)) {
            throw new IllegalArgumentException("Unsupported address type");
        }
        socket.bind(endpoint);
        vs().listen(socket.getFd(), backlog);
        this.bound = true;
    }

    public TSocket accept() throws IOException {
        ensureSocketOpen();
        ensureSocketBound();
        int clientFd = vs().accept(socket.getFd(), 0);
        return new TSocket(clientFd);
    }

    public void close() throws IOException {
        if (!isClosed()) {
            socket.close();
        }
    }

    public boolean isBound() {
        return bound;
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public TInetAddress getInetAddress() throws IOException {
        return socket.getInetAddress();
    }

    public int getLocalPort() throws IOException {
        return socket.getLocalPort();    }

    public TSocketAddress getLocalSocketAddress() throws IOException {
        return socket.getLocalSocketAddress();
    }

    public void setReuseAddress(boolean on) throws IOException {
        socket.setReuseAddress(on);
    }

    public boolean getReuseAddress() throws IOException {
        return socket.getReuseAddress();
    }

    public void setSoTimeout(int timeout) throws IOException {
        socket.setSoTimeout(timeout);
    }

    public int getSoTimeout() throws IOException {
        return socket.getSoTimeout();
    }

    public int getReceiveBufferSize() throws IOException {
        return socket.getReceiveBufferSize();
    }

    public void setReceiveBufferSize(int size) throws IOException {
        socket.setReceiveBufferSize(size);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ServerSocket[");

        if (!socket.isConnected()) {
            sb.append("unconnected");
        } else {
            try {
                sb.append("addr=").append(getInetAddress());
                sb.append(",port=").append(getLocalPort());
            } catch (IOException e) {
                sb.append("error while retrieving socket information");
            }
        }

        sb.append("]");
        return sb.toString();
    }

    private void ensureSocketOpen() throws IOException {
        if (isClosed()) {
            throw new IOException("Socket is closed");
        }
    }

    private void ensureSocketBound() throws IOException {
        if (!isBound()) {
            throw new IOException("Socket is not bound");
        }
    }
}
