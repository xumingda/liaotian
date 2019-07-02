package com.netease.nim.uikit.business.team.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.netease.nim.uikit.R;
import com.netease.nim.uikit.api.wrapper.NimToolBarOptions;
import com.netease.nim.uikit.business.team.viewholder.ZXingUtils;
import com.netease.nim.uikit.common.ToastHelper;
import com.netease.nim.uikit.common.activity.ToolBarOptions;
import com.netease.nim.uikit.common.activity.UI;
import com.netease.nim.uikit.common.ui.dialog.CustomAlertDialog;
import com.netease.nimlib.sdk.team.constant.TeamFieldEnum;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 群二维码
 * Created by hzxuwen on 2015/4/10.
 */
public class TeamCodeActivity extends UI implements View.OnClickListener {

    private static final String EXTRA_TID = "EXTRA_TID";
    public static final String EXTRA_DATA = "EXTRA_DATA";
    private static final String EXTRA_FIELD = "EXTRA_FIELD";
    protected CustomAlertDialog alertDialog;
    // view
    private ImageView iv_code;

    // data
    private String teamId;
    private TeamFieldEnum filed;
    private String initialValue;
    private Bitmap bitmap;

    /**
     * 修改群某一个属性公用界面
     *
     * @param activity
     * @param teamId
     * @param field
     * @param initialValue
     * @param requestCode
     */
    public static void start(Activity activity, String teamId, TeamFieldEnum field, String initialValue, int requestCode) {
        Intent intent = new Intent();
        intent.setClass(activity, TeamCodeActivity.class);
        intent.putExtra(EXTRA_TID, teamId);
        intent.putExtra(EXTRA_DATA, initialValue);
        intent.putExtra(EXTRA_FIELD, field);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 修改群某一个属性公用界面
     *
     * @param context
     * @param teamId
     * @param field
     * @param initialValue
     */
    public static void start(Context context, String teamId, TeamFieldEnum field, String initialValue) {
        Intent intent = new Intent();
        intent.setClass(context, TeamCodeActivity.class);
        intent.putExtra(EXTRA_TID, teamId);
        intent.putExtra(EXTRA_DATA, initialValue);
        intent.putExtra(EXTRA_FIELD, field);
        context.startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nim_team_code_activity);

        ToolBarOptions options = new NimToolBarOptions();
        setToolBar(R.id.toolbar, options);


        parseIntent();
        findViews();

        TextView toolbarView = findView(R.id.action_bar_right_clickable_textview);
        toolbarView.setText(R.string.save);
        toolbarView.setOnClickListener(this);
        toolbarView.setVisibility(View.GONE);
    }

    private void parseIntent() {
        teamId = getIntent().getStringExtra(EXTRA_TID);
        filed = (TeamFieldEnum) getIntent().getSerializableExtra(EXTRA_FIELD);
        initialValue = getIntent().getStringExtra(EXTRA_DATA);

        initData();
    }

    private void initData() {
        int limit = 0;
        switch (filed) {
            case Name:
                setTitle(R.string.team_settings_name);

                break;
            case Introduce:
                setTitle(R.string.team_code);
                break;
            case Extension:
                setTitle(R.string.team_extension);
                break;
        }


    }

    private void findViews() {
        alertDialog = new CustomAlertDialog(this);
        iv_code = (ImageView) findViewById(R.id.iv_code);
        Log.e("二维码","二维码:"+teamId);
        bitmap = ZXingUtils.createQRImage("2559925565", 400,  400);
        iv_code.setImageBitmap(bitmap);
        iv_code.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                showWatchPictureAction();
                return false;
            }
        });
    }
    // 图片长按
    protected void showWatchPictureAction() {
        if (alertDialog.isShowing()) {
            alertDialog.dismiss();
            return;
        }
        alertDialog.clearData();

        String title;
        title = getString(R.string.save_to_device);
        alertDialog.addItem(title, new CustomAlertDialog.onSeparateItemClickListener() {

            @Override
            public void onClick() {
               int i= savePicture(bitmap);
               if(i==2){
                   ToastHelper.showToastLong(TeamCodeActivity.this, getString(R.string.picture_save_to));
               }else{
                   ToastHelper.showToastLong(TeamCodeActivity.this, getString(R.string.picture_save_fail));
               }

            }
        });
        alertDialog.show();
    }
    // 保存图片
    public int savePicture(Bitmap bmp) {
//        ImageAttachment attachment = (ImageAttachment) message.getAttachment();
//        String path = attachment.getPath();
//        if (TextUtils.isEmpty(path)) {
//            return;
//        }

//        String srcFilename = attachment.getFileName();
//        //默认jpg
//        String extension = TextUtils.isEmpty(attachment.getExtension()) ? "jpg" : attachment.getExtension();
//        srcFilename += ("." + extension);
//
//        String picPath = StorageUtil.getSystemImagePath();
//        String dstPath = picPath + srcFilename;
//        if (AttachmentStore.copy(path, dstPath) != -1) {
//            try {
//                ContentValues values = new ContentValues(2);
//                values.put(MediaStore.Images.Media.MIME_TYPE, C.MimeType.MIME_JPEG);
//                values.put(MediaStore.Images.Media.DATA, dstPath);
//                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
//                ToastHelper.showToastLong(TeamCodeActivity.this, getString(R.string.picture_save_to));
//            } catch (Exception e) {
//                // may be java.lang.UnsupportedOperationException
//                ToastHelper.showToastLong(TeamCodeActivity.this, getString(R.string.picture_save_fail));
//            }
//        } else {
//            ToastHelper.showToastLong(WatchMessagePictureActivity.this, getString(R.string.picture_save_fail));
//        }
        //生成路径
        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        String dirName = "codeImg";
        File appDir = new File(root , dirName);
        if (!appDir.exists()) {
            appDir.mkdirs();
        }

        //文件名为时间
        long timeStamp = System.currentTimeMillis();
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String sd = sdf.format(new Date(timeStamp));
        String fileName = sd + ".jpg";

        //获取文件
        File file = new File(appDir, fileName);
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            //通知系统相册刷新
            TeamCodeActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                    Uri.fromFile(new File(file.getPath()))));
            return 2;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }
    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.action_bar_right_clickable_textview) {
            showKeyboard(false);
        } else {
        }
    }



    private void saved() {
        finish();
    }


    @Override
    public void onBackPressed() {
        showKeyboard(false);
        super.onBackPressed();
    }
}
