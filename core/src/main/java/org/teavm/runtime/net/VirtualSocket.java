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

public interface VirtualSocket {

    int socket(int proto, int sotype) throws SocketException;

    void connect(int fd, SockAddr sa) throws SocketException;

    void bind(int fd, SockAddr sa) throws SocketException;

    void listen(int fd, int backlog) throws SocketException;

    int sendTo(int fd, byte[] buf, int len, SockAddr sa) throws SocketException;

    int recvFrom(int fd, byte[] buf, int len, SockAddr sa) throws SocketException;

    void shutdown(int fd, int how) throws SocketException;

    int accept(int fd, int flags) throws SocketException;

    SockAddr getSockName(int fd) throws SocketException;

    SockAddr getPeerName(int fd) throws SocketException;

    void setSockBroadcast(int fd, int value) throws SocketException;

    void setSockReuseAddr(int fd, int value) throws SocketException;

    int getSockReuseAddr(int fd) throws SocketException;

    SockAddr[] getAddrInfo(String name, String service, int proto, int sotype, int hintsEnabled)
            throws SocketException;

    int getSockKeepAlive(int fd) throws SocketException;

    void setSockKeepAlive(int fd, int value) throws SocketException;

    int getSockRecvBufSize(int fd) throws SocketException;

    void setSockRecvBufSize(int fd, int size) throws SocketException;

    int getSockSendBufSize(int fd) throws SocketException;

    void setSockSendBufSize(int fd, int size) throws SocketException;

    int getSockLinger(int fd) throws SocketException;

    void setSockLinger(int fd, int value, int linger) throws SocketException;

    int getSockRecvTimeout(int fd) throws SocketException;

    void setSockRecvTimeout(int fd, int value) throws SocketException;

    int getSockTcpNoDelay(int fd) throws SocketException;

    void setSockTcpNoDelay(int fd, int value) throws SocketException;
}
