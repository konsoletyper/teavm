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
package org.teavm.runtime.net;

import java.net.SocketException;

public class VirtualSocketImpl implements VirtualSocket {

    @Override
    public int socket(int proto, int sotype) throws SocketException {
        throw new SocketException("socket: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void connect(int fd, SockAddr sa) throws SocketException {
        throw new SocketException("connect: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void bind(int fd, SockAddr sa) throws SocketException {
        throw new SocketException("bind: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void listen(int fd, int backlog) throws SocketException {
        throw new SocketException("listen: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int sendTo(int fd, byte[] buf, int len, SockAddr sa) throws SocketException {
        throw new SocketException("send_to: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int recvFrom(int fd, byte[] buf, int len, SockAddr sa) throws SocketException {
        throw new SocketException("recv_from: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void shutdown(int fd, int how) throws SocketException {
        throw new SocketException("shutdown: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int accept(int fd, int flags) throws SocketException {
        throw new SocketException("accept: not supported for JS and non-WASI Wasm");
    }

    @Override
    public SockAddr getSockName(int fd) throws SocketException {
        throw new SocketException("get_sock_name: not supported for JS and non-WASI Wasm");
    }

    @Override
    public SockAddr getPeerName(int fd) throws SocketException {
        throw new SocketException("get_peer_name: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void setSockBroadcast(int fd, int value) throws SocketException {
        throw new SocketException("sock_set_broadcast: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void setSockReuseAddr(int fd, int value) throws SocketException {
        throw new SocketException("sock_set_reuse_addr: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int getSockReuseAddr(int fd) throws SocketException {
        throw new SocketException("sock_get_reuse_addr: not supported for JS and non-WASI Wasm");
    }

    @Override
    public SockAddr[] getAddrInfo(
            String name, String service, int proto, int sotype, int hintsEnabled)
            throws SocketException {
        throw new SocketException("get_addr_info: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int getSockKeepAlive(int fd) throws SocketException {
        throw new SocketException("sock_get_keep_alive: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void setSockKeepAlive(int fd, int value) throws SocketException {
        throw new SocketException("sock_set_keep_alive: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int getSockRecvBufSize(int fd) throws SocketException {
        throw new SocketException("sock_get_recv_buf_size: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void setSockRecvBufSize(int fd, int size) throws SocketException {
        throw new SocketException("sock_set_recv_buf_size: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int getSockSendBufSize(int fd) throws SocketException {
        throw new SocketException("sock_get_send_buf_size: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void setSockSendBufSize(int fd, int size) throws SocketException {
        throw new SocketException("sock_set_send_buf_size: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int getSockLinger(int fd) throws SocketException {
        throw new SocketException("sock_get_linger: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void setSockLinger(int fd, int value, int linger) throws SocketException {
        throw new SocketException("sock_set_linger: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int getSockRecvTimeout(int fd) throws SocketException {
        throw new SocketException("sock_get_recv_timeout: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void setSockRecvTimeout(int fd, int value) throws SocketException {
        throw new SocketException("sock_set_recv_timeout: not supported for JS and non-WASI Wasm");
    }

    @Override
    public int getSockTcpNoDelay(int fd) throws SocketException {
        throw new SocketException("sock_get_tcp_no_delay: not supported for JS and non-WASI Wasm");
    }

    @Override
    public void setSockTcpNoDelay(int fd, int value) throws SocketException {
        throw new SocketException("sock_set_tcp_no_delay: not supported for JS and non-WASI Wasm");
    }
}
