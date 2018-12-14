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

import java.util.ArrayList;
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
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.teavm.tooling.TeaVMToolLog;

public class DevServer {
    private String mainClass;
    private String[] classPath;
    private String pathToFile = "/";
    private String fileName = "classes.js";
    private List<String> sourcePath = new ArrayList<>();
    private boolean indicator;
    private boolean reloadedAutomatically;
    private TeaVMToolLog log;
    private CodeServlet servlet;
    private List<DevServerListener> listeners = new ArrayList<>();

    private Server server;
    private int port = 9090;

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setClassPath(String[] classPath) {
        this.classPath = classPath;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPathToFile(String pathToFile) {
        if (!pathToFile.startsWith("/")) {
            pathToFile = "/" + pathToFile;
        }
        if (!pathToFile.endsWith("/")) {
            pathToFile += "/";
        }
        this.pathToFile = pathToFile;
    }

    public void setLog(TeaVMToolLog log) {
        this.log = log;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setIndicator(boolean indicator) {
        this.indicator = indicator;
    }

    public void setReloadedAutomatically(boolean reloadedAutomatically) {
        this.reloadedAutomatically = reloadedAutomatically;
    }

    public List<String> getSourcePath() {
        return sourcePath;
    }

    public void invalidateCache() {
        servlet.invalidateCache();
    }

    public void buildProject() {
        servlet.buildProject();
    }

    public void cancelBuild() {
        servlet.cancelBuild();
    }

    public void addListener(DevServerListener listener) {
        listeners.add(listener);
    }

    public void start() {
        server = new Server();
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        servlet = new CodeServlet(mainClass, classPath);
        servlet.setFileName(fileName);
        servlet.setPathToFile(pathToFile);
        servlet.setLog(log);
        servlet.getSourcePath().addAll(sourcePath);
        servlet.setIndicator(indicator);
        servlet.setAutomaticallyReloaded(reloadedAutomatically);
        servlet.setPort(port);
        for (DevServerListener listener : listeners) {
            servlet.addListener(listener);
        }
        context.addServlet(new ServletHolder(servlet), "/*");

        try {
            ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(context);
            wscontainer.addEndpoint(new DevServerEndpointConfig(servlet));
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
        server = null;
        servlet = null;
    }

    private class DevServerEndpointConfig implements ServerEndpointConfig {
        private Map<String, Object> userProperties = new HashMap<>();

        public DevServerEndpointConfig(CodeServlet servlet) {
            userProperties.put("teavm.servlet", servlet);
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
            return CodeWsEndpoint.class;
        }

        @Override
        public List<Extension> getExtensions() {
            return Collections.emptyList();
        }

        @Override
        public String getPath() {
            return pathToFile + fileName + ".ws";
        }

        @Override
        public List<String> getSubprotocols() {
            return Collections.emptyList();
        }
    }
}
