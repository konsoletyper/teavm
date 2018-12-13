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

import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/")
public class CodeWsEndpoint {
    private Session session;
    private CodeServlet servlet;

    @OnOpen
    public void open(Session session) {
        this.session = session;
        servlet = (CodeServlet) session.getUserProperties().get("teavm.servlet");
        if (servlet != null) {
            servlet.addWsEndpoint(this);
        }
    }

    @OnClose
    public void close() {
        if (servlet != null) {
            servlet.removeWsEndpoint(this);
        }
        servlet = null;
        session = null;
    }

    public void progress(double value) {
        if (session != null) {
            session.getAsyncRemote().sendText("{ \"command\": \"compiling\", \"progress\": " + value + " }");
        }
    }

    public void complete(boolean success) {
        if (session != null) {
            session.getAsyncRemote().sendText("{ \"command\": \"complete\", \"success\": " + success + " }");
        }
    }
}
