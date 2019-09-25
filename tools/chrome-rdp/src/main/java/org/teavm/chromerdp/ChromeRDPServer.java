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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class ChromeRDPServer {
    private int port = 2357;
    private ChromeRDPExchangeConsumer exchangeConsumer;
    private Server server;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ChromeRDPExchangeConsumer getExchangeConsumer() {
        return exchangeConsumer;
    }

    public void setExchangeConsumer(ChromeRDPExchangeConsumer exchangeConsumer) {
        this.exchangeConsumer = exchangeConsumer;
    }

    public void start() {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        try {
            ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
            wscontainer.addEndpoint(new RPDEndpointConfig());
            server.start();
            server.join();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private class RPDEndpointConfig implements ServerEndpointConfig {
        private Map<String, Object> userProperties = new HashMap<>();

        public RPDEndpointConfig() {
            userProperties.put("chrome.rdp", exchangeConsumer);
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
}
