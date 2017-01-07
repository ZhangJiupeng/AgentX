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

import cc.agentx.client.Configuration;
import cc.agentx.protocol.request.XRequestWrapper;
import cc.agentx.wrapper.Wrapper;
import cc.agentx.wrapper.WrapperFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

@ChannelHandler.Sharable
public final class XConnectHandler extends SimpleChannelInboundHandler<SocksCmdRequest> {
    private static final InternalLogger log;
    private static final Wrapper rawWrapper;

    static {
        log = InternalLoggerFactory.getInstance(XConnectHandler.class);
        rawWrapper = WrapperFactory.newRawWrapperInstance();
    }

    private final Bootstrap bootstrap = new Bootstrap();
    private final Configuration config;
    private final XRequestWrapper requestWrapper;
    private final boolean exposeRequest;
    private final Wrapper wrapper;

    public XConnectHandler() {
        this.config = Configuration.INSTANCE;
        this.requestWrapper = config.getXRequestWrapper();
        this.exposeRequest = requestWrapper.exposeRequest();
        this.wrapper = config.getWrapper();
    }

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final SocksCmdRequest request) throws Exception {
        boolean proxyMode = isAgentXNeeded(request.host());
        log.info("\tClient -> Proxy           \tTarget {}:{} [{}]", request.host(), request.port(), proxyMode ? "AGENTX" : "DIRECT");
        Promise<Channel> promise = ctx.executor().newPromise();
        promise.addListener(
                new FutureListener<Channel>() {
                    @Override
                    public void operationComplete(final Future<Channel> future) throws Exception {
                        final Channel outboundChannel = future.getNow();
                        if (future.isSuccess()) {
                            ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.SUCCESS, request.addressType()))
                                    .addListener(channelFuture -> {
                                        ByteBuf byteBuf = Unpooled.buffer();
                                        request.encodeAsByteBuf(byteBuf);
                                        if (byteBuf.hasArray()) {
                                            byte[] xRequestBytes = new byte[byteBuf.readableBytes()];
                                            byteBuf.getBytes(0, xRequestBytes);

                                            if (proxyMode) {
                                                // handshaking to remote proxy
                                                xRequestBytes = requestWrapper.wrap(xRequestBytes);
                                                outboundChannel.writeAndFlush(Unpooled.wrappedBuffer(
                                                        exposeRequest ? xRequestBytes : wrapper.wrap(xRequestBytes)
                                                ));
                                            }

                                            // task handover
                                            ReferenceCountUtil.retain(request); // auto-release? a trap?
                                            ctx.pipeline().remove(XConnectHandler.this);
                                            outboundChannel.pipeline().addLast(new XRelayHandler(ctx.channel(), proxyMode ? wrapper : rawWrapper, false));
                                            ctx.pipeline().addLast(new XRelayHandler(outboundChannel, proxyMode ? wrapper : rawWrapper, true));
                                        }
                                    });
                        } else {
                            ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, request.addressType()));

                            if (ctx.channel().isActive()) {
                                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                            }
                        }
                    }
                }
        );

        String host = request.host();
        int port = request.port();
        if (host.equals(config.getConsoleDomain())) {
            host = "localhost";
            port = config.getConsolePort();
        } else if (proxyMode) {
            host = config.getServerHost();
            port = config.getServerPort();
        }

        // ping target
        bootstrap.group(ctx.channel().eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new XPingHandler(promise, System.currentTimeMillis()))
                .connect(host, port)
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (!future.isSuccess()) {
                            ctx.channel().writeAndFlush(new SocksCmdResponse(SocksCmdStatus.FAILURE, request.addressType()));
                            if (ctx.channel().isActive()) {
                                ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                            }
                        }
                    }
                });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
        log.warn("\tBad Connection! ({})", cause.getMessage());
    }

    private boolean isAgentXNeeded(String domain) {
        // this method is reserved for pac querying
        return !domain.equals(config.getConsoleDomain())
                && !(domain.equals("localhost") || domain.equals("127.0.0.1"))
                && !config.getMode().equals("socks5");
    }
}
