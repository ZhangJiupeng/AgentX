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
 * @see <a href="https://ftp.icm.edu.pl/packages/socks/socks4/SOCKS4.protocol">
 * https://ftp.icm.edu.pl/packages/socks/socks4/SOCKS4.protocol</a>
 */
public class Socks4 {
    public static final int VERSION = 4;
    public static byte[] reject = {0, 0x5B};

    private Socks4() {
    }

}
