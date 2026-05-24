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

import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Session;

public class CodeWsEndpoint implements Session.Listener.AutoDemanding {
    private ProgressHandlerImpl progressHandler;
    private CodeServlet servlet;

    public CodeWsEndpoint(CodeServlet servlet) {
        this.servlet = servlet;
    }

    @Override
    public void onWebSocketOpen(Session session) {
        progressHandler = new ProgressHandlerImpl(session);
        servlet.addProgressHandler(progressHandler);
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason, Callback callback) {
        servlet.removeProgressHandler(progressHandler);
        callback.succeed();
    }

    static class ProgressHandlerImpl implements ProgressHandler {
        Session session;

        ProgressHandlerImpl(Session session) {
            this.session = session;
        }

        @Override
        public void progress(double value) {
            session.sendText("{ \"command\": \"compiling\", \"progress\": " + value + " }", Callback.NOOP);
        }

        @Override
        public void complete(boolean success) {
            session.sendText("{ \"command\": \"complete\", \"success\": " + success + " }", Callback.NOOP);
        }
    }
}
