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
 * @see <a href="https://www.w3.org/Protocols/HTTP/1.0/spec.html">https://www.w3.org/Protocols/HTTP/1.0/spec.html</a>
 * <a href="https://www.w3.org/Protocols/rfc2616/rfc2616.html">https://www.w3.org/Protocols/rfc2616/rfc2616.html</a>
 */
public class Http {
    public static final String VERSION_1_0 = "HTTP/1.0";
    public static final String VERSION_1_1 = "HTTP/1.1";

    public static final String METHOD_OPTIONS = "OPTIONS";
    public static final String METHOD_HEAD = "HEAD";
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_TRACE = "TRACE";
    public static final String METHOD_CONNECT = "GET";

    public static final String RESPONSE_100 = "100 Continue";
    public static final String RESPONSE_101 = "101 Switching Protocols";
    public static final String RESPONSE_102 = "102 Processing";
    public static final String RESPONSE_200 = "200 OK";
    public static final String RESPONSE_201 = "201 Created";
    public static final String RESPONSE_202 = "202 Accepted";
    public static final String RESPONSE_203 = "203 Non-Authoriative Information";
    public static final String RESPONSE_204 = "204 No Content";
    public static final String RESPONSE_205 = "205 Reset Content";
    public static final String RESPONSE_206 = "206 Partial Content";
    public static final String RESPONSE_207 = "207 Multi-Status";
    public static final String RESPONSE_300 = "300 Multiple Choices";
    public static final String RESPONSE_301 = "301 Moved Permanently";
    public static final String RESPONSE_302 = "302 Found";
    public static final String RESPONSE_303 = "303 See Other";
    public static final String RESPONSE_304 = "304 Not Modified";
    public static final String RESPONSE_305 = "305 User Proxy";
    public static final String RESPONSE_306 = "306 Unused";
    public static final String RESPONSE_307 = "307 Temporary Redirect";
    public static final String RESPONSE_400 = "400 Bad Request";
    public static final String RESPONSE_401 = "401 Unauthorized";
    public static final String RESPONSE_402 = "402 Payment Granted";
    public static final String RESPONSE_403 = "403 Forbidden";
    public static final String RESPONSE_404 = "404 File Not Found";
    public static final String RESPONSE_405 = "405 Method Not Allowed";
    public static final String RESPONSE_406 = "406 Not Acceptable";
    public static final String RESPONSE_407 = "407 Proxy Authentication Required";
    public static final String RESPONSE_408 = "408 Request Time-out";
    public static final String RESPONSE_409 = "409 Conflict";
    public static final String RESPONSE_410 = "410 Gone";
    public static final String RESPONSE_411 = "411 Length Required";
    public static final String RESPONSE_412 = "412 Precondition Failed";
    public static final String RESPONSE_413 = "413 Request Entity Too Large";
    public static final String RESPONSE_414 = "414 Request-URI Too Large";
    public static final String RESPONSE_415 = "415 Unsupported Media Type";
    public static final String RESPONSE_416 = "416 Requested range not satisfiable";
    public static final String RESPONSE_417 = "417 Expectation Failed";
    public static final String RESPONSE_422 = "422 Unprocessable Entity";
    public static final String RESPONSE_423 = "423 Locked";
    public static final String RESPONSE_424 = "424 Failed Dependency";
    public static final String RESPONSE_500 = "500 Internal Server Error";
    public static final String RESPONSE_501 = "501 Not Implemented";
    public static final String RESPONSE_502 = "502 Bad Gateway";
    public static final String RESPONSE_503 = "503 Service Unavailable";
    public static final String RESPONSE_504 = "504 Gateway Timeout";
    public static final String RESPONSE_505 = "505 HTTP Version Not Supported";
    public static final String RESPONSE_507 = "507 Insufficient Storage";

    public static final String CRLF = "\r\n";
}
