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

package cc.agentx.protocol;

/**
 * @see <a href="https://www.ietf.org/rfc/rfc1928.txt">
 * https://www.ietf.org/rfc/rfc1928.txt</a>
 */
public class Socks5 {
    public static final int VERSION = 5;
    public static final int ATYP_IPV4 = 1;
    public static final int ATYP_DOMAIN = 3;
    public static final int ATYP_IPV6 = 4;
    // preset replies
    public static byte[] echo = {5, 0};
    public static byte[] reject = {5, (byte) 0xff};
    public static byte[] succeed = {5, 0, 0, 1, 0, 0, 0, 0, 0, 0};
    public static byte[] error_1 = {5, 1, 0, 1, 0, 0, 0, 0, 0, 0}; // general socks server failure
    public static byte[] error_2 = {5, 2, 0, 1, 0, 0, 0, 0, 0, 0}; // connection not allowed by rule set
    public static byte[] error_3 = {5, 3, 0, 1, 0, 0, 0, 0, 0, 0}; // network unreachable
    public static byte[] error_4 = {5, 4, 0, 1, 0, 0, 0, 0, 0, 0}; // host unreachable
    public static byte[] error_5 = {5, 5, 0, 1, 0, 0, 0, 0, 0, 0}; // connection refused
    public static byte[] error_6 = {5, 6, 0, 1, 0, 0, 0, 0, 0, 0}; // ttl expired
    public static byte[] error_7 = {5, 7, 0, 1, 0, 0, 0, 0, 0, 0}; // command not supported
    public static byte[] error_8 = {5, 8, 0, 1, 0, 0, 0, 0, 0, 0}; // address type not supported
    private Socks5() {
    }

}
