package com.netease.nim.uikit.business.ait.selector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.netease.nim.uikit.R;
import com.netease.nim.uikit.api.NimUIKit;
import com.netease.nim.uikit.api.model.SimpleCallback;
import com.netease.nim.uikit.api.wrapper.NimToolBarOptions;
import com.netease.nim.uikit.business.ait.selector.adapter.AitContactAdapter;
import com.netease.nim.uikit.business.ait.selector.model.AitContactItem;
import com.netease.nim.uikit.business.ait.selector.model.ItemType;
import com.netease.nim.uikit.business.team.helper.TeamHelper;
import com.netease.nim.uikit.common.activity.ToolBarOptions;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.recyclerview.listener.OnItemClickListener;
import com.netease.nimlib.sdk.robot.model.NimRobotInfo;
import com.netease.nimlib.sdk.team.model.Team;
import com.netease.nimlib.sdk.team.model.TeamMember;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hzchenkang on 2017/6/21.
 */

public class AitContactSelectorActivity extends UI {
    private static final String EXTRA_ID = "EXTRA_ID";
    private static final String EXTRA_ROBOT = "EXTRA_ROBOT";

    public static final int REQUEST_CODE = 0x10;
    public static final String RESULT_TYPE = "type";
    public static final String RESULT_DATA = "data";

    private AitContactAdapter adapter;

    private String teamId;

    private boolean addRobot;

    private List<AitContactItem> items;
    private List<AitContactItem> tempSource;
    private List<TeamMember> memberList;
    private EditText et_search_content;
    private Button btn;
    //输入搜到的人的账号
    private List<String> aclist;
    public static void start(Context context, String tid, boolean addRobot) {
        Intent intent = new Intent();
        if (tid != null) {
            intent.putExtra(EXTRA_ID, tid);
        }
        if (addRobot) {
            intent.putExtra(EXTRA_ROBOT, true);
        }
        intent.setClass(context, AitContactSelectorActivity.class);

        ((Activity) context).startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nim_team_member_list_layout);
        parseIntent();
        initViews();
        initData();
    }

    private void initViews() {
        btn=(Button) findViewById(R.id.btn);
        et_search_content=(EditText)findViewById(R.id.et_search_content);
        RecyclerView recyclerView = findViewById(R.id.member_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        initAdapter(recyclerView);
        ToolBarOptions options = new NimToolBarOptions();
        options.titleString = "选择提醒的人";
        setToolBar(R.id.toolbar, options);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //初始回原来的数据
                items.clear();
                items.addAll(tempSource);
                //开始搜索
                aclist=new ArrayList<>();
                String inputName=et_search_content.getText().toString();
                if(TextUtils.isEmpty(inputName)){
                    items.clear();
                    items.addAll(tempSource);
                    adapter.setNewData(items);
                }else {
                    for (int i = 0; i < memberList.size(); i++) {

                        Log.e("人数", "人数：" + memberList.get(i).getAccount() + "显示的名字：");
                        //先判断群昵称是否有,若没有跟account比对
                        if (!TextUtils.isEmpty(memberList.get(i).getTeamNick())) {
                            if (memberList.get(i).getTeamNick().indexOf(inputName) != -1 || memberList.get(i).getTeamNick().equals(inputName)) {
                                aclist.add(memberList.get(i).getAccount());
                            }
                        } else {
                            String showName = TeamHelper.getTeamMemberDisplayName(memberList.get(i).getTid(), memberList.get(i).getAccount());
                            if (showName.indexOf(inputName) != -1 || showName.equals(inputName)) {
                                aclist.add(memberList.get(i).getAccount());
                            }
                        }

                    }
                    List<AitContactItem> newSource = new ArrayList<>();
                    for (int j = 0; j < aclist.size(); j++) {
                        for (int k = 0; k < memberList.size(); k++) {
                            if (aclist.get(j).equals(memberList.get(k).getAccount())) {
                                if(k==0){
                                    newSource.add(items.get(k+1));
                                }else{
                                    newSource.add(items.get(k));
                                }

                            }
                        }
                    }
                    items.clear();
                    items.addAll(newSource);
                    adapter.setNewData(items);
                }
                Log.e("人数","人数："+items.size()+"搜到的人：");
            }
        });
    }

    private void initAdapter(RecyclerView recyclerView) {
        items = new ArrayList<>();
        tempSource=new ArrayList<>();
        adapter = new AitContactAdapter(recyclerView, items);
        recyclerView.setAdapter(adapter);

        List<Integer> noDividerViewTypes = new ArrayList<>(1);
        noDividerViewTypes.add(ItemType.SIMPLE_LABEL);
        recyclerView.addItemDecoration(new AitContactDecoration(this, LinearLayoutManager.VERTICAL, noDividerViewTypes));

        recyclerView.addOnItemTouchListener(new OnItemClickListener<AitContactAdapter>() {

            @Override
            public void onItemClick(AitContactAdapter adapter, View view, int position) {
                AitContactItem item = adapter.getItem(position);
                Intent intent = new Intent();
                intent.putExtra(RESULT_TYPE, item.getViewType());
                if (item.getViewType() == ItemType.TEAM_MEMBER) {
                    intent.putExtra(RESULT_DATA, (TeamMember) item.getModel());
                } else if (item.getViewType() == ItemType.ROBOT) {
                    intent.putExtra(RESULT_DATA, (NimRobotInfo) item.getModel());
                }
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private void parseIntent() {
        Intent intent = getIntent();
        teamId = intent.getStringExtra(EXTRA_ID);
        addRobot = intent.getBooleanExtra(EXTRA_ROBOT, false);
    }

    private void initData() {
        memberList = new ArrayList<>();
        items = new ArrayList<AitContactItem>();
        tempSource=new ArrayList<>();
        if (addRobot) {
            initRobot();
        }
        if (teamId != null) {
            initTeamMemberAsync();
        } else {
            //data 加载结束，通知更新
            adapter.setNewData(items);
        }
    }

    private void initRobot() {
        List<NimRobotInfo> robots = NimUIKit.getRobotInfoProvider().getAllRobotAccounts();
        if (robots != null && !robots.isEmpty()) {
            items.add(0, new AitContactItem(ItemType.SIMPLE_LABEL, "机器人"));
            for (NimRobotInfo robot : robots) {
                items.add(new AitContactItem(ItemType.ROBOT, robot));
            }
        }
    }

    private void initTeamMemberAsync() {
        Team t = NimUIKit.getTeamProvider().getTeamById(teamId);
        if (t != null) {
            updateTeamMember(t);
        } else {
            NimUIKit.getTeamProvider().fetchTeamById(teamId, new SimpleCallback<Team>() {
                @Override
                public void onResult(boolean success, Team result, int code) {
                    if (success && result != null) {
                        // 继续加载群成员
                        updateTeamMember(result);
                    } else {
                        //data 加载结束，通知更新
                        adapter.setNewData(items);
                    }
                }
            });
        }
    }

    private void updateTeamMember(Team team) {
        NimUIKit.getTeamProvider().fetchTeamMemberList(teamId, new SimpleCallback<List<TeamMember>>() {
            @Override
            public void onResult(boolean success, List<TeamMember> members, int code) {
                if (success && members != null && !members.isEmpty()) {
                    memberList.addAll(members);
                    // filter self
                    for (TeamMember member : members) {
                        if (member.getAccount().equals(NimUIKit.getAccount())) {
                            members.remove(member);
                            break;
                        }
                    }


                    if (!members.isEmpty()) {
                        items.add(new AitContactItem(ItemType.SIMPLE_LABEL, "群成员"));
                        tempSource.add(new AitContactItem(ItemType.SIMPLE_LABEL, "群成员"));
                        for (TeamMember member : members) {
                            items.add(new AitContactItem(ItemType.TEAM_MEMBER, member));
                            tempSource.add(new AitContactItem(ItemType.TEAM_MEMBER, member));
                        }
                    }
                }
                //data 加载结束，通知更新
                adapter.setNewData(items);
            }
        });
    }
}
