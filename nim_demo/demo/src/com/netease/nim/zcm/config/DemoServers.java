package com.netease.nim.zcm.config;

public class DemoServers {

    //
    // 好友列表信息服务器地址
    //
    private static final String API_SERVER_TEST = "http://apptest.netease.im/api/"; // 测试
    private static final String API_SERVER = "https://app.netease.im/api/"; // 线上

    public static String apiServer() {
        return  "http://im.hjtgn.cn/api/";
        //测试用
//        return  "http://47.106.33.13:8888/api/";
    }


    public static String chatRoomAPIServer() {
        return apiServer() + "chatroom/";
    }
}
