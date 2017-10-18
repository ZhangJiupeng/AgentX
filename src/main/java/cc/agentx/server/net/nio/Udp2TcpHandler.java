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

public class Udp2TcpHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger log;

    static {
        log = InternalLoggerFactory.getInstance(Udp2TcpHandler.class);
    }

    private final XRequestResolver requestResolver;
    private final Wrapper wrapper;

    public Udp2TcpHandler(XRequestResolver requestResolver, Wrapper wrapper) {
        this.requestResolver = requestResolver;
        this.wrapper = wrapper;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        DatagramPacket datagram = (DatagramPacket) msg;
        InetSocketAddress sender = datagram.sender();
        Channel tcpChannel = XChannelMapper.getTcpChannel(sender);
        if (tcpChannel == null) {
            // udpSource not registered, actively discard this packet
            // without register, an udp channel cannot relate to any tcp channel, so remove the map
            XChannelMapper.removeUdpMapping(sender);
            log.warn("Bad Connection! (unexpected udp datagram from {})", sender);
        } else if (tcpChannel.isActive()) {
            ByteBuf byteBuf = datagram.content();
            try {
                if (!byteBuf.hasArray()) {
                    byte[] bytes = new byte[byteBuf.readableBytes()];
                    byteBuf.getBytes(0, bytes);
                    log.info("\t          Proxy << Target \tFrom   {}:{}", sender.getHostString(), sender.getPort());

                    // write udp payload via tcp channel
                    tcpChannel.writeAndFlush(Unpooled.wrappedBuffer(wrapper.wrap(requestResolver.wrap(XRequest.Channel.UDP, bytes))));
                    log.info("\tClient << Proxy           \tGet [{} bytes]", bytes.length);
                }
            } finally {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("\tBad Connection! ({})", cause.getMessage());
        XChannelMapper.closeChannelGracefullyByUdpChannel(ctx.channel());
    }
}
