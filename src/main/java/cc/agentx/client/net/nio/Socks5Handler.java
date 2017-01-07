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
                log.warn("\tBad Handshake! (protocol version not supported: 4)");
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
                        if (((SocksCmdRequest) request).cmdType() == SocksCmdType.CONNECT) {
                            ctx.pipeline().addLast(new XConnectHandler());
                            ctx.pipeline().remove(this);
                            ctx.fireChannelRead(request);
                        } else {
                            ctx.close();
                            log.warn("\tBad Handshake! (command not support: {})", ((SocksCmdRequest) request).cmdType());
                        }
                        break;
                    case UNKNOWN:
                        log.warn("\tBad Handshake! (unknown request type)");
                }
                break;
            case UNKNOWN:
                log.warn("\tBad Handshake! (protocol version not support: {}", request.protocolVersion());
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
