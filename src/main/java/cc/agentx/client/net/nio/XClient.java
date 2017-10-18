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

package cc.agentx.client.net.nio;

import cc.agentx.Constants;
import cc.agentx.client.Configuration;
import cc.agentx.client.net.Status;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socks.SocksInitRequestDecoder;
import io.netty.handler.codec.socks.SocksMessageEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

public final class XClient {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(XClient.class);
    private static EventLoopGroup bossGroup;
    private static EventLoopGroup workerGroup;

    private XClient() {
    }

    public static XClient getInstance() {
        return new XClient();
    }

    public void start() {
        Configuration config = Configuration.INSTANCE;
        InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE);
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast("logging", new LoggingHandler(LogLevel.DEBUG))
                                    .addLast(new SocksInitRequestDecoder())
                                    .addLast(new SocksMessageEncoder())
                                    .addLast(new Socks5Handler())
                                    .addLast(Status.TRAFFIC_HANDLER);
                        }
                    });
            log.info("\tStartup {}-{}-client [{}{}]", Constants.APP_NAME, Constants.APP_VERSION, config.getMode(), config.getMode().equals("socks5") ? "" : ":" + config.getProtocol());
            new Thread(() -> new UdpServer().start()).start();
            ChannelFuture future = bootstrap.bind(config.getLocalHost(), config.getLocalPort()).sync();
            future.addListener(future1 -> log.info("\tTCP listening at {}:{}...", config.getLocalHost(), config.getLocalPort()));
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            log.error("\tSocket bind failure ({})", e.getMessage());
        } finally {
            log.info("\tShutting down");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void stop() {
        if (bossGroup != null)
            bossGroup.shutdownGracefully();
        if (workerGroup != null)
            workerGroup.shutdownGracefully();
    }
}
