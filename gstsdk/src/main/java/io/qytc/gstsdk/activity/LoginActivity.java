package io.qytc.gstsdk.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.trtc.TRTCCloud;

import java.util.ArrayList;
import java.util.List;

import io.qytc.gstsdk.GetUserIDAndUserSig;
import io.qytc.gstsdk.R;
import io.qytc.gstsdk.common.ThirdLoginConstant;

/**
 * Module:   LoginActivity
 * <p>
 * Function: 该界面可以让用户输入一个【房间号】和一个【用户名】
 * <p>
 * Notice:
 * <p>
 * （1）房间号为数字类型，用户名为字符串类型
 * <p>
 * （2）在真实的使用场景中，房间号大多不是用户手动输入的，而是系统分配的，
 * 比如视频会议中的会议号是会控系统提前预定好的，客服系统中的房间号也是根据客服员工的工号决定的。
 */
public class LoginActivity extends Activity {
    private final static int REQ_PERMISSION_CODE = 0x1000;
    private GetUserIDAndUserSig mUserInfoLoader;
    private String mUserId = "";
    private String mUserSig = "";
    private String mAnchor = "yc0";
    private  int roomId = -1;
    private EditText etRoomId;
    private EditText etUserId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        Intent intent = getIntent();
        mUserId = intent.getStringExtra("userId");
//        mAnchor = intent.getStringExtra("anchor");

        etRoomId = findViewById(R.id.et_room_name);
        etUserId = findViewById(R.id.et_user_name);

        if(!TextUtils.isEmpty(mUserId)){
            etUserId.setText(mUserId);
        }
        loadUserInfo(etRoomId, etUserId);

        TextView tvEnterRoom = findViewById(R.id.tv_enter);
        tvEnterRoom.setOnClickListener(v -> startJoinRoom());

//        final ArrayList<String> userIds = mUserInfoLoader.getUserIdFromConfig();
//        if (userIds != null && userIds.size() > 0) {
//            UserSelectDialog dialog = new UserSelectDialog(getContext(), mUserInfoLoader.getUserIdFromConfig());
//            dialog.setTitle("请选择登录的用户:");
//            dialog.setCanceledOnTouchOutside(false);
//            dialog.setOnItemClickListener(position -> {
//                final EditText etUserId1 = findViewById(R.id.et_user_name);
//                etUserId1.setText(userIds.get(position));
//                etUserId1.setEnabled(false);
//            });
//            dialog.show();
//        } else {
//            showAlertDialog();
//        }

        // 申请动态权限
        checkPermission();
    }

    private void onJoinRoom(final int roomId, final String userId) {
        mUserInfoLoader = new GetUserIDAndUserSig(this);
        Integer sdkAppId = 1400222844;//mUserInfoLoader.getSdkAppIdFromXML();
        final Intent intent = new Intent(getContext(), RoomActivity.class);
        intent.putExtra(ThirdLoginConstant.ROOMID, roomId);
        intent.putExtra(ThirdLoginConstant.USERID, userId);
        intent.putExtra(ThirdLoginConstant.ANCHOR, mAnchor);
        intent.putExtra(ThirdLoginConstant.SDKAPPID, sdkAppId);

        mUserInfoLoader.getUserSigFromServer(userId, (userSig, errMsg) -> {
            if (!TextUtils.isEmpty(userSig)) {
                intent.putExtra(ThirdLoginConstant.USERSIGN, userSig);
                saveUserInfo(String.valueOf(roomId),userId,userSig);
                startActivity(intent);
                finish();
            } else {
                runOnUiThread(() -> Toast.makeText(getContext(), "获取签名失败", Toast.LENGTH_SHORT).show());
            }

        });
    }

    private void startJoinRoom() {
        try {
            roomId = Integer.valueOf(etRoomId.getText().toString());
        } catch (Exception e) {
            Toast.makeText(getContext(), "请输入有效的房间号", Toast.LENGTH_SHORT).show();
            return;
        }
        final String userId = etUserId.getText().toString();
        if (TextUtils.isEmpty(userId)) {
            Toast.makeText(getContext(), "请输入有效的用户名", Toast.LENGTH_SHORT).show();
            return;
        }

        onJoinRoom(roomId, userId);
    }

    private Context getContext() {
        return this;
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("注意")
                .setMessage("读取配置文件失败");
        AlertDialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<String> permissions = new ArrayList<>();
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA)) {
                permissions.add(Manifest.permission.CAMERA);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.RECORD_AUDIO)) {
                permissions.add(Manifest.permission.RECORD_AUDIO);
            }
            if (PackageManager.PERMISSION_GRANTED != ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (permissions.size() != 0) {
                ActivityCompat.requestPermissions(LoginActivity.this,
                        (String[]) permissions.toArray(new String[0]),
                        REQ_PERMISSION_CODE);
                return false;
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQ_PERMISSION_CODE:
                for (int ret : grantResults) {
                    if (PackageManager.PERMISSION_GRANTED != ret) {
                        Toast.makeText(getContext(), "用户没有允许需要的权限，使用可能会受到限制！", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
            default:
                break;
        }
    }

    private void saveUserInfo(String roomId, String userId, String userSig) {
        try {
            mUserId = userId;
//            mUserSig = userSig;
            SharedPreferences shareInfo = this.getSharedPreferences("per_data", 0);
            SharedPreferences.Editor editor = shareInfo.edit();
            editor.putString(ThirdLoginConstant.USERID, userId);
            editor.putString(ThirdLoginConstant.ROOMID, roomId);
//            editor.putString("userSig", userSig);
            editor.apply();
        } catch (Exception e) {

        }
    }

    private void loadUserInfo(EditText etRoomId, EditText etUserId) {
        try {
            TRTCCloud.getSDKVersion();
            SharedPreferences shareInfo = this.getSharedPreferences("per_data", 0);
            mUserId = shareInfo.getString(ThirdLoginConstant.USERID, "");
            String roomId = shareInfo.getString(ThirdLoginConstant.ROOMID, "");
//            mUserSig = shareInfo.getString("userSig", "");
            if (TextUtils.isEmpty(roomId)) {
                etRoomId.setText("2999");
            } else {
                etRoomId.setText(roomId);
            }
            etUserId.setText(mUserId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
