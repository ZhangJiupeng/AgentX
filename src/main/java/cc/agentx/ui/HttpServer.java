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

package cc.agentx.ui;

import cc.agentx.Constants;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * <h2>A tiny local web server for agentx</h2>
 * <p>it supports GET and HEAD method only, HOWEVER, will
 * gradually improved.<br>
 * <b>Notice:</b> this project is not for a web server,
 * you can see that the page configuration is embedded into
 * <code>cc.agentx.http.Initializer</code>. <br>However,
 * <code>cc.agentx.http</code> is designed separately,
 * it can be extracted into a single project.
 * <br>
 * Thus, this web server project might be developed in the future,
 * hold on and keep attention :)
 */
public class HttpServer {
    private static final int READ_TIMEOUT = 30 * 1000;
    private static final int BUFFER_SIZE = 1024;
    private final PageHandler handler;
    private final ExecutorService executor;
    private final AtomicBoolean isRunning;
    private final SimpleDateFormat dateFormat;
    private final int port;
    private final boolean info;

    public HttpServer(final int port, final String baseDir) {
        this(port, baseDir, false);
    }

    public HttpServer(final int port, final String baseDir, boolean info) {
        this.handler = new PageHandler(baseDir);
        this.executor = Executors.newCachedThreadPool();
        this.isRunning = new AtomicBoolean(false);
        this.dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        this.port = port;
        this.info = info;
    }

    public static int getIdlePort() throws IOException {
        int idlePort = 0;
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(idlePort));
        idlePort = serverSocket.getLocalPort();
        serverSocket.close();
        return idlePort;
    }

    public static void main(final String[] args) throws Throwable {
        int port;
        String baseDir;
        if (args.length == 1 && args[0].contains("help")) {
            System.out.println(HttpServer.class.getName() + "<port> <webroot>");
            return;
        }
        if (args.length == 2) {
            port = Integer.parseInt(args[0]);
            baseDir = args[1];
        } else {
            port = getIdlePort();
            baseDir = System.getProperty("user.dir").replaceAll("\\\\", "/") + "/www/";
        }
        new HttpServer(port, baseDir).start();
    }

    public int start() {
        isRunning.set(true);
        executor.submit(new ServiceListener(port));
        return port;
    }

    public void stop() {
        isRunning.set(false);
        shutdown(executor);
    }

    boolean shutdown(final ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(60, TimeUnit.SECONDS))
                    return false;
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (info)
            System.out.println(Constants.WEB_SERVER_NAME + " stopped.");
        return true;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    static class Closer {
        static void close(final Closeable c) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }

        static void close(final Socket c) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }

        static void close(final ServerSocket c) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    class ServiceListener implements Runnable {
        final int port;

        ServiceListener(final int port) {
            this.port = port;
        }

        @Override
        public void run() {
            ServerSocket server = null;
            try {
                server = new ServerSocket();
                server.setSoTimeout(1000);
                server.setReuseAddress(true);
                server.bind(new InetSocketAddress(port));
                if (info)
                    System.out.println(Constants.WEB_SERVER_NAME + " started"
                            + ((port == 80) ? "." : " at localhost:" + port + "."));
                while (isRunning.get()) {
                    Socket client = null;
                    try {
                        client = server.accept();
                        client.setSoTimeout(READ_TIMEOUT);
                        executor.submit(new ServiceProvider(client));
                    } catch (SocketTimeoutException ignored) {
                    } catch (Exception e) {
                        Closer.close(client);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
            } finally {
                Closer.close(server);
            }
        }
    }

    class ServiceProvider implements Runnable {
        private static final String HTTP_VER = "HTTP/1.0";
        private static final String CACHE_CONTROL = "Cache-Control: private, max-age=0";
        private static final String CONNECTION_CLOSE = "Connection: close";
        private static final String SERVER_NAME = "Server: " + Constants.WEB_SERVER_NAME + " " + Constants.APP_VERSION;
        private static final String CRLF = "\r\n";

        final Socket client;

        ServiceProvider(final Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            BufferedReader in = null;
            PrintStream out = null;
            try {
                in = new BufferedReader(new InputStreamReader(client.getInputStream()), BUFFER_SIZE);
                out = new PrintStream(new BufferedOutputStream(client.getOutputStream(), BUFFER_SIZE));
                // Read Head (GET / HTTP/1.0)
                final String header = in.readLine();
                final String[] headerTokens = header.split(" ");
                final String method = headerTokens[0];
                final String uri = URLDecoder.decode(headerTokens[1], "ISO-8859-1");
                final String version = headerTokens[2];
                // Read Headers
                // noinspection StatementWithEmptyBody
                while (!in.readLine().isEmpty()) {
                }
                if (!"HTTP/1.0".equals(version) && !"HTTP/1.1".equals(version)) {
                    throw HttpError.HTTP_400;
                } else if (!"GET".equals(method) && !"HEAD".equals(method)) {
                    throw HttpError.HTTP_405;
                } else {
                    Map<String, Object> result;
                    try {
                        String pureUri = uri;
                        String paramStr = uri.substring(uri.indexOf('?') + 1);
                        Map<String, String> parameters = null;
                        if (!paramStr.equals(pureUri)) {
                            parameters = new HashMap<>();
                            pureUri = uri.substring(0, uri.indexOf('?'));
                            if (paramStr.contains("&")) {
                                for (String group : paramStr.split("&")) {
                                    parameters.put(group.split("=")[0], group.split("=")[1]);
                                }
                            } else if (paramStr.contains("=")) {
                                parameters.put(paramStr.split("=")[0], paramStr.split("=")[1]);
                            }
                        }
                        if (parameters == null || paramStr.equals("")) {
                            result = handler.fetch(pureUri);
                        } else {
                            result = handler.fetch(pureUri, parameters);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw HttpError.HTTP_400;
                    }

                    // result parse
                    try {
                        switch (result.get("type").toString()) {
                            case "file":
                                InputStream inputStream = (InputStream) result.get("inputStream");
                                long length = Long.parseLong(result.get("length").toString());
                                Date lastModified = (Date) result.get("lastModified");
                                String mimeType = result.get("mimeType") == null ? null : result.get("mimeType").toString();
                                sendResponse(inputStream, out, length, lastModified, mimeType, !"HEAD".equals(method));
                                inputStream.close();
                                break;
                            case "redirect":
                                Object redirectUrl = result.get("url");
                                if (redirectUrl == null) {
                                    throw HttpError.HTTP_404;
                                }
                                sendRedirect(out, redirectUrl.toString());
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw HttpError.HTTP_500;
                    }
                }
            } catch (HttpError e) {
                sendError(out, e, e.getHttpText());
            } catch (SocketTimeoutException e) {
                sendError(out, HttpError.HTTP_408, e.getMessage());
            } catch (IOException e) {
                sendError(out, HttpError.HTTP_500, e.getMessage());
            } finally {
                Closer.close(in);
                Closer.close(out);
                Closer.close(client);
            }
        }

        synchronized String getHttpDate(final Date date) {
            return dateFormat.format(date);
        }

        void sendRedirect(final PrintStream out, String url) {
            out.append(HTTP_VER).append(" 302 Found").append(CRLF);
            out.append("Location: ").append(url);
            out.flush();
        }

        void sendResponse(final InputStream is, final PrintStream out, long length, final Date lastModified,
                          final String mimeType, final boolean body) throws IOException {
            out.append(HTTP_VER).append(" 200 OK").append(CRLF);
            out.append("Content-Length: ").append(String.valueOf(length)).append(CRLF);
            if (mimeType != null) {
                out.append("Content-Type: ").append(mimeType).append(CRLF);
            }
            out.append("Date: ").append(getHttpDate(new Date())).append(CRLF);
            if (lastModified != null) {
                out.append("Last-Modified: ").append(getHttpDate(lastModified)).append(CRLF);
            }

            // For Cross Domain!
            out.append("Access-Control-Allow-Origin: *").append(CRLF);

            out.append(CACHE_CONTROL).append(CRLF);
            out.append(CONNECTION_CLOSE).append(CRLF);
            out.append(SERVER_NAME).append(CRLF);
            out.append(CRLF);

            if (body) {
                final byte[] buf = new byte[BUFFER_SIZE];
                int len;
                while ((len = is.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
            }
            out.flush();
        }

        void sendError(final PrintStream out, final HttpError e, final String bodyText) {
            sendError(out, e.getHttpCode(), e.getHttpText(), bodyText);
        }

        void sendError(final PrintStream out, final int code, final String reason, final String bodyText) {
            String bodyPage = HttpError.wrapInErrorPage(bodyText);
            out.append(HTTP_VER).append(' ').append(String.valueOf(code)).append(' ').append(reason).append(CRLF);
            out.append("Content-Length: ").append(String.valueOf(bodyPage.length())).append(CRLF);
            out.append("Content-Type: text/html; charset=ISO-8859-1").append(CRLF);
            out.append(CACHE_CONTROL).append(CRLF);
            out.append(CONNECTION_CLOSE).append(CRLF);
            out.append(SERVER_NAME).append(CRLF);
            out.append(CRLF);
            out.append(bodyPage);
            out.flush();
        }
    }
}