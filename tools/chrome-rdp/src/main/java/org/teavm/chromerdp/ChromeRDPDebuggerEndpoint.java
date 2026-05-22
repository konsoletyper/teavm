/*
 *  Copyright 2014 Alexey Andreev.
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
package org.teavm.chromerdp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

public class ChromeRDPDebuggerEndpoint implements Session.Listener.AutoDemanding, ChromeRDPExchange {
    public static final int MAX_MESSAGE_SIZE = 65534;
    private final ChromeRDPExchangeConsumer consumer;
    private Session session;
    private List<ChromeRDPExchangeListener> listeners = new ArrayList<>();
    private StringBuilder messageBuffer = new StringBuilder();

    public ChromeRDPDebuggerEndpoint(ChromeRDPExchangeConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        this.session = session;
        if (consumer != null) {
            consumer.setExchange(this);
        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        if (consumer != null) {
            consumer.setExchange(null);
        }
        callback.succeed();
    }

    @Override
    public void disconnect() {
        session.close();
    }

    @Override
    public void onWebSocketText(String message) {
        char ctl = message.charAt(0);
        messageBuffer.append(message.substring(1));
        if (ctl == '.') {
            message = messageBuffer.toString();
            for (ChromeRDPExchangeListener listener : listeners) {
                try {
                    listener.received(message);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            messageBuffer = new StringBuilder();
        }
    }

    @Override
    public void send(String message) {
        int index = 0;
        while (message.length() - index > MAX_MESSAGE_SIZE) {
            int next = index + MAX_MESSAGE_SIZE;
            session.sendText("," + message.substring(index, next), Callback.NOOP);
            index = next;
        }
        session.sendText("." + message.substring(index), Callback.NOOP);
    }

    @Override
    public void addListener(ChromeRDPExchangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(ChromeRDPExchangeListener listener) {
        listeners.remove(listener);
    }
}
