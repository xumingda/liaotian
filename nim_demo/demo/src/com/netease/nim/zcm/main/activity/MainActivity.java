package com.netease.nim.zcm.main.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.netease.nim.avchatkit.AVChatProfile;
import com.netease.nim.avchatkit.activity.AVChatActivity;
import com.netease.nim.avchatkit.constant.AVChatExtras;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.api.model.main.LoginSyncDataStatusObserver;
import com.netease.nim.uikit.business.contact.selector.activity.ContactSelectActivity;
import com.netease.nim.uikit.business.team.helper.TeamHelper;
import com.netease.nim.uikit.common.ToastHelper;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.dialog.DialogMaker;
import com.netease.nim.uikit.common.ui.drop.DropCover;
import com.netease.nim.uikit.common.ui.drop.DropManager;
import com.netease.nim.uikit.common.util.log.LogUtil;
import com.netease.nim.uikit.support.permission.MPermission;
import com.netease.nim.uikit.support.permission.annotation.OnMPermissionDenied;
import com.netease.nim.uikit.support.permission.annotation.OnMPermissionGranted;
import com.netease.nim.uikit.support.permission.annotation.OnMPermissionNeverAskAgain;
import com.netease.nim.zcm.R;
import com.netease.nim.zcm.common.ui.viewpager.FadeInOutPageTransformer;
import com.netease.nim.zcm.common.ui.viewpager.PagerSlidingTabStrip;
import com.netease.nim.zcm.config.preference.Preferences;
import com.netease.nim.zcm.contact.activity.AddFriendActivity;
import com.netease.nim.zcm.jspush.ExampleUtil;
import com.netease.nim.zcm.jspush.SharedPrefrenceUtils;
import com.netease.nim.zcm.jspush.TagAliasOperatorHelper;
import com.netease.nim.zcm.login.LoginActivity;
import com.netease.nim.zcm.login.LogoutHelper;
import com.netease.nim.zcm.main.adapter.MainTabPagerAdapter;
import com.netease.nim.zcm.main.helper.SystemMessageUnreadManager;
import com.netease.nim.zcm.main.model.MainTab;
import com.netease.nim.zcm.main.reminder.ReminderItem;
import com.netease.nim.zcm.main.reminder.ReminderManager;
import com.netease.nim.zcm.session.SessionHelper;
import com.netease.nim.zcm.team.TeamCreateHelper;
import com.netease.nim.zcm.team.activity.AdvancedTeamSearchActivity;
import com.netease.nimlib.sdk.NIMClient;
import com.netease.nimlib.sdk.NimIntent;
import com.netease.nimlib.sdk.Observer;
import com.netease.nimlib.sdk.RequestCallback;
import com.netease.nimlib.sdk.ResponseCode;
import com.netease.nimlib.sdk.msg.MessageBuilder;
import com.netease.nimlib.sdk.msg.MsgService;
import com.netease.nimlib.sdk.msg.SystemMessageObserver;
import com.netease.nimlib.sdk.msg.SystemMessageService;
import com.netease.nimlib.sdk.msg.constant.SessionTypeEnum;
import com.netease.nimlib.sdk.msg.model.IMMessage;
import com.netease.nimlib.sdk.msg.model.RecentContact;
import com.netease.nimlib.sdk.team.TeamService;
import com.netease.nimlib.sdk.team.model.Team;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import static com.netease.nim.zcm.jspush.TagAliasOperatorHelper.ACTION_SET;
import static com.netease.nim.zcm.jspush.TagAliasOperatorHelper.TagAliasBean;
import static com.netease.nim.zcm.jspush.TagAliasOperatorHelper.sequence;

/**
 * 主界面
 * Created by huangjun on 2015/3/25.
 */
public class MainActivity extends UI implements ViewPager.OnPageChangeListener, ReminderManager.UnreadNumChangedCallback {
    public static boolean isForeground = false;
    private static final String EXTRA_APP_QUIT = "APP_QUIT";
    private static final int REQUEST_CODE_NORMAL = 1;
    private static final int REQUEST_CODE_ADVANCED = 2;
    private static final int REQUEST_CODE_MASS = 3;
    private static final int REQUEST_CODE_TEAM = 4;
    private static final int BASIC_PERMISSION_REQUEST_CODE = 100;
    private static final String[] BASIC_PERMISSIONS = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
    };


    private PagerSlidingTabStrip tabs;
    private ViewPager pager;
    private int scrollState;
    private MainTabPagerAdapter adapter;


    private boolean isFirstIn;
    private Observer<Integer> sysMsgUnreadCountChangedObserver = new Observer<Integer>() {
        @Override
        public void onEvent(Integer unreadCount) {
            SystemMessageUnreadManager.getInstance().setSysMsgUnreadCount(unreadCount);
            ReminderManager.getInstance().updateContactUnreadNum(unreadCount);
        }
    };


    public static void start(Context context) {
        start(context, null);
    }

    public static void start(Context context, Intent extras) {
        Intent intent = new Intent();
        intent.setClass(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (extras != null) {
            intent.putExtras(extras);
        }
        context.startActivity(intent);
    }

    // 注销
    public static void logout(Context context, boolean quit) {
        Intent extra = new Intent();
        extra.putExtra(EXTRA_APP_QUIT, quit);
        start(context, extra);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setToolBar(R.id.toolbar, R.string.app_name, R.drawable.actionbar_dark_logo);
        setTitle(R.string.app_name);
        isFirstIn = true;

        //不保留后台活动，从厂商推送进聊天页面，会无法退出聊天页面
        if (savedInstanceState == null && parseIntent()) {
            return;
        }
        init();
        registerMessageReceiver();  // used for receive msg
//        if(!SharedPrefrenceUtils.getBoolean(this,"isSetAlias")){
        //每次都设置别名
            setTagAndAlias();
//        }


    }

    private void init() {
        observerSyncDataComplete();
        findViews();
        setupPager();
        setupTabs();
        registerMsgUnreadInfoObserver(true);
        registerSystemMessageObservers(true);
        requestSystemMessageUnreadCount();
        initUnreadCover();
        requestBasicPermission();
    }

    private boolean parseIntent() {

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_APP_QUIT)) {
            intent.removeExtra(EXTRA_APP_QUIT);
            onLogout();
            return true;
        }

        if (intent.hasExtra(NimIntent.EXTRA_NOTIFY_CONTENT)) {
            IMMessage message = (IMMessage) intent.getSerializableExtra(NimIntent.EXTRA_NOTIFY_CONTENT);
            intent.removeExtra(NimIntent.EXTRA_NOTIFY_CONTENT);
            switch (message.getSessionType()) {
                case P2P:
                    SessionHelper.startP2PSession(this, message.getSessionId());
                    break;
                case Team:
                    SessionHelper.startTeamSession(this, message.getSessionId());
                    break;
            }

            return true;
        }

        if (intent.hasExtra(AVChatActivity.INTENT_ACTION_AVCHAT) && AVChatProfile.getInstance().isAVChatting()) {
            intent.removeExtra(AVChatActivity.INTENT_ACTION_AVCHAT);
            Intent localIntent = new Intent();
            localIntent.setClass(this, AVChatActivity.class);
            startActivity(localIntent);
            return true;
        }

        String account = intent.getStringExtra(AVChatExtras.EXTRA_ACCOUNT);
        if (intent.hasExtra(AVChatExtras.EXTRA_FROM_NOTIFICATION) && !TextUtils.isEmpty(account)) {
            intent.removeExtra(AVChatExtras.EXTRA_FROM_NOTIFICATION);
            SessionHelper.startP2PSession(this, account);
            return true;
        }

        return false;
    }

    private void observerSyncDataComplete() {
        boolean syncCompleted = LoginSyncDataStatusObserver.getInstance().observeSyncDataCompletedEvent(new Observer<Void>() {
            @Override
            public void onEvent(Void v) {
                DialogMaker.dismissProgressDialog();
            }
        });
        //如果数据没有同步完成，弹个进度Dialog
        if (!syncCompleted) {
            DialogMaker.showProgressDialog(MainActivity.this, getString(R.string.prepare_data)).setCanceledOnTouchOutside(false);
        }
    }

    private void findViews() {
        tabs = findView(R.id.tabs);
        pager = findView(R.id.main_tab_pager);
    }

    private void setupPager() {
        adapter = new MainTabPagerAdapter(getSupportFragmentManager(), this, pager);
        pager.setOffscreenPageLimit(adapter.getCacheCount());
        pager.setPageTransformer(true, new FadeInOutPageTransformer());
        pager.setAdapter(adapter);
        pager.setOnPageChangeListener(this);
    }

    private void setupTabs() {
        tabs.setOnCustomTabListener(new PagerSlidingTabStrip.OnCustomTabListener() {
            @Override
            public int getTabLayoutResId(int position) {
                return R.layout.tab_layout_main;
            }

            @Override
            public boolean screenAdaptation() {
                return true;
            }
        });
        tabs.setViewPager(pager);
        tabs.setOnTabClickListener(adapter);
        tabs.setOnTabDoubleTapListener(adapter);
    }


    /**
     * 注册未读消息数量观察者
     */
    private void registerMsgUnreadInfoObserver(boolean register) {
        if (register) {
            ReminderManager.getInstance().registerUnreadNumChangedCallback(this);
        } else {
            ReminderManager.getInstance().unregisterUnreadNumChangedCallback(this);
        }
    }

    /**
     * 注册/注销系统消息未读数变化
     */
    private void registerSystemMessageObservers(boolean register) {
        NIMClient.getService(SystemMessageObserver.class).observeUnreadCountChange(sysMsgUnreadCountChangedObserver, register);
    }

    /**
     * 查询系统消息未读数
     */
    private void requestSystemMessageUnreadCount() {
        int unread = NIMClient.getService(SystemMessageService.class).querySystemMessageUnreadCountBlock();
        SystemMessageUnreadManager.getInstance().setSysMsgUnreadCount(unread);
        ReminderManager.getInstance().updateContactUnreadNum(unread);
    }

    //初始化未读红点动画
    private void initUnreadCover() {
        DropManager.getInstance().init(this, (DropCover) findView(R.id.unread_cover),
                new DropCover.IDropCompletedListener() {
                    @Override
                    public void onCompleted(Object id, boolean explosive) {
                        if (id == null || !explosive) {
                            return;
                        }

                        if (id instanceof RecentContact) {
                            RecentContact r = (RecentContact) id;
                            NIMClient.getService(MsgService.class).clearUnreadCount(r.getContactId(), r.getSessionType());
                            return;
                        }

                        if (id instanceof String) {
                            if (((String) id).contentEquals("0")) {
                                NIMClient.getService(MsgService.class).clearAllUnreadCount();
                            } else if (((String) id).contentEquals("1")) {
                                NIMClient.getService(SystemMessageService.class).resetSystemMessageUnreadCount();
                            }
                        }
                    }
                });
    }






    private void requestBasicPermission() {
        MPermission.printMPermissionResult(true, this, BASIC_PERMISSIONS);
        MPermission.with(MainActivity.this)
                .setRequestCode(BASIC_PERMISSION_REQUEST_CODE)
                .permissions(BASIC_PERMISSIONS)
                .request();
    }

    private void onLogout() {
        Preferences.saveUserToken("");
        // 清理缓存&注销监听
        LogoutHelper.logout();
        // 启动登录
        LoginActivity.start(this);
        finish();
    }

    private void selectPage() {
        if (scrollState == ViewPager.SCROLL_STATE_IDLE) {
            adapter.onPageSelected(pager.getCurrentItem());
        }
    }

    /**
     * 设置最近联系人的消息为已读
     * <p>
     * account, 聊天对象帐号，或者以下两个值：
     * {@link MsgService#MSG_CHATTING_ACCOUNT_ALL} 目前没有与任何人对话，但能看到消息提醒（比如在消息列表界面），不需要在状态栏做消息通知
     * {@link MsgService#MSG_CHATTING_ACCOUNT_NONE} 目前没有与任何人对话，需要状态栏消息通知
     */
    private void enableMsgNotification(boolean enable) {
        boolean msg = (pager.getCurrentItem() != MainTab.RECENT_CONTACTS.tabIndex);
        if (enable | msg) {
            NIMClient.getService(MsgService.class).setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_NONE, SessionTypeEnum.None);
        } else {
            NIMClient.getService(MsgService.class).setChattingAccount(MsgService.MSG_CHATTING_ACCOUNT_ALL, SessionTypeEnum.None);
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {  //这个是发送过来的消息
            if(msg.what==100){
                ContactSelectActivity.Option option = new ContactSelectActivity.Option();
                option.title = "选择好友";
                option.type = ContactSelectActivity.ContactSelectType.BUDDY;
                option.multi = true;
                option.maxSelectNum = 500;
                NimUIKit.startContactSelector(MainActivity.this, option, REQUEST_CODE_MASS);
            }else if(msg.what==101){
                ToastHelper.showToast(NimUIKit.getContext(),"您已被禁止私聊");
            }else if(msg.what==104){
                ContactSelectActivity.Option advancedOption = TeamHelper.getCreateContactSelectOption(null, 50);
                NimUIKit.startContactSelector(MainActivity.this, advancedOption, REQUEST_CODE_ADVANCED);
            }
            else if(msg.what==105){
                ToastHelper.showToast(MainActivity.this, R.string.create_exception);
            }
            else{

                String text=(String) msg.obj;
                LogUtil.e("投诉返回","投诉返回:"+text);
                ToastHelper.showToast(NimUIKit.getContext(),text);
            }
        };
    };
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_menu, menu);
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
//            case R.id.create_normal_team:
//                //xmd判断是否有权限
//                new Thread(new Runnable(){
//                    @Override
//                    public void run() {
//                        if( Boolean.parseBoolean(checkCreateGroupAuth( Preferences.getUserAccount()))){
//                            ContactSelectActivity.Option option = TeamHelper.getCreateContactSelectOption(null, 50);
//                            NimUIKit.startContactSelector(MainActivity.this, option, REQUEST_CODE_NORMAL);
//                        }else{
//                            ToastHelper.showToast(MainActivity.this, R.string.create_exception);
//                        }
//
//                    }
//                }).start();
//
//
//
//
//                break;
            case R.id.saoyisao:
                //xmd扫一扫
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivityForResult(intent, 100);

                break;
            case R.id.create_regular_team:
                //xmd判断是否有权限
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        if( Boolean.parseBoolean(checkCreateGroupAuth( Preferences.getUserAccount()))){
                            handler.sendEmptyMessage(104);

                        }else{
                            handler.sendEmptyMessage(105);

                        }

                    }
                }).start();

                break;
                //群发群
            case R.id.mass_team_texting: {
//                ContactSelectActivity.Option massOption = TeamHelper.getCreateContactSelectOption(null, 500);
//                NimUIKit.startContactSelector(MainActivity.this, massOption, REQUEST_CODE_MASS);

                ContactSelectActivity.Option option = new ContactSelectActivity.Option();
                option.title = "选择群";
                option.showContactSelectArea=true;
                option.type = ContactSelectActivity.ContactSelectType.All;
                option.multi = true;
                option.maxSelectNum = 500;
                NimUIKit.startContactSelector(MainActivity.this, option, REQUEST_CODE_TEAM);
                break;
            }
            //群发个人
            case R.id.mass_people_texting: {
//                ContactSelectActivity.Option massOption = TeamHelper.getCreateContactSelectOption(null, 500);
//                NimUIKit.startContactSelector(MainActivity.this, massOption, REQUEST_CODE_MASS);
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        if( !Boolean.parseBoolean(checkForbidChat( NimUIKit.getAccount()))){
                            handler.sendEmptyMessage(100);
                        }else{
                            handler.sendEmptyMessage(101);

                        }

                    }
                }).start();

                break;
            }
            case R.id.search_advanced_team:
                AdvancedTeamSearchActivity.start(MainActivity.this);
                break;
            case R.id.add_buddy:
                AddFriendActivity.start(MainActivity.this);
                break;
            case R.id.search_btn:
                GlobalSearchActivity.start(MainActivity.this);
                break;
                //投诉
            case R.id.complaint:{
                final EditText et = new EditText(this);
                et.setHint("请输入投诉内容");
                new AlertDialog.Builder(this).setTitle("投诉")
                        .setIcon(R.drawable.actionbar_dark_logo_icon)
                        .setView(et)
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int j) {
                               complaint(et.getText().toString());

                            }
                        }).setNegativeButton("取消",null).show();
                break;
            }
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        parseIntent();
    }

    @Override
    public void onResume() {
        super.onResume();
        isForeground = true;
        // 第一次 ， 三方通知唤起进会话页面之类的，不会走初始化过程
        boolean temp = isFirstIn;
        isFirstIn = false;
        if (pager == null && temp) {
            return;
        }
        //如果不是第一次进 ， eg: 其他页面back
        if (pager == null) {
            init();
        }
        enableMsgNotification(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    public void onPause() {
        super.onPause();
        isForeground = false;
        if (pager == null) {
            return;
        }
        enableMsgNotification(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        registerMsgUnreadInfoObserver(false);
        registerSystemMessageObservers(false);
        DropManager.getInstance().destroy();
    }
    //for receive customer msg from jpush server
    private MessageReceiver mMessageReceiver;
    public static final String MESSAGE_RECEIVED_ACTION = "com.netease.nim.zcm.MESSAGE_RECEIVED_ACTION";
    public static final String KEY_TITLE = "title";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_EXTRAS = "extras";

    public void registerMessageReceiver() {
        mMessageReceiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(MESSAGE_RECEIVED_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, filter);
    }
    /**
     * 设置标签与别名setCostomMsg
     */
    private void setTagAndAlias() {
        /**
         *这里设置了别名，在这里获取的用户登录的信息
         *并且此时已经获取了用户的userId,然后就可以用用户的userId来设置别名了*/
        String alias =NimUIKit.getAccount();
        //上下文、别名【Sting行】、标签【Set型】、回调
        TagAliasBean tagAliasBean = new TagAliasBean();
        tagAliasBean.action = ACTION_SET;
        tagAliasBean.alias = alias;
        sequence++;
        tagAliasBean.isAliasAction = true;
        TagAliasOperatorHelper.getInstance().handleAction(getApplicationContext(),sequence,tagAliasBean);
        SharedPrefrenceUtils.setBoolean(this,"isSetAlias",true);
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                if (MESSAGE_RECEIVED_ACTION.equals(intent.getAction())) {
                    String messge = intent.getStringExtra(KEY_MESSAGE);
                    String extras = intent.getStringExtra(KEY_EXTRAS);
                    StringBuilder showMsg = new StringBuilder();
                    showMsg.append(KEY_MESSAGE + " : " + messge + "\n");
                    if (!ExampleUtil.isEmpty(extras)) {
                        showMsg.append(KEY_EXTRAS + " : " + extras + "\n");
                    }
                    setCostomMsg(showMsg.toString());
                }
            } catch (Exception e){
            }
        }
    }
    //设置数据
    private void setCostomMsg(String msg){
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**
         * 处理二维码扫描结果
         */
        if (requestCode == 100) {
            //处理扫描结果（在界面上显示）
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    LogUtil.e("结果","结果"+result);
                    requestTeamInfo(result);

                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(MainActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }
        }
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == REQUEST_CODE_NORMAL) {
            final ArrayList<String> selected = data.getStringArrayListExtra(ContactSelectActivity.RESULT_DATA);
            if (selected != null && !selected.isEmpty()) {
                TeamCreateHelper.createNormalTeam(MainActivity.this, selected, false, null);
            } else {
                ToastHelper.showToast(MainActivity.this, "请选择至少一个联系人！");
            }
        } else if (requestCode == REQUEST_CODE_ADVANCED) {
            final ArrayList<String> selected = data.getStringArrayListExtra(ContactSelectActivity.RESULT_DATA);
            TeamCreateHelper.createAdvancedTeam(MainActivity.this, selected);
        }else if (requestCode == REQUEST_CODE_MASS) {
            final ArrayList<String> selected = data.getStringArrayListExtra(ContactSelectActivity.RESULT_DATA);
            final EditText et = new EditText(this);
            new AlertDialog.Builder(this).setTitle("请输入消息")
                    .setIcon(R.drawable.actionbar_dark_logo_icon)
                    .setView(et)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int j) {
                            //按下确定键后的事件
                            for(int i=0;i<selected.size();i++){
                                IMMessage textMessage =MessageBuilder.createTextMessage(selected.get(i), SessionTypeEnum.P2P , et.getText().toString());
                                NIMClient.getService(MsgService.class).sendMessage(textMessage, false);
                            }

                        }
                    }).setNegativeButton("取消",null).show();


        }else if (requestCode == REQUEST_CODE_TEAM) {
            final ArrayList<String> selected = data.getStringArrayListExtra(ContactSelectActivity.RESULT_DATA);
            final EditText et = new EditText(this);
            new AlertDialog.Builder(this).setTitle("请输入消息")
                    .setIcon(R.drawable.actionbar_dark_logo_icon)
                    .setView(et)
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int j) {
                            //按下确定键后的事件
                            for(int i=0;i<selected.size();i++){
                                IMMessage textMessage =MessageBuilder.createTextMessage(selected.get(i), SessionTypeEnum.Team , et.getText().toString());
                                NIMClient.getService(MsgService.class).sendMessage(textMessage, false);
                            }

                        }
                    }).setNegativeButton("取消",null).show();


        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        tabs.onPageScrolled(position, positionOffset, positionOffsetPixels);
        adapter.onPageScrolled(position);
    }

    @Override
    public void onPageSelected(int position) {
        tabs.onPageSelected(position);
        selectPage();
        enableMsgNotification(false);
    }

    @Override
    public void onPageScrollStateChanged(int state) {
        tabs.onPageScrollStateChanged(state);
        scrollState = state;
        selectPage();
    }

    //未读消息数量观察者实现
    @Override
    public void onUnreadNumChanged(ReminderItem item) {
        MainTab tab = MainTab.fromReminderId(item.getId());
        if (tab != null) {
            tabs.updateTab(tab.tabIndex, item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        MPermission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @OnMPermissionGranted(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionSuccess() {
        try {
//            ToastHelper.showToast(this, "授权成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
        MPermission.printMPermissionResult(false, this, BASIC_PERMISSIONS);
    }

    @OnMPermissionDenied(BASIC_PERMISSION_REQUEST_CODE)
    @OnMPermissionNeverAskAgain(BASIC_PERMISSION_REQUEST_CODE)
    public void onBasicPermissionFailed() {
        try {
            ToastHelper.showToast(this, "未全部授权，部分功能可能无法正常运行！");
        } catch (Exception e) {
            e.printStackTrace();
        }
        MPermission.printMPermissionResult(false, this, BASIC_PERMISSIONS);
    }

    @Override
    protected boolean displayHomeAsUpEnabled() {
        return false;
    }


    /**创群权限获取**/
    public static String checkCreateGroupAuth(String account){
        //get的方式提交就是url拼接的方式
        String path = "http://im.hjtgn.cn/api/checkCreateGroupAuth?account="+account;
        try {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            //获得结果码
            int responseCode = connection.getResponseCode();
            if(responseCode ==200){
                //请求成功 获得返回的流
                InputStream is = connection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int i;
                while ((i = is.read()) != -1) {
                    baos.write(i);
                }
                return baos.toString();
            }else {
                //请求失败
                return null;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**私聊权限获取**/
    public static String checkForbidChat(String account){
        //get的方式提交就是url拼接的方式
        String path = "http://im.hjtgn.cn/api/checkForbidChat?account="+account;
        try {
            URL url = new URL(path);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(5000);
            connection.setRequestMethod("GET");
            //获得结果码
            int responseCode = connection.getResponseCode();
            if(responseCode ==200){
                //请求成功 获得返回的流
                InputStream is = connection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int i;
                while ((i = is.read()) != -1) {
                    baos.write(i);
                }
                return baos.toString();
            }else {
                //请求失败
                return null;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**发起投诉**/
//    public static String complaint(String content){
//        //get的方式提交就是url拼接的方式
//        String path = " http://im.hjtgn.cn/api/complaint?account="+NimUIKit.getAccount()+"&title=投诉"+"&content="+content;
//        try {
//            URL url = new URL(path);
//            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//            connection.setConnectTimeout(5000);
//            connection.setRequestMethod("GET");
//            //获得结果码
//            int responseCode = connection.getResponseCode();
//            if(responseCode ==200){
//                //请求成功 获得返回的流
//                InputStream is = connection.getInputStream();
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                int i;
//                while ((i = is.read()) != -1) {
//                    baos.write(i);
//                }
//                return baos.toString();
//            }else {
//                //请求失败
//                return null;
//            }
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        } catch (ProtocolException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }

    private void complaint(final String content) {

        new Thread(){
            public void run() {
                try {

                    //编码，将文字编码
                    String ss= URLEncoder.encode(content, "utf-8");
                    //使用HttpURLConnection获得网络数据
                    URL url=new URL("http://im.hjtgn.cn/api/complaint?");
                    HttpURLConnection urlConnection=(HttpURLConnection) url.openConnection();
                    urlConnection.setConnectTimeout(5000);
                    urlConnection.setReadTimeout(5000);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.connect();
                    String kk="account="+NimUIKit.getAccount()+"&title=投诉&content="+content;
                    PrintWriter writer=new PrintWriter(urlConnection.getOutputStream());
                    writer.write(kk);
                    writer.flush();
                    writer.close();
                    int code=urlConnection.getResponseCode();
                    if (code==200) {
                        InputStream is = urlConnection.getInputStream();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int i;
                        while ((i = is.read()) != -1) {
                            baos.write(i);
                        }

//                        InputStream inputStream=urlConnection.getInputStream();
//                        BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream));
//                        String liner;
//                        StringBuffer buffer=new StringBuffer();
//                        while ((liner=reader.readLine())!=null) {
//
//                        }
                        String str=baos.toString();
                        //创建Message给handler发送消息
                        Message message=new Message();
                        message.what=102;
                        message.obj=str;
                        handler.sendMessage(message);
                    }
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            };
        }.start();

    }

    private void requestTeamInfo(String teamId) {
        NIMClient.getService(TeamService.class).applyJoinTeam(teamId, null).setCallback(new RequestCallback<Team>() {
            @Override
            public void onSuccess(Team team) {

                ToastHelper.showToast(MainActivity.this, "加入成功！");
            }

            @Override
            public void onFailed(int code) {
                //仅仅是申请成功
                if (code == ResponseCode.RES_TEAM_APPLY_SUCCESS) {
                    ToastHelper.showToast(MainActivity.this, "申请已发出");
                }

                else if (code == ResponseCode.RES_TEAM_ALREADY_IN) {
                    ToastHelper.showToast(MainActivity.this,"已经在群里");
                } else if (code == ResponseCode.RES_TEAM_LIMIT) {
                    ToastHelper.showToast(MainActivity.this, "群数量已达上限");
                } else {
                    ToastHelper.showToast(MainActivity.this, "failed, error code =" + code);
                }
            }

            @Override
            public void onException(Throwable exception) {

            }
        });
    }
}
