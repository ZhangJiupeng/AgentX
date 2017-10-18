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

import cc.agentx.protocol.request.XRequestResolver;
import cc.agentx.protocol.request.XRequestResolverFactory;
import cc.agentx.util.KeyHelper;
import cc.agentx.wrapper.Wrapper;
import cc.agentx.wrapper.WrapperFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;

public class Configuration {
    private static final Logger log = LoggerFactory.getLogger(Configuration.class);
    private static final String BASE_PATH = System.getProperty("user.dir").replaceAll("\\\\", "/");
    private static final String[] CONFIG_FILE_PATH = {
            "/conf/client.json",
            "/client.json",
            "/conf/agentx.json",
            "/agentx.json",
            "/conf/config.json",
            "/config.json"
    };
    public static Configuration INSTANCE;

    @Expose
    private String localHost = "0.0.0.0";
    @Expose
    private int localPort = 1080;
    @Expose
    private String mode = "agentx";
    @Expose
    private String serverHost = "0.0.0.0";
    @Expose
    private int[] serverPort = {9999};
    @Expose
    private String protocol = "shadowsocks";
    @Expose
    private String encryption = "aes-256-cfb";
    @Expose
    private String password = "my_password";
    @Expose
    private String[] process = {"encrypt"};

    private String consoleDomain;

    private int consolePort;

    private Configuration() {
    }

    @Override
    public String toString() {
        return "{\n" +
                "  localHost: \"" + localHost + "\",\n" +
                "  localPort: " + localPort + ",\n" +
                "  mode: \"" + mode + "\",\n" +
                "  serverHost: \"" + serverHost + "\",\n" +
                "  serverPort: " + Arrays.toString(serverPort) + ",\n" +
                "  encryption: \"" + encryption + "\",\n" +
                "  password: \"" + password + "\",\n" +
                "  protocol: \"" + protocol + "\",\n" +
                "  process: " + Arrays.toString(process) + "\n" +
                '}';
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
        if (!INSTANCE.mode.equals("agentx") && !INSTANCE.mode.equals("socks5")) {
            throw new Exception("unknown mode \"" + INSTANCE.mode + "\"");
        }
        for (String processFunction : INSTANCE.process) {
            if (!WrapperFactory.exists(INSTANCE, processFunction)) {
                throw new Exception("unknown encryption \"" + INSTANCE.encryption + "\"" +
                        " or process function \"" + processFunction + "\"");
            }
        }
    }

    public static Configuration init() throws Exception {
        log.info("\tLoading configuration file...");
        load();
        log.info("\tChecking configuration items...");
        check();
        log.info("\tEnd of configuration");
        return INSTANCE;
    }

    public void setConsole(String consoleHost, int consolePort) {
        this.consoleDomain = consoleHost;
        this.consolePort = consolePort;
    }

    public int getServerPort() {
        return serverPort[KeyHelper.generateRandomInteger(0, serverPort.length)];
    }

    public String getLocalHost() {
        return localHost;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getServerHost() {
        return serverHost;
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

    public String getMode() {
        return mode;
    }

    public int getConsolePort() {
        return consolePort;
    }

    public String getConsoleDomain() {
        return consoleDomain;
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
