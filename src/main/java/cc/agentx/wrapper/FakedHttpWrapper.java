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

package cc.agentx.wrapper;

import cc.agentx.masq.HttpFaker;
import cc.agentx.protocol.Http;
import cc.agentx.util.KeyHelper;
import org.bouncycastle.util.Arrays;

import java.util.Base64;
import java.util.regex.Matcher;

public class FakedHttpWrapper extends Wrapper {
    private boolean requestMode;
    private Base64.Encoder encoder;
    private Base64.Decoder decoder;

    public FakedHttpWrapper(boolean requestMode) {
        this.requestMode = requestMode;
        this.encoder = Base64.getUrlEncoder();
        this.decoder = Base64.getUrlDecoder();
    }

    @Override
    public byte[] wrap(final byte[] bytes) {
        if (requestMode) {
            return warpInRequest(bytes);
        } else {
            return wrapInResponse(bytes);
        }
    }

    private byte[] warpInRequest(final byte[] bytes) {
        String encodedStr = encoder.encodeToString(bytes).replaceAll("=", "");

        // 20 percent of small data are faked by post method requests
        if (bytes.length > 512 || KeyHelper.generateRandomInteger(0, 10) < 2) {
            String header = HttpFaker.getRandomRequestHeader(Http.METHOD_POST, true);
            header = header.replaceAll(Matcher.quoteReplacement("$"), String.valueOf(encodedStr.length()));
            return Arrays.concatenate(header.getBytes(), encodedStr.getBytes());
        } else {
            String header = HttpFaker.getRandomRequestHeader(Http.METHOD_GET, true);
            header = header.replaceAll(Matcher.quoteReplacement("$"), encodedStr);
            return header.getBytes();
        }
    }

    private byte[] wrapInResponse(final byte[] bytes) {
        String header = HttpFaker.getRandomResponseHeader(Http.RESPONSE_200, true);
        header = header.replaceAll(Matcher.quoteReplacement("$"), String.valueOf(bytes.length));
        return Arrays.concatenate(header.getBytes(), bytes);
    }

    @Override
    public byte[] unwrap(final byte[] bytes) {
        if (requestMode) {
            return unwrapFromRequest(bytes);
        } else {
            return unwrapFromResponse(bytes);
        }
    }

    private byte[] unwrapFromRequest(final byte[] bytes) {
        String decodedStr = new String(bytes);
        String rawStr = null;
        if (decodedStr.startsWith(Http.METHOD_GET)) {
            for (String line : decodedStr.split(Http.CRLF)) {
                if (line.startsWith("Cookie: ")) {
                    for (String cookie : line.substring(5).split("; ")) {
                        String[] kvPair = cookie.split("=");
                        if (kvPair.length == 2 && kvPair[0].contains("_")) {
                            rawStr = kvPair[1];
                            break;
                        }
                    }
                    break;
                }
            }
            if (rawStr == null) {
                return new byte[0];
            }
        } else if (decodedStr.startsWith(Http.METHOD_POST)) {
            String[] parts = decodedStr.split(Http.CRLF.concat(Http.CRLF));
            if (parts.length != 2) {
                return new byte[0];
            }
            rawStr = parts[1];
        } else {
            throw new RuntimeException("unknown format");
        }
        StringBuilder buffer = new StringBuilder(rawStr);
        while (buffer.length() % 4 != 0)
            buffer.append('=');
        return decoder.decode(buffer.toString());
    }

    public byte[] unwrapFromResponse(final byte[] bytes) {
        // caution: placeholder bytes' end-pos must less than 200
        String fuzzyHeader = new String(Arrays.copyOfRange(bytes, 0, 200));
        if (!fuzzyHeader.startsWith(Http.VERSION_1_1)) {
            throw new RuntimeException("unknown format");
        }
        fuzzyHeader = fuzzyHeader.substring(fuzzyHeader.indexOf("Content-Length: ") + "Content-Length: ".length());
        fuzzyHeader = fuzzyHeader.substring(0, fuzzyHeader.indexOf(Http.CRLF));
        int rawLen = Integer.parseInt(fuzzyHeader);
        return Arrays.copyOfRange(bytes, bytes.length - rawLen, bytes.length);
    }

}
