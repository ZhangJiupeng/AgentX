/*
 * Copyright 2017 ZhangJiupeng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cc.agentx.client;

import cc.agentx.Constants;
import cc.agentx.client.net.nio.XClient;
import cc.agentx.ui.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void init() {
        try {
            Configuration.init();
        } catch (Exception e) {
            log.error("\tInitialization failed ({})", e.getMessage());
            System.exit(-1);
        }
    }

    public static HttpServer httpServer;
    public static XClient xClient;

    public static void startHttpServer() {
        int port;
        log.info("\tGetting idle port...");
        try {
            port = HttpServer.getIdlePort();
            httpServer = new HttpServer(port, System.getProperty("user.dir").concat("/web"));
            log.info("\tFound idle port {}", port);
        } catch (IOException e) {
            log.error("\tGet idle port failed ({})", e.getMessage());
            return;
        }
        httpServer.start();
        log.info("\tStartup {} at localhost:{}...", Constants.WEB_SERVER_NAME, port);
        Configuration.INSTANCE.setConsole("console.agentx.cc", port);
    }

    public static void closeHttpServer() {
        if (httpServer != null && httpServer.isRunning()) {
            httpServer.stop();
        }
    }

    public static void popConsolePage() {
        if (httpServer != null && httpServer.isRunning()) {
            try {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                    desktop.browse(java.net.URI.create("http://" + Configuration.INSTANCE.getConsoleDomain()));
                } else {
                    log.warn("\tPop console page failed (not support)");
                }
            } catch (Exception e) {
                log.warn("\tPop console page failed ({})", e.getMessage());
            }
        } else {
            log.warn("\t{} has closed", Constants.WEB_SERVER_NAME);
        }
    }

    public static void start() {
        try {
            Main.init();
            Main.startHttpServer();
            Main.popConsolePage();
            xClient = XClient.getInstance();
            xClient.start();
        } finally {
            Main.closeHttpServer();
            log.info("\tBye!");
        }
    }

    public static void stop() {
        xClient.stop();
        Main.closeHttpServer();
        log.info("\tBye!");
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            Main.start();
        }
        switch (args[0]) {
            case "start":
                Main.start();
                break;
            case "stop":
                Main.stop();
                break;
            default:
                log.error("\tUnknown args0={}", args[0]);
        }
    }

}
