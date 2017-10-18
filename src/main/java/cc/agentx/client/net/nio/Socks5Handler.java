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
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socks.*;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

@ChannelHandler.Sharable
public final class Socks5Handler extends SimpleChannelInboundHandler<SocksRequest> {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(Socks5Handler.class);

    @Override
    public void channelRead0(ChannelHandlerContext ctx, SocksRequest request) throws Exception {
        switch (request.protocolVersion()) {
            case SOCKS4a:
                log.warn("\tBad Handshake! (socks version not supported: 4)");
                ctx.write(new SocksInitResponse(SocksAuthScheme.UNKNOWN));
                if (ctx.channel().isActive()) {
                    ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                }
                break;
            case SOCKS5:
                switch (request.requestType()) {
                    case INIT:
                        ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                        ctx.write(new SocksInitResponse(SocksAuthScheme.NO_AUTH));
                        break;
                    case AUTH:
                        ctx.pipeline().addFirst(new SocksCmdRequestDecoder());
                        ctx.write(new SocksAuthResponse(SocksAuthStatus.SUCCESS));
                        break;
                    case CMD:
                        switch (((SocksCmdRequest) request).cmdType()) {
                            case UDP:
                                // udp relay only supports IPv4 in this version
                                if (((SocksCmdRequest) request).addressType() != SocksAddressType.IPv4) {
                                    ctx.write(new SocksCmdResponse(SocksCmdStatus.ADDRESS_NOT_SUPPORTED, ((SocksCmdRequest) request).addressType()));
                                    log.warn("\tBad Handshake! (UDP request addr_type not support: {})", ((SocksCmdRequest) request).addressType());
                                    ctx.close();
                                    break;
                                }
                            case CONNECT:
                                ctx.pipeline().addLast(new XConnectHandler()); // handover
                                ctx.pipeline().remove(this);
                                ctx.fireChannelRead(request);
                                break;
                            default:
                                ctx.close();
                                log.warn("\tBad Handshake! (command not support: {})", ((SocksCmdRequest) request).cmdType());
                        }
                        break;
                    case UNKNOWN:
                        log.warn("\tBad Handshake! (unknown request type: {})", request.requestType());
                }
                break;
            case UNKNOWN:
                log.warn("\tBad Handshake! (unknown protocol version: {}", request.protocolVersion());
                ctx.close();
                break;
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        if (ctx.channel().isActive()) {
            ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
