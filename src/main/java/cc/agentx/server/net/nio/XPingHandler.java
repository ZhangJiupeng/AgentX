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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

public final class XPingHandler extends ChannelInboundHandlerAdapter {
    private static final InternalLogger log = InternalLoggerFactory.getInstance(XPingHandler.class);

    private final Promise<Channel> promise;
    private final long initializeTimeMillis;

    public XPingHandler(Promise<Channel> promise, long initializeTimeMillis) {
        log.info("\t          Proxy -> Target \tPing");
        this.promise = promise;
        this.initializeTimeMillis = initializeTimeMillis;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("\t          Proxy <- Target \tPong ~{}ms", System.currentTimeMillis() - initializeTimeMillis);
        ctx.pipeline().remove(this);
        promise.setSuccess(ctx.channel());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        log.warn("\tBad Ping! ({})", throwable.getMessage());
        promise.setFailure(throwable);
    }
}
