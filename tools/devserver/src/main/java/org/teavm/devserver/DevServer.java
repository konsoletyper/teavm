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
import java.util.List;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.teavm.tooling.TeaVMToolLog;

public class DevServer {
    private String mainClass;
    private String[] classPath;
    private String pathToFile = "/";
    private String fileName = "classes.js";
    private List<String> sourcePath = new ArrayList<>();
    private boolean indicator;
    private boolean deobfuscateStack;
    private boolean reloadedAutomatically;
    private TeaVMToolLog log;
    private CodeServlet servlet;
    private List<DevServerListener> listeners = new ArrayList<>();

    private Server server;
    private int port = 9090;
    private int debugPort;
    private String proxyUrl;
    private String proxyPath = "/";

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public void setClassPath(String[] classPath) {
        this.classPath = classPath;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setDebugPort(int debugPort) {
        this.debugPort = debugPort;
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

    public void setDeobfuscateStack(boolean deobfuscateStack) {
        this.deobfuscateStack = deobfuscateStack;
    }

    public void setReloadedAutomatically(boolean reloadedAutomatically) {
        this.reloadedAutomatically = reloadedAutomatically;
    }

    public void setProxyUrl(String proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public void setProxyPath(String proxyPath) {
        this.proxyPath = proxyPath;
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
        servlet.setDeobfuscateStack(deobfuscateStack);
        servlet.setAutomaticallyReloaded(reloadedAutomatically);
        servlet.setPort(port);
        servlet.setDebugPort(debugPort);
        servlet.setProxyUrl(proxyUrl);
        servlet.setProxyPath(proxyPath);
        for (DevServerListener listener : listeners) {
            servlet.addListener(listener);
        }
        ServletHolder servletHolder = new ServletHolder(servlet);
        servletHolder.setAsyncSupported(true);
        context.addServlet(servletHolder, "/*");

        try {
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
}
