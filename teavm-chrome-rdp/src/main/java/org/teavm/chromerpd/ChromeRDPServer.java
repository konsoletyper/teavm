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
package org.teavm.chromerpd;

import java.util.*;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.teavm.debugging.JavaScriptDebugger;

/**
 *
 * @author Alexey Andreev
 */
public class ChromeRDPServer {
    private int port = 2357;
    private Appendable output = System.err;
    private ChromeRDPDebugger debugger = new ChromeRDPDebugger();

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Appendable getOutput() {
        return output;
    }

    public void setOutput(Appendable output) {
        this.output = output;
    }

    public void start() {
        final Server server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);

        try {
            wscontainer.addEndpoint(new RPDEndpointConfig());
            server.start();
            server.dump(output);
            server.join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class RPDEndpointConfig implements ServerEndpointConfig {
        private Map<String, Object> userProperties = new HashMap<>();

        public RPDEndpointConfig() {
            userProperties.put("chrome.rdp", debugger);
        }

        @Override
        public List<Class<? extends Decoder>> getDecoders() {
            return Collections.emptyList();
        }

        @Override
        public List<Class<? extends Encoder>> getEncoders() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, Object> getUserProperties() {
            return userProperties;
        }

        @Override
        public Configurator getConfigurator() {
            return null;
        }

        @Override
        public Class<?> getEndpointClass() {
            return ChromeRDPDebuggerEndpoint.class;
        }

        @Override
        public List<Extension> getExtensions() {
            return Collections.emptyList();
        }

        @Override
        public String getPath() {
            return "/";
        }

        @Override
        public List<String> getSubprotocols() {
            return Collections.emptyList();
        }
    }

    public JavaScriptDebugger getDebugger() {
        return debugger;
    }
}
