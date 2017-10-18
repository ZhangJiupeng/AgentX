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

package cc.agentx.masq;

import cc.agentx.protocol.Http;
import cc.agentx.util.KeyHelper;

import java.security.SecureRandom;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class HttpFaker {
    private static final SecureRandom randomizer = new SecureRandom();
    private static final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    private static final List<String> randomUrlList = new ArrayList<>(); // urls with no parameters
    private static final List<String> randomUrlList0 = new ArrayList<>(); // urls with parameters
    private static final Lock lock = new ReentrantLock();
    private static final int randomUrlListSize = 100;

    private static final String[] entities = {
            "pub", "app", "articles", "question", "answer",
            "plugins", "account", "keywords", "blog", "en",
            "general", "doc", "game", "paper", "details",
            "index", "file", "resources", "book", "user",
            "map", "mail", "order", "people", "photo",
            "faq", "video", "support", "tools", "page",
            "news", "wiki", "html", "api", "package"
    };

    private static final String[] operations = {
            "login", "signup", "register", "new", "index",
            "add", "remove", "update", "search", "scan",
            "register", "list", "like", "dislike", "q",
            "s", "submit", "download", "upload", "blob",
            "generate", "get", "delete", "confirm", "find"
    };

    private static final String[] userAgents = {
            "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_3) AppleWebKit/537.75.14 (KHTML, like Gecko) Version/7.0.3 Safari/7046A194A",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.246",
            "Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko",
            "Mozilla/5.0 (compatible, MSIE 11, Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko"
    };

    private static final String[] domains = {
            "www.bing.com", "www.huawei.com", "www.mi.com", "www.apple.com", "www.amazon.com",
            "www.dell.com", "www.microsoft.com", "www.alibaba.com", "www.kfc.com", "www.yahoo.com",
            "www.163.com", "www.jd.com", "www.microsoft.com", "www.techcrunch.com", "www.zhihu.com",
            "user.bing.com", "product.huawei.com", "forum.mi.com", "help.apple.com", "buy.amazon.com",
            "translate.bing.com", "service.jd.com", "bill.taobao.com", "help.yahoo.com", "www.github.io"
    };

    private static final String alphabeticString =
            "abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String alphanumericString =
            "0123456789" +
                    "abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String pickup(String[] strings) {
        return strings[randomizer.nextInt(strings.length)];
    }

    public static String randomAlphanumericString(int length) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < length; i++) {
            buffer.append(alphanumericString.charAt(randomizer.nextInt(62)));
        }
        return buffer.toString();
    }

    public static String randomAlphabeticString(int length, boolean lowercase) {
        StringBuilder buffer = new StringBuilder();
        if (lowercase) {
            for (int i = 0; i < length; i++) {
                buffer.append((char) (97 + randomizer.nextInt(26)));
            }
        } else {
            for (int i = 0; i < length; i++) {
                buffer.append(alphabeticString.charAt(randomizer.nextInt(52)));
            }
        }
        return buffer.toString();
    }

    public static String getRandomUrl(boolean withParameters) {
        return "http://".concat(pickup(domains)).concat(getRandomUri(withParameters));
    }

    public static String getRandomUri(boolean withParameters) {
        synchronized (lock) {
            if (!withParameters) {
                if (randomUrlList.size() == 0) {
                    for (int i = 0; i < randomUrlListSize; i++) {
                        randomUrlList.add(randomUri(false));
                    }
                }
                return randomUrlList.remove(randomUrlList.size() - 1);

            } else {
                if (randomUrlList0.size() == 0) {
                    for (int i = 0; i < randomUrlListSize; i++) {
                        randomUrlList0.add(randomUri(true));
                    }
                }
                return randomUrlList0.remove(randomUrlList0.size() - 1);
            }
        }
    }

    public static String getRandomUserAgent() {
        return pickup(userAgents);
    }

    public static String getRandomRequestHeader(String method, boolean placeholder) {
        StringBuilder buffer = new StringBuilder();
        switch (method) {
            case Http.METHOD_GET:
                buffer.append("GET ").append(getRandomUri(randomizer.nextBoolean()));
                buffer.append(" ").append(Http.VERSION_1_1).append(Http.CRLF);
                buffer.append("Host: ").append(pickup(domains)).append(Http.CRLF);
                buffer.append("Connection: keep-alive").append(Http.CRLF);
                if (randomizer.nextBoolean())
                    buffer.append("Cache-Control: max-age=0").append(Http.CRLF);
                if (randomizer.nextBoolean())
                    buffer.append("Accept: ").append("*/*").append(Http.CRLF);
                buffer.append("DNT: 1").append(Http.CRLF);
                buffer.append("User-Agent: ").append(getRandomUserAgent()).append(Http.CRLF);
                buffer.append("Accept-Encoding: gzip, deflate").append(Http.CRLF);
                buffer.append("Accept-Language: zh-CN,zh;q=0.").append(randomizer.nextInt(9) + 1).append(Http.CRLF);
                if (placeholder) {
                    buffer.append("Cookie: ");
                    for (int i = 0; i < randomizer.nextInt(2); i++)
                        buffer.append(randomAlphabeticString(randomizer.nextInt(3) + 1, true)).append("=").append(randomAlphanumericString(randomizer.nextInt(16) + 1)).append("; ");
                    buffer.append(randomAlphabeticString(randomizer.nextInt(3) + 1, true)).append("_").append(randomAlphabeticString(randomizer.nextInt(3) + 1, true)).append("=").append("$").append("; ");
                    for (int i = 0; i < randomizer.nextInt(2); i++)
                        buffer.append(randomAlphabeticString(randomizer.nextInt(3) + 1, true)).append("=").append(randomAlphanumericString(randomizer.nextInt(16) + 1)).append("; ");
                    buffer.append(randomAlphabeticString(randomizer.nextInt(3) + 1, true)).append("=").append(randomAlphanumericString(randomizer.nextInt(16) + 1)).append(Http.CRLF);
                }
                buffer.append(Http.CRLF);
                break;
            case Http.METHOD_POST:
                String host = pickup(domains);
                buffer.append("POST ").append(getRandomUri(false)).append(" ").append(Http.VERSION_1_1).append(Http.CRLF);
                buffer.append("Host: ").append(host).append(Http.CRLF);
                buffer.append("Connection: keep-alive").append(Http.CRLF);
                buffer.append("Content-Length: ").append(placeholder ? "$" : "0").append(Http.CRLF);
                buffer.append("Accept: ").append("*/*").append(Http.CRLF);
                if (randomizer.nextInt(10) < 3)
                    buffer.append("Origin: http://").append(host).append(Http.CRLF);
                else if (randomizer.nextInt(10) < 7) {
                    buffer.append("Referer: ").append("http://").append(host).append(randomUri(false)).append(Http.CRLF);
                }
                buffer.append("User-Agent: ").append(getRandomUserAgent()).append(Http.CRLF);
                buffer.append("Content-Type: application/octet-stream").append(Http.CRLF);
                buffer.append("DNT: 1").append(Http.CRLF);
                buffer.append("Accept-Encoding: gzip, deflate").append(Http.CRLF);
                buffer.append("Accept-Language: zh-CN,zh;q=0.").append(randomizer.nextInt(9) + 1).append(Http.CRLF);
                buffer.append(Http.CRLF);
                break;
            default:
                throw new RuntimeException("method not support: " + method);
        }
        return buffer.toString();
    }

    public static String getRandomResponseHeader(String response, boolean placeholder) {
        if (!response.equals(Http.RESPONSE_200)) {
            throw new RuntimeException("response not support");
        }
        StringBuilder buffer = new StringBuilder();
        buffer.append(Http.VERSION_1_1).append(" ").append(response).append(Http.CRLF);
        switch (randomizer.nextInt(10)) {
            case 0:
                buffer.append("Server: nginx").append(Http.CRLF);
                break;
            case 1:
                buffer.append("Server: AmazonS3").append(Http.CRLF);
                break;
            case 2:
                buffer.append("Server: Tengine").append(Http.CRLF);
                break;
            case 3:
                buffer.append("Server: Apache").append(Http.CRLF);
                break;
            case 4:
                buffer.append("Server: cafe").append(Http.CRLF);
                break;
        }
        buffer.append("Connection: keep-alive").append(Http.CRLF);
        buffer.append("Content-Type: ").append(randomizer.nextBoolean() ? "application/octet-stream" : "gzip").append(Http.CRLF);
        buffer.append("Content-Length: ").append(placeholder ? "$" : randomizer.nextInt(8192) + 1).append(Http.CRLF);
        String time = dateFormat.format(new Date());
        buffer.append("Date: ").append(time).append(Http.CRLF);
        buffer.append("Last-Modified: ").append(time).append(Http.CRLF);
        buffer.append(Http.CRLF);
        return buffer.toString();
    }

    public static String randomUri(boolean withParameters) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("/").append(pickup(entities)).append("/");

        if (randomizer.nextBoolean()) {
            if (randomizer.nextBoolean())
                buffer.append(KeyHelper.generateRandomInteger(2000, 2018)).append("/")
                        .append(KeyHelper.generateRandomInteger(1, 13)).append("/")
                        .append(KeyHelper.generateRandomInteger(1, 29)).append("/");
            if (randomizer.nextBoolean())
                buffer.append(randomizer.nextInt(65536)).append("/");
            else
                buffer.append(randomAlphanumericString(randomizer.nextInt(16) + 1))
                        .append(randomizer.nextBoolean() ? ".html" : "/");
        } else {
            buffer.append(randomizer.nextBoolean() ? pickup(operations)
                    : randomAlphabeticString(randomizer.nextInt(5) + 1, true)).append(".html");
        }

        if (withParameters) {
            buffer.append("?");
            for (int i = 0; i < randomizer.nextInt(3) + 1; i++) {
                if (i > 0)
                    buffer.append("&");
                buffer.append(randomAlphabeticString(randomizer.nextInt(3) + 1, true)).append("=")
                        .append(randomizer.nextBoolean() ? randomizer.nextInt(65536)
                                : randomAlphanumericString(randomizer.nextInt(32)));
            }
        }
        return buffer.toString();
    }

}