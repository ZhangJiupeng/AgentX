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

public class XRequest {
    private Type atyp;
    private String host;
    private int port;
    private int subsequentDataLength;
    private Channel channel = Channel.TCP;

    public XRequest(Type atyp, String host, int port, int subsequentDataLength) {
        this.atyp = atyp;
        this.host = host;
        this.port = port;
        this.subsequentDataLength = subsequentDataLength;
    }

    public XRequest(byte[] bytes) {
        String[] target = new String(bytes).split(":");
        if (target[0].equals(Type.IPV4.name()))
            this.atyp = Type.IPV4;
        if (target[0].equals(Type.DOMAIN.name()))
            this.atyp = Type.DOMAIN;
        if (target[0].equals(Type.IPV6.name()))
            this.atyp = Type.IPV6;
        this.host = target[1];
        this.port = Integer.parseInt(target[2]);
        this.subsequentDataLength = Integer.parseInt(target[3]);
    }

    public Channel getChannel() {
        return channel;
    }

    public XRequest setChannel(Channel channel) {
        this.channel = channel;
        return this;
    }

    public byte[] getBytes() {
        return (atyp.name() + ":" + host + ":" + port + ":" + subsequentDataLength).getBytes();
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getSubsequentDataLength() {
        return subsequentDataLength;
    }

    public Type getAtyp() {
        return atyp;
    }

    @Override
    public String toString() {
        return "XRequest{" +
                "atyp=" + atyp +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", subsequentDataLength=" + subsequentDataLength +
                ", channel=" + channel +
                '}';
    }

    public enum Channel {
        TCP, UDP
    }

    public enum Type {
        IPV4, DOMAIN, IPV6, UNKNOWN
    }
}
