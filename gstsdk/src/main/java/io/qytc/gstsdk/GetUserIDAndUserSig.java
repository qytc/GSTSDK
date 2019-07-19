package io.qytc.gstsdk;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import io.qytc.gstsdk.common.HttpHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;


public class GetUserIDAndUserSig {
    private static final String TAG        = GetUserIDAndUserSig.class.getSimpleName();
    private int mSdkAppId;
    private ArrayList<String> mUserIdArray;
    private ArrayList<String> mUserSigArray;
    private Activity activity;
    private HttpHelper httpHelper;
    private final static String SERVER_URL = "http://ums1.whqunyu.com:8888/api/v1/generateUserSig";

    private final static String JSON_ERRORCODE = "code";
    private final static String JSON_ERRORINFO = "msg";
    private final static String JSON_DATA = "data";
    public GetUserIDAndUserSig(Activity activity){
        mSdkAppId = 0;
        mUserIdArray = new ArrayList<>();
        mUserSigArray = new ArrayList<>();
        this.activity=activity;
        loadFromConfig(activity);
    }

    /**
     * 获取config中配置的appid
     */
    public int getSdkAppIdFromConfig() {
        return mSdkAppId;
    }

    public int getSdkAppIdFromXML(){
        try {
            ActivityInfo info=activity.getPackageManager().getActivityInfo(activity.getComponentName(), PackageManager.GET_META_DATA);
            return Integer.parseInt(info.metaData.getString("SdkAppId"));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 获取config中配置的userid列表
     */
    public ArrayList<String> getUserIdFromConfig() {
        return mUserIdArray;
    }

    /**
     * 获取config中配置的usersig列表
     */
    public ArrayList<String> getUserSigFromConfig() {
        return mUserSigArray;
    }

    /**
     * 从本地的测试用配置文件中读取一批userid 和 usersig
     * 配置文件可以通过访问腾讯云TRTC控制台（https://console.cloud.tencent.com/rav）中的【快速上手】页面来获取
     * 配置文件中的 userid 和 usersig 是由腾讯云预先计算生成的，每一组 usersig 的有效期为 180天
     *
     * 该方案仅适合本地跑通demo和功能调试，产品真正上线发布，要使用服务器获取方案，即 getUserSigFromServer
     *
     * 参考文档：https://cloud.tencent.com/document/product/647/17275#GetForDebug
     *
     */
    public void loadFromConfig(Context context) {
        InputStream is = null;
        try {
            is = context.getResources().openRawResource(R.raw.config);
            String jsonData = readTextFromInputStream(is);
            loadJsonData(jsonData);
        } catch (Exception e) {
            mUserIdArray = new ArrayList<>();
            mUserSigArray = new ArrayList<>();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Exception e) {

            }
        }
    }

    public interface IGetUserSigListener {
        void onComplete(String userSig, String errMsg);
    }
    /**
     * 通过 http 请求到客户的业务服务器上获取 userid 和 usersig
     * 这种方式可以将签发 usersig 的计算工作放在您的业务服务器上进行，这样一来，usersig 的签发工作就可以安全可控
     *
     * 但本demo中的 getUserSigFromServer 函数仅作为示例代码，要跑通该逻辑，您需要参考：https://cloud.tencent.com/document/product/647/17275#GetFromServer
     */
    public void getUserSigFromServer(String userId,String appId,IGetUserSigListener listener) {

        OkHttpClient httpClient=new OkHttpClient();
        try {
            JSONObject jsonReq = new JSONObject();
            jsonReq.put("userId", userId);
            jsonReq.put("appId",appId);
            RequestBody body = RequestBody.create(MediaType.parse("application/json"), jsonReq.toString());
            Request req = new Request.Builder()
                    .url(SERVER_URL)
                    .post(body)
                    .build();
            httpClient.newCall(req).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.w(TAG, "loadUserSig->fail: "+e.toString());
                    if (listener != null) {
                        listener.onComplete(null, "http request failed");
                    }
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()){
                        Log.w(TAG, "loadUserSig->fail: "+response.message());
                        if (listener != null) {
                            listener.onComplete(null, response.message());
                        }
                    }else{
                        try {
                            JSONTokener jsonTokener = new JSONTokener(response.body().string());
                            JSONObject msgJson = (JSONObject) jsonTokener.nextValue();
                            int code = msgJson.getInt(JSON_ERRORCODE);
                            if (0 != code){
                                if (listener != null) {
                                    listener.onComplete(null, msgJson.getString(JSON_ERRORINFO));
                                }
                            }else{
                                String userSig = msgJson.getString(JSON_DATA);
                                if (listener != null) {
                                    listener.onComplete(userSig, msgJson.getString(JSON_ERRORINFO));
                                }
                            }
                        }catch (Exception e){
                            Log.i(TAG, "loadUserSig->exception: "+e.toString());
                            if (listener != null) {
                                listener.onComplete(null, e.toString());
                            }
                        }
                    }
                }
            });
        } catch (Exception e){
            if (listener != null) {
                listener.onComplete(null, e.toString());
            }
        }
    }


    /** 读取资源文件 */
    private String readTextFromInputStream(InputStream is) throws Exception{
        InputStreamReader reader = new InputStreamReader(is);
        BufferedReader bufferedReader = new BufferedReader(reader);
        StringBuffer buffer = new StringBuffer("");
        String str;
        while (null != (str = bufferedReader.readLine())){
            buffer.append(str);
            buffer.append("\n");
        }
        return buffer.toString();
    }

    /** 解析JSON配置文件 */
    private void loadJsonData(String jsonData) {
        if (TextUtils.isEmpty(jsonData)) return;
        try {
            JSONTokener jsonTokener = new JSONTokener(jsonData);
            JSONObject msgJson = (JSONObject) jsonTokener.nextValue();
            mSdkAppId = msgJson.getInt("sdkappid");
            JSONArray jsonUsersArr = msgJson.getJSONArray("users");
            if (null != jsonUsersArr) {
                for (int i = 0; i < jsonUsersArr.length(); i++) {
                    JSONObject jsonUser = jsonUsersArr.getJSONObject(i);
                    mUserIdArray.add(jsonUser.getString("userId"));
                    mUserSigArray.add(jsonUser.getString("userToken"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mSdkAppId = -1;
        }
    }
}
