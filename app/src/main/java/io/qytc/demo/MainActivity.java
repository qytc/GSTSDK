package io.qytc.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;

import io.qytc.gstsdk.activity.LoginActivity;
import io.qytc.gstsdk.common.ThirdLoginConstant;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = findViewById(R.id.button);

        button.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), LoginActivity.class);
            intent.putExtra(ThirdLoginConstant.ROOMID, 12345);//房间号
            intent.putExtra(ThirdLoginConstant.USERID, "yc");//当前用户ID
            intent.putExtra(ThirdLoginConstant.ANCHOR, "jml");//会议主讲人Id
            intent.putExtra(ThirdLoginConstant.SDKAPPID, 1400222844);//应用所分配ID
            startActivity(intent);
        });
    }
}
