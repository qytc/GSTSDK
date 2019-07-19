package io.qytc.gstsdk.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.qytc.gstsdk.GetUserIDAndUserSig;
import io.qytc.gstsdk.common.ThirdLoginConstant;

public class LoginActivity extends Activity {
    private final static int REQ_PERMISSION_CODE = 0x1000;
    private GetUserIDAndUserSig mUserInfoLoader;
    private String mUserId = "";
    private String mUserSig = "";
    private String mAnchor = "";
    private Integer roomId = -1;
    private Integer sdkAppId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        mUserId = intent.getStringExtra(ThirdLoginConstant.USERID);
        if (TextUtils.isEmpty(mUserId)) {
            Toast.makeText(this, "请传入有效的用户名", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mAnchor = intent.getStringExtra(ThirdLoginConstant.ANCHOR);
        if (TextUtils.isEmpty(mAnchor)) {
            Toast.makeText(this, "请传入有效的主会场", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        roomId = intent.getIntExtra(ThirdLoginConstant.ROOMID, -1);
        if (roomId == -1) {
            Toast.makeText(this, "请传入有效的房间号", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        sdkAppId = intent.getIntExtra(ThirdLoginConstant.SDKAPPID, -1);
        if (sdkAppId == -1) {
            Toast.makeText(this, "请传入有效的sdkAppId", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // 申请动态权限
        checkPermission();
        mUserInfoLoader = new GetUserIDAndUserSig(this);
        onJoinRoom();
    }

    private void onJoinRoom() {
        final Intent intent = new Intent(getContext(), RoomActivity.class);
        intent.putExtra(ThirdLoginConstant.ROOMID, roomId);
        intent.putExtra(ThirdLoginConstant.USERID, mUserId);
        intent.putExtra(ThirdLoginConstant.ANCHOR, mAnchor);
        intent.putExtra(ThirdLoginConstant.SDKAPPID, sdkAppId);

        mUserInfoLoader.getUserSigFromServer(mUserId, String.valueOf(sdkAppId), (userSig, errMsg) -> {
            if (!TextUtils.isEmpty(userSig)) {
                intent.putExtra(ThirdLoginConstant.USERSIGN, userSig);
                startActivity(intent);
            } else {
                runOnUiThread(() -> Toast.makeText(getContext(), "获取签名失败", Toast.LENGTH_SHORT).show());
            }
            finish();
        });
    }

    private Context getContext() {
        return this;
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

}
