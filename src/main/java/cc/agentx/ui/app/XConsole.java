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

package cc.agentx.ui.app;

import cc.agentx.Constants;
import cc.agentx.client.net.Status;
import io.netty.handler.traffic.TrafficCounter;

import java.util.Map;

/**
 * web console for agentx-client
 */
public class XConsole {
    public String[] welcome(String uri) {
        return new String[]{"html", "<html lang=\"en\" class=\"gr__127_0_0_1\"><head><meta charset=\"UTF-8\"><title>AgentX Web Console 1.3</title><style>body{font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,\"Helvetica Neue\",Arial,sans-serif;font-size:1rem;line-height:1.5;color:#373a3c;background-color:rgba(236,238,239,.5)}.curtain{position:absolute;top:50%;left:50%;transform:translate(-50%,-58%);text-align:center}.curtain h1{margin-top:0;margin-bottom:.5rem;font-size:4rem;font-weight:300}.curtain p{font-size:1.25rem;font-weight:300;margin-top:0;margin-bottom:1rem;display:block}#github-btn{border-radius:1.2rem;display:inline-block;height:1.2rem;line-height:0;background:0 0}#github-icon{fill:#aaa;margin-bottom:-2px;width:1.2rem;height:1.2rem}#github-icon:hover{fill:#4078c0}</style></head><body data-gr-c-s-loaded=\"true\"><div class=\"curtain\"><h1>Welcome!</h1><p><span>AgentX 1.3</span> <a id=\"github-btn\" href=\"https://github.com/zhangjiupeng/agentx\" target=\"_blank\"><svg id=\"github-icon\" aria-hidden=\"true\" version=\"1.1\" viewBox=\"0 0 16 16\"><path fill-rule=\"evenodd\" d=\"M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0 0 16 8c0-4.42-3.58-8-8-8z\"></path></svg></a></p></div></body></html>"};
    }

    public String[] version(String uri) {
        return new String[]{"text", Constants.APP_NAME + " " + Constants.APP_VERSION};
    }

//    public String[] listDns(String uri) {
//        return new String[]{"text", "Lst DNS"};
//    }
//
//    public String[] clearDns(String uri, Map<String, String> params) {
//        return new String[]{"page", params.get("name"), params.get("token")};
//    }

    public String[] getTraffic(String uri, Map<String, String> params) {
        TrafficCounter counter = Status.TRAFFIC_HANDLER.trafficCounter();
        return new String[]{"text", "{\"readSum\":" + counter.cumulativeReadBytes() +
                ",\"read\":" + counter.currentReadBytes() +
                ",\"writeSum\":" + counter.cumulativeWrittenBytes() +
                ",\"write\":" + counter.currentWrittenBytes() + "}"};
    }
}
