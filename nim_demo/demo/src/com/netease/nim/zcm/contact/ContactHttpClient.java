package com.netease.nim.zcm.contact;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.netease.nim.uikit.common.http.NimHttpClient;
import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nim.uikit.common.util.string.MD5;
import com.netease.nim.zcm.DemoCache;
import com.netease.nim.zcm.config.DemoServers;
import com.netease.nim.zcm.main.model.LoginResponse;
import com.netease.nim.zcm.main.model.VersionResponse;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * 通讯录数据获取协议的实现
 * <p/>
 * Created by huangjun on 2015/3/6.
 */
public class ContactHttpClient {
    private static final String TAG = "ContactHttpClient";

    // code
    private static final int RESULT_CODE_SUCCESS = 200;

    // api
    private static final String API_NAME_REGISTER = "createDemoUser";
    private static final String API_NAME_LOGIN = "login";
    private static final String API_NAME_GETVERSION= "getVersion";
    // header
    private static final String HEADER_KEY_APP_KEY = "appkey";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_USER_AGENT = "User-Agent";

    // request
    private static final String REQUEST_USER_NAME = "username";
    private static final String REQUEST_NICK_NAME = "nickname";
    private static final String REQUEST_PASSWORD = "password";

    // result
    private static final String RESULT_KEY_RES = "res";
    private static final String RESULT_KEY_ERROR_MSG = "errmsg";


    public interface ContactHttpCallback<T> {
        void onSuccess(T t);

        void onFailed(int code, String errorMsg);
    }

    private static ContactHttpClient instance;

    public static synchronized ContactHttpClient getInstance() {
        if (instance == null) {
            instance = new ContactHttpClient();
        }

        return instance;
    }

    private ContactHttpClient() {
        NimHttpClient.getInstance().init(DemoCache.getContext());
    }


    /**
     * 向应用服务器创建账号（注册账号）
     * 由应用服务器调用WEB SDK接口将新注册的用户数据同步到云信服务器
     */
    public void register(String account, String nickName, String password, final ContactHttpCallback<Void> callback) {
        String url = DemoServers.apiServer() + API_NAME_REGISTER;
        Log.e("url", "url:" + url);
        password = MD5.getStringMD5(password);
        try {
            nickName = URLEncoder.encode(nickName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Map<String, String> headers = new HashMap<>(1);
        String appKey = readAppKey();
        headers.put(HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        headers.put(HEADER_USER_AGENT, "nim_demo_android");
        headers.put(HEADER_KEY_APP_KEY, appKey);

        StringBuilder body = new StringBuilder();
        body.append(REQUEST_USER_NAME).append("=").append(account.toLowerCase()).append("&")
                .append(REQUEST_NICK_NAME).append("=").append(nickName).append("&")
                .append(REQUEST_PASSWORD).append("=").append(password);
        String bodyString = body.toString();

        NimHttpClient.getInstance().execute(url, headers, bodyString, new NimHttpClient.NimHttpCallback() {
            @Override
            public void onResponse(String response, int code, Throwable exception) {
                LogUtil.e(TAG, " : xmnd reg = " + response.toString());
                if (code != 200 || exception != null) {
                    String errMsg = exception != null ? exception.getMessage() : "null";
                    LogUtil.e(TAG, "register failed : code = " + code + ", errorMsg = " + errMsg);

                    if (callback != null) {
                        callback.onFailed(code, errMsg);
                    }
                    return;
                }

                try {
                    JSONObject resObj = JSONObject.parseObject(response);
                    int resCode = resObj.getIntValue(RESULT_KEY_RES);
                    if (resCode == RESULT_CODE_SUCCESS) {
                        callback.onSuccess(null);
                    } else {
                        String error = resObj.getString(RESULT_KEY_ERROR_MSG);
                        callback.onFailed(resCode, error);
                    }
                } catch (JSONException e) {
                    callback.onFailed(-1, e.getMessage());
                }
            }
        });
    }

    /**
     * 向服务器发起登录请求
     **/
    public void login(String nickName, String password,String version, final ContactHttpCallback<LoginResponse> callback) {
        String url = DemoServers.apiServer() + API_NAME_LOGIN;
        Log.e("url", "url:" + url);
        password = MD5.getStringMD5(password);
        try {
            nickName = URLEncoder.encode(nickName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        Map<String, String> headers = new HashMap<>(1);
        String appKey = readAppKey();
        headers.put(HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        headers.put(HEADER_USER_AGENT, "nim_demo_android");
        headers.put(HEADER_KEY_APP_KEY, appKey);

        StringBuilder body = new StringBuilder();
        body.append(REQUEST_USER_NAME).append("=").append(nickName).append("&")
                .append(REQUEST_PASSWORD).append("=").append(password).append("&").append("platform").append("=").append("android").append("&").append("version").append("=").append(version);
        String bodyString = body.toString();
        NimHttpClient.getInstance().execute(url, headers, bodyString, new NimHttpClient.NimHttpCallback() {
            @Override
            public void onResponse(String response, int code, Throwable exception) {
                try {

                    if (code != 200 || exception != null) {
                        String errMsg = exception != null ? exception.getMessage() : "null";

                        if (callback != null) {
                            callback.onFailed(code, errMsg);
                        }
                        return;
                    }
                    Gson gson = new Gson();
                    LoginResponse loginresponse = gson.fromJson(response, LoginResponse.class);
                    LogUtil.e(TAG, " : xmnd = " + loginresponse.toString());

                    JSONObject resObj = JSONObject.parseObject(response);
                    int resCode = resObj.getIntValue(RESULT_KEY_RES);
                    if (resCode == RESULT_CODE_SUCCESS||resCode==300) {
                        callback.onSuccess(loginresponse);
                    } else {
                        String error = resObj.getString("msg");
                        callback.onFailed(resCode, error);
                    }
                } catch (JSONException e) {
                    callback.onFailed(-1, e.getMessage());
                }
            }
        });
    }

    /**
     * 向服务器发起获取版本号请求
     **/
    public void getNewVersion( final ContactHttpCallback<VersionResponse> callback) {
        String url = DemoServers.apiServer() + API_NAME_GETVERSION;
        Log.e("url", "url:" + url);


        Map<String, String> headers = new HashMap<>(1);
        String appKey = readAppKey();
        headers.put(HEADER_CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        headers.put(HEADER_USER_AGENT, "nim_demo_android");
        headers.put(HEADER_KEY_APP_KEY, appKey);

        StringBuilder body = new StringBuilder();
        body.append("platform").append("=").append("android");
        String bodyString = body.toString();
        LogUtil.e(TAG, "  body:" + body);
        NimHttpClient.getInstance().execute(url, headers, bodyString, new NimHttpClient.NimHttpCallback() {
            @Override
            public void onResponse(String response, int code, Throwable exception) {
                try {

                    if (code !=200 || exception != null) {
                        String errMsg = exception != null ? exception.getMessage() : "null";

                        if (callback != null) {
                            callback.onFailed(code, errMsg);
                        }
                        return;
                    }
                    //xmd解析获取版本错误修改
                    Gson gson = new Gson();
                    VersionResponse versionResponse = gson.fromJson(response, VersionResponse.class);

                    JSONObject resObj = JSONObject.parseObject(response);
                    int resCode = resObj.getIntValue("code");
                    if (resCode == 0) {
                        callback.onSuccess(versionResponse);
                    } else {
                        String error = resObj.getString("msg");
                        callback.onFailed(resCode, error);
                    }
                } catch (JSONException e) {
                    callback.onFailed(-1, e.getMessage());
                }catch (Exception e) {
                    callback.onFailed(-1, e.getMessage());
                }
            }
        });
    }
    private String readAppKey() {
        try {
            ApplicationInfo appInfo = DemoCache.getContext().
                    getPackageManager().
                    getApplicationInfo(DemoCache.getContext().getPackageName(), PackageManager.GET_META_DATA);
            if (appInfo != null) {
                return appInfo.metaData.getString("com.netease.nim.appKey");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
