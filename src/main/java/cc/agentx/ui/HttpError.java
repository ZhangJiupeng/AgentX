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

package cc.agentx.ui;

import cc.agentx.Constants;

import java.util.regex.Matcher;

/**
 * <b>Notice:</b> this project is not for a web server,
 * you can see that the page configuration is embedded into
 * <code>cc.agentx.http.Initializer</code>. <br>However,
 * <code>cc.agentx.http</code> is designed separately,
 * it can be extracted into a single project.
 * <br>
 * Thus, this web server project might be developed in the future,
 * hold on and keep attention :)
 */
public class HttpError extends Throwable {
    public static final HttpError HTTP_400 = new HttpError(400, "Bad Request");
    public static final HttpError HTTP_404 = new HttpError(404, "Not Found");
    public static final HttpError HTTP_405 = new HttpError(405, "Method Not Allowed");
    public static final HttpError HTTP_408 = new HttpError(405, "Request Timeout");
    public static final HttpError HTTP_500 = new HttpError(500, "Internal Server Error");

    private static final String ERROR_PAGE_CONTENT = Initializer.getStaticErrorPage();
    private final int code;
    private final String text;

    HttpError(final int code, final String text) {
        this.code = code;
        this.text = text;
    }

    public static String wrapInErrorPage(String bodyText) {
        return ERROR_PAGE_CONTENT.replaceAll(Matcher.quoteReplacement("$0"), bodyText)
                .replaceAll(Matcher.quoteReplacement("$1"), bodyText)
                .replaceAll(Matcher.quoteReplacement("$2"), "AgentX " + Constants.APP_VERSION)
                .replaceAll(Matcher.quoteReplacement("$3"), "https://github.com/zhangjiupeng/agentx");
    }

    public int getHttpCode() {
        return code;
    }

    public String getHttpText() {
        return text;
    }
}