package io.qytc.demo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;

import io.qytc.gstsdk.activity.LoginActivity;
import io.qytc.gstsdk.common.ThirdLoginConstant;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private EditText userId_et,username_et;
    private String userId,name;
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preferences=getSharedPreferences("info",MODE_PRIVATE);

        userId= preferences.getString("userId",null);
        name=preferences.getString("name",null);


        userId_et=findViewById(R.id.userId_et);
        userId_et.setText(userId);
        username_et=findViewById(R.id.username_et);
        username_et.setText(name);

        Button button=findViewById(R.id.button);
        button.setOnClickListener(view -> {
            userId=userId_et.getText().toString();
            name=username_et.getText().toString();
            start(userId,name);
        });
//

        start(userId,name);
    }

    private void start(String userId,String name){

        if(userId==null || name==null){
            return;
        }
        if(userId.isEmpty() || name.isEmpty()){
            return;
        }

        SharedPreferences.Editor edit = preferences.edit();
        edit.putString("userId",userId);
        edit.putString("name",name);
        edit.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra(ThirdLoginConstant.ROOMID, 12345);//房间号
        String model = Build.MODEL;
        intent.putExtra(ThirdLoginConstant.USERID, model.equals("Hi3798MV200") ? "stb" : userId);//当前用户ID
        intent.putExtra(ThirdLoginConstant.USERNAME, model.equals("Hi3798MV200") ? "机顶盒" : name);//当前用户ID

        intent.putExtra(ThirdLoginConstant.ANCHOR, "jml");//会议主讲人Id
        intent.putExtra(ThirdLoginConstant.SDKAPPID, 1400222844);//应用所分配ID
        startActivity(intent);
    }
}
