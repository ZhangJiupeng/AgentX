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

import cc.agentx.protocol.Http;
import cc.agentx.protocol.Socks5;
import cc.agentx.wrapper.FakedHttpWrapper;

import java.util.Arrays;

public class FakedHttpRequestResolver extends XRequestResolver {
    private FakedHttpWrapper wrapper;

    public FakedHttpRequestResolver() {
        this.wrapper = new FakedHttpWrapper(true);
    }

    @Override
    public byte[] wrap(XRequest.Channel channel, final byte[] bytes) {
        if (channel == XRequest.Channel.UDP) {
            throw new RuntimeException("udp request is not acceptable in fakedhttp mode"); // future works
        } else {
            byte[] addrBytes = Arrays.copyOfRange(bytes, 3, bytes.length);
            XRequest.Type atyp;
            String host;
            int port;
            switch (addrBytes[0]) {
                case Socks5.ATYP_IPV4:
                    atyp = XRequest.Type.IPV4;
                    host = "" + (addrBytes[1] & 0xff) + "." + (addrBytes[2] & 0xff)
                            + "." + (addrBytes[3] & 0xff) + "." + (addrBytes[4] & 0xff);
                    port = ((addrBytes[5] & 0xff) << 8) | (addrBytes[6] & 0xff);
                    break;
                case Socks5.ATYP_DOMAIN:
                    atyp = XRequest.Type.DOMAIN;
                    int length = addrBytes[1] & 0xff;
                    host = new String(addrBytes, 2, length);
                    port = ((addrBytes[length + 2] & 0xff) << 8) + (addrBytes[length + 3] & 0xff);
                    break;
                case Socks5.ATYP_IPV6:
                    atyp = XRequest.Type.IPV6;
                    host = String.format(
                            "%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x", addrBytes[1] & 0xff
                            , addrBytes[2] & 0xff, addrBytes[3] & 0xff, addrBytes[4] & 0xff, addrBytes[5] & 0xff, addrBytes[6] & 0xff
                            , addrBytes[7] & 0xff, addrBytes[8] & 0xff, addrBytes[9] & 0xff, addrBytes[10] & 0xff, addrBytes[11] & 0xff
                            , addrBytes[12] & 0xff, addrBytes[13] & 0xff, addrBytes[14] & 0xff, addrBytes[15] & 0xff, addrBytes[16] & 0xff
                    );
                    port = ((addrBytes[17] & 0xff) << 8) + (addrBytes[18] & 0xff);
                    break;
                default:
                    throw new RuntimeException("unknown ATYP: " + addrBytes[0]);
            }
            return wrapper.wrap((atyp.name() + ":" + host + ":" + port).getBytes());
        }
    }

    @Override
    public XRequest parse(final byte[] bytes) {
        String str = new String(bytes);
        String httpText = str.substring(0, str.indexOf(Http.CRLF.concat(Http.CRLF)) + Http.CRLF.concat(Http.CRLF).length());
        int headerLength = httpText.getBytes().length;
        if (str.startsWith(Http.METHOD_GET)) {
            String addr = new String(wrapper.unwrap(Arrays.copyOfRange(bytes, 0, headerLength)));
            return new XRequest((addr + ":" + (bytes.length - headerLength)).getBytes());
        } else if (str.startsWith(Http.METHOD_POST)) {
            httpText = httpText.substring(httpText.indexOf("Content-Length: ") + "Content-Length: ".length());
            headerLength += Integer.parseInt(httpText.substring(0, httpText.indexOf(Http.CRLF)));
            String addr = new String(wrapper.unwrap(Arrays.copyOfRange(bytes, 0, headerLength)));
            return new XRequest((addr + ":" + (bytes.length - headerLength)).getBytes());
        } else {
            throw new RuntimeException("unknown format");
        }
    }

    @Override
    public boolean exposeRequest() {
        return true;
    }
}
