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

import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

@WebSocket
public class CodeWsEndpoint {
    private Map<Session, ProgressHandlerImpl> progressHandlerMap = new HashMap<>();
    private CodeServlet servlet;

    public CodeWsEndpoint(CodeServlet servlet) {
        this.servlet = servlet;
    }

    @OnWebSocketConnect
    public void open(Session session) {
        ProgressHandlerImpl progressHandler = new ProgressHandlerImpl(session);
        progressHandlerMap.put(session, progressHandler);
        servlet.addProgressHandler(progressHandler);
    }

    @OnWebSocketClose
    public void close(Session session, int code, String reason) {
        ProgressHandlerImpl handler = progressHandlerMap.remove(session);
        servlet.removeProgressHandler(handler);
    }

    static class ProgressHandlerImpl implements ProgressHandler {
        Session session;

        ProgressHandlerImpl(Session session) {
            this.session = session;
        }

        @Override
        public void progress(double value) {
            session.getRemote().sendStringByFuture("{ \"command\": \"compiling\", \"progress\": " + value + " }");
        }

        @Override
        public void complete(boolean success) {
            session.getRemote().sendStringByFuture("{ \"command\": \"complete\", \"success\": " + success + " }");
        }
    }
}
