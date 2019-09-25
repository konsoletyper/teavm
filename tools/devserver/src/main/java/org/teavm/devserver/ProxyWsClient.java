/*
 *  Copyright 2018 Alexey Andreev.
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
package org.teavm.devserver;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class ProxyWsClient {
    private Session session;
    private ProxyWsClient target;
    private boolean closed;
    private List<Consumer<ProxyWsClient>> pendingMessages = new ArrayList<>();

    public void setTarget(ProxyWsClient target) {
        if (this.target != null) {
            throw new IllegalStateException();
        }
        this.target = target;
        flush();
        target.flush();
    }

    @OnWebSocketConnect
    public void connect(Session session) {
        session.getPolicy().setMaxBinaryMessageSize(100_000_000);
        session.getPolicy().setMaxTextMessageSize(100_000_000);
        this.session = session;
        flush();
        if (target != null) {
            target.flush();
        }
    }

    @OnWebSocketClose
    public void close(int code, String reason) {
        closed = true;
        if (!target.closed) {
            target.closed = true;
            session.close(code, reason);
        }
    }

    @OnWebSocketMessage
    public void onMessage(byte[] buf, int offset, int length) {
        send(t -> t.session.getRemote().sendBytesByFuture(ByteBuffer.wrap(buf, offset, length)));
    }

    @OnWebSocketMessage
    public void onMessage(String text) {
        send(t -> t.session.getRemote().sendStringByFuture(text));
    }

    private void send(Consumer<ProxyWsClient> message) {
        if (target == null || target.session == null || !target.session.isOpen()) {
            if (pendingMessages != null) {
                pendingMessages.add(message);
            }
        } else {
            message.accept(target);
        }
    }

    private void flush() {
        if (pendingMessages == null || target == null || target.session == null || !target.session.isOpen()) {
            return;
        }
        for (Consumer<ProxyWsClient> message : pendingMessages) {
            message.accept(target);
        }
        pendingMessages = null;
    }
}
