package com.yeauty.service.impl;

import com.yeauty.service.LoginService;
import com.yeauty.util.HttpClientUtils;
import com.yeauty.util.JsonUtils;
import com.yeauty.util.MD5Utils;
import org.springframework.stereotype.Service;
import sun.java2d.pipe.SpanIterator;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @author Yeauty
 * @version 1.0
 * @Description:TODO
 * @date 2018/4/7 11:44
 */
@Service
public class LoginServiceImpl implements LoginService {
    @Override
    public Map<String, String> login(String account, String password) {
        Map headers = new HashMap<>();
        headers.put("Accept","application/json, text/plain");
        headers.put("Accept-Encoding","gzip, deflate");
        headers.put("Accept-Language","zh-CN");
        headers.put("Connection","keep-alive");
        headers.put("Content-Type","application/json");
        headers.put("Host","api.12306.com");
        headers.put("Origin","http://www.12306.com");
        headers.put("Referer","http://www.12306.com/");
        headers.put("User-Agent", HttpClientUtils.pcUserAgentArray[new Random().nextInt(HttpClientUtils.pcUserAgentArray.length)]);

        String json = HttpClientUtils.doPostJson("http://api.12306.com/oauth/token?client_id=client&client_secret=secret&grant_type=password&password="+ MD5Utils.md5(password)+"&username="+account, "{}", headers);
        return JsonUtils.jsonToMap(json,String.class,String.class);
    }
}
