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

import cc.agentx.protocol.request.XRequest;
import cc.agentx.protocol.request.XRequestWrapper;
import cc.agentx.server.Configuration;
import cc.agentx.server.cache.DnsCache;
import cc.agentx.wrapper.Wrapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.UnknownHostException;
import java.util.Arrays;

@ChannelHandler.Sharable
public final class XConnectHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(XConnectHandler.class);

    private final Bootstrap bootstrap = new Bootstrap();
    private final ByteArrayOutputStream tailDataBuffer;
    private final XRequestWrapper requestWrapper;
    private final boolean exposedRequest;
    private final Wrapper wrapper;

    private boolean requestParsed;

    public XConnectHandler() {
        this.tailDataBuffer = new ByteArrayOutputStream();
        Configuration config = Configuration.INSTANCE;
        this.requestWrapper = config.getXRequestWrapper();
        this.exposedRequest = requestWrapper.exposeRequest();
        this.wrapper = config.getWrapper();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            ByteBuf byteBuf = (ByteBuf) msg;
            if (!byteBuf.hasArray()) {
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.getBytes(0, bytes);

                if (!requestParsed) {
                    if (!exposedRequest) {
                        bytes = wrapper.unwrap(bytes);
                        if (bytes == null) {
                            log.info("\tClient -> Proxy           \tHalf Request");
                            return;
                        }
                    }
                    XRequest xRequest = requestWrapper.parse(bytes);
                    String host = xRequest.getHost();
                    int port = xRequest.getPort();
                    int dataLength = xRequest.getSubsequentDataLength();
                    if (dataLength > 0) {
                        byte[] tailData = Arrays.copyOfRange(bytes, bytes.length - dataLength, bytes.length);
                        if (exposedRequest) {
                            tailData = wrapper.unwrap(tailData);
                            if (tailData != null) {
                                tailDataBuffer.write(tailData, 0, tailData.length);
                            }
                        } else {
                            tailDataBuffer.write(tailData, 0, tailData.length);
                        }
                    }
                    log.info("\tClient -> Proxy           \tTarget {}:{}{}", host, port, DnsCache.isCached(host) ? " [Cached]" : "");
                    if (xRequest.getAtyp() == XRequest.Type.DOMAIN) {
                        try {
                            host = DnsCache.get(host);
                            if (host == null) {
                                host = xRequest.getHost();
                            }
                        } catch (UnknownHostException e) {
                            log.warn("\tClient <- Proxy           \tBad DNS! ({})", e.getMessage());
                            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                            return;
                        }
                    }

                    Promise<Channel> promise = ctx.executor().newPromise();
                    promise.addListener(
                            new FutureListener<Channel>() {
                                @Override
                                public void operationComplete(final Future<Channel> future) throws Exception {
                                    final Channel outboundChannel = future.getNow();
                                    if (future.isSuccess()) {
                                        // handle tail
                                        byte[] tailData = tailDataBuffer.toByteArray();
                                        tailDataBuffer.close();
                                        if (tailData.length > 0) {
                                            log.info("\tClient ==========> Target \tSend Tail [{} bytes]", tailData.length);
                                        }
                                        outboundChannel.writeAndFlush((tailData.length > 0)
                                                ? Unpooled.wrappedBuffer(tailData) : Unpooled.EMPTY_BUFFER)
                                                .addListener(channelFuture -> {
                                                    // task handover
                                                    outboundChannel.pipeline().addLast(new XRelayHandler(ctx.channel(), wrapper, false));
                                                    ctx.pipeline().addLast(new XRelayHandler(outboundChannel, wrapper, true));
                                                    ctx.pipeline().remove(XConnectHandler.this);
                                                });

                                    } else {
                                        if (ctx.channel().isActive()) {
                                            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                                        }
                                    }
                                }
                            }
                    );

                    final String finalHost = host;
                    bootstrap.group(ctx.channel().eventLoop())
                            .channel(NioSocketChannel.class)
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                            .option(ChannelOption.SO_KEEPALIVE, true)
                            .handler(new XPingHandler(promise, System.currentTimeMillis()))
                            .connect(host, port).addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (!future.isSuccess()) {
                                if (ctx.channel().isActive()) {
                                    log.warn("\tClient <- Proxy           \tBad Ping! ({}:{})", finalHost, port);
                                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                                }
                            }
                        }
                    });

                    requestParsed = true;
                } else {
                    bytes = wrapper.unwrap(bytes);
                    if (bytes != null)
                        tailDataBuffer.write(bytes, 0, bytes.length);
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
        cause.printStackTrace();
        log.warn("\tBad Connection! ({})", cause.getMessage());
    }
}