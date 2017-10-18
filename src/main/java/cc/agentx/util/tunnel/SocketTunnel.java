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

package cc.agentx.util.tunnel;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * a transparent socket tunnel
 * <p>
 * data exchange with src will be automatically redirect to dst.<br>
 * for users, data forwarding is hardly perceptible.<br>
 * for developers, it is no need to change your external code.<br>
 * just enjoy!
 */
public class SocketTunnel implements Runnable {

    private InetSocketAddress srcAddr;
    private InetSocketAddress dstAddr;
    private ServerSocketChannel server;
    private Map<SocketChannel, SocketChannel> bridge;
    private Selector selector;
    private ByteBuffer buffer;

    public SocketTunnel(InetSocketAddress src, InetSocketAddress dst) {
        this(src, dst, 65536);
    }

    public SocketTunnel(InetSocketAddress src, InetSocketAddress dst, int bufferSize) {
        this.srcAddr = src;
        this.dstAddr = dst;
        this.bridge = new HashMap<>(1 << 6, 0.75f);
        this.buffer = ByteBuffer.allocate(bufferSize);
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("Example - " + SocketTunnel.class.getName() + " 0.0.0.0:80 123.123.123.123:80");
            return;
        }
        String[] src = args[0].split(":");
        String[] dst = args[1].split(":");
        new SocketTunnel(
                new InetSocketAddress(src[0], Integer.parseInt(src[1])),
                new InetSocketAddress(dst[0], Integer.parseInt(dst[1]))
        ).startup();
        System.out.println("socket tunnel started!\t" + args[0] + " -> " + args[1]);
    }

    public void startup() throws IOException {
        selector = Selector.open();
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.socket().setReuseAddress(true);
        server.socket().bind(srcAddr);
        server.register(selector, SelectionKey.OP_ACCEPT);
        new Thread(this).start();
    }

    public void shutdown() throws IOException {
        if (server.isOpen()) {
            server.close();
        }
        selector.selectNow();
        buffer.clear();
    }

    public void restart() throws IOException {
        server.close();
        selector.selectNow();
        server = ServerSocketChannel.open();
        server.configureBlocking(false);
        server.socket().setReuseAddress(true);
        server.socket().bind(srcAddr);
        server.register(selector, SelectionKey.OP_ACCEPT);
        bridge.clear();
    }

    @Override
    public void run() {
        try {
            while (true) {
                selector.select();
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> i = keys.iterator();
                if (i.hasNext()) {
                    SelectionKey key = i.next();
                    keys.remove(key);
                    handleEvent(key);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleEvent(SelectionKey key) {
        if (key.isAcceptable())
            buildConnection(key);

        else if (key.isReadable())
            transferData(key);
    }

    private void buildConnection(SelectionKey key) {
        try {
            // connect to dstAddr, sign bridge
            SocketChannel dstSocket;
            try {
                dstSocket = SocketChannel.open(dstAddr);
            } catch (ConnectException ce) {
                System.err.print("connection broke (" + ce.getMessage() + "), restarting... ");
                restart();
                System.err.println("ok!");
                return;
            }

            SocketChannel srcSocket;
            srcSocket = server.accept();
            srcSocket.configureBlocking(false);
            srcSocket.socket().setSoLinger(true, 0);
            srcSocket.register(selector, SelectionKey.OP_READ);

            // copy src-socket attributes
            dstSocket.socket().setReceiveBufferSize(srcSocket.socket().getReceiveBufferSize());
            dstSocket.socket().setSoTimeout(srcSocket.socket().getSoTimeout());
            dstSocket.socket().setTcpNoDelay(srcSocket.socket().getTcpNoDelay());
            dstSocket.socket().setKeepAlive(srcSocket.socket().getKeepAlive());
            dstSocket.socket().setOOBInline(srcSocket.socket().getOOBInline()); // urgent data
            dstSocket.socket().setSoLinger(true, 0);
            dstSocket.configureBlocking(false);
            bridge.put(srcSocket, dstSocket);
            bridge.put(dstSocket, srcSocket);
            dstSocket.register(selector, SelectionKey.OP_READ);

        } catch (IOException e) {
            e.printStackTrace();
        }
        key.interestOps(SelectionKey.OP_ACCEPT);
    }

    public void transferData(SelectionKey key) {
        SocketChannel activeSocket = (SocketChannel) key.channel();
        SocketChannel passiveSocket = bridge.get(activeSocket);
        try {
            // throws when closed
            activeSocket.read(buffer);

            byte[] bytes = new byte[buffer.position()];
            if (buffer.position() == 0) {
                // end of stream
                doRecycle(activeSocket, passiveSocket, key);
                return;
            } else {
                System.arraycopy(buffer.array(), 0, bytes, 0, bytes.length);
            }

            buffer.flip();
            if (activeSocket.socket().getLocalSocketAddress() == srcAddr) {
                passiveSocket.write(buffer);
            } else {
                passiveSocket.write(buffer);
            }
            buffer.clear();

        } catch (Exception e) {
            doRecycle(activeSocket, passiveSocket, key);
        } finally {
            buffer.clear();
        }

    }

    public void doRecycle(SocketChannel activeSocket, SocketChannel passiveSocket, SelectionKey key) {
        try {
            if (passiveSocket != null)
                passiveSocket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            bridge.remove(passiveSocket);
            bridge.remove(activeSocket);
            key.cancel();
        }
    }

    public InetSocketAddress getDstAddr() {
        return dstAddr;
    }

    public InetSocketAddress getSrcAddr() {
        return srcAddr;
    }
}
