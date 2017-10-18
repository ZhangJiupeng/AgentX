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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.net.InetSocketAddress;

public class XChannelMapper {
    private static final InternalLogger log;

    static {
        log = InternalLoggerFactory.getInstance(XChannelMapper.class);
    }

    private static BiMap<InetSocketAddress, Channel> udpTable = HashBiMap.create();
    private static BiMap<InetSocketAddress, Channel> socksTable = HashBiMap.create();
    private static BiMap<InetSocketAddress, Channel> tcpTable = HashBiMap.create();

    static void putSocksChannel(InetSocketAddress udpSource, Channel socksChannel) {
        socksTable.put(udpSource, socksChannel);
    }

    static void putTcpChannel(InetSocketAddress udpSource, Channel tcpChannel) {
        tcpTable.put(udpSource, tcpChannel);
    }

    static void putUdpChannel(InetSocketAddress udpSource, Channel udpChannel) {
        udpTable.put(udpSource, udpChannel);
    }

    static InetSocketAddress getUdpSourceBySocksChannel(Channel socksChannel) {
        return socksTable.inverse().get(socksChannel);
    }

    static InetSocketAddress getUdpSourceByTcpChannel(Channel tcpChannel) {
        return tcpTable.inverse().get(tcpChannel);
    }

    static InetSocketAddress getUdpSourceByUdpChannel(Channel udpChannel) {
        return udpTable.inverse().get(udpChannel);
    }

    static Channel getUdpChannelBySocksChannel(Channel socksChannel) {
        return udpTable.get(getUdpSourceBySocksChannel(socksChannel));
    }

    static Channel getUdpChannelByTcpChannel(Channel tcpChannel) {
        return udpTable.get(getUdpSourceByTcpChannel(tcpChannel));
    }

    static Channel getUdpChannel(InetSocketAddress udpSource) {
        return udpTable.get(udpSource);
    }

    static Channel getSocksChannel(InetSocketAddress udpSource) {
        return socksTable.get(udpSource);
    }

    static Channel getTcpChannel(InetSocketAddress udpSource) {
        return tcpTable.get(udpSource);
    }

    static Channel removeUdpMapping(InetSocketAddress udpSource) {
        return udpTable.remove(udpSource);
    }

    static Channel removeSocksMapping(InetSocketAddress udpSource) {
        return socksTable.remove(udpSource);
    }

    static Channel removeTcpMapping(InetSocketAddress udpSource) {
        return tcpTable.remove(udpSource);
    }

    static void closeChannelGracefully(InetSocketAddress udpSource) {
        Channel socksChannel = removeSocksMapping(udpSource);
        Channel udpChannel = removeUdpMapping(udpSource);
        Channel tcpChannel = removeTcpMapping(udpSource);
        if (tcpChannel.isActive()) {
            tcpChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            log.info("\t          Proxy << Target \tDisconnect");
        }
        if (socksChannel.isActive()) {
            socksChannel.close();
            log.info("\tClient << Proxy           \tDisconnect");
        }
        if (udpChannel.isActive()) {
            udpChannel.close();
        }
    }

    static void closeChannelGracefullyByTcpChannel(Channel tcpChannel) {
        closeChannelGracefully(getUdpSourceByTcpChannel(tcpChannel));
    }

    static void closeChannelGracefullyByUdpChannel(Channel udpChannel) {
        closeChannelGracefully(getUdpSourceByUdpChannel(udpChannel));
    }

    static void closeChannelGracefullyBySocksChannel(Channel socksChannel) {
        closeChannelGracefully(getUdpSourceBySocksChannel(socksChannel));
    }
}
