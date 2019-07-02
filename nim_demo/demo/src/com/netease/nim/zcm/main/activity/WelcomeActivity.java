package com.netease.nim.zcm.main.activity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.netease.nim.avchatkit.activity.AVChatActivity;
import com.netease.nim.avchatkit.constant.AVChatExtras;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.dialog.DialogMaker;
import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nim.zcm.DemoCache;
import com.netease.nim.zcm.R;
import com.netease.nim.zcm.common.util.sys.SysInfoUtil;
import com.netease.nim.zcm.config.preference.Preferences;
import com.netease.nim.zcm.contact.ContactHttpClient;
import com.netease.nim.zcm.login.LoginActivity;
import com.netease.nim.zcm.main.model.VersionResponse;
import com.netease.nim.zcm.mixpush.DemoMixPushMessageHandler;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.NimIntent;
import com.netease.nimlib.sdk.mixpush.MixPushService;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;

import java.util.ArrayList;
import java.util.Map;

/**
 * 欢迎/导航页（app启动Activity）
 * <p/>
 * Created by huangjun on 2015/2/1.
 */
public class WelcomeActivity extends UI {

    private static final String TAG = "WelcomeActivity";

    private boolean customSplash = false;
    private String versionName;
    private String downloadUrl;
    private static boolean firstEnter = true; // 是否首次进入

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        DemoCache.setMainTaskLaunching(true);

        if (savedInstanceState != null) {
            setIntent(new Intent()); // 从堆栈恢复，不再重复解析之前的intent
        }

        if (!firstEnter) {
            onIntent(); // APP进程还在，Activity被重新调度起来
        } else {
            showSplashView(); // APP进程重新起来
        }
        //获取版本
        versionName = getAppVersionName(this);
    }
    //获取App版本
    public static String getAppVersionName(Context context) {
        String versionName = "";

        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;

            if (TextUtils.isEmpty(versionName)) {
                return "";
            }
        } catch (Exception e) {
            Log.e("tag", e.getMessage());
        }

        return versionName;
    }
    private void showSplashView() {
        // 首次进入，打开欢迎界面
        getWindow().setBackgroundDrawableResource(R.drawable.splash_bg);
        customSplash = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        /*
         * 如果Activity在，不会走到onCreate，而是onNewIntent，这时候需要setIntent
         * 场景：点击通知栏跳转到此，会收到Intent
         */
        setIntent(intent);
        if (!customSplash) {
            onIntent();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (firstEnter) {
            firstEnter = false;
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (!NimUIKit.isInitComplete()) {
                        LogUtil.i(TAG, "wait for uikit cache!");
                        new Handler().postDelayed(this, 100);
                        return;
                    }

                    customSplash = false;
                    if (canAutoLogin()) {

                        ContactHttpClient.getInstance().getNewVersion(  new ContactHttpClient.ContactHttpCallback<VersionResponse>() {
                            @Override
                            public void onSuccess(VersionResponse versionResponse) {
                                Log.e("登录","登录:"+versionResponse.toString());
                                if(versionResponse.getData().getVersion()!=null&&versionResponse.getData().getVersion()!=""){
                                    double newVersionName = Double.parseDouble(versionResponse.getData().getVersion());
                                    if (newVersionName > Double.parseDouble(versionName)) {
                                        downloadUrl = versionResponse.getData().getUrl();
                                        showUpdataDialog();
                                    }else{
                                        onIntent();
                                    }
                                }

                                DialogMaker.dismissProgressDialog();
                            }

                            @SuppressLint("StringFormatInvalid")
                            @Override
                            public void onFailed(int code, String errorMsg) {
//                                ToastHelper.showToast(WelcomeActivity.this, getString(R.string.login_failed, String.valueOf(code), errorMsg));
                                DialogMaker.dismissProgressDialog();
                            }
                        });

                    } else {
                        LoginActivity.start(WelcomeActivity.this);
                        finish();
                    }
                }
            };
            if (customSplash) {
                new Handler().postDelayed(runnable, 1000);
            } else {
                runnable.run();
            }
        }
    }
    /**
     * 弹出对话框
     */
    protected void showUpdataDialog() {
        AlertDialog.Builder builer = new AlertDialog.Builder(this,R.style.Theme_AppCompat_Light_Dialog_Alert);
        builer.setTitle("版本升级");
        builer.setMessage("软件更新");
        //调用这个方法时，按对话框以外的地方和按返回键都无法响应。
        builer.setCancelable(false);
        //当点确定按钮时从服务器上下载 新的apk 然后安装
        builer.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
//                downLoadApk();
                Uri uri = Uri.parse(downloadUrl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
                dialog.cancel();
            }
        });
//        //当点取消按钮时不做任何举动
//        builer.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
//            }
//        });
        AlertDialog dialog = builer.create();
        if(!isFinishing()) {
            dialog.show();
        }
    }
    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DemoCache.setMainTaskLaunching(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    // 处理收到的Intent
    private void onIntent() {
        LogUtil.i(TAG, "onIntent...");

        if (TextUtils.isEmpty(DemoCache.getAccount())) {
            // 判断当前app是否正在运行
            if (!SysInfoUtil.stackResumed(this)) {
                LoginActivity.start(this);
            }
            finish();
        } else {
            // 已经登录过了，处理过来的请求xmd
            ContactHttpClient.getInstance().getNewVersion(  new ContactHttpClient.ContactHttpCallback<VersionResponse>() {
                @Override
                public void onSuccess(VersionResponse versionResponse) {
                    Log.e("登录","登录:"+versionResponse.toString());
                    if(versionResponse.getData().getVersion()!=null&&versionResponse.getData().getVersion()!=""){
                        double newVersionName = Double.parseDouble(versionResponse.getData().getVersion());
                        if (newVersionName > Double.parseDouble(versionName)) {
                            downloadUrl = versionResponse.getData().getUrl();
                            showUpdataDialog();
                        }else{
                            Intent intent = getIntent();
                            if (intent != null) {
                                if (intent.hasExtra(NimIntent.EXTRA_NOTIFY_CONTENT)) {
                                    parseNotifyIntent(intent);
                                    return;
                                } else if (NIMClient.getService(MixPushService.class).isFCMIntent(intent)) {
                                    parseFCMNotifyIntent(NIMClient.getService(MixPushService.class).parseFCMPayload(intent));
                                } else if (intent.hasExtra(AVChatExtras.EXTRA_FROM_NOTIFICATION) || intent.hasExtra(AVChatActivity.INTENT_ACTION_AVCHAT)) {
                                    parseNormalIntent(intent);
                                }
                            }

                            if (!firstEnter && intent == null) {
                                finish();
                            } else {
                                showMainActivity();
                            }
                        }

                    }
                    DialogMaker.dismissProgressDialog();

                }

                @SuppressLint("StringFormatInvalid")
                @Override
                public void onFailed(int code, String errorMsg) {
//                                ToastHelper.showToast(WelcomeActivity.this, getString(R.string.login_failed, String.valueOf(code), errorMsg));
                    DialogMaker.dismissProgressDialog();
                }
            });

        }
    }

    /**
     * 已经登陆过，自动登陆
     */
    private boolean canAutoLogin() {
        String account = Preferences.getUserAccount();
        String token = Preferences.getUserToken();

        Log.i(TAG, "get local sdk token =" + token);
        return !TextUtils.isEmpty(account) && !TextUtils.isEmpty(token);
    }

    private void parseNotifyIntent(Intent intent) {
        ArrayList<IMMessage> messages = (ArrayList<IMMessage>) intent.getSerializableExtra(NimIntent.EXTRA_NOTIFY_CONTENT);
        if (messages == null || messages.size() > 1) {
            showMainActivity(null);
        } else {
            showMainActivity(new Intent().putExtra(NimIntent.EXTRA_NOTIFY_CONTENT, messages.get(0)));
        }
    }

    private void parseFCMNotifyIntent(String payloadString) {
        Map<String, String> payload = JSON.parseObject(payloadString, Map.class);
        String sessionId = payload.get(DemoMixPushMessageHandler.PAYLOAD_SESSION_ID);
        String type = payload.get(DemoMixPushMessageHandler.PAYLOAD_SESSION_TYPE);
        if (sessionId != null && type != null) {
            int typeValue = Integer.valueOf(type);
            IMMessage message = MessageBuilder.createEmptyMessage(sessionId, SessionTypeEnum.typeOfValue(typeValue), 0);
            showMainActivity(new Intent().putExtra(NimIntent.EXTRA_NOTIFY_CONTENT, message));
        } else {
            showMainActivity(null);
        }
    }

    private void parseNormalIntent(Intent intent) {
        showMainActivity(intent);
    }

    private void showMainActivity() {
        showMainActivity(null);
    }

    private void showMainActivity(Intent intent) {
        MainActivity.start(WelcomeActivity.this, intent);
        finish();
    }


}
