package com.puppet;

import com.github.catvod.spider.XPathSpider;
import com.github.catvod.crawler.Spider;
import okhttp3.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class PanWebShare extends Spider {

    // 你的真实API地址（隐藏在JAR内部）
    private static final String TARGET_API = "https://api.exlop.com/api";
    private OkHttpClient client;
    private Map<String, String> headers;

    public PanWebShare() {
        initOkHttp();
    }

    private void initOkHttp() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        headers = new HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.put("Referer", "https://api.exlop.com/");
    }

    // ---------- TVBox 必须实现的接口 ----------

    /**
     * 首页内容 (homeContent)
     * 对应API参数：?ac=vod
     */
    @Override
    public String homeContent(boolean filter) {
        try {
            // 调用真实API获取首页数据
            String json = fetchApi("ac=vod");
            // 将原始API的JSON转换为TVBox需要的格式
            return convertHomeJson(json);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"code\":500,\"msg\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 分类内容 (categoryContent)
     * 对应API参数：?ac=vod&t={类别ID}&pg={页码}
     */
    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) {
        try {
            String params = "ac=vod&t=" + tid + "&pg=" + pg;
            String json = fetchApi(params);
            return convertCategoryJson(json, tid, pg);
        } catch (Exception e) {
            return "{\"code\":500,\"msg\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 详情内容 (detailContent)
     * 对应API参数：?ac=detail&ids={视频ID}
     * 这正是你提到的接口
     */
    @Override
    public String detailContent(List<String> ids) {
        try {
            String params = "ac=detail&ids=" + ids.get(0);
            String json = fetchApi(params);
            return convertDetailJson(json);
        } catch (Exception e) {
            return "{\"code\":500,\"msg\":\"" + e.getMessage() + "\"}";
        }
    }

    /**
     * 搜索 (searchContent)
     * 对应API参数：?ac=vod&wd={关键词}&pg={页码}
     */
    @Override
    public String searchContent(String key, boolean quick, String pg) {
        try {
            String params = "ac=vod&wd=" + key + "&pg=" + pg;
            String json = fetchApi(params);
            return convertSearchJson(json, key, pg);
        } catch (Exception e) {
            return "{\"code\":500,\"msg\":\"" + e.getMessage() + "\"}";
        }
    }

    // ---------- 核心HTTP请求方法 ----------

    /**
     * 调用真实API
     */
    private String fetchApi(String params) throws Exception {
        String url = TARGET_API + "?" + params;
        Request request = new Request.Builder()
                .url(url)
                .headers(Headers.of(headers))
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("API请求失败: " + response.code());
            }
            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        }
    }

    // ---------- JSON转换器 (关键！将你的API数据转为TVBox格式) ----------

    private String convertHomeJson(String originalJson) {
        // 这里需要根据你的API实际返回结构来解析
        // 以下是示例转换逻辑
        return "{\"class\":[" +
                "{\"type_id\":\"1\",\"type_name\":\"电影\"}," +
                "{\"type_id\":\"2\",\"type_name\":\"电视剧\"}," +
                "{\"type_id\":\"3\",\"type_name\":\"动漫\"}" +
                "],\"list\":[" +
                "{\"vod_id\":\"123\",\"vod_name\":\"示例电影\",\"vod_pic\":\"https://example.com/pic.jpg\",\"vod_remarks\":\"高清\"}" +
                "]}";
    }

    private String convertCategoryJson(String originalJson, String tid, String pg) {
        // 转换分类列表数据
        return "{\"page\":" + pg + ",\"pagecount\":10,\"limit\":20,\"total\":100,\"list\":[" +
                "{\"vod_id\":\"123\",\"vod_name\":\"分类视频\",\"vod_pic\":\"https://example.com/pic.jpg\",\"vod_remarks\":\"8.5分\"}" +
                "]}";
    }

    private String convertDetailJson(String originalJson) {
        // 转换详情数据 - 这是最重要的部分
        // 假设你的API返回：{"list":[{"vod_id":"123","vod_name":"影片名","vod_pic":"...","vod_play_from":"源1$$源2","vod_play_url":"第1集$url1#第2集$url2"}]}
        return "{\"list\":[{" +
                "\"vod_id\":\"123\"," +
                "\"vod_name\":\"示例影片\"," +
                "\"vod_pic\":\"https://example.com/pic.jpg\"," +
                "\"vod_year\":\"2023\"," +
                "\"vod_area\":\"中国\"," +
                "\"vod_actor\":\"演员1,演员2\"," +
                "\"vod_director\":\"导演\"," +
                "\"vod_content\":\"剧情简介...\"," +
                "\"vod_play_from\":\"源1$$源2\"," +  // 多个播放源用$$分隔
                "\"vod_play_url\":\"第1集$http://play.url/1.mp4#第2集$http://play.url/2.mp4\"" +  // 剧集用#$分隔，集名和URL用$分隔
                "}]}";
    }

    private String convertSearchJson(String originalJson, String key, String pg) {
        // 转换搜索数据
        return "{\"page\":" + pg + ",\"pagecount\":1,\"limit\":20,\"total\":5,\"list\":[" +
                "{\"vod_id\":\"456\",\"vod_name\":\"搜索到的影片\",\"vod_pic\":\"https://example.com/pic2.jpg\",\"vod_remarks\":\"7.8分\"}" +
                "]}";
    }
}
