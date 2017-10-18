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

package cc.agentx.server;

import cc.agentx.protocol.request.XRequestResolver;
import cc.agentx.protocol.request.XRequestResolverFactory;
import cc.agentx.server.cache.DnsCache;
import cc.agentx.util.tunnel.SocketTunnel;
import cc.agentx.wrapper.Wrapper;
import cc.agentx.wrapper.WrapperFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executors;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class Configuration {
    private static final Logger log = LoggerFactory.getLogger(Configuration.class);
    private static final String BASE_PATH = System.getProperty("user.dir").replaceAll("\\\\", "/");
    private static final String[] CONFIG_FILE_PATH = {
            "/conf/server.json",
            "/server.json",
            "/conf/agentx.json",
            "/agentx.json",
            "/conf/config.json",
            "/config.json"
    };

    public static Configuration INSTANCE;
    public static GlobalTrafficShapingHandler TRAFFIC_HANDLER;

    @Expose
    private String host = "0.0.0.0";
    @Expose
    private int port = 9999;
    @Expose
    private int[] relayPort = {};
    @Expose
    private String protocol = "shadowsocks";
    @Expose
    private String encryption = "aes-256-cfb";
    @Expose
    private String password = "my_password";
    @Expose
    private String[] process = {"encrypt"};
    @Expose
    private int dnsCacheCapacity = 1000;
    @Expose
    private int writeLimit = 0;
    @Expose
    private int readLimit = 0;

    private SocketTunnel[] relays;

    private Configuration() {
    }

    @Override
    public String toString() {
        return "{\n" +
                "  host: \"" + host + "\",\n" +
                "  port: " + port + ",\n" +
                "  relayPort: " + Arrays.toString(relayPort) + ",\n" +
                "  protocol: \"" + protocol + "\",\n" +
                "  encryption: \"" + encryption + "\",\n" +
                "  password: \"" + password + "\",\n" +
                "  process: " + Arrays.toString(process) + ",\n" +
                "  dnsCacheCapacity: " + dnsCacheCapacity + ",\n" +
                "  writeLimit: " + writeLimit + ",\n" +
                "  readLimit: " + readLimit + "\n" +
                "}";
    }

    @SuppressWarnings("Duplicates")
    private static void load() throws Exception {
        String json = "";

        InputStream inputStream = null;
        for (String path : CONFIG_FILE_PATH) {
            inputStream = Configuration.class.getResourceAsStream(path);
            if (inputStream != null) {
                log.info("\tFound resource [{}]", path);
                break;
            }
            log.debug("\tCould NOT find resource [{}]", path);
        }

        for (String path : CONFIG_FILE_PATH) {
            File file = new File(BASE_PATH, path);
            if (file.exists()) {
                inputStream = new FileInputStream(file);
                log.info("\tFound resource [{}]", file.getAbsolutePath());
                break;
            }
            log.debug("\tCould NOT find resource [{}]", file.getAbsolutePath());
        }

        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().setPrettyPrinting().create();

        if (inputStream == null) {
            log.warn("\tCould NOT find resource [{}]", "config.json");
            File configFile = new File(BASE_PATH, "config.json");
            if (configFile.createNewFile()) {
                log.warn("\tCreate default [config.json] at [{}]", configFile.getPath());
                BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));
                writer.write(json = gson.toJson(new Configuration()));
                writer.close();
                log.warn("\tPlease reboot this program after configuration.");
                System.exit(0);
            } else {
                throw new RuntimeException("file not found (" + configFile.getPath() + ")");
            }
        } else {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            while (reader.ready()) {
                json += reader.readLine().concat("\n");
            }
            reader.close();
        }

        Configuration.INSTANCE = gson.fromJson(json, Configuration.class);
        log.debug(INSTANCE.toString());
    }

    private static void check() throws Exception {
        if (!XRequestResolverFactory.exists(INSTANCE.protocol)) {
            throw new Exception("unknown protocol \"" + INSTANCE.protocol + "\"");
        }
        for (String processFunction : INSTANCE.process) {
            if (!WrapperFactory.exists(INSTANCE, processFunction)) {
                throw new Exception("unknown encryption \"" + INSTANCE.encryption + "\"" +
                        " or process function \"" + processFunction + "\"");
            }
        }
    }

    public static void startupRelays() {
        if (INSTANCE.relays == null) {
            int[] ports = INSTANCE.relayPort;
            INSTANCE.relays = new SocketTunnel[ports.length];
            for (int i = 0; i < ports.length; i++) {
                INSTANCE.relays[i] = new SocketTunnel(new InetSocketAddress(ports[i]), new InetSocketAddress(INSTANCE.port));
            }
        }
        for (SocketTunnel tunnel : INSTANCE.relays) {
            try {
                tunnel.startup();
                log.info("\tRelay: [{}] -> [{}] started", tunnel.getSrcAddr(), tunnel.getDstAddr());
            } catch (IOException e) {
                log.error("\tRelay: [{}] -> [{}] start failed ({})", tunnel.getSrcAddr(), tunnel.getDstAddr(), e.getMessage());
                log.warn("\tRolling back...");
                shutdownRelays();
                throw new RuntimeException("relays startup aborted");
            }
        }
    }

    public static void shutdownRelays() {
        if (INSTANCE != null && INSTANCE.relays != null) {
            for (SocketTunnel t : INSTANCE.relays) {
                try {
                    t.shutdown();
                    log.info("\tRelay: [{}] -> [{}] stopped", t.getSrcAddr(), t.getDstAddr());
                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void init() throws Exception {
        if (INSTANCE != null) {
            return;
        }
        log.info("\tLoading configuration file...");
        load();
        log.info("\tChecking configuration items...");
        check();
        if (INSTANCE.relayPort.length > 0) {
            log.info("\tStarting Relays...");
            startupRelays();
        }
        log.info("\tInitializing dns cache...");
        DnsCache.init(INSTANCE.dnsCacheCapacity);
        log.info("\tInitializing global network traffic handler...");
        TRAFFIC_HANDLER = new GlobalTrafficShapingHandler(Executors.newScheduledThreadPool(1), 1000);
        TRAFFIC_HANDLER.setWriteLimit(INSTANCE.writeLimit);
        TRAFFIC_HANDLER.setReadLimit(INSTANCE.readLimit);
        log.info("\tEnd of configuration");
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getEncryption() {
        return encryption;
    }

    public String getPassword() {
        return password;
    }

    public int getDnsCacheCapacity() {
        return dnsCacheCapacity;
    }

    public int getWriteLimit() {
        return writeLimit;
    }

    public int getReadLimit() {
        return readLimit;
    }

    public Wrapper getWrapper() {
        Wrapper[] wrappers = new Wrapper[process.length];
        for (int i = 0; i < process.length; i++) {
            try {
                wrappers[i] = WrapperFactory.getInstance(this, process[i]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return WrapperFactory.getInstance(wrappers);
    }

    public XRequestResolver getXRequestResolver() {
        try {
            return XRequestResolverFactory.getInstance(protocol);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
