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
import cc.agentx.protocol.request.XRequestResolver;
import cc.agentx.wrapper.Wrapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;

public final class Tcp2UdpHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger log;

    static {
        log = InternalLoggerFactory.getInstance(XRelayHandler.class);
    }

    private final XRequestResolver requestResolver;
    private final Wrapper wrapper;

    private InetSocketAddress udpTarget;

    public Tcp2UdpHandler(InetSocketAddress udpTarget, XRequestResolver requestResolver, Wrapper wrapper) {
        this.udpTarget = udpTarget;
        this.requestResolver = requestResolver;
        this.wrapper = wrapper;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf byteBuf = (ByteBuf) msg;
        try {
            if (!byteBuf.hasArray()) {
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.getBytes(0, bytes);
                bytes = wrapper.unwrap(bytes);

                // resolve xRequest
                XRequest request = requestResolver.parse(bytes);
                String host = request.getHost();
                int port = request.getPort();
                byte[] content = Arrays.copyOfRange(bytes, bytes.length - request.getSubsequentDataLength(), bytes.length);

                // update udp_target if xRequest.target changes
                if (!(udpTarget.getHostString().equals(host) && udpTarget.getPort() == port)) {
                    log.warn("UDP target changed: {}:{} -> {}:{}, update channel_map", udpTarget.getHostString(), udpTarget.getPort(), host, port);
                    XChannelMapper.removeTcpMapping(udpTarget);
                    XChannelMapper.removeUdpMapping(udpTarget);
                    udpTarget = new InetSocketAddress(host, port);
                    XChannelMapper.putTcpChannel(udpTarget, ctx.channel());
                }
                log.info("\tClient >> Proxy           \tTarget {}:{}", host, port);

                // redirect tcp -> udp
                Channel udpChannel = XChannelMapper.getUdpChannel(udpTarget);
                if (udpChannel == null) {
                    log.warn("Bad Connection! (udp channel closed)");
                    XChannelMapper.closeChannelGracefullyByTcpChannel(ctx.channel());
                } else if (udpChannel.isActive()) {
                    udpChannel.writeAndFlush(new DatagramPacket(Unpooled.wrappedBuffer(content), udpTarget, UdpServer.getUdpAddr()));
                    log.info("\t          Proxy >> Target \tSend [{} bytes]", content.length);
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
    public void channelInactive(ChannelHandlerContext ctx) {
        XChannelMapper.closeChannelGracefullyByTcpChannel(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("\tBad Connection! ({})", cause.getMessage());
        XChannelMapper.closeChannelGracefullyByTcpChannel(ctx.channel());
    }
}
