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
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;

import java.util.ArrayList;
import java.util.List;

import io.qytc.gstsdk.GetUserIDAndUserSig;
import io.qytc.gstsdk.R;
import io.qytc.gstsdk.dialog.UserSelectDialog;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        final EditText etRoomId = findViewById(R.id.et_room_name);
        final EditText etUserId = findViewById(R.id.et_user_name);

        loadUserInfo(etRoomId, etUserId);

        TextView tvEnterRoom = findViewById(R.id.tv_enter);
        tvEnterRoom.setOnClickListener(v -> startJoinRoom());

        // 如果配置有config文件，则从config文件中选择userId
        mUserInfoLoader = new GetUserIDAndUserSig(this);
        final ArrayList<String> userIds = mUserInfoLoader.getUserIdFromConfig();
        if (userIds != null && userIds.size() > 0) {
            UserSelectDialog dialog = new UserSelectDialog(getContext(), mUserInfoLoader.getUserIdFromConfig());
            dialog.setTitle("请选择登录的用户:");
            dialog.setCanceledOnTouchOutside(false);
            dialog.setOnItemClickListener(position -> {
                final EditText etUserId1 = findViewById(R.id.et_user_name);
                etUserId1.setText(userIds.get(position));
                etUserId1.setEnabled(false);
            });
            dialog.show();
        } else {
            showAlertDialog();
        }

        // 申请动态权限
        checkPermission();
    }

    /**
     * Function: 读取用户输入，并创建（或加入）音视频房间
     * <p>
     * 此段示例代码最主要的作用是组装 TRTC SDK 进房所需的 TRTCParams
     * <p>
     * TRTCParams.sdkAppId => 可以在腾讯云实时音视频控制台（https://console.cloud.tencent.com/rav）获取
     * TRTCParams.userId   => 此处即用户输入的用户名，它是一个字符串
     * TRTCParams.roomId   => 此处即用户输入的音视频房间号，比如 125
     * TRTCParams.userSig  => 此处示例代码展示了两种获取 usersig 的方式，一种是从【控制台】获取，一种是从【服务器】获取
     * <p>
     * （1）控制台获取：可以获得几组已经生成好的 userid 和 usersig，他们会被放在一个 json 格式的配置文件中，仅适合调试使用
     * （2）服务器获取：直接在服务器端用我们提供的源代码，根据 userid 实时计算 usersig，这种方式安全可靠，适合线上使用
     * <p>
     * 参考文档：https://cloud.tencent.com/document/product/647/17275
     */
    private void onJoinRoom(final int roomId, final String userId) {
        final Intent intent = new Intent(getContext(), RoomActivity.class);
        intent.putExtra("roomId", roomId);
        intent.putExtra("userId", userId);
        intent.putExtra("AppScene", TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL);
        int sdkAppId = mUserInfoLoader.getSdkAppIdFromConfig();
        if (sdkAppId > 0) {
            //（1） 从控制台获取的 json 文件中，简单获取几组已经提前计算好的 userid 和 usersig
            ArrayList<String> userIdList = mUserInfoLoader.getUserIdFromConfig();
            ArrayList<String> userSigList = mUserInfoLoader.getUserSigFromConfig();
            int position = userIdList.indexOf(userId);
            String userSig = "";
            if (userSigList != null && userSigList.size() > position) {
                userSig = userSigList.get(position);
            }
            intent.putExtra("sdkAppId", sdkAppId);
            intent.putExtra("userSig", userSig);
            startActivity(intent);
        } else {
            //appId 可以在腾讯云实时音视频控制台（https://console.cloud.tencent.com/rav）获取
            sdkAppId = -1;
            if (!TextUtils.isEmpty(mUserId) && mUserId.equalsIgnoreCase(userId) && !TextUtils.isEmpty(mUserSig)) {
                intent.putExtra("sdkAppId", sdkAppId);
                intent.putExtra("userSig", mUserSig);
                saveUserInfo(String.valueOf(roomId), userId, mUserSig);
                startActivity(intent);
            } else {
                //（2） 通过 http 协议向一台服务器获取 userid 对应的 usersig
                final int finalSdkAppId = sdkAppId;
                mUserInfoLoader.getUserSigFromServer(sdkAppId, roomId, userId, "12345678", (userSig, errMsg) -> {
                    if (!TextUtils.isEmpty(userSig)) {
                        intent.putExtra("sdkAppId", finalSdkAppId);
                        intent.putExtra("userSig", userSig);
                        saveUserInfo(String.valueOf(roomId), userId, userSig);
                        startActivity(intent);
                    } else {
                        runOnUiThread(() -> Toast.makeText(getContext(), "从服务器获取userSig失败", Toast.LENGTH_SHORT).show());
                    }
                });
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    private void startJoinRoom() {
        final EditText etRoomId = findViewById(R.id.et_room_name);
        final EditText etUserId = findViewById(R.id.et_user_name);
        int roomId = 123;
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
    //////////////////////////////////    动态权限申请   ////////////////////////////////////////

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
            mUserSig = userSig;
            SharedPreferences shareInfo = this.getSharedPreferences("per_data", 0);
            SharedPreferences.Editor editor = shareInfo.edit();
            editor.putString("userId", userId);
            editor.putString("roomId", roomId);
            editor.putString("userSig", userSig);
            editor.putLong("userTime", System.currentTimeMillis());
            editor.commit();
        } catch (Exception e) {

        }
    }

    private void loadUserInfo(EditText etRoomId, EditText etUserId) {
        try {
            TRTCCloud.getSDKVersion();
            SharedPreferences shareInfo = this.getSharedPreferences("per_data", 0);
            mUserId = shareInfo.getString("userId", "");
            String roomId = shareInfo.getString("roomId", "");
            mUserSig = shareInfo.getString("userSig", "");
            if (TextUtils.isEmpty(roomId)) {
                etRoomId.setText("2999");
            } else {
                etRoomId.setText(roomId);
            }
            if (TextUtils.isEmpty(mUserId)) {
                etUserId.setText(String.valueOf(System.currentTimeMillis() % 1000000));
            } else {
                etUserId.setText(mUserId);
            }
        } catch (Exception e) {

        }
    }

}
