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

package cc.agentx.protocol.request;

/**
 * <b>Notice:</b><br><p>
 * 1. In wrap(), we should translate a standard socks5 request into our
 * customized protocol request implements, make sure the return bytes
 * are recognizable and separable.
 * <pre>
 * +-----+-----+-------+------+----------+----------+
 * | VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
 * +-----+-----+-------+------+----------+----------+
 * |  1  |  1  | X'00' |  1   | Variable |    2     |
 * +-----+-----+-------+------+----------+----------+
 * </pre><p>
 * 2. In parse(), we should solve the packet-splicing problem by ourselves.
 * It should return a valid XRequest object <br>
 * Actually, the subsequent-data-length not belongs to the request-body.
 * Thus the subsequent data will be extracted to buffer according to this
 * parameter.
 */
public abstract class XRequestResolver {

    public abstract byte[] wrap(XRequest.Channel channel, byte[] bytes);

    public abstract XRequest parse(final byte[] bytes);

    public abstract boolean exposeRequest();

    public byte[] wrap(byte[] bytes) {
        return wrap(XRequest.Channel.TCP, bytes);
    }

    public byte[] unwrap(final byte[] bytes) {
        return parse(bytes).getBytes();
    }
}
