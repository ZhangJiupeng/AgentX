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

package cc.agentx.server.net.nio;

import cc.agentx.server.Configuration;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

public class UdpServer {
    private static final EventLoopGroup group = new NioEventLoopGroup();
    private static final InternalLogger log = InternalLoggerFactory.getInstance(UdpServer.class);

    private static String udpHost = "0.0.0.0";
    private static int udpPort = 9999;
    private static InetSocketAddress udpAddr;

    private final Configuration config = Configuration.INSTANCE;


    private void initAddr() throws UnknownHostException {
        InetAddress host = InetAddress.getByName(config.getHost());
        if (!host.isAnyLocalAddress()) {
            throw new RuntimeException("<host> is not local");
        }
        udpHost = host.getHostAddress();
        udpPort = config.getPort();
        udpAddr = new InetSocketAddress(udpHost, udpPort);
    }

    public void start() {
        try {
            initAddr();
        } catch (UnknownHostException e) {
            log.warn("Bad Parameter ({})", e.getMessage());
        }

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInboundHandlerAdapter() {
                        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                            DatagramPacket packet = (DatagramPacket) msg;
                            XChannelMapper.putUdpChannel(packet.sender(), ctx.channel());

                            ctx.pipeline().addLast(new Udp2TcpHandler(config.getXRequestResolver(), config.getWrapper()));
                            ctx.pipeline().remove(this);
                            ctx.fireChannelRead(msg);
                        }
                    });
            log.info("Startup udp tunnel on {}:{}", udpHost, udpPort);
            ChannelFuture future = bootstrap.bind(udpHost, udpPort).sync();
            future.addListener(future1 -> log.info("\tUDP listening at {}:{}...", udpHost, udpPort));
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("\tSocket bind failure (udp tunnel: {})", e.getMessage());
        } finally {
            log.info("\tUdp service shutting down");
            group.shutdownGracefully();
        }
    }

    public static InetSocketAddress getUdpAddr() {
        return udpAddr;
    }

}
