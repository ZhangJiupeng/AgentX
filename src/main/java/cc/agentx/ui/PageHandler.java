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

import java.io.*;
import java.lang.reflect.Method;
import java.util.*;
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
public class PageHandler {
    private String baseDir;
    private Map<String, String> resMap;
    private Map<String, String> mimeTable;
    private Set<String> ignoredSuffix;

    public PageHandler(String baseDir) {
        this.baseDir = baseDir.replaceAll("\\\\", "/");
        this.mimeTable = Initializer.getMIMETable();
        resMap = new HashMap<>();
        ignoredSuffix = new HashSet<>();
        Initializer.parsePageIndex(resMap);
        Initializer.parseIgnoredSuffix(ignoredSuffix);
    }

    public synchronized Map<String, Object> fetch(String uri) throws HttpError {
        return fetch(uri, null);
    }

    public synchronized Map<String, Object> fetch(String uri, Map<String, String> parameters) throws HttpError {
        String resourcePath = "";
        boolean needDynamicParse = parameters != null;
        if (ignoredSuffix.contains(uri.substring(uri.lastIndexOf('.') + 1).toLowerCase())) {
            needDynamicParse = false;
            resourcePath = uri;
        }
        if (resourcePath.equals("")) {
            resourcePath = resMap.get(uri);
            if (resourcePath != null) {
                needDynamicParse = resourcePath.contains("@");
            } else {
                throw HttpError.HTTP_404;
            }
        }
        if (needDynamicParse) {
            try {
                String[] paths = resourcePath.split("@");
                String pagePath = paths[0];
                String targetMethodName = paths[1].substring(paths[1].lastIndexOf('.') + 1);
                String targetClassName = paths[1].substring(0, paths[1].lastIndexOf('.'));
                Class targetClass = Class.forName(targetClassName);
                Method targetMethod;
                String[] resp;
                if (parameters == null) {
                    // noinspection unchecked
                    targetMethod = targetClass.getDeclaredMethod(targetMethodName, String.class);
                    resp = (String[]) targetMethod.invoke(targetClass.newInstance(), uri);
                } else {
                    // noinspection unchecked
                    targetMethod = targetClass.getDeclaredMethod(targetMethodName, String.class, Map.class);
                    resp = (String[]) targetMethod.invoke(targetClass.newInstance(), uri, parameters);
                }
                if (resp == null) {
                    throw HttpError.HTTP_404;
                } else {
                    switch (resp[0]) {
                        case "page": {
                            File file = new File(baseDir, pagePath);
                            if (!file.exists()) {
                                throw HttpError.HTTP_404;
                            }
                            BufferedReader reader = new BufferedReader(new FileReader(file));
                            StringBuilder buffer = new StringBuilder();
                            while (reader.ready()) {
                                buffer.append(reader.readLine()).append("\n");
                            }
                            reader.close();
                            String respStr = buffer.toString();
                            for (int i = 1; i < resp.length; i++) {
                                respStr = respStr.replaceAll(Matcher.quoteReplacement("$" + (i - 1)), resp[i]);
                            }
                            byte[] respBytes = respStr.getBytes("UTF-8");
                            return new HashMap<String, Object>() {{
                                put("type", "file");
                                put("inputStream", new ByteArrayInputStream(respBytes));
                                put("length", respBytes.length);
                                put("lastModified", new Date());
                                put("mimeType", "text/html");
                            }};

                        }
                        case "text": {
                            byte[] respBytes = resp[1].getBytes("UTF-8");
                            return new HashMap<String, Object>() {{
                                put("type", "file");
                                put("inputStream", new ByteArrayInputStream(respBytes));
                                put("length", respBytes.length);
                                put("lastModified", new Date());
                                put("mimeType", "text/plain");
                            }};
                        }
                        case "html": {
                            byte[] respBytes = resp[1].getBytes("UTF-8");
                            return new HashMap<String, Object>() {{
                                put("inputStream", new ByteArrayInputStream(respBytes));
                                put("length", respBytes.length);
                                put("lastModified", new Date());
                                put("mimeType", "text/html");
                                put("type", "file");
                            }};
                        }
                        case "redirect":
                            return new HashMap<String, Object>() {{
                                put("type", "redirect");
                                put("url", resp[1]);
                            }};
                        case "error":
                            throw new HttpError(Integer.parseInt(resp[1]), resp[2]);
                        default:
                            throw new Exception(Arrays.toString(resp));
                    }
                }
            } catch (NoSuchMethodException nm) {
                nm.printStackTrace();
                throw HttpError.HTTP_404;
            } catch (Exception e) {
                e.printStackTrace();
                throw HttpError.HTTP_500;
            }
        } else {
            try {
                InputStream inputStream = null;
                long fileLen = 0;
                Date lastModified = null;
                File file = new File(baseDir, resourcePath).getCanonicalFile();
                if (!file.getPath().replaceAll("\\\\", "/").startsWith(baseDir)) {
                    throw HttpError.HTTP_400;
                }
                if (file.isDirectory()) {
                    file = new File(file, "index.html");
                }
                if (file.isFile()) {
                    inputStream = new FileInputStream(file);
                    fileLen = file.length();
                    lastModified = new Date(file.lastModified());
                }
                if (inputStream == null) {
                    throw HttpError.HTTP_404;
                }
                String fileSuffix = file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase();
                final InputStream finalInputStream = inputStream;
                final long finalFileLen = fileLen;
                final Date finalLastModified = lastModified;
                return new HashMap<String, Object>() {{
                    put("type", "file");
                    put("inputStream", finalInputStream);
                    put("length", finalFileLen);
                    put("lastModified", finalLastModified);
                    put("mimeType", mimeTable.get(fileSuffix));
                }};
            } catch (IOException e) {
                e.printStackTrace();
                throw HttpError.HTTP_500;
            }
        }
    }
}
