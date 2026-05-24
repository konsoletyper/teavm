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
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

public class ProxyWsClient implements Session.Listener.AutoDemanding {
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

    @Override
    public void onWebSocketOpen(Session session) {
        this.session = session;
        flush();
        if (target != null) {
            target.flush();
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        closed = true;
        if (!target.closed) {
            target.closed = true;
            target.session.close(statusCode, reason, Callback.NOOP);
        }
        callback.succeed();
    }

    @Override
    public void onWebSocketBinary(ByteBuffer payload, Callback callback) {
        byte[] data = new byte[payload.remaining()];
        payload.get(data);
        ByteBuffer copy = ByteBuffer.wrap(data);
        send(t -> t.session.sendBinary(copy, Callback.NOOP));
        callback.succeed();
    }

    @Override
    public void onWebSocketText(String text) {
        send(t -> t.session.sendText(text, Callback.NOOP));
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
