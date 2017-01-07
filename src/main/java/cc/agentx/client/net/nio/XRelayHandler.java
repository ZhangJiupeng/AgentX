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

import cc.agentx.wrapper.Wrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public final class XRelayHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger log;

    static {
        log = InternalLoggerFactory.getInstance(XRelayHandler.class);
    }

    private final Channel dstChannel;
    private final Wrapper wrapper;
    private final boolean uplink;

    public XRelayHandler(Channel dstChannel, Wrapper wrapper, boolean uplink) {
        this.dstChannel = dstChannel;
        this.wrapper = wrapper;
        this.uplink = uplink;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (dstChannel.isActive()) {
            ByteBuf byteBuf = (ByteBuf) msg;
            try {
                if (!byteBuf.hasArray()) {
                    byte[] bytes = new byte[byteBuf.readableBytes()];
                    byteBuf.getBytes(0, bytes);
                    if (uplink) {
                        dstChannel.writeAndFlush(Unpooled.wrappedBuffer(wrapper.wrap(bytes)));
                        log.info("\tClient ==========> Target \tSend [{} bytes]", bytes.length);
                    } else {
                        bytes = wrapper.unwrap(bytes);
                        if (bytes != null) {
                            dstChannel.writeAndFlush(Unpooled.wrappedBuffer(bytes));
                            log.info("\tClient <========== Target \tGet [{} bytes]", bytes.length);
                        }
                    }
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (dstChannel.isActive()) {
            if (!uplink) {
                log.info("\t          Proxy <- Target \tDisconnect");
                log.info("\tClient <- Proxy           \tDisconnect");
            }
            dstChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.info("\t          Proxy <- Target \tDisconnect");
        log.info("\tClient <- Proxy           \tDisconnect");
        ctx.close();
    }
}