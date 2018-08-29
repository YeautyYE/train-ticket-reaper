package com.yeauty.util;


import org.apache.http.*;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class HttpClientUtils {


    public final static String[] mobileUserAgentArray = new String[]{
            "Mozilla/5.0 (iPhone; CPU iPhone OS 9_2_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13D15 MicroMessenger/6.3.16 NetType/WIFI Language/zh_CN",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13E238 MicroMessenger/6.3.16 NetType/WIFI Language/zh_CN",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 9_3_2 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Mobile/13F69 MicroMessenger/6.3.16 NetType/WIFI Language/zh_CN",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.3.8 (KHTML, like Gecko) Mobile/11D257 MicroMessenger/6.5.22 NetType/WIFI Language/zh_CN",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Mobile/15A372 MicroMessenger/6.5.22 NetType/WIFI Language/zh_CN"
    };

    public final static String[] pcUserAgentArray = new String[]{
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/49.0.2623.110 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2623.110 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/47.0.2623.110 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2623.110 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2623.110 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/44.0.2623.110 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/43.0.2623.110 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2623.110 Safari/537.36",
            "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:50.0) Gecko/20100101 Firefox/50.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.104 Safari/537.36 Core/1.53.2595.400 QQBrowser/9.6.10872.400"
    };


    public static String doGet(String url, Map<String, String> param, Map<String, String> headers, String charset, String proxyHost, Integer proxyPort) {
        // 创建Http请求配置参数
        RequestConfig.Builder requestBuilder = RequestConfig.custom()
                // 获取连接超时时间
                .setConnectionRequestTimeout(10000)
                // 请求超时时间
                .setConnectTimeout(10000)
                // 响应超时时间
                .setSocketTimeout(10000);
        HttpHost httpHost = null;
        if (proxyHost != null && proxyPort != null) {
            httpHost = new HttpHost(proxyHost, proxyPort);
            requestBuilder.setProxy(httpHost);
        }


        RequestConfig requestConfig = requestBuilder.build();

        // 创建httpClient
        HttpClientBuilder httpClientBuilder = HttpClients.custom();

        httpClientBuilder
                // 把请求相关的超时信息设置到连接客户端
                .setDefaultRequestConfig(requestConfig)
                // 把请求重试设置到连接客户端
                .setRetryHandler(new RetryHandler());

        CloseableHttpClient httpClient = httpClientBuilder.build();

        HttpClientContext httpClientContext = HttpClientContext.create();
        CloseableHttpResponse response = null;
        String resultString = "";
        try {
            // 创建uri
            URIBuilder builder = new URIBuilder(url);
            if (param != null) {
                for (String key : param.keySet()) {
                    builder.addParameter(key, param.get(key));
                }
            }
            URI uri = builder.build();

            // 创建http GET请求
            HttpGet httpGet = new HttpGet(uri);
            //请求头
            setHeaders(headers, httpGet);

            // 执行请求
            response = httpClient.execute(httpGet);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                resultString = EntityUtils.toString(response.getEntity(), charset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResponse(httpClient, response);
        }
        return resultString;
    }

    public static String doGet(String url, Map<String, String> param, Map<String, String> headers, String charset) {
        return doGet(url, param, headers, charset, null, null);
    }

    public static String doGet(String url, Map<String, String> param, String charset) {
        return doGet(url, param, null, charset);
    }

    public static String doGet(String url, Map<String, String> param, Map<String, String> headers) {
        return doGet(url, param, headers, "utf-8");
    }

    public static String doGet(String url, Map<String, String> param) {
        return doGet(url, param, "utf-8");
    }

    public static String doGet(String url) {
        return doGet(url, null);
    }

    public static String doAutoGet(String url, String referer, String charset, String proxyHost, Integer proxyPort) {
        Map<String, String> header = new HashMap<>();
        header.put("User-Agent", pcUserAgentArray[new Random().nextInt(pcUserAgentArray.length)]);
        header.put("Host", getHost(url));
        if (referer != null && !referer.trim().equals("")) {
            header.put("Referer", referer);
        }
        return doGet(url, null, header, charset, proxyHost, proxyPort);
    }

    public static String doAutoGet(String url, String referer, String charset) {
        return doAutoGet(url, referer, charset, null, null);
    }

    public static String doAutoGet(String url) {
        return doAutoGet(url, null, null);
    }

    public static String doPost(String url, Map<String, String> param, String json, Map<String, String> headers, String charset, String proxyHost, Integer proxyPort) {
        // 创建Http请求配置参数
        RequestConfig.Builder requestBuilder = RequestConfig.custom()
                // 获取连接超时时间
                .setConnectionRequestTimeout(2000)
                // 请求超时时间
                .setConnectTimeout(2000)
                // 响应超时时间
                .setSocketTimeout(2000);
        HttpHost httpHost = null;
        if (proxyHost != null && proxyPort != null) {
            httpHost = new HttpHost(proxyHost, proxyPort);
            requestBuilder.setProxy(httpHost);
        }


        RequestConfig requestConfig = requestBuilder.build();

        // 创建httpClient
        HttpClientBuilder httpClientBuilder = HttpClients.custom();

        httpClientBuilder
                // 把请求相关的超时信息设置到连接客户端
                .setDefaultRequestConfig(requestConfig)
                // 把请求重试设置到连接客户端
                .setRetryHandler(new RetryHandler());

        CloseableHttpClient httpClient = httpClientBuilder.build();

        HttpClientContext httpClientContext = HttpClientContext.create();
        CloseableHttpResponse response = null;
        String resultString = "";
        try {
            // 创建Http Post请求
            HttpPost httpPost = new HttpPost(url);
            //请求头
            setHeaders(headers, httpPost);
            // 设置参数
            if (json != null) {
                setJsonParam(json, httpPost);
            } else {
                setFormParam(param, httpPost);
            }
            // 执行请求
            response = httpClient.execute(httpPost);
            // 判断返回状态是否为200
            if (response.getStatusLine().getStatusCode() == 200) {
                resultString = EntityUtils.toString(response.getEntity(), charset);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeResponse(httpClient, response);
        }
        return resultString;
    }

    public static String doPost(String url, Map<String, String> param, String json, Map<String, String> headers, String charset) {
        return doPost(url, param, json, headers, charset, null, null);
    }

    public static String doPost(String url, Map<String, String> param, Map<String, String> headers, String charset) {
        return doPost(url, param, null, headers, charset);
    }

    public static String doPost(String url, Map<String, String> param, String charset) {
        return doPost(url, param, null, charset);
    }

    public static String doPost(String url, Map<String, String> param) {
        return doPost(url, param, "utf-8");
    }

    public static String doPost(String url) {
        return doPost(url, null);
    }

    public static String doPostJson(String url, String json, Map<String, String> headers, String charset) {
        return doPost(url, null, json, headers, charset);
    }

    public static String doPostJson(String url, String json, Map<String, String> headers) {
        return doPost(url, null, json, headers, "utf-8");
    }

    public static String cookieContentByGet(String url, String userAgent, String referer, String cookie) throws IOException {
        List<String> cookies = cookiesByGet(url, userAgent, referer, cookie);
        String cookieContent = "";
        for (String c : cookies) {
            cookieContent = cookieContent + c;
        }
        if (cookieContent != null && !cookieContent.trim().equals("")) {
            return cookieContent;
        } else {
            return null;
        }
    }

    public static List<String> cookiesByGet(String url, String userAgent, String referer, String cookie) throws IOException {
        HttpClientBuilder builder = HttpClients.custom();
        CloseableHttpClient client = builder.build();
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Host", getHost(url));
        if (referer != null) {
            httpGet.addHeader("Referer", referer);
        }
        httpGet.addHeader("User-Agent", userAgent);
        if (cookie != null) {
            httpGet.addHeader("Cookie", cookie);
        }

        List<String> resultList = new LinkedList<>();
        CloseableHttpResponse response = client.execute(httpGet);
        for (Header header : response.getAllHeaders()) {
            if ("Set-Cookie".equals(header.getName())) {
                HeaderElement[] elements = header.getElements();
                resultList.add(elements[0].toString().split(";")[0]);
            }
        }
        if (resultList != null && resultList.size() > 0) {
            return resultList;
        } else {
            return null;
        }
    }

    public static String getHost(String url) {
        if (url == null || url.trim().equals("")) {
            return "";
        }
        String host = "";
        Pattern p = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+");
        Matcher matcher = p.matcher(url);
        if (matcher.find()) {
            host = matcher.group();
        }
        return host;
    }

    public static String ramdonUA() {

        return pcUserAgentArray[new Random().nextInt(pcUserAgentArray.length)];
    }

    /**
     * 关闭响应
     *
     * @param httpClient
     * @param response
     */
    private static void closeResponse(CloseableHttpClient httpClient, CloseableHttpResponse response) {
        try {
            if (response != null) {
                response.close();
            }
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置json参数
     *
     * @param json
     * @param httpPost
     */
    private static void setJsonParam(String json, HttpPost httpPost) {
        StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
    }

    /**
     * 设置表单参数 主要给post使用
     *
     * @param param
     * @param httpPost
     * @throws UnsupportedEncodingException
     */
    private static void setFormParam(Map<String, String> param, HttpPost httpPost) throws UnsupportedEncodingException {
        if (param != null) {
            List<NameValuePair> paramList = new ArrayList<>();
            for (String key : param.keySet()) {
                paramList.add(new BasicNameValuePair(key, param.get(key)));
            }
            // 模拟表单
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(paramList);
            httpPost.setEntity(entity);
        }
    }

    /**
     * 设置头信息
     *
     * @param headers
     * @param httpPost
     */
    private static void setHeaders(Map<String, String> headers, HttpRequestBase httpPost) {
        if (headers != null) {
            Iterator<Entry<String, String>> it = headers.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, String> entry = it.next();
                httpPost.setHeader(entry.getKey(), entry.getValue());
            }
        }
    }

}


class RetryHandler implements HttpRequestRetryHandler {

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {

        if (executionCount >= 3) {// 如果已经重试了3次，就放弃
            return false;
        }

        if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
            return true;
        }

        if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
            return false;
        }

        if (exception instanceof InterruptedIOException) {// 超时
            return true;
        }

        if (exception instanceof UnknownHostException) {// 目标服务器不可达
            return false;
        }

        if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
            return false;
        }

        if (exception instanceof SSLException) {// ssl握手异常
            return false;
        }

        HttpClientContext clientContext = HttpClientContext.adapt(context);
        HttpRequest request = clientContext.getRequest();

        // 如果请求是幂等的，就再次尝试
        if (!(request instanceof HttpEntityEnclosingRequest)) {
            return true;
        }
        return false;
    }
}
