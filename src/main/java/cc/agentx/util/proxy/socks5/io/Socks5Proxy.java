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

package cc.agentx.util.proxy.socks5.io;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
 * a single class version of socks5 proxy
 * block-io with cached thread pool
 */
public class Socks5Proxy extends Thread {
    public static final int ATYP_IP_V4 = 0x1;
    public static final int ATYP_DOMAIN_NAME = 0x3;
    public static final int ATYP_IP_V6 = 0x4;
    public static final byte[] MSG_REJECT_SOCKS4 = {0, 0x5b};
    public static final byte[] MSG_ECHO = {0x5, 0};
    public static final byte[] MSG_VERIFY = {0x5, 0, 0, 0x1, 0, 0, 0, 0, 0, 0};
    private static final int SOCKS_VERSION_5 = 0x5;
    private static final int SOCKS_VERSION_4 = 0x4;
    public static boolean _DEBUG_MODE = false;
    private static Random random = new Random();
    private Class<? extends FilterInputStream> inputStreamClazz = BufferedInputStream.class;
    private Class<? extends FilterOutputStream> outputStreamClazz = BufferedOutputStream.class;
    private ExecutorService cachedThreadPool;
    private InetSocketAddress serverAddress;
    private ServerSocket serverSocket;
    private PrintStream out, err;

    public Socks5Proxy() {
        this(1080);
    }

    public Socks5Proxy(int port) {
        this(null, port);
    }

    public Socks5Proxy(String host) {
        this(host, 1080);
    }


    public Socks5Proxy(String host, int port) {
        if (_DEBUG_MODE)
            out = System.out;
        cachedThreadPool = Executors.newCachedThreadPool();
        serverAddress = host == null ? new InetSocketAddress(port)
                : new InetSocketAddress(host, port);
    }

    /**
     * Reserve for extensions
     *
     * @param inputStreamClazz  user-defined inputStream
     * @param outputStreamClazz user-defined outputStream
     */
    public Socks5Proxy(String host, int port,
                       Class<? extends FilterInputStream> inputStreamClazz,
                       Class<? extends FilterOutputStream> outputStreamClazz) {
        this(host, port);
        this.inputStreamClazz = inputStreamClazz;
        this.outputStreamClazz = outputStreamClazz;
    }

    public void setInputStreamClazz(Class<? extends FilterInputStream> inputStreamClazz) {
        this.inputStreamClazz = inputStreamClazz;
    }

    public void setOutputStreamClazz(Class<? extends FilterOutputStream> outputStreamClazz) {
        this.outputStreamClazz = outputStreamClazz;
    }

    public void run() {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(serverAddress, 50);
        } catch (IOException e) {
            warn("SOCKS5Proxy: " + e.getMessage());
        }

        if (serverSocket.isBound()) {
            log("SOCKS5Proxy: Waiting for connections on "
                    + serverSocket.getLocalSocketAddress() + "...");
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    socket.setTcpNoDelay(true); // avoid nagle algorithm
                    socket.setKeepAlive(true);
                    cachedThreadPool.execute(getSocks5Tunnel(socket));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            warn("SOCKS5Proxy: Socket bind failure.");
            Thread.currentThread().interrupt();
        }
    }

    private void log(String text) {
        if (out != null)
            out.println(text);
    }

    private void warn(String text) {
        if (err != null)
            err.println(text);
        else System.err.println(text);
    }

    public Socks5Proxy setOut(PrintStream out) {
        this.out = out;
        return this;
    }

    public Socks5Proxy setErr(PrintStream err) {
        this.err = err;
        return this;
    }

    public Socks5Tunnel getSocks5Tunnel(Socket socket) {
        return new Socks5Tunnel(socket);
    }

    private int getRandomIdentifier(int length) {
        int base = (int) Math.pow(10, length - 1);
        return random.nextInt(base * 9) + base;
    }

    class Socks5Tunnel implements Runnable {
        // adaption parameters - for optimize, read the code before changing them
        private static final int LOCAL_TIME_OUT_MILLIS = 0;
        private static final int REMOTE_TIME_OUT_MILLIS = 30 * 1000;
        private static final int HANDSHAKE_BUF_SIZE = 64;
        private static final int UPLOAD_BUF_SIZE = 256 * 1024;    // 256kb
        private static final int DOWNLOAD_BUF_SIZE = 1024 * 1024; // 1mb
        private int registerNumber;
        private long pollingDelayMillis;
        private InetSocketAddress registerAddress;
        private Socket localSocket;
        private FilterInputStream inputStream;
        private FilterOutputStream outputStream;

        public Socks5Tunnel(Socket socket) {
            this.registerNumber = getRandomIdentifier(5);
            this.localSocket = socket;
        }

        @Override
        public void run() {
            // local handshake
            log(registerNumber + "\tClient -> Proxy           \tFrom " + localSocket.getRemoteSocketAddress().toString().substring(1));
            try {
                handshake(localSocket,
                        this.inputStream = inputStreamClazz.getConstructor(InputStream.class).newInstance(localSocket.getInputStream()),
                        this.outputStream = outputStreamClazz.getConstructor(OutputStream.class).newInstance(localSocket.getOutputStream())
                );
            } catch (Exception e) {
                warn(registerNumber + "\tBad Handshake! (" + e.getMessage() + ")");
                try {
                    localSocket.close();
                    log(registerNumber + "\tClient <- Proxy           \tDisconnect");
                } catch (IOException ignored) {
                }
                return;
            }

            // connect to remote
            Socket remoteSocket = new Socket();
            try {
                log(registerNumber + "\t          Proxy -> Target \tPing");
                long datum = System.currentTimeMillis();
                remoteSocket.connect(registerAddress, REMOTE_TIME_OUT_MILLIS);
                remoteSocket.setKeepAlive(localSocket.getKeepAlive());
                long shift = System.currentTimeMillis() - datum;
                pollingDelayMillis = Math.max(50, shift); // slow down the polling frequency to save CPU resources
                log(registerNumber + "\t          Proxy <- Target \tPong " + ((pollingDelayMillis <= 50) ? "" : "~" + pollingDelayMillis + "ms"));
                pollingDelayMillis = Math.max(pollingDelayMillis, 600); // avoid slow transmission (50 <= delay <= 600)
            } catch (Exception e) {
                warn(registerNumber + "\tBad Connection! (" + e.getMessage() + ")");
                try {
                    remoteSocket.close();
                    localSocket.close();
                    log(registerNumber + "\tClient <- Proxy           \tDisconnect");
                } catch (IOException ignored) {
                }
                return;
            }

            // transmit data via tunnel
            cachedThreadPool.execute(getDownloaderInstance(localSocket, outputStream, remoteSocket, pollingDelayMillis));
            cachedThreadPool.execute(getUploaderInstance(localSocket, inputStream, remoteSocket, pollingDelayMillis));
        }

        private void handshake(Socket localSocket, InputStream localIn, OutputStream localOut) throws Exception {
            byte[] buffer = new byte[HANDSHAKE_BUF_SIZE];

            // get rid of zombie connections from local endpoint
            localSocket.setSoTimeout(LOCAL_TIME_OUT_MILLIS);

            // recognize protocol
            int length = localIn.read(buffer);
            if (length <= 0) {
                throw new IOException("connection refused");
            }
            if (buffer[0] != SOCKS_VERSION_5) {
                if (_DEBUG_MODE) {
                    String hexString = "";
                    for (byte b : buffer) {
                        hexString += String.format("%02X", b);
                    }
                    warn(registerNumber + "\t<Unrecognized Data>   HEX\t" + hexString);
                    if (Character.isLetterOrDigit(buffer[0])) {
                        warn(registerNumber + "\t                      TXT\t" + new String(buffer));
                    }
                }
                if (buffer[0] == SOCKS_VERSION_4) {
                    localOut.write(Socks5Proxy.MSG_REJECT_SOCKS4);
                    localOut.flush();
                }
                throw new IOException(String.format("protocol version not supported: 0x%2X", buffer[0] & 0xFF));
            }

            // send echo - no authentication support because its rarely been used or supported
            localOut.write(Socks5Proxy.MSG_ECHO);
            localOut.flush();

            // parse dst address
            length = localIn.read(buffer);
            if (length <= 0) {
                throw new IOException("connection refused");
            }
            switch (buffer[3]) {
                case ATYP_IP_V4:
                    signByIPv4(buffer);
                    break;
                case ATYP_DOMAIN_NAME:
                    signByDomain(buffer, length);
                    break;
                case ATYP_IP_V6:
                    signByIPv6(buffer);
                default:
                    throw new RuntimeException("unknown ATYP field (" + (buffer[3] & 0xFF) + ")");
            }

            // echo validity
            localOut.write(Socks5Proxy.MSG_VERIFY);
            localOut.flush();
        }

        private void signByIPv4(byte[] cReq) throws Exception {
            String ip = "" + (cReq[4] & 0xFF) + '.' + (cReq[5] & 0xFF)
                    + '.' + (cReq[6] & 0xFF) + '.' + (cReq[7] & 0xFF);
            int port = ((cReq[8] & 0xFF) << 8) + (cReq[9] & 0xFF);
            log(registerNumber + "\tClient -> Proxy           \tTarget " + ip + ":" + port);
            registerAddress = new InetSocketAddress(ip, port);
        }

        private void signByDomain(byte[] cReq, int length) throws Exception {
            String domain = new String(cReq, 5, length - 7);
            int port = ((cReq[length - 2] & 0xFF) << 8) + (cReq[length - 1] & 0xFF);
            log(registerNumber + "\tClient -> Proxy           \tTarget " + domain + ":" + port);
            registerAddress = new InetSocketAddress(domain, port);
        }

        private void signByIPv6(byte[] cReq) throws Exception {
            String ip = String.format(
                    "%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x", cReq[4] & 0xFF
                    , cReq[5] & 0xFF, cReq[6] & 0xFF, cReq[7] & 0xFF, cReq[8] & 0xFF, cReq[9] & 0xFF
                    , cReq[10] & 0xFF, cReq[11] & 0xFF, cReq[12] & 0xFF, cReq[13] & 0xFF, cReq[14] & 0xFF
                    , cReq[15] & 0xFF, cReq[16] & 0xFF, cReq[17] & 0xFF, cReq[18] & 0xFF, cReq[19] & 0xFF
            );
            int port = ((cReq[20] & 0xFF) << 8) + (cReq[21] & 0xFF);
            log(registerNumber + "\tClient -> Proxy           \tTarget " + ip + ":" + port);
            registerAddress = new InetSocketAddress(ip, port);
        }

        private Runnable getDownloaderInstance(Socket local, FilterOutputStream localOut, Socket remote, long delay) {
            return () -> {
                try {
                    BufferedInputStream remoteInput = new BufferedInputStream(remote.getInputStream());
                    byte[] remoteBuffer = new byte[DOWNLOAD_BUF_SIZE];
                    int length;
                    while (!remote.isClosed()) {
                        length = remoteInput.read(remoteBuffer);
                        if (length > 0) {
                            localOut.write(remoteBuffer, 0, length);
                            localOut.flush();
                            log(registerNumber + "\tClient <========== Target \tGet [" + length + " bytes]");
                            sleep(delay);
                        } else if (length == -1) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    try {
                        remote.close();
                        local.close();
                    } catch (IOException e0) {
                        e0.printStackTrace();
                    }
                } finally {
                    try {
                        local.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // log(registerNumber + "\tClient <- Proxy           \tDisconnect [Downloader]");
                    log(registerNumber + "\tClient <- Proxy           \tDisconnect");
                }
            };
        }

        private Runnable getUploaderInstance(Socket local, FilterInputStream localIn, Socket remote, long delay) {
            return () -> {
                try {
                    byte[] buffer = new byte[UPLOAD_BUF_SIZE];
                    BufferedOutputStream outputStream = new BufferedOutputStream(remote.getOutputStream());
                    int length;
                    while (!local.isClosed()) {
                        length = localIn.read(buffer);
                        if (length > 0) {
                            outputStream.write(buffer, 0, length);
                            outputStream.flush();
                            log(registerNumber + "\tClient ==========> Target \tSend [" + length + " bytes]");
                            sleep(delay);
                        } else if (length == -1) {
                            break;
                        }
                    }
                } catch (IOException e) {
                    try {
                        local.close();
                        remote.close();
                    } catch (IOException e0) {
                        e0.printStackTrace();
                    }
                } finally {
                    try {
                        remote.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // log(registerNumber + "\tClient <- Proxy           \tDisconnect [Uploader]");
                }
            };
        }

        private void sleep(long timeMillis) {
            try {
                Thread.sleep(timeMillis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}