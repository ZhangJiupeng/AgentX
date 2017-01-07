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

import cc.agentx.protocol.Socks5;

import java.util.Arrays;

public class ShadowsocksRequestWrapper extends XRequestWrapper {

    public ShadowsocksRequestWrapper() {
    }

    /**
     * <pre>
     * +-------------------+----------------------------+
     * |  SOCKS5 FIELD (3) |      SHADOWSOCKS REQ       |
     * +-----+-----+-------+------+----------+----------+
     * | VER | CMD |  RSV  | ATYP | DST.ADDR | DST.PORT |
     * +-----+-----+-------+------+----------+----------+
     * |  1  |  1  | X'00' |  1   | Variable |    2     |
     * +-----+-----+-------+------+----------+----------+
     * </pre>
     */
    @Override
    public byte[] wrap(final byte[] bytes) {
        return Arrays.copyOfRange(bytes, 3, bytes.length);
    }

    @Override
    public XRequest parse(final byte[] bytes) {
        XRequest.Type atyp;
        String ip;
        int port, dataLength;
        switch (bytes[0]) {
            case Socks5.ATYP_IPV4:
                atyp = XRequest.Type.IPV4;
                ip = "" + (bytes[1] & 0xff) + "." + (bytes[2] & 0xff)
                        + "." + (bytes[3] & 0xff) + "." + (bytes[4] & 0xff);
                port = ((bytes[5] & 0xff) << 8) | (bytes[6] & 0xff);
                dataLength = bytes.length - 7;
                break;
            case Socks5.ATYP_DOMAIN:
                atyp = XRequest.Type.DOMAIN;
                int length = bytes[1] & 0xff;
                ip = new String(bytes, 2, length);
                port = ((bytes[length + 2] & 0xff) << 8) + (bytes[length + 3] & 0xff);
                dataLength = bytes.length - 4 - length;
                break;
            case Socks5.ATYP_IPV6:
                atyp = XRequest.Type.IPV6;
                ip = String.format(
                        "%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x", bytes[1] & 0xff
                        , bytes[2] & 0xff, bytes[3] & 0xff, bytes[4] & 0xff, bytes[5] & 0xff, bytes[6] & 0xff
                        , bytes[7] & 0xff, bytes[8] & 0xff, bytes[9] & 0xff, bytes[10] & 0xff, bytes[11] & 0xff
                        , bytes[12] & 0xff, bytes[13] & 0xff, bytes[14] & 0xff, bytes[15] & 0xff, bytes[16] & 0xff
                );
                port = ((bytes[17] & 0xff) << 8) + (bytes[18] & 0xff);
                dataLength = bytes.length - 19;
                break;
            default:
                throw new RuntimeException("unknown shadowsocks request type: " + bytes[0]);
        }
        return new XRequest(atyp, ip, port, dataLength);
    }

    @Override
    public boolean exposeRequest() {
        return false;
    }
}
