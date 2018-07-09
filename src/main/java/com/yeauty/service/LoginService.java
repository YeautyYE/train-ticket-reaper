package com.yeauty.service;

import java.util.Map;

/**
 * @author Yeauty
 * @version 1.0
 * @Description:TODO
 * @date 2018/4/7 11:44
 */
public interface LoginService {

    Map<String,String> login(String account, String password);
}
